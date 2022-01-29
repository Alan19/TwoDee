package advancement;

import java.util.ArrayList;
import java.util.List;

public class SpecialtySkill {
    private final List<Integer> apSpent;
    private int startingFacets;
    private int currentFacets;

    public SpecialtySkill(int currentFacets) {
        this.startingFacets = currentFacets;
        this.currentFacets = currentFacets;
        this.apSpent = new ArrayList<>();
    }

    public static SpecialtySkill create(int cost) {
        SpecialtySkill specialtySkill = new SpecialtySkill(4);
        specialtySkill.apSpent.add(cost);
        specialtySkill.startingFacets = 0;
        specialtySkill.currentFacets = 4;
        return specialtySkill;
    }

    public List<Integer> getAPLog() {
        return apSpent;
    }

    public int getAPSpent() {
        return apSpent.stream().mapToInt(value -> value).sum();
    }

    public int getCurrentFacets() {
        return currentFacets;
    }

    public int getAPCost() {
        return currentFacets + 2;
    }

    public void advance() {
        apSpent.add(getAPCost());
        currentFacets += 2;
    }

    public int getStartingFacets() {
        return startingFacets;
    }
}
