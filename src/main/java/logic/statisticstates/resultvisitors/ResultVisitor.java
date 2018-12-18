package logic.statisticstates.resultvisitors;

import logic.DiceResult;

public interface ResultVisitor {
    void visit(DiceResult result);
}
