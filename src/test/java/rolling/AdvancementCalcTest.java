package rolling;

import advancement.GeneralSkillCalculator;
import advancement.SpecialtySkill;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class AdvancementCalcTest {

    @Test
    void testTall() {
        final List<SpecialtySkill> specialtySkills = new GeneralSkillCalculator(ImmutableList.of(8, 4), true, 12, true, 8).generate(10L);
        final int sum = specialtySkills.stream().flatMapToInt(specialtySkill -> specialtySkill.getAPLog().stream().mapToInt(value -> value)).sum();
        Assertions.assertEquals(108, sum);
    }

    @Test
    void testWideExpertGeneral() {
        final List<SpecialtySkill> specialtySkills = new GeneralSkillCalculator(ImmutableList.of(8, 4, 10, 8), true, 4, true, 8).generate(14L);
        final int sum = specialtySkills.stream().flatMapToInt(specialtySkill -> specialtySkill.getAPLog().stream().mapToInt(value -> value)).sum();
        Assertions.assertEquals(74, sum);
    }

    @Test
    void testWideExpertD10() {
        final List<SpecialtySkill> specialtySkills = new GeneralSkillCalculator(ImmutableList.of(6, 4), true, 4, true, 12).generate(10L);
        final int sum = specialtySkills.stream().mapToInt(SpecialtySkill::getAPSpent).sum();
        Assertions.assertEquals(62, sum);
    }

    @Test
    void testWideNoExpertD10() {
        final List<SpecialtySkill> specialtySkills = new GeneralSkillCalculator(ImmutableList.of(6, 4), false, 4, true, 12).generate(10L);
        final int sum = specialtySkills.stream().mapToInt(SpecialtySkill::getAPSpent).sum();
        Assertions.assertEquals(46, sum);
    }
}
