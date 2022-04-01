package listeners;

import com.google.common.collect.ImmutableList;
import language.LanguageCommand;
import language.LanguageLogic;
import org.javacord.api.event.interaction.AutocompleteCreateEvent;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.listener.interaction.AutocompleteCreateListener;

import java.util.stream.Collectors;

public class LanguageAutocompleteListener implements AutocompleteCreateListener {
    private final LanguageLogic languageLogic;

    public LanguageAutocompleteListener(LanguageLogic languageLogic) {
        this.languageLogic = languageLogic;
    }

    @Override
    public void onAutocompleteCreate(AutocompleteCreateEvent event) {
        final SlashCommandInteractionOption focusedOption = event.getAutocompleteInteraction().getFocusedOption();
        if (ImmutableList.of(LanguageCommand.LANGUAGE_NAME, "language1", "language2", LanguageCommand.TARGET).contains(focusedOption.getName())) {
            event.getAutocompleteInteraction().respondWithChoices(
                    languageLogic.getLanguages()
                            .stream()
                            .filter(slashCommandOptionChoice -> slashCommandOptionChoice.toLowerCase().contains(focusedOption.getStringValue().orElse("").toLowerCase()))
                            .limit(25)
                            .map(s -> SlashCommandOptionChoice.create(s, s))
                            .collect(Collectors.toList()));
        }
    }

}
