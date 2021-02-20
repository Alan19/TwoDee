package dicerolling;

import discord.TwoDee;
import logic.RandomColor;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class DiceRoller {
    public static final String NONE = "*none*";
    private final RollResult rollResult;
    private final DicePool dicePool;

    public DiceRoller(DicePool dicePool) {
        this.dicePool = dicePool;
        rollResult = new RollResult(dicePool.getNumberOfKeptDice());
        Random random = new Random();
        rollDice(random, dicePool);
    }

    public int getDoom() {
        return rollResult.getDoomGenerated();
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
                .addField("Regular dice", formatResults(result.getRegularDice()), true)
                .addField("Plot dice", formatResults(result.getPlotDice()), true)
                .addField("Kept dice", formatResults(result.getKeptDice()), true)
                .addField("Picked", resultsToString(result.getAllPickedDice()), true)
                .addField("Flat bonuses", resultsToString(result.getFlatBonuses()), true)
                .addField("Total", String.valueOf(result.getTotal()), true)
                .addField("Dropped", resultsToString(result.getAllDroppedDice()), true)
                .addField("Tier Hit", result.getTierHit());
    }

    private String createDicePoolString(DicePool dicePool) {
        StringBuilder dicePoolString = new StringBuilder();
        dicePool.getRegularDice().forEach(integer -> dicePoolString.append(" d").append(integer));
        dicePool.getPlotDice().forEach(integer -> dicePoolString.append(" pd").append(integer));
        dicePool.getKeptDice().forEach(integer -> dicePoolString.append(" kd").append(integer));
        dicePool.getFlatBonuses().stream().map(integer -> (integer > 0) ? ("+" + integer) : integer).forEach(dicePoolString::append);
        return dicePoolString.toString();
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
                .mapToInt(die -> plotDice.size() <= 1 ? Math.max(random.nextInt(die) + 1, die / 2) : random.nextInt(die) + 1)
                .forEach(rollResult::addPlotDice);
    }

    private void rollDie(Random random, List<Integer> dice) {
        dice.stream()
                .mapToInt(die -> random.nextInt(die) + 1)
                .forEach(rollResult::addRegularDice);
    }

    private void rollKeptDie(Random random, List<Integer> keptDice) {
        keptDice.stream()
                .mapToInt(keptDie -> random.nextInt(keptDie) + 1)
                .forEach(rollResult::addKeptDice);
    }

    //Replaces brackets in the string. If the string is blank, returns "none" in italics
    private String resultsToString(List<Integer> result) {
        return result.isEmpty() ? NONE : result.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    public int getTotal() {
        return rollResult.getTotal();
    }
}
