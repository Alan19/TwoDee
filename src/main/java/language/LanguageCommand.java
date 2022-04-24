package language;

import com.google.api.client.util.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenSlashEvent;
import roles.PlayerHandler;
import sheets.SheetsHandler;
import util.DiscordHelper;
import util.Match;
import util.RandomColor;
import util.Tier;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LanguageCommand implements VelenSlashEvent {
    public static final String LANGUAGE_NAME = "language-name";
    public static final String TARGET = "target-language";
    public static final String LANGUAGE_1 = "language1";
    public static final String LANGUAGE_2 = "language2";
    private final LanguageLogic languageLogic;

    public LanguageCommand(LanguageLogic languageLogic) {
        this.languageLogic = languageLogic;
    }

    @Override
    public void onEvent(SlashCommandCreateEvent originalEvent, SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        event.respondLater()
                .thenAccept(updater -> {
                    Consumer<EmbedBuilder> consumer = ((Consumer<EmbedBuilder>) embedBuilder -> {
                        embedBuilder.setAuthor(user);
                        embedBuilder.setColor(RandomColor.getRandomColor());
                    }).andThen(updater::addEmbed);

                    args.getOptionWithName("add")
                            .map(this::handleAdd)
                            .ifPresent(consumer);

                    args.getOptionWithName("remove")
                            .map(this::handleRemove)
                            .ifPresent(consumer);

                    args.getOptionWithName("connect")
                            .map(this::handleConnect)
                            .ifPresent(consumer);

                    args.getOptionWithName("query")
                            .map(this::handleQuery)
                            .ifPresent(consumer);

                    args.getOptionWithName("calculate")
                            .map(option -> handleCalculate(user, event, option))
                            .ifPresent(consumer);

                    args.getOptionWithName("validate")
                            .map(option -> handleValidate(user))
                            .ifPresent(consumer);

                    updater.update();
                });
    }

    public static void setup(Velen velen, LanguageLogic languageLogic) {
        VelenCommand.ofSlash(
                "language",
                "Query, create, connect, and remove Facets Languages.",
                velen,
                new LanguageCommand(languageLogic),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("add")
                        .setDescription("Add a new Language.")
                        .setOptions(Lists.newArrayList(
                                SlashCommandOption.createStringOption(
                                        LANGUAGE_NAME,
                                        "The name of the language to create",
                                        true
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.STRING,
                                        "description",
                                        "The new language's description"
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.BOOLEAN,
                                        "constellation",
                                        "If the new language is spoken by members of another constellation"
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.BOOLEAN,
                                        "court",
                                        "If the new language is a court dialect"
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.BOOLEAN,
                                        "family",
                                        "If the new language is the parent of a language family"
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.BOOLEAN,
                                        "regional",
                                        "If the new language a regional dialect"
                                ),
                                SlashCommandOption.create(
                                        SlashCommandOptionType.BOOLEAN,
                                        "vulgar",
                                        "If the new language is a vulgar dialect"
                                )
                        )),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("remove")
                        .setDescription("Remove a Language.")
                        .setOptions(Lists.newArrayList(
                                SlashCommandOption.createStringOption(
                                        LANGUAGE_NAME,
                                        "The name of the language to be removed",
                                        true,
                                        true
                                )
                        )),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("query")
                        .setDescription("Displays information on a language, and its immediate relatives.")
                        .setOptions(Lists.newArrayList(
                                SlashCommandOption.createStringOption(
                                        LANGUAGE_NAME,
                                        "The name of the Language to query",
                                        true,
                                        true
                                )
                        )),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("connect")
                        .setDescription("Connects two Languages together in the map.")
                        .setOptions(Lists.newArrayList(
                                SlashCommandOption.createStringOption(
                                        LANGUAGE_1,
                                        "One of the Languages to be linked",
                                        true,
                                        true
                                ),
                                SlashCommandOption.createStringOption(
                                        LANGUAGE_2,
                                        "The second Language to be linked",
                                        true,
                                        true
                                )
                        )),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("calculate")
                        .setDescription("Calculates the difficulty of linguistics checks, will use User as default input.")
                        .setOptions(Lists.newArrayList(
                                SlashCommandOption.createStringOption(
                                        TARGET,
                                        "The Language to Target",
                                        true,
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
                                        "A comma separated list of languages to use as manual input for a custom language pool",
                                        false
                                )
                        )),
                new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("validate")
                        .setDescription("Check that a character's languages are considered valid and findable.")
        ).attach();
    }

    private EmbedBuilder handleAdd(SlashCommandInteractionOption option) {
        Optional<String> nameOpt = option.getOptionStringValueByName(LANGUAGE_NAME);
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
        Optional<String> nameOpt = option.getOptionStringValueByName(LANGUAGE_NAME);
        if (nameOpt.isPresent()) {
            return nameOpt.flatMap(languageLogic::getByName)
                    .map(languageLogic::remove)
                    .map(value -> value.fold(
                            error -> new EmbedBuilder()
                                    .setTitle("Failed to Remove Language")
                                    .setDescription("Failed with error: " + error.getMessage()),
                            language -> new EmbedBuilder()
                                    .setTitle("Language Removed")
                                    .setDescription("Removed language " + language.getName() + " from graph")
                    ))
                    .orElseGet(() -> new EmbedBuilder()
                            .setTitle("Failed to Remove Language")
                            .setDescription("No language named '" + nameOpt.get() + "' was found")
                    );
        }
        else {
            return new EmbedBuilder()
                    .setTitle("Failed to Remove Language")
                    .setDescription("'name' is a required value");
        }
    }

    private EmbedBuilder handleConnect(SlashCommandInteractionOption option) {
        Optional<Language> languageU = option.getOptionStringValueByName(LANGUAGE_1)
                .flatMap(languageLogic::getByName);

        Optional<Language> languageV = option.getOptionStringValueByName(LANGUAGE_2)
                .flatMap(languageLogic::getByName);

        if (!languageU.isPresent()) {
            return new EmbedBuilder()
                    .setTitle("Failed to Connect Languages")
                    .setDescription(option.getOptionStringValueByName(LANGUAGE_1).orElse("NONE") + " was not found");
        }
        else if (!languageV.isPresent()) {
            return new EmbedBuilder()
                    .setTitle("Failed to Connect Languages")
                    .setDescription(option.getOptionStringValueByName(LANGUAGE_2).orElse("NONE") + " was not found");
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
        return option.getOptionStringValueByName(LANGUAGE_NAME)
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
                        .setDescription("No Language '" + option.getOptionStringValueByName(LANGUAGE_NAME).orElse("NONE") + "' exists")
                );
    }

    private String joinPartyLinguisticsPaths(Map<String, Try<List<Language>>> partyLanguageMap) {
        return partyLanguageMap.entrySet().stream()
                .map(stringListEntry -> String.format(
                        "%s: %s",
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

    private EmbedBuilder handleCalculate(User user, InteractionBase event, SlashCommandInteractionOption option) {
        Optional<Language> targetLanguageOpt = option.getOptionStringValueByName(TARGET)
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
                                return Pair.of(DiscordHelper.getUsernameFromInteraction(event, userPlayerPair.getKey()), languageNames);
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
                        .setTitle("Shortest Path to " + targetLanguageOpt.get().getName())
                        .setDescription("The shortest path(s) to " + targetLanguageOpt.get().getName() +
                                " with the selected character(s) are:\n" + joinPartyLinguisticsPaths(foundPaths)
                        )
                        .addField("Real Time Translation (Wits) / Grasp Idioms (Vision)", getGroupDifficultyString(foundPaths, 1))
                        .addField("Translate Behind Conversation (Reason)", getGroupDifficultyString(foundPaths, 0));
            }
        }
        else {
            return new EmbedBuilder()
                    .setTitle("Failed to Calculate Difficulty")
                    .setDescription("No Target Language found for: " + option.getOptionStringValueByName(TARGET).orElse("NONE"));
        }
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
                                                .toText()
                                )
                ))
                .collect(Collectors.joining("\n"));
    }

    private EmbedBuilder handleValidate(User user) {
        return PlayerHandler.getPlayerFromUser(user)
                .map(SheetsHandler::getLanguages)
                .map(languagesTry -> languagesTry.fold(
                        error -> new EmbedBuilder()
                                .setTitle("Failed to Validate")
                                .setDescription("Failed to find languages to validate"),
                        languages -> {
                            Multimap<Match, String> matches = Multimaps.index(languages, languageLogic::checkMatch);

                            EmbedBuilder embedBuilder = new EmbedBuilder()
                                    .setTitle("Validations")
                                    .setDescription("Lists of Matches");

                            if (matches.containsKey(Match.EXACT)) {
                                embedBuilder.addField(Match.EXACT.getText(), String.join(", ", matches.get(Match.EXACT)));
                            }
                            if (matches.containsKey(Match.CLOSE)) {
                                embedBuilder.addField(Match.CLOSE.getText(), String.join(", ", matches.get(Match.CLOSE)));
                            }
                            if (matches.containsKey(Match.NONE)) {
                                embedBuilder.addField(Match.NONE.getText(), String.join(", ", matches.get(Match.NONE)));
                            }

                            return embedBuilder;
                        }
                ))
                .orElseGet(() -> new EmbedBuilder()
                        .setTitle("Failed to Validate")
                        .setDescription("Failed to find Player to validate")
                );
    }
}
