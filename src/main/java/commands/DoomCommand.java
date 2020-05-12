package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import doom.DoomWriter;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class DoomCommand implements CommandExecutor {
    @Command(aliases = {"~d", "~doom"}, description = "A command that allows you to manage doom points.", privateMessages = false, usage = "~d [add [number]|sub [number]|set number]")
    public void newDoom(Message message, TextChannel channel) {
        String messageContent = message.getContent();
        if (messageContent.matches("~(d|doom)( (add( [1-9]\\d*)?|sub( [1-9]\\d*)?|set [1-9]\\d*))?")) {
            String[] args = messageContent.split(" ");
            DoomWriter doomWriter = new DoomWriter();
            switch (args.length) {
                case 1:
                    channel.sendMessage(new DoomWriter().generateDoomEmbed());
                    break;
                case 2:
                    switch (args[1]) {
                        case "add":
                            channel.sendMessage(doomWriter.addDoom(1));
                            break;
                        case "sub":
                            channel.sendMessage(doomWriter.addDoom(-1));
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + args[1]);
                    }
                    break;
                case 3:
                    int doomVal = Integer.parseInt(args[2]);
                    switch (args[1]) {
                        case "add":
                            channel.sendMessage(doomWriter.addDoom(doomVal));
                            break;
                        case "sub":
                            channel.sendMessage(doomWriter.addDoom(doomVal * -1));
                            break;
                        case "set":
                            channel.sendMessage(doomWriter.setDoom(doomVal));
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + args[1]);
                    }
                    break;
                default:
                    channel.sendMessage(new EmbedBuilder().setTitle("Invalid doom command!"));
                    break;
            }
        }
        else {
            channel.sendMessage(new EmbedBuilder().setTitle("Invalid doom command!"));
        }
    }
}
