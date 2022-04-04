package listeners;

import logic.RollPoolLogic;
import org.javacord.api.event.interaction.AutocompleteCreateEvent;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.listener.interaction.AutocompleteCreateListener;
import sheets.SheetsHandler;
import util.UtilFunctions;

import java.util.stream.Collectors;

public class PoolAutocompleteListener implements AutocompleteCreateListener {
    @Override
    public void onAutocompleteCreate(AutocompleteCreateEvent event) {
        final SlashCommandInteractionOption option = event.getAutocompleteInteraction().getFocusedOption();
        if (option.getName().equals(RollPoolLogic.POOL_NAME)) {
            SheetsHandler.getSavedPoolChoices(event.getAutocompleteInteraction().getUser())
                    .map(pairs -> pairs.stream()
                            .filter(s -> UtilFunctions.containsIgnoreCase(s.getName(), option.getStringValue().orElse("")))
                            .collect(Collectors.toList()))
                    .onSuccess(choices -> event.getAutocompleteInteraction().respondWithChoices(choices));

        }
    }
}
