package statistics;

import org.javacord.api.entity.message.embed.EmbedBuilder;

public class GenerateOverloadMessage implements StatisticsState {
    @Override
    public void process(StatisticsContext context) {
        context.setEmbedBuilder(
                new EmbedBuilder()
                        .setDescription("That's way too many dice for me to handle. Try using less dice.")
        );
    }
}
