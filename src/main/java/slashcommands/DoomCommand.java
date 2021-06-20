package slashcommands;

import doom.DoomHandler;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DoomCommand extends ApplicationCommandBuilder {
    public DoomCommand() {
        final List<ApplicationCommandOption> applicationCommandOptions = new ArrayList<>();
        final ApplicationCommandOptionBuilder nameOption = new ApplicationCommandOptionBuilder()
                .setType(ApplicationCommandOptionType.STRING)
                .setName("name")
                .setDescription("the name of the doom pool to modify")
                .setRequired(false);
        final ApplicationCommandOptionBuilder countOption = new ApplicationCommandOptionBuilder()
                .setName("count")
                .setDescription("the amount to modify the doom pool by")
                .setType(ApplicationCommandOptionType.INTEGER)
                .setRequired(false);
        final ApplicationCommandOptionBuilder[] applicationCommandOptionBuilders = {countOption, nameOption};
//        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "add", "Adds to the doom pool", applicationCommandOptionBuilders));
//        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "sub", "Subtracts from the doom pool", applicationCommandOptionBuilders));
//        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "set", "Sets from the doom pool to the specified amount", countOption.setRequired(true), nameOption));
//        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "select", "Chooses the specified doom pool as the active doom pool", nameOption));
        applicationCommandOptions.add(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND, "query", "Queries the value of all doom pools", nameOption.setDescription("the name of the doom pool to query")));
        setName("doom");
        setDescription("Accesses the doom pool");
        addOption(ApplicationCommandOption.createWithOptions(ApplicationCommandOptionType.SUB_COMMAND_GROUP, "mode", "how to modify the doom pool", applicationCommandOptions));
    }

    @Override
    public CompletableFuture<ApplicationCommand> createForServer(Server server) {
        respondToEvent(server.getApi());
        return super.createForServer(server);
    }

    @Override
    public CompletableFuture<ApplicationCommand> createGlobal(DiscordApi api) {
        respondToEvent(api);
        return super.createGlobal(api);
    }

    public void respondToEvent(DiscordApi api) {
        api.addInteractionCreateListener(event -> event.getApplicationCommandInteraction().ifPresent(applicationCommand -> {
            if (applicationCommand.getCommandName().equals("doom")) {
                applicationCommand.getOptionByName("mode").map(ApplicationCommandInteractionOption::getOptions).ifPresent(applicationCommandInteractionOptions -> {
                    final ApplicationCommandInteractionOption modeOption = applicationCommandInteractionOptions.get(0);
                    switch (modeOption.getName()) {
                        case "query":
                            if (modeOption.getOptions().isEmpty()) {
                                applicationCommand.createImmediateResponder().addEmbed(DoomHandler.generateDoomEmbed()).respond();
                            }
                            else {
                                applicationCommand.createImmediateResponder().addEmbed(DoomHandler.generateDoomEmbed(modeOption.getOptionStringValueByName("name").get())).respond();
                            }
                    }
                });
            }
        }));
    }
}
