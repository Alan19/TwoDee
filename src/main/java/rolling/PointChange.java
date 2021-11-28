package rolling;

public class PointChange {
    private final Result result;
    private final int newDoom;
    private final int newPlotPoints;

    public PointChange(Result result, int newDoom, int newPlotPoints) {
        this.result = result;
        this.newDoom = newDoom;
        this.newPlotPoints = newPlotPoints;
    }

    public Result getResult() {
        return result;
    }

    public int getNewDoom() {
        return newDoom;
    }

    public int getNewPlotPoints() {
        return newPlotPoints;
    }
}
