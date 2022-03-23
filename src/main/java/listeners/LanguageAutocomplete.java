package listeners;

import linguistics.LanguageGraph;
import org.javacord.api.event.interaction.AutocompleteCreateEvent;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.listener.interaction.AutocompleteCreateListener;

import java.util.stream.Collectors;

public class LanguageAutocomplete implements AutocompleteCreateListener {
    @Override
    public void onAutocompleteCreate(AutocompleteCreateEvent event) {
        final SlashCommandInteractionOption focusedOption = event.getAutocompleteInteraction().getFocusedOption();
        if (focusedOption.getName().equals("target-language")) {
            event.getAutocompleteInteraction().respondWithChoices(LanguageGraph.getLanguageGraph()
                    .nodes()
                    .stream()
                    .map(language -> SlashCommandOptionChoice.create(language.getName(), language.getName()))
                    .filter(slashCommandOptionChoice -> slashCommandOptionChoice.getName().toLowerCase().contains(focusedOption.getStringValue().orElse("").toLowerCase()))
                    .limit(25)
                    .collect(Collectors.toList()));
        }
    }
}
