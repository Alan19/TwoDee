package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class HelpCommand implements CommandExecutor {
    private final CommandHandler commandHandler;

    public HelpCommand(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Command(aliases = {"~help", "~commands", "~h"}, description = "Shows this page")
    public void onHelpCommand(DiscordApi api, MessageAuthor author, TextChannel channel) {
        EmbedBuilder embed = new EmbedBuilder();
        for (CommandHandler.SimpleCommand simpleCommand : commandHandler.getCommands()) {
            if (!simpleCommand.getCommandAnnotation().showInHelpPage()) {
                continue; // skip command
            }
            String usage = simpleCommand.getCommandAnnotation().usage();
            if (usage.isEmpty()) { // no usage provided, using the first alias
                usage = simpleCommand.getCommandAnnotation().aliases()[0];
            }
            String description = simpleCommand.getCommandAnnotation().description();
            embed.addField(usage, description);
        }
        api.getUserById(author.getIdAsString()).thenAcceptAsync(user -> user.sendMessage(embed));
        new MessageBuilder()
                .setEmbed(
                        new EmbedBuilder()
                                .setAuthor(author)
                                .setDescription("Sent you a PM")
                )
                .send(channel);
    }
}
