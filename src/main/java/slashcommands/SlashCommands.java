package slashcommands;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SlashCommands {
    public static CompletableFuture<ApplicationCommand> registerBleedCommand(DiscordApi api) {
        final Server server = getServer(api);
        final List<ApplicationCommandOption> applicationCommandOptions = new ArrayList<>();
        applicationCommandOptions.add(ApplicationCommandOption.create(ApplicationCommandOptionType.MENTIONABLE, "targets", "The party or player to bleed", true));
        applicationCommandOptions.add(ApplicationCommandOption.create(ApplicationCommandOptionType.INTEGER, "modifier", "The bonus or penalty on the bleed", false));
        applicationCommandOptions.add(ApplicationCommandOption.create(ApplicationCommandOptionType.BOOLEAN, "bleed-all", "If all plot points should be lost instead of only half of above the cap", false));
        final ApplicationCommandBuilder builder = new ApplicationCommandBuilder().setName("bleed").setDescription("Applies plot point bleed!").setOptions(applicationCommandOptions);
        return builder.createForServer(server);
    }

    public static CompletableFuture<ApplicationCommand> registerDoomCommand(DiscordApi api) {
        final List<ApplicationCommandOption> applicationCommandOptions = new ArrayList<>();
        final ApplicationCommandOptionBuilder[] applicationCommandOptionBuilders = {getNameOption(), getCountOption()};
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "add", "Adds to the doom pool", applicationCommandOptionBuilders));
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "sub", "Subtracts from the doom pool", applicationCommandOptionBuilders));
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "set", "Sets from the doom pool to the specified amount", getCountOption().setRequired(true), getNameOption()));
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "select", "Chooses the specified doom pool as the active doom pool", getNameOption().setRequired(true)));
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "query", "Queries the value of all doom pools", getNameOption().setDescription("the name of the doom pool to query")));
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "delete", "Deletes the doom pool from the doom pool tracker", getNameOption().setDescription("the name of the doom pool to delete")));
        final ApplicationCommandBuilder builder = new ApplicationCommandBuilder()
                .setName("doom")
                .setDescription("Accesses the doom pool")
                .addOption(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND_GROUP, "mode", "how to modify the doom pool", applicationCommandOptions));
        return builder.createForServer(getServer(api));
    }

    public static CompletableFuture<ApplicationCommand> registerPlotPointCommand(DiscordApi api) {
        final List<ApplicationCommandOption> applicationCommandOptions = new ArrayList<>();
        final ApplicationCommandOptionBuilder[] applicationCommandOptionBuilders = {getMentionableOption(), getPlotPointCountOption()};
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "add", "Adds to the specified plot point pool(s)", applicationCommandOptionBuilders));
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "sub", "Subtracts from the specified plot point pool(s)", applicationCommandOptionBuilders));
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "set", "Sets the specified plot point pools to the specified amount", getCountOption().setRequired(true), getNameOption()));
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "query", "Queries the value of all plot point pools", getNameOption().setDescription("the name of the plot point pool to query")));
        final ApplicationCommandBuilder builder = new ApplicationCommandBuilder()
                .setName("plotpoints")
                .setDescription("Accesses the plot point pools")
                .addOption(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND_GROUP, "mode", "how to modify the plot point pools", applicationCommandOptions));
        return builder.createForServer(getServer(api));
    }

    private static ApplicationCommandOptionBuilder getNameOption() {
        return new ApplicationCommandOptionBuilder()
                .setType(ApplicationCommandOptionType.STRING)
                .setName("name")
                .setDescription("the name of the doom pool to modify")
                .setRequired(false);
    }

    private static ApplicationCommandOptionBuilder getMentionableOption() {
        return new ApplicationCommandOptionBuilder()
                .setType(ApplicationCommandOptionType.MENTIONABLE)
                .setName("name")
                .setDescription("the user(s) to target with the command")
                .setRequired(false);
    }

    private static ApplicationCommandOptionBuilder getCountOption() {
        return new ApplicationCommandOptionBuilder()
                .setName("count")
                .setDescription("the amount to modify the doom pool by")
                .setType(ApplicationCommandOptionType.INTEGER)
                .setRequired(false);
    }

    private static ApplicationCommandOptionBuilder getPlotPointCountOption() {
        return new ApplicationCommandOptionBuilder()
                .setName("count")
                .setDescription("the amount to modify the plot point pool by")
                .setType(ApplicationCommandOptionType.INTEGER)
                .setRequired(false);
    }

    private static Server getServer(DiscordApi api) {
        return api.getServerById(468046159781429250L).get();
    }

}
