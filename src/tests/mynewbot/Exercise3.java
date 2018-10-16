/*
 Level 0: Getting used to Java through microrts.
 + in GameVisualSimulationTest, replace line 30 with: AI ai1 = new MyBot(100, 100, utt, new BFSPathFinding());
 */

package tests.mynewbot;


import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

import java.util.ArrayList;
import java.util.List;


public class Exercise3 extends AIWithComputationBudget {
    private UnitTypeTable utt;
    private PathFinding pathFinding;
    private int calls;
    private int player;

    /**
     * Constructs the controller with the specified time and iterations budget
     *
     * @param timeBudget       time in milisseconds
     * @param iterationsBudget number of allowed iterations
     * @param utt Table that defines the unit types for this game.
     * @param pathFinding Instance for performing path planning queries.
     */
    public Exercise3(int timeBudget, int iterationsBudget, UnitTypeTable utt, PathFinding pathFinding) {
        super(timeBudget, iterationsBudget);
        this.pathFinding = pathFinding;
        this.utt = utt;
        calls = 0;
    }

    @Override
    public int reset() {
        calls = 0;
        return 0;
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        calls ++;
        this.player = player;
        System.out.println("[Player " + this.player + "] Getting asked for an action for the " + calls + "th time!");
        return new PlayerAction();
    }

    @Override
    public AI clone() {
        Exercise3 myClone = new Exercise3(TIME_BUDGET, ITERATIONS_BUDGET, utt, pathFinding);
        myClone.calls = calls;
        myClone.player = player;
        return myClone;
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

    @Override
    public void preGameAnalysis(GameState gs, long milliseconds, String readWriteFolder) throws Exception {
    }

}