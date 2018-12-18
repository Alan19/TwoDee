package logic.statisticstates;

import org.javacord.api.entity.message.embed.EmbedBuilder;

public class GenerateNoDiceMessage implements StatisticsState {
    @Override
    public void process(StatisticsContext context) {
        context.setEmbedBuilder(
                new EmbedBuilder()
                        .setDescription("I can't generate statistics when there are no dice!")
        );
    }
}
