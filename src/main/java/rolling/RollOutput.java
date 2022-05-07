package rolling;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.List;

public record RollOutput(List<EmbedBuilder> embeds, boolean plotDiceUsed, int rollTotal, boolean opportunity) {
}
