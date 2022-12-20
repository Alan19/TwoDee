package statistics.strategies;

import rolling.BuildablePoolResult;
import util.EmbedField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpportunityStrategy implements ResultStrategy {
    @Override
    public List<EmbedField> executeStrategy(HashMap<BuildablePoolResult, Long> resultMap, long totalCombinations) {
        String embedValue = DifficultyStrategy.getDifficultyMap().values().stream()
                .map(difficultyEntry -> difficultyEntry.getLeft() + ": " + generatePercentage(getOpportunityCountForDifficulty(resultMap, difficultyEntry.getMiddle()), totalCombinations))
                .collect(Collectors.joining("\n"));
        return List.of(new EmbedField("Chance of failure with an opportunity", embedValue));
    }

    private long getOpportunityCountForDifficulty(HashMap<BuildablePoolResult, Long> resultMap, int difficultyTarget) {
        return resultMap.entrySet().stream()
                .filter(resultEntry -> resultEntry.getKey().getDoomGenerated() > 0 && resultEntry.getKey().getTotal() < difficultyTarget)
                .mapToLong(Map.Entry::getValue)
                .reduce(0, Math::addExact);
    }
}
