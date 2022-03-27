package language;

import com.google.common.collect.Lists;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenSlashEvent;

import java.util.List;
import java.util.Optional;

public class LanguageCommand implements VelenSlashEvent {
    private final LanguageLogic languageLogic;

    public LanguageCommand(LanguageLogic languageLogic) {
        this.languageLogic = languageLogic;
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        args.getOptionWithName("add")
                .map(option -> handleAdd(args))
                .ifPresent(response -> firstResponder.respond()
                        .thenApply(value -> value.addEmbed(response)
                                .update()
                        )
                );
    }

    private EmbedBuilder handleAdd(VelenArguments arguments) {
        Optional<String> nameOpt = arguments.getStringOptionWithName("name");
        if (nameOpt.isPresent()) {
            return languageLogic.add(new Language(
                    nameOpt.get(),
                    arguments.getStringOptionWithName("description").orElse(null),
                    arguments.getBooleanOptionWithName("constellation").orElse(false),
                    arguments.getBooleanOptionWithName("court").orElse(false),
                    arguments.getBooleanOptionWithName("family").orElse(false),
                    arguments.getBooleanOptionWithName("regional").orElse(false),
                    arguments.getBooleanOptionWithName("vulgar").orElse(false)
            )).map(language -> new EmbedBuilder()
                    .setTitle("New Language Added")
                    .setDescription("Added new Language " + language.getName() + " to graph with no connections")
            ).getOrElseGet(error -> new EmbedBuilder()
                    .setTitle("Failed to Add New Language")
                    .setDescription("Failed with error: " + error.getMessage())
            );
        }
        else {
            return new EmbedBuilder()
                    .setTitle("Failed to Add New Language")
                    .setDescription("Failed to find value for Name");
        }
    }

    public static void setup(Velen velen, LanguageLogic languageLogic) {
        VelenCommand.ofSlash(
                "language",
                "Query, Create, Connect, Remove Facets Languages",
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
        ).attach();
    }
}
