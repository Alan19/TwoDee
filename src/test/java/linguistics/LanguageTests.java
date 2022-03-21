package linguistics;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.EndpointPair;
import org.jgrapht.alg.DijkstraShortestPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static linguistics.LanguageGraph.*;

@SuppressWarnings("UnstableApiUsage")
class LanguageTests {

    @Test
    void testLanguage() {
        final List<EndpointPair<Language>> pathBetween = DijkstraShortestPath.findPathBetween(getGraphAdapter(), GREEK, TOKALAKI);
        Assertions.assertEquals(4, pathBetween.size());
    }

    @Test
    void testLanguageList() {
        List<Language> playerLanguages = ImmutableList.of(YGGIC, PUNCTUA, STARWORD, TOKALAKI, CELESTIAL, GREEK);
        final int steps = playerLanguages.stream()
                .mapToInt(language -> DijkstraShortestPath.findPathBetween(getGraphAdapter(), language, CENSERVITI).size())
                .min()
                .orElseThrow(IllegalStateException::new);
        Assertions.assertEquals(2, steps);
    }
}
