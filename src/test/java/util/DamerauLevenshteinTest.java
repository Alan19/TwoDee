package util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DamerauLevenshteinTest {

    @Test
    void testCloseDistance() {
        Assertions.assertEquals(2, DamerauLevenshtein.calculateDistance("doom", "Doom!"));
    }

    @Test
    void testFarDistance() {
        Assertions.assertEquals(6, DamerauLevenshtein.calculateDistance("doom", "Sunday"));
    }
}
