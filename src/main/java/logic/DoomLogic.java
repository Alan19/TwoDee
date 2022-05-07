package logic;

import doom.DoomHandler;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenHybridHandler;
import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;
import pw.mihou.velen.interfaces.hybrid.objects.subcommands.VelenSubcommand;
import pw.mihou.velen.interfaces.hybrid.responder.VelenGeneralResponder;
import util.DiscordHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DoomLogic implements VelenHybridHandler {

    public static final String POOL_NAME = "doom-pool-name";

    public static void setupDoomCommand(Velen velen) {
        final List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "Adds to the doom pool, defaults to the active pool.", getNameOption().setAutocompletable(true), getCountOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "sub", "Subtracts from the doom pool, defaults to the active pool.", getNameOption().setAutocompletable(true), getCountOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "set", "Sets from the doom pool to the specified amount.", getCountOption().setRequired(true), getNameOption().setAutocompletable(true)));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "select", "Selects the specified doom pool as the active doom pool.", getNameOption().setRequired(true).setAutocompletable(true)));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "query", "Queries the value of a doom pool. Defaults to the active pool.", getNameOption().setDescription("The name of the doom pool to query").setAutocompletable(true)));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "delete", "Deletes the doom pool from the doom pool tracker.", getNameOption().setDescription("The name of the doom pool to delete").setAutocompletable(true)));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "create", "Creates a new doom pool. The new pool has 0 doom by default.", getNameOption().setRequired(true), getCountOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "list", "Lists all doom pools and their values."));

        DoomLogic doomLogic = new DoomLogic();
        VelenCommand.ofHybrid("doom", "Modifies the doom pool!", velen, doomLogic)
                .addOptions(options.toArray(new SlashCommandOption[]{}))
                .addFormats(
                        "doom add :[count:of(numeric)] :[doom-pool-name:of(string)]",
                        "doom add :[count:of(numeric)]",
                        "doom sub :[count:of(numeric)] :[doom-pool-name:of(string)]",
                        "doom sub :[count:of(numeric)]",
                        "doom set :[count:of(numeric)] :[doom-pool-name:of(string)]",
                        "doom set :[count:of(numeric)]",
                        "doom select :[doom-pool-name:of(string)]",
                        "doom delete :[doom-pool-name:of(string)]",
                        "doom create :[doom-pool-name:of(string)] :[count:of(numeric)]",
                        "doom create :[doom-pool-name:of(string)]",
                        "doom query :[doom-pool-name:of(string)]",
                        "doom :[mode:of(subcommand)]",
                        "doom list")
                .addShortcuts("d")
                .attach();
    }

    private static SlashCommandOptionBuilder getNameOption() {
        return new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.STRING)
                .setName(POOL_NAME)
                .setDescription("The name of the doom pool to modify")
                .setRequired(false);
    }

    private static SlashCommandOptionBuilder getCountOption() {
        return new SlashCommandOptionBuilder()
                .setName("count")
                .setDescription("The amount to modify the doom pool by")
                .setType(SlashCommandOptionType.LONG)
                .setRequired(false);
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
                case "query":
                default:
                    return actualPoolName.equals("") ? DoomHandler.generateDoomEmbed() : DoomHandler.generateDoomEmbed(actualPoolName);
            }
        }
    }

    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        final Optional<VelenSubcommand> subcommand = DiscordHelper.getSubcommandInHybridCommand(event.isMessageEvent(), args.getOptions());
        final String subcommandName = subcommand.map(VelenSubcommand::getName).orElse("query");
        // Hacky workaround to still restrict input while allowing subcommands with no parameters
        if (Arrays.asList("add", "sub", "select", "set", "delete", "create", "list", "query").contains(subcommandName)) {
            final int count = subcommand.flatMap(velenSubcommand -> velenSubcommand.withName("count"))
                    .flatMap(VelenOption::asInteger)
                    .orElse(1);
            final String name = subcommand.flatMap(velenSubcommand -> velenSubcommand.withName(POOL_NAME))
                    .flatMap(VelenOption::asString)
                    .orElse(DoomHandler.getActivePool());
            responder.addEmbed(handleCommand(subcommandName, name, count)).respond();
        }
    }
}
