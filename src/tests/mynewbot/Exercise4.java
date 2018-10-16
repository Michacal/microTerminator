package tests.mynewbot;


import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class Exercise4 extends AIWithComputationBudget{

    private UnitTypeTable utt;
    private PathFinding pathFinding;
    private int calls;
    private int player;

    /**
     * Constructs the controller with the specified time and iterations budget
     *
     * @param timeBudget       time in milliseconds.
     * @param iterationsBudget number of allowed iterations.
     * @param utt Table that defines the unit types for this game.
     * @param pathFinding Instance for performing path planning queries.
     */
    public Exercise4(int timeBudget, int iterationsBudget, UnitTypeTable utt, PathFinding pathFinding) {
        super(timeBudget, iterationsBudget);
        this.utt = utt;
        this.pathFinding = pathFinding;
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
//        System.out.println("Getting asked for an action for the " + calls + "th time!");

        if(gs.canExecuteAnyAction(player)) {
            System.out.println("\nAt game tick [" + gs.getTime() + "]:");
            System.out.println("Resources available for me: " + gs.getPlayer(player).getResources());
            System.out.println("Resources available for opponent: " + gs.getPlayer(1-player).getResources());

            int countMyUnits = 0;
            int countOpponentUnits = 0;
            int countNeutralUnits = 0;
            HashSet<String> neutralTypes = new HashSet<>();

            for (Unit u : gs.getUnits()) {
                if (u.getPlayer() == player) {
                    countMyUnits ++;
                    if (u.getType().name.equals("Worker")) {
                        System.out.println("Worker available actions: " + u.getUnitActions(gs));
                        if (gs.getActionAssignment(u) == null) {
                            System.out.println("Worker at (" + u.getX() + "," + u.getY() + ") with ID " + u.getID() + " has no assignment");
                        }
                    }
                } else if (u.getPlayer() == 1 - player) {
                    countOpponentUnits ++;
                } else {
                    countNeutralUnits ++;
                    neutralTypes.add(u.getType().name);
                }
            }
            System.out.println("There are " + countMyUnits + " units belonging to me; " + countOpponentUnits + " units belonging to opponent and " + countNeutralUnits + " neutral units");
            System.out.println("Neutral unit types: " + neutralTypes);
        }


        return new PlayerAction();
    }

    @Override
    public AI clone() {
        Exercise4 myClone = new Exercise4(TIME_BUDGET, ITERATIONS_BUDGET, utt, pathFinding);
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
