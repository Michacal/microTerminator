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

public class MyBot extends AIWithComputationBudget {

    private UnitTypeTable m_utt;
    private PathFinding m_pathFinding;
    private int action_count;
    private int playerID;

    private StrategyExercise5 myStrategy;



    public MyBot(int timeBudget, int iterationBudget, UnitTypeTable utt, PathFinding pathFinding) {

        super(timeBudget, iterationBudget);
        this.m_utt = utt;
        this.m_pathFinding = pathFinding;
        this.action_count = 0;
        this.playerID = 0;
        this.myStrategy = new StrategyExercise5();
    }


    @Override
    public int reset() {
        action_count = 0;
        return action_count;
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        action_count++;
        this.playerID = player;

        System.out.println("\n at game tick:[" + gs.getTime() + "]");
        System.out.println("My available resources " + gs.getPlayer(playerID).getResources() );
        System.out.println("resources for opponent " + gs.getPlayer(1- playerID).getResources());

        int countMyUnits = 0;
        int countOpponentUnits = 0;
        int countNeutralUnits = 0;
        HashSet<String> neutralTypes = new HashSet<>();



        if (gs.canExecuteAnyAction(playerID)) {
            for (Unit u : gs.getUnits()) {
                if (u.getPlayer() == playerID) {
                    countMyUnits++;
                    if (u.getType().name.equals("Worker")) {
                        System.out.println("worker available actions " + u.getUnitActions(gs));
                        if (gs.getActionAssignment(u) == null) {
                            System.out.println("worker at (" + u.getX() + "," + u.getY() + " ) with ID" + u.getID() + " has no assignement");
                        }
                    }
                } else if (u.getPlayer() == 1 - playerID) {
                    countOpponentUnits++;
                } else {
                    countNeutralUnits++;
                    neutralTypes.add(u.getType().name);
                }
                System.out.println("there are " + countMyUnits + " of my units " + countOpponentUnits + " oppponent units " + countNeutralUnits + " neutral units");
                System.out.println("Neutral unit types: " + neutralTypes);
            }

        }
        myStrategy.setLevel(2);
        return myStrategy.execute(player, gs, m_utt, m_pathFinding );
    }


    @Override
    public AI clone() {
        MyBot newmyBot = new MyBot(TIME_BUDGET, ITERATIONS_BUDGET, m_utt, m_pathFinding);
        newmyBot.action_count = this.action_count;
        newmyBot.playerID = this.playerID;
        newmyBot.myStrategy = myStrategy.clone();
        return newmyBot;
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

}

