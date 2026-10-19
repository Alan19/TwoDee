package statistics.strategies;

import statistics.StatisticsLogic;
import statistics2.StatisticsRollResult;
import util.EmbedField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SumStrategy implements ResultStrategy {

    @Override
    public List<EmbedField> executeStrategy(Map<StatisticsRollResult, Long> resultMap, long totalCombinations) {
        Map<Integer, Long> rollResultToOccurancesMap = StatisticsLogic.getRollResultToOccurancesMap(resultMap);
        EmbedField embedField = new EmbedField();
        embedField.setTitle("Chance to get a:");
        embedField.setContent(rollResultToOccurancesMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(outcome -> "%d: %s".formatted(outcome.getKey(), generatePercentage(outcome.getValue(), totalCombinations)))
                .collect(Collectors.joining("\n")));
        ArrayList<EmbedField> output = new ArrayList<>();
        output.add(embedField);
        return output;
    }
}
