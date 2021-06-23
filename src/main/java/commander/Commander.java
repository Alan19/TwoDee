package commander;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.event.interaction.InteractionCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.interaction.ApplicationCommandBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.InteractionCreateListener;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.server.ServerJoinListener;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Commander implements MessageCreateListener, InteractionCreateListener, ServerJoinListener {
    private final Pattern commandPattern;
    private final DiscordApi api;
    private final Map<String, CommandSpec> commandSpecs;

    public Commander(DiscordApi api, String prefix, CommandSpec... commandSpecs) {
        this.api = api;
        this.commandPattern = Pattern.compile(prefix + "(?<command>\\w+)");
        this.commandSpecs = new LinkedHashMap<>();
        for (CommandSpec commandSpec : commandSpecs) {
            this.commandSpecs.put(commandSpec.getName(), commandSpec);
            for (String alias : commandSpec.getAlias()) {
                this.commandSpecs.put(alias, commandSpec);
            }
        }
    }

    public void register() {
        this.api.addMessageCreateListener(this);
        this.api.addInteractionCreateListener(this);
        this.api.addServerJoinListener(this);
        this.api.getServers()
                .forEach(server -> CompletableFuture.allOf(this.commandSpecs.values()
                        .parallelStream()
                        .map(value -> new ApplicationCommandBuilder()
                                .setName(value.getName())
                                .setDescription(value.getDescription())
                                .createForServer(server)
                        )
                        .toArray(CompletableFuture[]::new)
                ));

    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        Matcher matcher = commandPattern.matcher(event.getMessageContent());
        if (matcher.matches()) {
            String command = matcher.group("command");
            CommandSpec commandSpec = commandSpecs.get(command);
            if (commandSpec != null) {
                commandSpec.getHandler()
                        .accept(new CommandContext(event.getMessage()));
            }
        }
    }

    @Override
    public void onInteractionCreate(InteractionCreateEvent event) {
        event.getApplicationCommandInteraction()
                .ifPresent(interaction -> {
                    CommandSpec commandSpec = commandSpecs.get(interaction.getCommandName());
                    if (commandSpec != null) {
                        interaction.createImmediateResponder()
                                .setContent("?")
                                .respond();
                        commandSpec.getHandler()
                                .accept(new CommandContext(
                                        null,
                                        event.getInteraction().getUser(),
                                        interaction.getChannel().orElse(null),
                                        interaction.getServer().orElse(null)
                                ));
                    }
                });
    }

    @Override
    public void onServerJoin(ServerJoinEvent event) {
        CompletableFuture.allOf(this.commandSpecs.values()
                .parallelStream()
                .map(value -> new ApplicationCommandBuilder()
                        .setName(value.getName())
                        .setDescription(value.getDescription())
                        .createForServer(event.getServer())
                )
                .toArray(CompletableFuture[]::new)
        );
    }
}
