package statistics;

import dicerolling.DicePool;
import dicerolling.PoolProcessor;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;

import java.util.List;

/**
 * A class that checks if the amount of dice being used is too much for the bot to generate statistics in a reasonable
 * amount of time
 */
public class ScanDice implements StatisticsState {
    private final MessageAuthor author;
    private final String message;

    public ScanDice(Message message) {
        this.message = message.getContent();
        this.author = message.getAuthor();
    }

    @Override
    public void process(StatisticsContext context) {
        PoolProcessor poolOptions = new PoolProcessor(message, author);
        if (!poolOptions.validPool()) {
            context.setState(new GenerateNoDiceMessage());
        }
        else if (getTotalCombos(poolOptions.getDicePool()) < 0) {
            context.setState(new GenerateOverloadMessage());
        }
        else if (poolOptions.getErrorEmbed() != null) {
            context.setEmbedBuilder(poolOptions.getErrorEmbed());
        }
        else {
            context.setState(new GenerateStatistics(poolOptions.getDicePool()));
        }
    }

    //Get the total number of combinations by finding the product of all of the number of faces in all of the dice
    private long getTotalCombos(DicePool dicePool) {
        List<Integer> regularDice = dicePool.getRegularDice();
        long totalCombos = regularDice.stream().mapToInt(combo -> combo).asLongStream().reduce(1, (a, b) -> a * b);

        /*
            Plot dice have a minimum value of the die size / 2
            This means that you need to divide it by two and add one to get the number of combinations from it if the
            value is greater than 2
         */
        totalCombos *= dicePool.getPlotDice().stream().mapToInt(pdCombo -> pdCombo).mapToLong(pdCombo -> pdCombo > 2 ? pdCombo / 2 + 1 : pdCombo).reduce(1, (a, b) -> a * b);

        //Kept die is treated the same as normal dice
        totalCombos *= dicePool.getKeptDice().stream().mapToInt(combo -> combo).asLongStream().reduce(1, (a, b) -> a * b);
        return totalCombos;
    }
}
