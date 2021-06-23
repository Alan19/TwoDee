package commander;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.function.Consumer;

public class CommandResponse {
    private final String content;
    private final EmbedBuilder embed;

    public CommandResponse(String content, EmbedBuilder embed) {
        this.content = content;
        this.embed = embed;
    }

    public void handle(Consumer<String> contentConsumer, Consumer<EmbedBuilder> embedConsumer) {
        if (content != null) {
            contentConsumer.accept(content);
        }
        if (embed != null) {
            embedConsumer.accept(embed);
        }
    }

    public static CommandResponse of(String content) {
        return new CommandResponse(content, null);
    }

    public static CommandResponse of(EmbedBuilder embed) {
        return new CommandResponse(null, embed);
    }
}
