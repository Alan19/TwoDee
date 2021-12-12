package advancement;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GeneralSkillCalculator {
    private final boolean attemptExpert;
    private final List<SpecialtySkill> specialties;
    private final long minimumFacets;
    private final boolean adolescentInterests;
    private final long nervewrightMana;

    public GeneralSkillCalculator(List<Integer> specialtyFacets, boolean attemptExpert, long minimumFacets, boolean adolescentInterests, long nervewrightMana) {
        this.attemptExpert = attemptExpert;
        specialties = specialtyFacets.stream().map(SpecialtySkill::new).collect(Collectors.toList());
        this.minimumFacets = minimumFacets;
        this.adolescentInterests = adolescentInterests;
        this.nervewrightMana = nervewrightMana;
    }

    private void nextStep(long targetFacets) {
        final boolean hasExpert = specialties.stream().anyMatch(specialtySkill -> specialtySkill.getCurrentFacets() >= 14);
        if ((!attemptExpert || hasExpert) && (specialties.stream().anyMatch(specialtySkill -> specialtySkill.getCurrentFacets() >= targetFacets) || targetFacets < 14)) {
            final Optional<SpecialtySkill> max = getHighestNonMaxedSpecialty(minimumFacets);
            if (max.isPresent()) {
                max.get().advance();
            }
            else if (canBuyNewSpecialty()) {
                buyNewSpecialty();
            }
            else {
                advanceCheapestSpecialty();
            }
        }
        else if (getHighestSpecialty().isPresent()) {
            advanceHighestSpecialty();
        }
        else {
            buyNewSpecialty();
        }
    }

    private void buyNewSpecialty() {
        specialties.add(SpecialtySkill.create(getNewSpecialtyCost()));
    }

    private void advanceHighestSpecialty() {
        getHighestSpecialty().ifPresent(SpecialtySkill::advance);
    }

    private Optional<SpecialtySkill> getHighestSpecialty() {
        return specialties.stream().max(Comparator.comparingInt(SpecialtySkill::getCurrentFacets));
    }

    /**
     * Advances the cheapest specialty in the list of specialties. If for some reason the cheapest specialty cannot be found, throw an exception since the program is stuck in an infinite loop
     */
    private void advanceCheapestSpecialty() {
        if (getCheapestSpecialty().isPresent()) {
            getCheapestSpecialty().get().advance();
        }
        else {
            throw new IllegalStateException("Unable to continue!\nSkills:" + specialties.stream()
                    .map(SpecialtySkill::getCurrentFacets)
                    .map(String::valueOf)
                    .collect(Collectors.joining("\n- ")));
        }
    }

    private Optional<SpecialtySkill> getCheapestSpecialty() {
        return specialties.stream().min(Comparator.comparingInt(SpecialtySkill::getAPCost));
    }

    private boolean canBuyNewSpecialty() {
        return specialties.size() < getGeneralSkillFacets() / 2 + 2;
    }

    private int getGeneralSkillFacets() {
        if (specialties.stream().mapToInt(SpecialtySkill::getCurrentFacets).sum() / 4 > 12) {
            return Math.min(specialties.stream().mapToInt(SpecialtySkill::getCurrentFacets).sum() / 4, specialties.stream().max(Comparator.comparingInt(SpecialtySkill::getCurrentFacets)).map(SpecialtySkill::getCurrentFacets).orElse(0));
        }
        else {
            return specialties.stream().mapToInt(SpecialtySkill::getCurrentFacets).sum() / 4;
        }
    }

    private Optional<SpecialtySkill> getHighestNonMaxedSpecialty(long target) {
        return specialties.stream()
                .filter(specialtySkill -> specialtySkill.getCurrentFacets() < target)
                .max(Comparator.comparingInt(SpecialtySkill::getCurrentFacets));
    }

    public List<SpecialtySkill> generate(Long targetFacets) {
        while (getGeneralSkillFacets() < targetFacets) {
            nextStep(targetFacets);
        }
        return specialties;
    }

    /**
     * Determines the cost of a new specialty skill (skillsoft adept > adolescent interests > default)
     *
     * @return The amount of AP to purchase a new specialty skill
     */
    public int getNewSpecialtyCost() {
        final int baseNewSpecialtyCost = 4 + specialties.size();
        if (nervewrightMana >= baseNewSpecialtyCost) {
            return 0;
        }
        else if (adolescentInterests && specialties.size() < 3) {
            return 2;
        }
        else {
            return baseNewSpecialtyCost;
        }
    }
}
