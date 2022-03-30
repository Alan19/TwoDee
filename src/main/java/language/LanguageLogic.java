package language;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.gson.*;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.guava.MutableGraphAdapter;
import util.DamerauLevenshtein;
import util.GsonHelper;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class LanguageLogic {
    private static final Logger LOGGER = LogManager.getLogger(LanguageLogic.class);
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final MutableGraph<Language> languageGraph;
    private final Map<String, Language> languages;
    private final Consumer<MutableGraph<Language>> onUpdate;
    private final Graph<Language, EndpointPair<Language>> graphAdapter;

    public LanguageLogic(MutableGraph<Language> languageGraph, Map<String, Language> languages, Consumer<MutableGraph<Language>> onUpdate) {
        this.languageGraph = languageGraph;
        this.languages = languages;
        this.onUpdate = onUpdate;
        this.graphAdapter = new MutableGraphAdapter<>(languageGraph);
    }

    public Try<Language> add(@Nonnull Language language) {
        if (languages.containsKey(language.getName())) {
            return Try.failure(new IllegalArgumentException("Language with Name: " + language.getName() + " already exists"));
        }
        else {
            languages.put(language.getName(), language);
            languageGraph.addNode(language);
            onUpdate.accept(languageGraph);
            return Try.success(language);
        }
    }

    public Try<Language> remove(@Nonnull Language language) {
        if (languageGraph.removeNode(language)) {
            languages.remove(language.getName());
            return Try.success(language);
        }
        else {
            return Try.failure(new IllegalArgumentException("Language " + language.getName() + "does not exist in Graph"));
        }
    }

    public Try<Pair<Language, Language>> connect(@Nonnull Language languageU, @Nonnull Language languageV) {
        if (!languages.containsKey(languageU.getName())) {
            return Try.failure(new IllegalArgumentException("Language " + languageU.getName() + "does not exist in Graph"));
        }
        else if (!languages.containsKey(languageV.getName())) {
            return Try.failure(new IllegalArgumentException("Language " + languageV.getName() + "does not exist in Graph"));
        }
        else {
            if (languageGraph.putEdge(languageU, languageV)) {
                return Try.success(Pair.of(languageU, languageV));
            }
            else {
                return Try.failure(new IllegalArgumentException("Languages are already connected"));
            }
        }
    }

    public Optional<Language> getByName(@Nonnull String name) {
        if (languages.containsKey(name)) {
            return Optional.of(languages.get(name));
        }
        else {
            return DamerauLevenshtein.getClosest(name, languages.keySet(), true)
                    .map(languages::get);
        }
    }

    public Collection<Language> getConnections(@Nonnull Language language) {
        if (languageGraph.nodes().contains(language)) {
            return languageGraph.adjacentNodes(language);
        }
        else {
            return Collections.emptyList();
        }
    }

    public Try<List<Language>> getPath(@Nonnull Language target, Collection<Language> startPoints) {
        if (startPoints.contains(target)) {
            return Try.success(Lists.newArrayList(target, target));
        }
        final List<EndpointPair<Language>> collect = new ArrayList<>(startPoints.stream()
                .map(language -> DijkstraShortestPath.findPathBetween(graphAdapter, language, target))
                .min(Comparator.comparingInt(List::size))
                .orElseThrow(IllegalStateException::new));
        List<Language> solution = new ArrayList<>();
        solution.add(collect.get(0).nodeV());
        solution.add(collect.get(0).nodeU());
        for (int i = 1; i < collect.size(); i++) {
            solution.add(collect.get(i).nodeU());
        }
        return Try.success(solution);
    }

    public static LanguageLogic of(MutableGraph<Language> languageGraph) {
        return new LanguageLogic(
                languageGraph,
                languageGraph.nodes()
                        .stream()
                        .collect(Collectors.toMap(Language::getName, Function.identity())),
                graph -> {
                }
        );
    }

    public static LanguageLogic of(MutableGraph<Language> languageGraph, Consumer<MutableGraph<Language>> onUpdate) {
        return new LanguageLogic(
                languageGraph,
                languageGraph.nodes()
                        .stream()
                        .collect(Collectors.toMap(Language::getName, Function.identity())),
                onUpdate
        );
    }

    public static LanguageLogic of(Consumer<MutableGraph<Language>> onUpdate) {
        return new LanguageLogic(
                GraphBuilder.undirected()
                        .build(),
                Maps.newHashMap(),
                onUpdate
        );
    }

    public static Try<LanguageLogic> of(File file) {
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                JsonArray languageArray = jsonObject.getAsJsonArray("languages");
                MutableGraph<Language> graph = GraphBuilder.undirected()
                        .build();

                for (JsonElement languageElement : languageArray) {
                    graph.addNode(Language.fromJson(languageElement.getAsJsonObject()));
                }

                Map<String, Language> languageMap = graph.nodes()
                        .stream()
                        .collect(Collectors.toMap(Language::getName, Function.identity()));

                for (JsonElement connectionElement : jsonObject.getAsJsonArray("connections")) {
                    JsonObject connectionObject = connectionElement.getAsJsonObject();
                    Language from = languageMap.get(GsonHelper.getAsString(connectionObject, "from", null));
                    Language to = languageMap.get(GsonHelper.getAsString(connectionObject, "to", null));

                    if (from == null) {
                        LOGGER.warn("Failed to find Language {}", GsonHelper.getAsString(connectionObject, "from", null));
                    }
                    else if (to == null) {
                        LOGGER.warn("Failed to find Language {}", GsonHelper.getAsString(connectionObject, "to", null));
                    }
                    else {
                        graph.putEdge(from, to);
                    }
                }

                return Try.success(new LanguageLogic(graph, languageMap, saveGraphToFile(file)));
            } catch (IOException e) {
                return Try.failure(e);
            }
        }
        else {
            return Try.failure(new IllegalArgumentException("File doesn't exist"));
        }
    }

    private static Consumer<MutableGraph<Language>> saveGraphToFile(File file) {
        return graph -> {
            JsonObject jsonObject = new JsonObject();
            JsonArray languages = new JsonArray();
            for (Language language : graph.nodes()) {
                languages.add(language.toJson());
            }
            jsonObject.add("languages", languages);

            JsonArray connections = new JsonArray();
            for (EndpointPair<Language> edges : graph.edges()) {
                JsonObject edgeObject = new JsonObject();
                edgeObject.addProperty("from", edges.nodeU().getName());
                edgeObject.addProperty("to", edges.nodeV().getName());
                connections.add(edgeObject);
            }
            jsonObject.add("connections", connections);

            try (FileWriter fileWriter = new FileWriter(file, false)) {
                GSON.toJson(jsonObject, fileWriter);
            } catch (IOException e) {
                LOGGER.error("Failed to Save Language Graph updates", e);
                LOGGER.error(GSON.toJson(jsonObject));
            }
        };
    }
}
