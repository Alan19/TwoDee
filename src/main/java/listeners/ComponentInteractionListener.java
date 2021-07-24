package listeners;

import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

public class ComponentInteractionListener implements MessageComponentCreateListener {

    //Listen for a user reacting to the delete component in a stats embed and if they press it, delete the message.
    @Override
    public void onComponentCreate(MessageComponentCreateEvent event) {
        final MessageComponentInteraction interaction = event.getMessageComponentInteraction();
        if (interaction.getCustomId().equals("delete-stats")) {
            interaction.createOriginalMessageUpdater().removeAllEmbeds().setContent("").update().exceptionally(ExceptionLogger.get());
        }
    }
}
