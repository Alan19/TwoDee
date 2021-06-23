package commander;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.InteractionCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.interaction.InteractionCreateListener;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Commander implements MessageCreateListener, InteractionCreateListener {
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
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        Matcher matcher = commandPattern.matcher(event.getMessageContent());
        if (matcher.matches()) {
            String command = matcher.group("command");
            CommandSpec commandSpec = commandSpecs.get(command);
            if (commandSpec != null) {
                commandSpec.getHandler().accept(new CommandContext());
            }
        }
    }

    @Override
    public void onInteractionCreate(InteractionCreateEvent event) {

    }
}
