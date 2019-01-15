package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import doom.DoomWriter;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class DoomCommand implements CommandExecutor {
    @Command(aliases = {"~d", "~doom"}, description = "A command that allows you to manage doom points.", privateMessages = false, usage = "~t add|sub|set number_of_points")
    //Generates an embed of the new doom value
    public void newDoom(Message message, TextChannel channel) {
        String messageContent = message.getContent();
        String[] args = messageContent.split(" ");
        DoomWriter doomWriter = new DoomWriter();
        if (args.length == 1) {
            new MessageBuilder()
                    .setEmbed(new DoomWriter().generateDoomEmbed())
                    .send(channel);
        }
        if (args.length != 3) {
            new MessageBuilder()
                    .setEmbed(new EmbedBuilder()
                            .setTitle("Invalid command!"))
                    .send(channel);
        } else {
            String commandType = args[1];
            int doomVal = Integer.parseInt(args[2]);

            switch (commandType) {
                case "add":
                    new MessageBuilder()
                            .setEmbed(doomWriter.addDoom(doomVal))
                            .send(channel);
                    break;
                case "sub":
                    new MessageBuilder()
                            .setEmbed(doomWriter.addDoom(doomVal * -1))
                            .send(channel);
                    break;
                case "set":
                    new MessageBuilder()
                            .setEmbed(doomWriter.setDoom(doomVal))
                            .send(channel);
                    break;
                default:
                    new MessageBuilder()
                    .setEmbed(doomWriter.generateDoomEmbed())
                    .send(channel);
                    break;
            }
        }
    }
}
