package linguistics;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import org.jgrapht.Graph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.guava.ImmutableGraphAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
public class LanguageGraph {
    public static final Language ABYSSAL = new Language("Abyssal").setFamily().setDescription("Abyssal languages presume that you are in floating in water");
    public static final Language CETACEAN = new Language("Cetacean");
    public static final Language ATLANTEAN = new Language("Atlantean").setFamily();
    public static final Language JANNORI = new Language("Jannori");
    public static final Language PUNCTUA = new Language("Punctua").setDescription("A rhythmic / musical variation");
    public static final Language MERISH = new Language("Merish");
    public static final Language OLYMPIAN = new Language("Olympian");
    public static final Language ASPHODEL = new Language("Asphodel");
    public static final Language CHTHONIC = new Language("Chthonic");
    public static final Language GREEK = new Language("Greek").setDescription("Common language of Arkipol");
    public static final Language ROMANTIC_ENGLISH = new Language("Romantic English");
    public static final Language MOUNTAIN_OF_PERFECTION = new Language("Mountain of Perfection").setConstellation().setDescription("Always spoken by residents of the Mountain of Ideals");
    public static final Language MACEDONIAN = new Language("Macedonian");
    public static final Language NGUVU = new Language("Nguvu");
    public static final Language BALISLAV = new Language("Balislav").setFamily();
    public static final Language POLISH = new Language("Polish");
    public static final Language RAUSKI = new Language("Rauski").setDescription("Commonly spoken by members of Rauski cultures");
    public static final Language CELESTIAL = new Language("Celestial").setFamily();
    public static final Language DURAL = new Language("Dural");
    public static final Language COURT_CHAOS = new Language("Court Chaos").setConstellation().setDescription("Often spoken by Courtiers of Chaos");
    public static final Language INFERNAL = new Language("Infernal");
    public static final Language COSMA = new Language("Cosma").setFamily();
    public static final Language METALLIC = new Language("Metallic");
    public static final Language FEDERATION = new Language("Federation").setDescription("Common language of Tetropol");
    public static final Language COIMPERIAL = new Language("Coimperial");
    public static final Language IMPERIAL_ENGLISH = new Language("Imperial English");
    public static final Language VOIDTALK = new Language("Voidtalk");
    public static final Language DRACONIC = new Language("Draconic").setDescription("The nonverbal part of these languages requires wings or similar limbs, a long neck and tail.").setFamily();
    public static final Language CHROMATIC = new Language("Chromatic");
    public static final Language SPECTRACANT = new Language("Spectracant").setDescription("Official language of the Lumina Republics");
    public static final Language DOV = new Language("Dov");
    public static final Language KRILLU = new Language("Krillu");
    public static final Language ENGLISH = new Language("English").setFamily();
    public static final Language COURT_SINTIC = new Language("Court Sintic").setCourt();
    public static final Language STARWORD = new Language("Starword").setDescription("Commonly spoken by members of the Quizerog");
    public static final Language VULGAR_ENGLISH = new Language("Vulgar English").setVulgar();
    public static final Language ESSENI = new Language("Esseni").setFamily();
    public static final Language ELVISH = new Language("Elvish").setFamily();
    public static final Language ELDRISH = new Language("Eldrish").setDescription("Dominant language of the Harmony Fellowship");
    public static final Language NEPTUNIAN = new Language("Neptunian");
    public static final Language SYLVAN = new Language("Sylvan");
    public static final Language RIKKI = new Language("Rikki").setFamily();
    public static final Language TELLERAN = new Language("Telleran").setConstellation().setDescription("Often spoken by Arcadians");
    public static final Language ELLELAN = new Language("Ellelan").setDescription("Commonly spoken by the city of Jalipol");
    public static final Language FLAMMENTISH = new Language("Flammentish").setFamily().setDescription("A visual language family, limited to species who can produce light naturally and flexibly. Common among Lumina.");
    public static final Language LAMBENTISH = new Language("Lambentish").setDescription("Official language of the Radhi Consolidation");
    public static final Language HINDI = new Language("Hindi").setFamily();
    public static final Language FORMAL_HINDI = new Language("Formal Hindi");
    public static final Language DAEVIC = new Language("Daevic");
    public static final Language REGIONAL_HINDI = new Language("Regional Hindi").setRegional();
    public static final Language NAGA = new Language("Naga");
    public static final Language IROKO = new Language("Iroko").setFamily();
    public static final Language WORLD_TREE = new Language("World Tree");
    public static final Language BORELLIAN = new Language("Borellian");
    public static final Language GUAN = new Language("Guan");
    public static final Language CENSERVITI = new Language("Censerviti").setDescription("Dominant language of the Censerviti");
    public static final Language SINTIC = new Language("Sintic").setFamily();
    public static final Language MAYAN = new Language("Mayan");
    public static final Language YGGIC = new Language("Yggic").setDescription("Common language for Shinopol");
    public static final Language KISWAHILI = new Language("Kiswahili").setFamily().setDescription("Common language for Lounopol");
    public static final Language SWAHILI = new Language("Swahili");
    public static final Language REGIONAL_KISWAHILI = new Language("Regional Kiswahili");
    public static final Language APHROS = new Language("Aphros");
    public static final Language NORI = new Language("Nori");
    public static final Language REGIONAL_SINTIC = new Language("Regional Sintic");
    public static final Language SABRANISH = new Language("Sabranish").setFamily().setDescription("Dominant language of its namesake");
    public static final Language COURT_SABRANISH = new Language("Court Sabranish").setCourt();
    public static final Language SRILLIAN = new Language("Srillian");
    public static final Language REGIONAL_SABRANISH = new Language("Regional Sabranish");
    public static final Language SALAMANDI = new Language("Salamandi").setFamily();
    public static final Language TOKALAKI = new Language("Tokalaki").setFamily().setDescription("Dominant language of its namesake");
    public static final Language ROOTHI = new Language("Roothi");
    public static final Language PEATHI = new Language("Peathi");
    public static final Language PUMMTHI = new Language("Pummthi");
    public static final Language NOHHIXHIN = new Language("Nohhixhin");
    public static final Language MASUI = new Language("Masui").setDescription("Commonly spoken by members of Harmonia cultures");

