package util;

import java.awt.*;
import java.util.Random;

public class RandomColor {

    private final Random random;

    private RandomColor() {
        random = new Random();
    }

    public static Color getRandomColor() {
        return RandomColorHolder.INSTANCE.generateRandomColor();
    }

    private Color generateRandomColor() {
        return new Color(random.nextFloat(), random.nextFloat(), random.nextFloat());
    }

    private static class RandomColorHolder {
        private static final RandomColor INSTANCE = new RandomColor();
    }
}
