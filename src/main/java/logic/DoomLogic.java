package logic;

import doom.DoomHandler;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;
import pw.mihou.velen.interfaces.routed.VelenRoutedOptions;
import util.UtilFunctions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoomLogic implements VelenEvent, VelenSlashEvent {

    public static void setupDoomCommand(Velen velen) {
        final List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "Adds to the doom pool", getNameOption(), getCountOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "sub", "Subtracts from the doom pool", getNameOption(), getCountOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "set", "Sets from the doom pool to the specified amount", getCountOption().setRequired(true), getNameOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "select", "Chooses the specified doom pool as the active doom pool", getNameOption().setRequired(true)));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "query", "Queries the value of all doom pools", getNameOption().setDescription("the name of the doom pool to query")));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "delete", "Deletes the doom pool from the doom pool tracker", getNameOption().setDescription("the name of the doom pool to delete")));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "create", "Create new doom pool", getNameOption().setRequired(true), getCountOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "list", "List Doom Pools"));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "info", "Doom Pool Info", getNameOption().setRequired(true)));

        DoomLogic doomLogic = new DoomLogic();
        VelenCommand.ofHybrid("doom", "Modifies the doom pool!", velen, doomLogic, doomLogic)
                .addOptions(options.toArray(new SlashCommandOption[]{}))
                .addShortcuts("d")
                .attach();
    }

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
                .setType(SlashCommandOptionType.LONG)
                .setRequired(false);
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] params, VelenRoutedOptions options) {
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
            new MessageBuilder().addEmbed(handleCommand(mode, poolName, count).setFooter(MessageFormat.format("Requested by {0}", UtilFunctions.getUsernameInChannel(user, event.getChannel())), user.getAvatar())).send(event.getChannel());
        }
        else {
            new MessageBuilder().addEmbed(DoomHandler.generateDoomEmbed().setFooter(MessageFormat.format("Requested by {0}", UtilFunctions.getUsernameInChannel(user, event.getChannel())), user.getAvatar())).send(event.getChannel());
        }
    }

    private EmbedBuilder handleCommand(String mode, String poolName, int count) {
        String actualPoolName = DoomHandler.findPool(poolName);
        if (actualPoolName == null) {
            if (mode == null || mode.isEmpty() || mode.equalsIgnoreCase("list")) {
                return DoomHandler.generateDoomEmbed();
            }
            else {
                return new EmbedBuilder()
                        .setTitle("Error")
                        .setDescription("No Doom Pool with Name ''**" + poolName + "**'' exists.");
            }
        }
        else {
            switch (mode) {
                case "add":
                    return DoomHandler.addDoom(actualPoolName, count);
                case "sub":
                    return DoomHandler.addDoom(actualPoolName, count * -1);
                case "select":
                    return DoomHandler.setActivePool(actualPoolName);
                case "set":
                    return DoomHandler.setDoom(actualPoolName, count);
                case "delete":
                    return DoomHandler.deletePool(actualPoolName);
                case "create":
                    return DoomHandler.createPool(actualPoolName, count);
                case "list":
                    return DoomHandler.generateDoomEmbed();
                case "info":
                    return DoomHandler.generateDoomEmbed(actualPoolName);
                default:
                    return actualPoolName.equals("") ? DoomHandler.generateDoomEmbed() : DoomHandler.generateDoomEmbed(actualPoolName);
            }
        }
    }

    @Override
    public void onEvent(SlashCommandCreateEvent originalEvent, SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final SlashCommandInteractionOption subcommandOption = event.getOptions().get(0);
        final String mode = subcommandOption.getName();
        final Optional<String> poolName = subcommandOption.getOptionStringValueByName("name");
        final Optional<Integer> count = subcommandOption.getOptionLongValueByName("count").map(Math::toIntExact);
        if (event.getChannel().isPresent()) {
            final EmbedBuilder query = handleCommand(mode, poolName.orElseGet(() -> mode.equals("query") ? "" : DoomHandler.getActivePool()), count.orElse(1));
            firstResponder.addEmbed(query.setFooter("Requested by " + UtilFunctions.getUsernameFromSlashEvent(event, user), user.getAvatar())).respond();
        }
        else {
            firstResponder.setContent("Unable to find a channel!").respond();
        }
    }

}
