package listeners;

import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.listener.interaction.ButtonClickListener;

public class ComponentInteractionListener implements ButtonClickListener {

    //Listen for a user reacting to the message delete component in a stats embed and if they press it, delete the message.
    @Override
    public void onButtonClick(ButtonClickEvent event) {
        // TODO Fix this when booleans are fixed
        final ButtonInteraction interaction = event.getButtonInteraction();
        if (interaction.getCustomId().equals("delete-stats")) {
            interaction.createImmediateResponder().removeAllEmbeds().setContent("").respond();
        }
    }
}
