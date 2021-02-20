package dicerolling;

import com.google.common.collect.Multiset;

public interface PoolResult {
    FastRollResult addRegularDice(int diceValue);

    FastRollResult addPlotDice(int diceValue);

    FastRollResult addKeptDice(int diceValue);

    FastRollResult addFlatBonus(int bonus);

    Multiset<Integer> copyDiceList(Multiset<Integer> diceSet);

    int getTotal();

    int getDoomGenerated();
}
