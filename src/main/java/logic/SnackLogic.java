package logic;

import commander.CommandContext;
import commander.CommandSpec;
import commander.CommandSpecBuilder;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import util.OptionalHelper;
import util.RandomColor;

public class SnackLogic {
    public static CommandSpec getSpec() {
        return CommandSpecBuilder.of("snack")
                .withAlias("snac")
                .withDescription("Gives you a snack?")
                .withUsage("~snack")
                .withHandler(SnackLogic::handle)
                .build();
    }

    public static void handle(CommandContext context) {
        OptionalHelper.tuple3(context.getUser(), context.getDisplayName(), context.getChannel())
                .map(tuple -> new MessageBuilder().setEmbed(new EmbedBuilder()
                        .setColor(RandomColor.getRandomColor())
                        .setAuthor(tuple.getLeft())
                        .setTitle("A snack for " + tuple.getMiddle())
                        .setDescription("Here is a cookie!")
                        .setImage("https://upload.wikimedia.org/wikipedia/commons/5/5c/Choc-Chip-Cookie.png"))
                        .send(tuple.getRight())
                );

    }
}
