package listeners;

import doom.DoomHandler;
import logic.DoomLogic;
import org.javacord.api.event.interaction.AutocompleteCreateEvent;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.listener.interaction.AutocompleteCreateListener;
import util.UtilFunctions;

import java.util.List;
import java.util.stream.Collectors;

public class DoomPoolAutocomplete implements AutocompleteCreateListener {
    @Override
    public void onAutocompleteCreate(AutocompleteCreateEvent event) {
        if (event.getAutocompleteInteraction().getFocusedOption().getName().equals(DoomLogic.POOL_NAME)) {
            final String enteredValue = event.getAutocompleteInteraction().getFocusedOption().getStringValue().orElse("");
            final List<SlashCommandOptionChoice> choices = DoomHandler.getDoomPools().keySet().stream()
                    .filter(poolName -> UtilFunctions.containsIgnoreCase(poolName, enteredValue))
                    .map(s -> SlashCommandOptionChoice.create(s, s))
                    .collect(Collectors.toList());
            event.getAutocompleteInteraction().respondWithChoices(choices);
        }
    }

}
