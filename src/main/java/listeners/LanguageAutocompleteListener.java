package listeners;

import com.google.common.collect.ImmutableList;
import language.LanguageCommand;
import language.LanguageLogic;
import org.javacord.api.event.interaction.AutocompleteCreateEvent;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.listener.interaction.AutocompleteCreateListener;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LanguageAutocompleteListener implements AutocompleteCreateListener {
    private static final List<String> PARAMETER_NAMES = ImmutableList.of(
            LanguageCommand.LANGUAGE_NAME,
            "language1",
            "language2",
            LanguageCommand.TARGET
    );

    private final LanguageLogic languageLogic;

    public LanguageAutocompleteListener(LanguageLogic languageLogic) {
        this.languageLogic = languageLogic;
    }

    @Override
    public void onAutocompleteCreate(AutocompleteCreateEvent event) {
        final SlashCommandInteractionOption focusedOption = event.getAutocompleteInteraction().getFocusedOption();
        if (PARAMETER_NAMES.contains(focusedOption.getName())) {
            String optionValue = focusedOption.getStringValue()
                    .map(string -> string.toLowerCase(Locale.ROOT))
                    .orElse("");

            event.getAutocompleteInteraction()
                    .respondWithChoices(
                            languageLogic.findPossibilities(optionValue)
                                    .map(s -> SlashCommandOptionChoice.create(s.getName(), s.getName()))
                                    .collect(Collectors.toList())
                    );
        }
    }

}
