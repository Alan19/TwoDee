package language;

import com.google.common.collect.Lists;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenSlashEvent;

import java.util.List;

public class LanguageCommand implements VelenSlashEvent {
    private final LanguageLogic languageLogic;

    public LanguageCommand(LanguageLogic languageLogic) {
        this.languageLogic = languageLogic;
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {

    }

    public static void setup(Velen velen, LanguageLogic languageLogic) {
        VelenCommand.ofSlash(
                "language",
                "Query, Create, Remove Facets Languages",
                velen,
                new LanguageCommand(languageLogic)
        ).addOption(
                SlashCommandOption.createWithOptions(
                        SlashCommandOptionType.SUB_COMMAND,
                        "add",
                        "Add a New Language",
                        Lists.newArrayList(
                                SlashCommandOption.create(
                                        SlashCommandOptionType.STRING,
                                        "name",
                                        "Name",
                                        true
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.STRING,
                                        "description",
                                        "Description"
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.BOOLEAN,
                                        "constellation",
                                        "Constellation"
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.BOOLEAN,
                                        "court",
                                        "Court"
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.BOOLEAN,
                                        "Family",
                                        "Family"
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.BOOLEAN,
                                        "regional",
                                        "Regional"
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.BOOLEAN,
                                        "vulgar",
                                        "Vulgar"
                                )
                        )
                )
        );
    }
}
