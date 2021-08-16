package logic;

import doom.DoomHandler;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;
import util.UtilFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoomLogic implements VelenEvent, VelenSlashEvent {
    private static SlashCommandOptionBuilder getNameOption() {
        return new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.STRING)
                .setName("name")
                .setDescription("the name of the doom pool to modify")
                .setRequired(false);
    }

    private static SlashCommandOptionBuilder getCountOption() {
        return new SlashCommandOptionBuilder()
                .setName("count")
                .setDescription("the amount to modify the doom pool by")
                .setType(SlashCommandOptionType.INTEGER)
                .setRequired(false);
    }

    public static void setupDoomCommand(Velen velen) {
        final List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "Adds to the doom pool", getNameOption(), getCountOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "sub", "Subtracts from the doom pool", getNameOption(), getCountOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "set", "Sets from the doom pool to the specified amount", getCountOption().setRequired(true), getNameOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "select", "Chooses the specified doom pool as the active doom pool", getNameOption().setRequired(true)));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "query", "Queries the value of all doom pools", getNameOption().setDescription("the name of the doom pool to query")));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "delete", "Deletes the doom pool from the doom pool tracker", getNameOption().setDescription("the name of the doom pool to delete")));

        DoomLogic doomLogic = new DoomLogic();
        VelenCommand.ofHybrid("doom", "Modifies the doom pool!", velen, doomLogic, doomLogic)
                .addOption(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "mode", "how to modify the doom pool", options))
                .addShortcuts("d")

                .attach();
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] params) {
        if (params.length > 0) {
            String mode;
            String poolName = "";
            int count = 1;
            mode = params[0];
            if (params.length > 1) {
                poolName = params[1];
            }
            if (params.length > 2) {
                count = UtilFunctions.tryParseInt(params[2]).orElse(1);
            }
            event.getChannel().sendMessage(handleCommand(mode, poolName, count).setFooter("Requested by " + UtilFunctions.getUsernameInChannel(user, event.getChannel()), user.getAvatar()));
        }
        else {
            event.getChannel().sendMessage(DoomHandler.generateDoomEmbed().setFooter("Requested by " + UtilFunctions.getUsernameInChannel(user, event.getChannel()), user.getAvatar()));
        }
    }

    private EmbedBuilder handleCommand(String mode, String poolName, int count) {
        switch (mode) {
            case "add":
                return DoomHandler.addDoom(count);
            case "sub":
                return DoomHandler.addDoom(count * -1);
            case "select":
                return DoomHandler.setActivePool(poolName);
            case "set":
                return DoomHandler.setDoom(poolName, count);
            case "delete":
                return DoomHandler.deletePool(poolName);
            default:
                return poolName.equals("") ? DoomHandler.generateDoomEmbed() : DoomHandler.generateDoomEmbed(poolName);
        }
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Optional<SlashCommandInteractionOption> mode = event.getOptionByName("mode").flatMap(SlashCommandInteractionOptionsProvider::getFirstOption);
        final Optional<String> poolName = mode.flatMap(slashCommandInteractionOption -> slashCommandInteractionOption.getOptionStringValueByName("name"));
        final Optional<Integer> count = mode.flatMap(slashCommandInteractionOption -> slashCommandInteractionOption.getOptionIntValueByName("count"));
        if (event.getChannel().isPresent()) {
            final String modeAsString = mode.map(SlashCommandInteractionOption::getName).orElse("query");
            final EmbedBuilder query = handleCommand(modeAsString, poolName.orElseGet(() -> modeAsString.equals("query") ? "" : DoomHandler.getActivePool()), count.orElse(1));
            firstResponder.addEmbed(query.setFooter("Requested by " + UtilFunctions.getUsernameFromSlashEvent(event, user), user.getAvatar())).respond();
        }
        else {
            firstResponder.setContent("Unable to find a channel!").respond();
        }
    }

}
