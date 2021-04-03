package dicerolling;

public interface PoolResult {
    PoolResult addRegularDice(int diceValue);

    PoolResult addPlotDice(int diceValue);

    PoolResult addKeptDice(int diceValue);

    PoolResult addFlatBonus(int bonus);

    int getTotal();

    int getDoomGenerated();
}