    private static LanguageGraph instance = new LanguageGraph();
    private static final Graph<Language, EndpointPair<Language>> graphAdapter = new ImmutableGraphAdapter<>(getLanguageGraph());
    private final ImmutableGraph<Language> languages = GraphBuilder.undirected()
            .<Language>immutable()
            .putEdge(ABYSSAL, CETACEAN)
            .putEdge(ABYSSAL, CETACEAN)
            .putEdge(ABYSSAL, JANNORI)
            .putEdge(ABYSSAL, MERISH)
            .putEdge(ABYSSAL, NEPTUNIAN)
            .putEdge(CETACEAN, ATLANTEAN)
            .putEdge(JANNORI, PUNCTUA)
            .putEdge(MERISH, NAGA)
            .putEdge(NEPTUNIAN, OLYMPIAN)
            .putEdge(ATLANTEAN, ASPHODEL)
            .putEdge(ATLANTEAN, GREEK)
            .putEdge(ATLANTEAN, OLYMPIAN)
            .putEdge(ATLANTEAN, MACEDONIAN)
            .putEdge(ASPHODEL, CHTHONIC)
            .putEdge(GREEK, ROMANTIC_ENGLISH)
            .putEdge(OLYMPIAN, MOUNTAIN_OF_PERFECTION)
            .putEdge(MACEDONIAN, NGUVU)
            .putEdge(BALISLAV, POLISH)
            .putEdge(BALISLAV, RAUSKI)
            .putEdge(CELESTIAL, CHTHONIC)
            .putEdge(CELESTIAL, COURT_CHAOS)
            .putEdge(CELESTIAL, MOUNTAIN_OF_PERFECTION)
            .putEdge(CELESTIAL, INFERNAL)
            .putEdge(CHTHONIC, DURAL)
            .putEdge(COSMA, DURAL)
            .putEdge(COSMA, FEDERATION)
            .putEdge(COSMA, COIMPERIAL)
            .putEdge(COSMA, VOIDTALK)
            .putEdge(DURAL, METALLIC)
            .putEdge(FEDERATION, POLISH)
            .putEdge(COIMPERIAL, IMPERIAL_ENGLISH)
            .putEdge(VOIDTALK, PUNCTUA)
            .putEdge(DRACONIC, CHROMATIC)
            .putEdge(DRACONIC, DOV)
            .putEdge(DRACONIC, METALLIC)
            .putEdge(CHROMATIC, SPECTRACANT)
            .putEdge(METALLIC, KRILLU)
            .putEdge(ENGLISH, IMPERIAL_ENGLISH)
            .putEdge(ENGLISH, ROMANTIC_ENGLISH)
            .putEdge(ENGLISH, STARWORD)
            .putEdge(ENGLISH, VULGAR_ENGLISH)
            .putEdge(IMPERIAL_ENGLISH, COURT_SINTIC)
            .putEdge(STARWORD, VOIDTALK)
            .putEdge(VULGAR_ENGLISH, RAUSKI)
            .putEdge(ESSENI, KRILLU)
            .putEdge(ESSENI, NOHHIXHIN)
            .putEdge(ESSENI, MASUI)
            .putEdge(NOHHIXHIN, METALLIC)
            .putEdge(MASUI, ELDRISH)
            .putEdge(ELVISH, ELDRISH)
            .putEdge(ELVISH, SYLVAN)
            .putEdge(ELVISH, TELLERAN)
            .putEdge(ELVISH, ELLELAN)
            .putEdge(ELDRISH, NEPTUNIAN)
            .putEdge(SYLVAN, RIKKI)
            .putEdge(ELLELAN, POLISH)
            .putEdge(FLAMMENTISH, LAMBENTISH)
            .putEdge(FLAMMENTISH, PUNCTUA)
            .putEdge(FLAMMENTISH, SPECTRACANT)
            .putEdge(HINDI, DAEVIC)
            .putEdge(HINDI, FORMAL_HINDI)
            .putEdge(HINDI, REGIONAL_HINDI)
            .putEdge(HINDI, NAGA)
            .putEdge(DAEVIC, INFERNAL)
            .putEdge(IROKO, WORLD_TREE)
            .putEdge(IROKO, BORELLIAN)
            .putEdge(IROKO, CENSERVITI)
            .putEdge(IROKO, MAYAN)
            .putEdge(IROKO, YGGIC)
            .putEdge(BORELLIAN, GUAN)
            .putEdge(CENSERVITI, SINTIC)
            .putEdge(YGGIC, SYLVAN)
            .putEdge(KISWAHILI, NGUVU)
            .putEdge(KISWAHILI, SWAHILI)
            .putEdge(KISWAHILI, REGIONAL_KISWAHILI)
            .putEdge(NGUVU, MACEDONIAN)
            .putEdge(SWAHILI, YGGIC)
            .putEdge(REGIONAL_KISWAHILI, NAGA)
            .putEdge(RIKKI, APHROS)
            .putEdge(RIKKI, NORI)
            .putEdge(APHROS, MERISH)
            .putEdge(NORI, JANNORI)
            .putEdge(SINTIC, COURT_SINTIC)
            .putEdge(SINTIC, REGIONAL_SINTIC)
            .putEdge(SINTIC, GUAN)
            .putEdge(GUAN, COSMA)
            .putEdge(SABRANISH, COURT_SABRANISH)
            .putEdge(SABRANISH, SRILLIAN)
            .putEdge(SABRANISH, REGIONAL_SABRANISH)
            .putEdge(SRILLIAN, BORELLIAN)
            .putEdge(SALAMANDI, ENGLISH)
            .putEdge(SALAMANDI, SABRANISH)
            .putEdge(SALAMANDI, TOKALAKI)
            .putEdge(TOKALAKI, ROOTHI)
            .putEdge(TOKALAKI, PEATHI)
            .putEdge(TOKALAKI, PUMMTHI)
            .putEdge(ROOTHI, SYLVAN)
            .putEdge(PEATHI, DURAL)
            .putEdge(PUMMTHI, BALISLAV)
            .build();

    private LanguageGraph() {
    }

    public static ImmutableGraph<Language> getLanguageGraph() {
        return instance.languages;
    }

    public static Optional<Language> lookupLanguage(String target) {
        return instance.languages.nodes().stream().filter(language -> language.getName().equalsIgnoreCase(target)).findFirst();
    }

    public static Graph<Language, EndpointPair<Language>> getGraphAdapter() {
        return graphAdapter;
    }

    public static List<Language> getShortestPathFromLanguagesKnown(List<Language> knownLanguages, Language destination) {
        if (knownLanguages.contains(destination)) {
            return ImmutableList.of(destination, destination);
        }
        final List<EndpointPair<Language>> collect = knownLanguages.stream()
                .map(language -> DijkstraShortestPath.findPathBetween(getGraphAdapter(), language, destination))
                .sorted(Comparator.comparingInt(value -> value.size()))
                .findFirst()
                .orElseThrow(IllegalStateException::new)
                .stream()
                .collect(Collectors.toList());
        List<Language> solution = new ArrayList<>();
        solution.add(collect.get(0).nodeV());
        solution.add(collect.get(0).nodeU());
        for (int i = 1; i < collect.size(); i++) {
            solution.add(collect.get(i).nodeU());
        }
        return solution;
    }
}
