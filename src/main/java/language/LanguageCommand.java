package language;

import com.google.api.client.util.Maps;
import com.google.common.collect.Lists;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenSlashEvent;
import roles.PlayerHandler;
import sheets.SheetsHandler;
import util.DiscordHelper;
import util.RandomColor;
import util.Tier;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LanguageCommand implements VelenSlashEvent {
    private final LanguageLogic languageLogic;

    public LanguageCommand(LanguageLogic languageLogic) {
        this.languageLogic = languageLogic;
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        args.getOptionWithName("add")
                .map(this::handleAdd)
                .ifPresent(firstResponder::addEmbed);

        args.getOptionWithName("remove")
                .map(this::handleRemove)
                .ifPresent(firstResponder::addEmbed);

        args.getOptionWithName("connect")
                .map(this::handleConnect)
                .ifPresent(firstResponder::addEmbed);

        args.getOptionWithName("query")
                .map(this::handleQuery)
                .ifPresent(firstResponder::addEmbed);

        args.getOptionWithName("calculate")
                .map(option -> handleCalculate(user, event, option))
                .ifPresent(firstResponder::addEmbed);

        firstResponder.respond();
    }

    private EmbedBuilder handleAdd(SlashCommandInteractionOption option) {
        Optional<String> nameOpt = option.getOptionStringValueByName("name");
        if (nameOpt.isPresent()) {
            return languageLogic.add(new Language(
                    nameOpt.get(),
                    option.getOptionStringValueByName("description").orElse(null),
                    option.getOptionBooleanValueByName("constellation").orElse(false),
                    option.getOptionBooleanValueByName("court").orElse(false),
                    option.getOptionBooleanValueByName("family").orElse(false),
                    option.getOptionBooleanValueByName("regional").orElse(false),
                    option.getOptionBooleanValueByName("vulgar").orElse(false)
            )).fold(
                    error -> new EmbedBuilder()
                            .setTitle("Failed to Add New Language")
                            .setDescription("Failed with error: " + error.getMessage()),
                    language -> new EmbedBuilder()
                            .setTitle("New Language Added")
                            .setDescription("Added new Language " + language.getName() + " to graph with no connections")
            );
        }
        else {
            return new EmbedBuilder()
                    .setTitle("Failed to Add New Language")
                    .setDescription("'name' is a required value'");
        }
    }

    private EmbedBuilder handleRemove(SlashCommandInteractionOption option) {
        Optional<String> nameOpt = option.getOptionStringValueByName("name");
        if (nameOpt.isPresent()) {
            return nameOpt.flatMap(languageLogic::getByName)
                    .map(languageLogic::remove)
                    .map(value -> value.fold(
                            error -> new EmbedBuilder()
                                    .setTitle("Failed to Remove Language")
                                    .setDescription("Failed with error: " + error.getMessage()),
                            language -> new EmbedBuilder()
                                    .setTitle("Language Removed")
                                    .setDescription("Removed Language " + language.getName() + " to graph with no connections")
                    ))
                    .orElseGet(() -> new EmbedBuilder()
                            .setTitle("Failed to Remove Language")
                            .setDescription("No Language named '" + nameOpt.get() + "' was found")
                    );
        }
        else {
            return new EmbedBuilder()
                    .setTitle("Failed to Remove Language")
                    .setDescription("'name' is a required value");
        }
    }

    private EmbedBuilder handleConnect(SlashCommandInteractionOption option) {
        Optional<Language> languageU = option.getOptionStringValueByName("language1")
                .flatMap(languageLogic::getByName);

        Optional<Language> languageV = option.getOptionStringValueByName("language2")
                .flatMap(languageLogic::getByName);

        if (!languageU.isPresent()) {
            return new EmbedBuilder()
                    .setTitle("Failed to Connect Languages")
                    .setDescription(option.getOptionStringValueByName("language1").orElse("NONE") + " was not found");
        }
        else if (!languageV.isPresent()) {
            return new EmbedBuilder()
                    .setTitle("Failed to Connect Languages")
                    .setDescription(option.getOptionStringValueByName("language2").orElse("NONE") + " was not found");
        }
        else {
            return languageLogic.connect(languageU.get(), languageV.get())
                    .fold(
                            error -> new EmbedBuilder()
                                    .setTitle("Failed to Connect Languages")
                                    .setDescription("Failed with Error: " + error.getMessage()),
                            connected -> new EmbedBuilder()
                                    .setTitle("Connected Languages")
                                    .setDescription(
                                            connected.getLeft().getName() + " and " +
                                                    connected.getRight().getName() + " connected."
                                    )
                    );
        }
    }

    private EmbedBuilder handleQuery(SlashCommandInteractionOption option) {
        return option.getOptionStringValueByName("name")
                .flatMap(languageLogic::getByName)
                .map(language -> {
                    Collection<Language> connections = languageLogic.getConnections(language);

                    return new EmbedBuilder()
                            .setTitle(language.getName())
                            .setDescription(language.getDescription())
                            .addField("Relatives", connections.stream()
                                    .map(relative -> " - " + relative.getName())
                                    .collect(Collectors.joining("\n"))
                            );
                })
                .orElseGet(() -> new EmbedBuilder()
                        .setTitle("Failed to Query Language")
                        .setDescription("No Language '" + option.getOptionStringValueByName("name").orElse("NONE") + "' exists")
                );
    }

    private EmbedBuilder handleCalculate(User user, InteractionBase event, SlashCommandInteractionOption option) {
        Optional<Language> targetLanguageOpt = option.getOptionStringValueByName("target")
                .flatMap(languageLogic::getByName);

        if (targetLanguageOpt.isPresent()) {
            Map<String, Try<Collection<Language>>> userLanguages = Maps.newHashMap();

            option.getOptionStringValueByName("languages")
                    .map(languages -> Arrays.stream(languages.split(","))
                            .map(languageLogic::getByName)
                            .flatMap(value -> value.map(Stream::of).orElse(Stream.empty()))
                            .collect(Collectors.toSet())
                    ).ifPresent(languages -> userLanguages.put("Entered", Try.success(languages)));

            option.getOptionMentionableValueByName("characters")
                    .map(PlayerHandler::getPlayersFromMentionable)
                    .map(players -> players.stream()
                            .map(userPlayerPair -> {
                                Try<Collection<Language>> languageNames = SheetsHandler.getLanguages(userPlayerPair.getRight())
                                        .map(languages -> languages.stream()
                                                .map(languageLogic::getByName)
                                                .flatMap(value -> value.map(Stream::of).orElse(Stream.empty()))
                                                .collect(Collectors.toSet())
                                        );
                                return Pair.of(DiscordHelper.getUsernameFromInteraction(event, user), languageNames);
                            })
                    ).ifPresent(stream -> stream.forEach(
                            pair -> userLanguages.put(pair.getLeft(), pair.getRight())
                    ));

            if (userLanguages.isEmpty()) {
                PlayerHandler.getPlayersFromMentionable(user)
                        .forEach(userPlayerPair -> userLanguages.put(
                                DiscordHelper.getUsernameFromInteraction(event, user),
                                SheetsHandler.getLanguages(userPlayerPair.getRight())
                                        .map(languages -> languages.stream()
                                                .map(languageLogic::getByName)
                                                .flatMap(value -> value.map(Stream::of).orElse(Stream.empty()))
                                                .collect(Collectors.toCollection(HashSet::new))
                                        )
                        ));
            }

            if (userLanguages.isEmpty()) {
                return new EmbedBuilder()
                        .setTitle("Failed to Calculate Linguistics Checks")
                        .setDescription("Failed to find any input");
            }
            else {
                Map<String, Try<List<Language>>> foundPaths = userLanguages.entrySet()
                        .stream()
                        .map(entry -> Pair.of(
                                entry.getKey(),
                                entry.getValue()
                                        .flatMap(languages -> languageLogic.getPath(targetLanguageOpt.get(), languages))
                        ))
                        .collect(Collectors.toMap(Pair::getKey, Pair::getRight));

                return new EmbedBuilder()
                        .setTitle("Shortest Path to " + targetLanguageOpt.get())
                        .setAuthor(user)
                        .setDescription("The shortest path to " + targetLanguageOpt.get().getName() +
                                " with this party is:\n" + joinPartyLinguisticsPaths(foundPaths)
                        )
                        .addField("Real Time Translation (Wits) / Grasp Idioms (Vision)", getGroupDifficultyString(foundPaths, 1))
                        .addField("Translate Behind Conversation (Reason)", getGroupDifficultyString(foundPaths, 0))
                        .setColor(RandomColor.getRandomColor());
            }
        }
        else {
            return new EmbedBuilder()
                    .setTitle("Failed to Calculate Difficulty")
                    .setDescription("No Target Language found for: " + option.getOptionStringValueByName("target").orElse("NONE"));
        }
    }

    private String joinPartyLinguisticsPaths(Map<String, Try<List<Language>>> partyLanguageMap) {
        return partyLanguageMap.entrySet().stream()
                .map(stringListEntry -> String.format("%s: %s",
                        stringListEntry.getKey(),
                        stringListEntry.getValue()
                                .fold(
                                        Throwable::getMessage,
                                        languages -> languages.stream()
                                                .map(Language::getName)
                                                .collect(Collectors.joining(" → "))
                                )
                ))
                .collect(Collectors.joining("\n"));
    }

    private String getGroupDifficultyString(Map<String, Try<List<Language>>> partyLanguageMap, int modifier) {
        return partyLanguageMap.entrySet().stream()
                .map(stringListEntry -> String.format(
                        "- %s: %s",
                        stringListEntry.getKey(),
                        stringListEntry.getValue()
                                .fold(
                                        Throwable::getMessage,
                                        languages -> Tier.getByIndex(languages.size() + modifier)
                                )
                ))
                .collect(Collectors.joining("\n"));
    }

    public static void setup(Velen velen, LanguageLogic languageLogic) {
        VelenCommand.ofSlash(
                "language",
                "Query, Create, Connect, Remove Facets Languages",
                velen,
                new LanguageCommand(languageLogic),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("add")
                        .setDescription("Add a New Language")
                        .setOptions(Lists.newArrayList(
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
                        )),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("remove")
                        .setDescription("Remove Language")
                        .setOptions(Lists.newArrayList(
                                SlashCommandOption.create(
                                        SlashCommandOptionType.STRING,
                                        "name",
                                        "Name",
                                        true
                                )
                        )),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("query")
                        .setDescription("Displays information on a language, and its immediate relatives.")
                        .setOptions(Lists.newArrayList(
                                SlashCommandOption.create(
                                        SlashCommandOptionType.STRING,
                                        "name",
                                        "Name",
                                        true
                                )
                        )),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("connect")
                        .setDescription("Connect Languages")
                        .setOptions(Lists.newArrayList(
                                SlashCommandOption.create(
                                        SlashCommandOptionType.STRING,
                                        "language1",
                                        "First Language",
                                        true
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.STRING,
                                        "language2",
                                        "Second Language",
                                        true
                                )
                        )),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("calculate")
                        .setDescription("Calculates the difficulty of linguistics checks, will use User as default input")
                        .setOptions(Lists.newArrayList(
                                SlashCommandOption.create(
                                        SlashCommandOptionType.STRING,
                                        "target",
                                        "The Language to Target",
                                        true
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.MENTIONABLE,
                                        "characters",
                                        "The Character or Party to pull languages from as input",
                                        false
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.STRING,
                                        "languages",
                                        "A comma separated list of languages to use as manual input",
                                        false
                                )
                        ))
        ).attach();
    }
}
