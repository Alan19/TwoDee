package dicerolling;

import discord.TwoDee;
import logic.RandomColor;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import statistics.resultvisitors.DifficultyVisitor;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class DiceRoller {
    public static final String NONE = "*none*";
    private final RollResult rollResult;
    private final DicePool dicePool;

    public DiceRoller(DicePool dicePool) {
        this.dicePool = dicePool;
        rollResult = new RollResult(dicePool.getNumberOfKeptDice(), false);
        Random random = new Random();
        rollDice(random, dicePool);
    }

    public int getDoom() {
        return rollResult.getDoom();
    }

    public EmbedBuilder generateResults(MessageAuthor author) {
        //Build embed
        return buildResultEmbed(author, rollResult);
    }

    private EmbedBuilder buildResultEmbed(MessageAuthor author, RollResult result) {
        return new EmbedBuilder()
                .setDescription("Here's the result for" + createDicePoolString(dicePool))
                .setTitle(TwoDee.getRollTitleMessage())
                .setAuthor(author)
                .setColor(RandomColor.getRandomColor())
                .addField("Regular dice", formatResults(result.getDice()), true)
                .addField("Picked", resultsToString(result.getPickedDice()), true)
                .addField("Dropped", resultsToString(result.getDropped()), true)
                .addField("Plot dice", resultsToString(result.getPlotDice()), true)
                .addField("Kept dice", resultsToString(result.getKeptDice()), true)
                .addField("Flat bonuses", resultsToString(result.getFlatBonus()), true)
                .addField("Total", String.valueOf(result.getTotal()), true)
                .addField("Tier Hit", tiersHit(result.getTotal()));
    }

    private String createDicePoolString(DicePool dicePool) {
        StringBuilder dicePoolString = new StringBuilder();
        dicePool.getRegularDice().forEach(integer -> dicePoolString.append(" d").append(integer));
        dicePool.getPlotDice().forEach(integer -> dicePoolString.append(" pd").append(integer));
        dicePool.getKeptDice().forEach(integer -> dicePoolString.append(" kd").append(integer));
        dicePool.getFlatBonuses().stream().map(integer -> (integer > 0) ? ("+" + integer) : integer).forEach(dicePoolString::append);
        return dicePoolString.toString();
    }

    private String tiersHit(int total) {
        DifficultyVisitor difficultyVisitor = new DifficultyVisitor();
        if (total < 3) {
            return "None";
        }
        StringBuilder output = new StringBuilder();
        for (Map.Entry<Integer, String> diffEntry : difficultyVisitor.getDifficultyMap().entrySet()) {
            if (difficultyVisitor.generateStageDifficulty(diffEntry.getKey() + 1) > total) {
                output.append(diffEntry.getValue());
                break;
            }
        }
        if (total < 10) {
            return String.valueOf(output);
        }
        for (Map.Entry<Integer, String> diffEntry : difficultyVisitor.getDifficultyMap().entrySet()) {
            if (difficultyVisitor.generateStageExtraordinaryDifficulty(diffEntry.getKey() + 1) > total) {
                output.append(", Extraordinary ").append(diffEntry.getValue());
                break;
            }
        }
        return String.valueOf(output);
    }

    /**
     * Converts a list of integers to a string with the 1s bolded
     *
     * @param s A list of integers with dice outcomes
     * @return A string of integers separated by commas with the 1s bolded
     */
    private String formatResults(List<Integer> s) {
        StringBuilder resultString = new StringBuilder();
        if (s.size() > 1) {
            for (int i = 0; i < s.size() - 1; i++) {
                if (s.get(i) == 1) {
                    resultString.append("**1**, ");
                }
                else {
                    resultString.append(s.get(i)).append(", ");
                }
            }
            if (s.get(s.size() - 1) == 1) {
                resultString.append("**1**");
            }
            else {
                resultString.append(s.get(s.size() - 1));
            }
        }
        else if (s.size() == 1) {
            if (s.get(0) == 1) {
                resultString.append("**1**");
            }
            else {
                resultString.append(s.get(0));
            }
        }
        else {
            return NONE;
        }
        return resultString.toString();
    }


    //Roll all of the dice. Plot dice have a minimum value of its maximum roll/2
    private void rollDice(Random random, DicePool dicePool) {
        //Roll dice
        rollDie(random, dicePool.getRegularDice());
        rollKeptDie(random, dicePool.getKeptDice());
        rollPlotDice(random, dicePool.getPlotDice());
        addFlatBonus(dicePool.getFlatBonuses());
    }

    private void addFlatBonus(List<Integer> flatBonus) {
        flatBonus.forEach(rollResult::addFlatBonus);
    }

    /**
     * Adds plot dice to the roll result. A plot dice has a minimum value of half its facets
     *
     * @param random   The random number generator that generates the dice value
     * @param plotDice The list of plot dice to roll
     */
    private void rollPlotDice(Random random, List<Integer> plotDice) {
        plotDice.stream()
                .mapToInt(die -> Math.max(random.nextInt(die) + 1, die / 2))
                .forEach(rollResult::addPlotResult);
    }

    private void rollDie(Random random, List<Integer> dice) {
        dice.stream()
                .mapToInt(die -> random.nextInt(die) + 1)
                .forEach(rollResult::addResult);
    }

    private void rollKeptDie(Random random, List<Integer> keptDice) {
        keptDice.stream()
                .mapToInt(keptDie -> random.nextInt(keptDie) + 1)
                .forEach(rollResult::addKeptResult);
    }

    //Replaces brackets in the string. If the string is blank, returns "none" in italics
    private String resultsToString(List<Integer> result) {
        return result.isEmpty() ? NONE : result.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    public int getTotal() {
        return rollResult.getTotal();
    }
}
