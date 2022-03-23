package logic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import linguistics.Language;
import linguistics.LanguageGraph;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenSlashEvent;
import sheets.SheetsHandler;
import util.RandomColor;
import util.UtilFunctions;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LinguisticsLogic implements VelenSlashEvent {

    public static final String TARGET_LANGUAGE = "target-language";
    public static final String LANGUAGES = "language-pool";

    public static void setupLinguisticsCommand(Velen velen) {
        final List<SlashCommandOption> options = new ArrayList<>();
        SlashCommandOptionBuilder targetLanguage = new SlashCommandOptionBuilder().setName(TARGET_LANGUAGE).setType(SlashCommandOptionType.STRING).setDescription("The target language").setRequired(true).setAutocompletable(true);
        final SlashCommandOption languages = new SlashCommandOptionBuilder().setName(LANGUAGES).setType(SlashCommandOptionType.STRING).setDescription("The languages your character understands, separated by commas").setRequired(true).build();
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "query", "Displays information on a certain language, and its immediate relatives.", targetLanguage));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP,
                "calculate",
                "Calculates the difficulty of linguistics checks",
                new SlashCommandOptionBuilder()
                        .setName("manual")
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setDescription("Calculates the shortest path from your provided list of languages to a target language.")
                        .setOptions(ImmutableList.of(languages, targetLanguage.build())),
                new SlashCommandOptionBuilder()
                        .setName("auto")
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setDescription("Calculates the shortest path to the target language based on the languages on your character sheet.")
                        .setOptions(ImmutableList.of(targetLanguage.build(), SlashCommandOption.create(SlashCommandOptionType.MENTIONABLE, "players", "The player or role to draw the language pool from")))));
        VelenCommand.ofSlash("linguistics", "Various commands to assist with the Linguistics skill", velen, new LinguisticsLogic())
                .addOptions(options.toArray(new SlashCommandOption[0]))
                .attach();
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        // TODO Verify manual list, add a subcommand to query languages from players and list spelling mistakes, add comments, and print error messages
        final Optional<SlashCommandInteractionOption> query = args.getOptionWithName("query");
        query.flatMap(slashCommandInteractionOption -> slashCommandInteractionOption.getOptionStringValueByName(TARGET_LANGUAGE).flatMap(LanguageGraph::lookupLanguage))
                .ifPresent(language -> firstResponder.addEmbed(getAdjacentLanguages(language)).respond());
        final Optional<SlashCommandInteractionOption> calculate = args.getOptionWithName("calculate");
        List<Language> path;
        if (calculate.isPresent()) {
            final Map<Integer, String> difficultyMap = ImmutableMap.of(1, "Easy (3)",
                    2, "Average (7)",
                    3, "Hard (11)",
                    4, "Formidable (15)",
                    5, "Heroic (19)",
                    6, "Incredible (23)",
                    7, "Ridiculous (27)",
                    8, "Impossible (31)");
            final Optional<SlashCommandInteractionOption> manualOption = calculate.get().getOptionByName("manual");
            if (manualOption.isPresent()) {
                final List<Language> languagePool = manualOption.flatMap(slashCommandInteractionOption -> slashCommandInteractionOption.getOptionStringValueByName(LANGUAGES).map(this::getLanguagePool)).orElseThrow(IllegalArgumentException::new);
                final Language targetLanguage = manualOption.flatMap(slashCommandInteractionOption -> slashCommandInteractionOption.getOptionStringValueByName(LANGUAGES).map(s -> getTargetLanguage(slashCommandInteractionOption))).orElseThrow(IllegalArgumentException::new);
                path = LanguageGraph.getShortestPathFromLanguagesKnown(languagePool, targetLanguage);
                final EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("Shortest Path to " + targetLanguage.getName())
                        .setAuthor(user)
                        .setDescription(MessageFormat.format("The shortest path to {0} with your character''s language list is:\n{1}", targetLanguage.getName(), path.stream().map(Language::getName).collect(Collectors.joining(" → "))))
                        .addField("Real Time Translation (Wits) / Grasp Idioms (Vision)", difficultyMap.get(3 + Math.max(0, path.size() - 2)))
                        .addField("Translate Behind Conversation (Reason)", difficultyMap.get(2 + Math.max(0, path.size() - 2)));
                firstResponder.addEmbed(builder).respond();
            }
            calculate.get().getOptionByName("auto").ifPresent(slashCommandInteractionOption -> {
                final Mentionable mentionable = slashCommandInteractionOption.getOptionMentionableValueByName("players").orElse(user);
                final Language targetLanguage = slashCommandInteractionOption.getOptionStringValueByName(TARGET_LANGUAGE).flatMap(LanguageGraph::lookupLanguage).orElseThrow(IllegalArgumentException::new);
                event.respondLater().thenAccept(updater -> {
                    if (mentionable instanceof Role) {
                        Map<String, List<Language>> partyLanguageMap = new HashMap<>();
                        ((Role) mentionable).getUsers()
                                .stream()
                                .map(user1 -> Pair.of(UtilFunctions.getUsernameFromSlashEvent(event, user1), SheetsHandler.getLanguages(user1)))
                                .forEach(pair -> pair.getRight().onSuccess(languages -> partyLanguageMap.put(pair.getLeft(), LanguageGraph.getShortestPathFromLanguagesKnown(getLanguagePool(languages), targetLanguage))));
                        final EmbedBuilder builder = new EmbedBuilder()
                                .setTitle("Shortest Path to " + targetLanguage.getName())
                                .setAuthor(user)
                                .setDescription("The shortest path to " + targetLanguage.getName() + " with this party is:\n" + joinPartyLinguisticsPaths(partyLanguageMap))
                                // TODO Add user name before difficulty
                                .addField("Real Time Translation (Wits) / Grasp Idioms (Vision)", difficultyMap.get(3 + Math.max(0, partyLanguageMap.values().stream().mapToInt(List::size).min().orElseThrow(IllegalStateException::new) - 2)))
                                .addField("Translate Behind Conversation (Reason)", partyLanguageMap.values().stream().map(List::size).map(difficultyMap::get).collect(Collectors.joining("\n")))
                                .setColor(RandomColor.getRandomColor());
                        updater.addEmbed(builder).update();
                    }
                });
            });
        }
    }

    private String joinPartyLinguisticsPaths(Map<String, List<Language>> partyLanguageMap) {
        return partyLanguageMap.entrySet().stream().map(stringListEntry -> stringListEntry.getKey() + ": " + stringListEntry.getValue().stream().map(Language::getName).collect(Collectors.joining(" → "))).collect(Collectors.joining("\n"));
    }

    private Language getTargetLanguage(SlashCommandInteractionOption slashCommandInteractionOption) {
        return slashCommandInteractionOption.getOptionStringValueByName(TARGET_LANGUAGE).flatMap(LanguageGraph::lookupLanguage).orElseThrow(IllegalArgumentException::new);
    }

    private List<Language> getLanguagePool(String s) {
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .map(LanguageGraph::lookupLanguage)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private EmbedBuilder getAdjacentLanguages(Language language) {
        final EmbedBuilder builder = new EmbedBuilder().setTitle(language.getName());
        if (!language.getDescription().isEmpty()) {
            builder.setDescription(language.getDescription());
        }
        final Set<Language> languages = LanguageGraph.getLanguageGraph().adjacentNodes(language);
        if (!languages.isEmpty()) {
            builder.addField("Relatives", languages.stream().map(language1 -> "- " + language1.getName()).collect(Collectors.joining("\n")));
        }
        return builder;
    }
}
