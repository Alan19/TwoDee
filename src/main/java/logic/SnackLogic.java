package logic;

import commander.CommandContext;
import commander.CommandResponse;
import commander.CommandSpec;
import commander.CommandSpecBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import util.OptionalHelper;
import util.RandomColor;

import java.util.Optional;

public class SnackLogic {
    public static CommandSpec getSpec() {
        return CommandSpecBuilder.of("snack")
                .withAlias("snac")
                .withDescription("Gives you a snack?")
                .withUsage("~snack")
                .withHandler(SnackLogic::handle)
                .build();
    }

    public static Optional<CommandResponse> handle(CommandContext context) {
        return OptionalHelper.tupled(context.getUser(), context.getDisplayName())
                .map(tuple -> new EmbedBuilder()
                        .setColor(RandomColor.getRandomColor())
                        .setAuthor(tuple.getLeft())
                        .setTitle("A snack for " + tuple.getRight())
                        .setDescription("Here is a cookie!")
                        .setImage("https://upload.wikimedia.org/wikipedia/commons/5/5c/Choc-Chip-Cookie.png")
                )
                .map(CommandResponse::of);
    }
}
