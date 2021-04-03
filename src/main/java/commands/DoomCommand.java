package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import doom.DoomHandler;
import org.codehaus.plexus.util.StringUtils;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class DoomCommand implements CommandExecutor {
    @Command(aliases = {"~d", "~doom"}, description = "A command that allows you to manage doom points.", privateMessages = true, usage = "~d poolname [add [number]|sub [number]|set number]|select|delete]")
    public void doomCommand(Message message, TextChannel channel, String[] params) {
        if (params.length == 0) {
            channel.sendMessage(DoomHandler.generateDoomEmbed());
            return;
        }
        else if (params.length == 1) {
            channel.sendMessage(DoomHandler.generateDoomEmbed(params[0]));
            return;
        }
        String mode = params[1];
        if (mode.equals("select")) {
            channel.sendMessage(DoomHandler.setActivePool(params[0]));
            return;
        }
        else if (mode.equals("delete")) {
            channel.sendMessage(DoomHandler.deletePool(params[0]));
            return;
        }
        final int doomVal = (params.length == 3 && StringUtils.isNumeric(params[2])) ? Integer.parseInt(params[2]) : 1;
        if (mode.equals("add")) {
            channel.sendMessage(DoomHandler.addDoom(params[0], doomVal));
            return;
        }
        else if (mode.equals("sub")) {
            channel.sendMessage(DoomHandler.addDoom(params[0], doomVal * -1));
            return;
        }
        else if (params.length == 3 && mode.equals("set")) {
            channel.sendMessage(DoomHandler.setDoom(params[0], doomVal));
            return;
        }
        channel.sendMessage(new EmbedBuilder().setTitle("Invalid doom command!"));
    }
}
