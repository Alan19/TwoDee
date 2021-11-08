package util;

import io.vavr.control.Try;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexExtractor {
    private static final Pattern AMOUNT_EXTRACTOR = Pattern.compile("\\b(?<start>\\d+)\\D+(?<end>\\d+)");

    public static Try<Integer> getDifference(String string) {
        Matcher extracted = AMOUNT_EXTRACTOR.matcher(string);
        if (extracted.matches()) {
            int begin = Integer.parseInt(extracted.group("start"));
            int end = Integer.parseInt(extracted.group("end"));
            return Try.success(end - begin);
        }
        else {
            return Try.failure(new IllegalArgumentException("Embed Field did not match expected Pattern"));
        }
    }
}
