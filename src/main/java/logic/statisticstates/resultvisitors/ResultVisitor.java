package logic.statisticstates.resultvisitors;

import logic.DiceResult;

/**
 * Each visitor can return an object that stores a Embed field title and description after all objects have been visited
 */
public interface ResultVisitor {
    void visit(DiceResult result);
}
