package util;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class DamerauLevenshteinTest {

    @Test
    void testCloseDistance() {
        Assertions.assertEquals(2, DamerauLevenshtein.calculateDistance("doom", "Doom!"));
    }

    @Test
    void testFarDistance() {
        Assertions.assertEquals(6, DamerauLevenshtein.calculateDistance("doom", "Sunday"));
    }

    @Test
    void testFindClose() {
        Assertions.assertEquals(
                DamerauLevenshtein.getClosest("doom", Sets.newHashSet("Doom!", "Sunday"), false),
                Optional.of("Doom!")
        );
    }

}
