package listeners;

import org.javacord.api.entity.message.Message;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;

public class ComponentInteractionListener implements MessageComponentCreateListener {

    //Listen for a user reacting to the delete component in a stats embed and if they press it, delete the message.
    @Override
    public void onComponentCreate(MessageComponentCreateEvent event) {
        // TODO Fix this when booleans are fixed
        final MessageComponentInteraction interaction = event.getMessageComponentInteraction();
        if (interaction.getCustomId().equals("delete-stats")) {
            interaction.createImmediateResponder().respond().thenAccept(updater -> interaction.getMessage().ifPresent(Message::delete));
        }
    }
}
