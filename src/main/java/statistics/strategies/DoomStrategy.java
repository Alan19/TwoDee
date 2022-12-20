package statistics.strategies;

import rolling.BuildablePoolResult;
import statistics.StatisticsLogic;
import util.EmbedField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DoomStrategy implements ResultStrategy {

    @Override
    public List<EmbedField> executeStrategy(HashMap<BuildablePoolResult, Long> resultMap, long totalCombinations) {
        Map<Integer, Long> opportunitiesMap = StatisticsLogic.getRollToOpportunitiesMap(resultMap);
        return List.of(new EmbedField("Chance to generate doom", opportunitiesMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ": " + generatePercentage(entry.getValue(), totalCombinations))
                .collect(Collectors.joining("\n"))));
    }
}
