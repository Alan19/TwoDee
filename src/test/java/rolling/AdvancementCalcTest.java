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
        final List<SpecialtySkill> specialtySkills = new GeneralSkillCalculator(ImmutableList.of(8, 4), true, true, 12, true, 8).generate(10L);
        final int sum = specialtySkills.stream().flatMapToInt(specialtySkill -> specialtySkill.getAPLog().stream().mapToInt(value -> value)).sum();
        Assertions.assertEquals(108, sum);
        System.out.println(specialtySkills);
    }

    @Test
    void testWide() {
        final List<SpecialtySkill> specialtySkills = new GeneralSkillCalculator(ImmutableList.of(8, 4, 10, 8), true, false, 12, true, 8).generate(14L);
        final int sum = specialtySkills.stream().flatMapToInt(specialtySkill -> specialtySkill.getAPLog().stream().mapToInt(value -> value)).sum();
        Assertions.assertEquals(74, sum);
        System.out.println(specialtySkills);
    }
}
