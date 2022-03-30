package language;

import com.google.common.collect.Sets;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("UnstableApiUsage")
public class LanguageLogicTest {

    private final Language TEST_LANG_1 = new Language("test_lang");
    private final Language TEST_LANG_2 = new Language("test_lang_2");

    private MutableGraph<Language> createTestGraph() {
        MutableGraph<Language> mutableGraph = GraphBuilder.directed()
                .build();
        mutableGraph.addNode(TEST_LANG_1);
        mutableGraph.addNode(TEST_LANG_2);
        mutableGraph.putEdge(TEST_LANG_1, TEST_LANG_2);
        return mutableGraph;
    }

    @Test
    void testValidAdd() {
        Language newLanguage = new Language("another_language");
        Assertions.assertEquals(
                LanguageLogic.of(createTestGraph())
                        .add(newLanguage),
                Try.success(newLanguage)
        );
    }

    @Test
    void testExistingAddFailure() {
        Language newLanguage = new Language("another_language");
        Language replicaLanguage = new Language("another_language");
        LanguageLogic languageLogic = LanguageLogic.of(createTestGraph());
        Assertions.assertEquals(
                languageLogic.add(newLanguage),
                Try.success(newLanguage)
        );

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> languageLogic.add(replicaLanguage).get()
        );
    }

    @Test
    void testValidGetByName() {
        Assertions.assertEquals(
                LanguageLogic.of(createTestGraph())
                        .getByName(TEST_LANG_1.getName()),
                Optional.of(TEST_LANG_1)
        );
    }

    @Test
    void testValidCloseGetByName() {
        Assertions.assertEquals(
                LanguageLogic.of(createTestGraph())
                        .getByName(TEST_LANG_1.getName().substring(2)),
                Optional.of(TEST_LANG_1)
        );
    }

    @Test
    void testAlmostFoundGetByName() {
        Assertions.assertEquals(
                LanguageLogic.of(createTestGraph())
                        .getByName(TEST_LANG_1.getName().substring(3)),
                Optional.empty()
        );
    }

    @Test
    void testNotFoundGetByName() {
        Assertions.assertEquals(
                LanguageLogic.of(createTestGraph())
                        .getByName("no"),
                Optional.empty()
        );
    }

    @Test
    void testDoesUpdate() {
        AtomicBoolean didUpdate = new AtomicBoolean(false);
        Language newLanguage = new Language("another_language");
        Assertions.assertEquals(
                LanguageLogic.of(createTestGraph(), graph -> didUpdate.set(true))
                        .add(newLanguage),
                Try.success(newLanguage)
        );
        Assertions.assertTrue(didUpdate.get());
    }

    @Test
    void testValidConnect() {
        LanguageLogic languageLogic = LanguageLogic.of(createTestGraph());
        Language newConnection = new Language("new_connection");
        Assertions.assertEquals(
                languageLogic.add(newConnection),
                Try.success(newConnection)
        );

        Assertions.assertEquals(
                languageLogic.connect(TEST_LANG_1, newConnection),
                Try.success(Pair.of(
                        TEST_LANG_1,
                        newConnection
                ))
        );
    }

    @Test
    void testConnectLangNotInGraph() {
        LanguageLogic languageLogic = LanguageLogic.of(createTestGraph());
        Language newConnection = new Language("new_connection");

        Assertions.assertThrows(
                IllegalArgumentException.class,
                languageLogic.connect(TEST_LANG_1, newConnection)::get
        );
    }

    @Test
    void testConnectAlreadyExists() {
        LanguageLogic languageLogic = LanguageLogic.of(createTestGraph());

        Assertions.assertThrows(
                IllegalArgumentException.class,
                languageLogic.connect(TEST_LANG_1, TEST_LANG_2)::get
        );
    }

    @Test
    void testRemoveValid() {
        LanguageLogic languageLogic = LanguageLogic.of(createTestGraph());

        Assertions.assertEquals(
                languageLogic.remove(TEST_LANG_1),
                Try.success(TEST_LANG_1)
        );
    }

    @Test
    void testRemoveNotFound() {
        LanguageLogic languageLogic = LanguageLogic.of(createTestGraph());

        Assertions.assertThrows(
                IllegalArgumentException.class,
                languageLogic.remove(new Language("not_found"))::get
        );
    }

    @Test
    void testValidConnections() {
        Assertions.assertEquals(
                LanguageLogic.of(createTestGraph()).getConnections(TEST_LANG_1),
                Sets.newHashSet(
                        TEST_LANG_2
                )
        );
    }

    @Test
    void testNotInGraphConnections() {
        Assertions.assertEquals(
                LanguageLogic.of(createTestGraph()).getConnections(new Language("not_found")),
                Collections.emptyList()
        );
    }
}
