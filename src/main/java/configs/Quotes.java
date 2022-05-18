package configs;

import java.util.Random;

public class Quotes {
    private static final Random random = new Random();
    private String[] startupQuotes;
    private String[] rollQuotes;

    public Quotes() {
        startupQuotes = new String[]{};
        rollQuotes = new String[]{};
    }

    public String getRandomStartupQuote() {
        return startupQuotes[random.nextInt(startupQuotes.length)];
    }

    public void setStartupQuotes(String[] startupQuotes) {
        this.startupQuotes = startupQuotes;
    }

    public String getRandomRollQuote() {
        return rollQuotes[random.nextInt(rollQuotes.length)];
    }

    public void setRollQuotes(String[] rollQuotes) {
        this.rollQuotes = rollQuotes;
    }
}
