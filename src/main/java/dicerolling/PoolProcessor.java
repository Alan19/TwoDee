package dicerolling;

import commands.EnhancementToggleCommand;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import sheets.SheetsManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PoolProcessor {
    private final String command;
    private final MessageAuthor author;
    private final DicePool dicePool = new DicePool();
    private HashMap<String, Integer> skillMap;
    private EmbedBuilder errorEmbed;

    public PoolProcessor(String command, MessageAuthor author) {
        this.command = command;
        this.author = author;
        preprocess();
        if (dicePool.getRegularDice().isEmpty() && dicePool.getPlotDice().isEmpty() && dicePool.getKeptDice().isEmpty() && dicePool.getFlatBonuses().isEmpty()) {
            errorEmbed = new EmbedBuilder().setAuthor(author).setTitle("Invalid Dice Pool!").setDescription("There's no dice for me to roll!");
        }
    }

    public EmbedBuilder getErrorEmbed() {
        return errorEmbed;
    }

    public DicePool getDicePool() {
        return dicePool;
    }

    private void preprocess() {
        String trimmedCommand = command.replaceAll("\\s+", " ");
        String[] paramArray = trimmedCommand.split(" ");
        int nextDiceFacetMod = 0;
        int maxFacets = 12;
        String nextDiceType = "d";
        dicePool.enableEnhancement(getDefaultEnhancementOption(author));
        for (String param : paramArray) {
            if (param.matches("-fsu=[1-9]\\d*")) {
                nextDiceFacetMod = Integer.parseInt(param.substring(5));
            }
            else if (param.matches("-fsd=[1-9]\\d*")) {
                nextDiceFacetMod = Integer.parseInt(param.substring(5)) * -1;
            }
            else if (param.matches("-maxf=[1-9]\\d*")) {
                maxFacets = Integer.parseInt(param.substring(4));
            }
            else if (param.matches("(-diff=[a-zA-Z]+|-diff)")) {
                processDifficultyLevel(param);
            }
            else if (param.matches("-t=[1-9]\\d*")) {
                dicePool.setNumberOfKeptDice(Integer.parseInt(param.substring(3)));
            }
            else if (param.matches("(-pdisc=[1-9]\\d*|-pdisc)")) {
                processPlotPointDiscount(param);
            }
            else if (param.matches("-enh=(true|false)")) {
                dicePool.enableEnhancement(Boolean.parseBoolean(param.substring(9)));
            }
            else if (param.matches("-opp=(true|false)")) {
                dicePool.setOpportunities(Boolean.parseBoolean(param.substring(5)));
            }
            else if (param.matches("-nd=(d|kd|pd)")) {
                nextDiceType = param.substring(4);
            }
            //Any type of dice
            else if (param.matches("\\d*([kp])?d\\d+")) {
                addDiceToPool(nextDiceFacetMod, param);
                //Reset facet modifier
                nextDiceFacetMod = 0;
            }
            //Flat bonus or penalty
            else if (param.matches("([-+])\\d+")) {
                dicePool.addFlatBonus(Integer.parseInt(param.substring(1)));
            }
            //Minimum dice facets (inclusive)
            else if (param.matches("-minf=[1-9]\\d*")) {
                dicePool.setMinFacets(Integer.parseInt(param.substring(6)));
                dicePool.setRegularDice(dicePool.getRegularDice().stream().filter(integer -> integer >= Integer.parseInt(param.substring(6))).collect(Collectors.toList()));
            }
            //Skill
            else if (param.chars().allMatch(Character::isLetter)) {
                final boolean foundSKill = addSkillFromSpreadsheetToPool(nextDiceFacetMod, maxFacets, nextDiceType, param);
                if (!foundSKill) {
                    errorEmbed = new EmbedBuilder()
                            .setAuthor(author)
                            .setTitle("Skill not found!")
                            .setDescription("Cannot find: " + param);
                    return;
                }
                //Reset default dice settings
                nextDiceFacetMod = 0;
                maxFacets = 12;
                nextDiceType = "d";
            }

        }
    }

    /**
     * Checks whether a player should be allowed to enhance their roll or not
     *
     * @param author The player who is rolling
     * @return true if the player is on the override list and enhancements are disabled or if the player is not on the override list and enhancements are enabled, false if enhancement is disabled and the player is not on the override list or if enhancements are enabled and the player is on the override list
     */
    private boolean getDefaultEnhancementOption(MessageAuthor author) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("resources/bot.properties"));
            final String overrideList = prop.getProperty(EnhancementToggleCommand.ENHANCEMENT_OVERRIDE);
            final String defaultOption = prop.getProperty(EnhancementToggleCommand.ENHANCEMENT);
            final ArrayList<User> overrideUsers = Arrays.stream(overrideList.split(","))
                    .map(s -> author.getApi().getCachedUserById(s))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toCollection(ArrayList::new));

            //Gamemasters can always enhance
            Optional<User> gameMasterOptional = author.getApi().getCachedUserById(prop.getProperty("gameMaster"));
            if (gameMasterOptional.isPresent() && author.asUser().isPresent() && gameMasterOptional.get().equals(author.asUser().get())) {
                return true;
            }
            return (author.asUser().isPresent() && overrideUsers.contains(author.asUser().get())) != defaultOption.equals(EnhancementToggleCommand.ON);
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    private void processDifficultyLevel(String param) {
        dicePool.setDifficulty(param.startsWith("-diff=") ? param.substring(6) : "default");
    }

    private void processPlotPointDiscount(String param) {
        dicePool.setPlotPointDiscount(param.startsWith("-pdisc=") ? Integer.parseInt(param.substring(7)) : Integer.MAX_VALUE);
    }

    /**
     * Add a collection of dice to a pool. e.g. 3d12, d4, pd6, kd9
     *
     * @param nextDiceFacetMod The number of facets to modify the dice by
     * @param param            The parameter with a valid dice
     */
    private void addDiceToPool(int nextDiceFacetMod, String param) {
        StringBuilder numberOfDice = new StringBuilder();

        //Find number of dice being rolled
        int i = 0;
        while (Character.isDigit(param.charAt(i))) {
            numberOfDice.append(param.charAt(i));
            i++;
        }

        int numberOfDiceInt = numberOfDice.toString().equals("") ? 1 : Integer.parseInt(numberOfDice.toString());
        StringBuilder diceType = new StringBuilder();
        while (Character.isAlphabetic(param.charAt(i))) {
            diceType.append(param.charAt(i));
            i++;
        }

        int diceFacets = Integer.parseInt(param.substring(i));

        int totalFacets = numberOfDiceInt * diceFacets + nextDiceFacetMod;
        //Case for d1-d4
        if (diceFacets > 0 && diceFacets < 4 && nextDiceFacetMod == 0) {
            IntStream.range(0, numberOfDiceInt).forEach(j -> dicePool.addDice(diceType.toString(), diceFacets));
        }
        //Case for multiple dice
        else if (totalFacets > diceFacets) {
            for (int j = 0; j < totalFacets / diceFacets; j++) {
                dicePool.addDice(diceType.toString(), diceFacets);
            }
            if (totalFacets % diceFacets > 2) {
                dicePool.addDice(diceType.toString(), totalFacets % diceFacets);
            }
        }
        //Normal case
        else if (totalFacets > 2) {
            dicePool.addDice(diceType.toString(), totalFacets);
        }
    }

    /**
     * Adds a skill from a character spreadsheet to the dice pool
     *
     * @param nextDiceFacetMod The amount of facets to modify the next dice by
     * @param maxFacets        The max facets you are rolling for the dice
     * @param nextDiceType     The type of dice (normal, plot, kept) to add to the pool
     * @param param            The name of the skill to fetch
     * @return true if the skill is found, false if not
     */
    private boolean addSkillFromSpreadsheetToPool(int nextDiceFacetMod, int maxFacets, String nextDiceType, String param) {
        int skillFacets = retrieveSkill(param, author.getIdAsString());
        if (skillFacets == -1) {
            return false;
        }
        skillFacets += nextDiceFacetMod;
        if (skillFacets > 0) {
            if (skillFacets > maxFacets) {
                for (int i = 0; i < skillFacets / maxFacets; i++) {
                    dicePool.addDice(nextDiceType, maxFacets);
                }
                if (skillFacets % maxFacets > 2) {
                    dicePool.addDice(nextDiceType, skillFacets % maxFacets);
                }
            }
            else {
                dicePool.addDice(nextDiceType, skillFacets);
            }
        }
        return true;
    }

    /**
     * Retrieves the facet value of the specified skill in a spreadsheet
     *
     * @param skill The skill to search
     * @param id    The id of the message sender as a string
     * @return The facet value of the string, or -1 if the skill was not found
     */
    private int retrieveSkill(String skill, String id) {
        try {
            //Only pull info the first time
            if (skillMap == null) {
                SheetsManager characterInfo = new SheetsManager(id);
                List<List<Object>> values = characterInfo.getResult().getValues();
                final Object[] skillArray = values.stream().filter(objects -> objects.size() == 2 && ((String) objects.get(1)).matches("\\d+")).toArray();
                skillMap = new HashMap<>();
                for (Object o : skillArray) {
                    List<String> pair = (List<String>) o;
                    skillMap.put(pair.get(0).replace(" ", "").toLowerCase(), Integer.parseInt(pair.get(1)));
                }
            }
            return skillMap.getOrDefault(skill, -1);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean validPool() {
        return !dicePool.getRegularDice().isEmpty() || !dicePool.getPlotDice().isEmpty() || !dicePool.getKeptDice().isEmpty();
    }

}
