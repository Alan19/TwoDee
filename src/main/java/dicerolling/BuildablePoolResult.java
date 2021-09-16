package dicerolling;

public interface BuildablePoolResult extends PoolResult {
    BuildablePoolResult addRegularDice(int diceValue);

    BuildablePoolResult addPlotDice(int diceValue);

    BuildablePoolResult addKeptDice(int diceValue);

    BuildablePoolResult addFlatBonus(int bonus);

}
