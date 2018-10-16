package tests.mynewbot;

import ai.abstraction.pathfinding.PathFinding;
import rts.*;
import rts.units.Unit;
import rts.units.UnitTypeTable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static rts.UnitAction.*;

public class StrategyExercise5 {
    private int level = 1;
    private Unit activeWorker = null;
    private ArrayList<UnitAction> buildBarracksActions;
    private ArrayList<UnitAction> harvestActions;
    private boolean harvesting, buildingBarracks, attacking;

    public StrategyExercise5() {
        buildBarracksActions = new ArrayList<>();
        harvestActions = new ArrayList<>();
        harvesting = true;
    }

    void setLevel(int level) {
        this.level = level;
    }

    int getLevel() {
        return level;
    }

    PlayerAction execute(int player, GameState gs, UnitTypeTable utt, PathFinding pf) {
        switch(level) {
            case 2: return simpleBehaviour(player, gs, utt, pf);
            default:
            case 1: return printGameInformation(player, gs);
        }
    }

    /**
     * Level 1 exercises.
     * @param player    Your player ID
     * @param gs        The current game state
     * @return          Empty PlayerAction object (does nothing)
     */
    private PlayerAction printGameInformation(int player, GameState gs) {
        if (gs.canExecuteAnyAction(player)) {
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
                    if (u.getType().name.equals("Base")) {
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

    /**
     * Level 2 exercises.
     * @param player    Your player ID.
     * @param gs        The current game state.
     * @param utt       The Unit Type Table
     * @param pf        Path finding algorithm
     * @return          PlayerAction containing unit assignments.
     */
    private PlayerAction simpleBehaviour(int player, GameState gs, UnitTypeTable utt, PathFinding pf) {

        // Create a new player action. All actions used to your units are assigned here.
        PlayerAction pa = new PlayerAction();

        if (gs.canExecuteAnyAction(player)) {

            // Find the currently reserved resources and add them to the new PlayerAction object
            PhysicalGameState pgs = gs.getPhysicalGameState();
            for (Unit u: gs.getUnits()) {
                UnitActionAssignment uaa = gs.getActionAssignment(u);
                if (uaa != null) {
                    ResourceUsage ru = uaa.action.resourceUsage(u, pgs);
                    pa.getResourceUsage().merge(ru);
                }
            }

            // We only have 1 worker to start with, find it and set this as active worker.
            if (activeWorker == null) {
                for (Unit u : gs.getUnits()) {
                    if (u.getPlayer() == player) {
                        if (u.getType().canHarvest) {
                            activeWorker = u;
                        }
                    }
                }
            }

            // This block deals with the behaviour (sequence of commands) in TODOs 1-4
            if (gs.getActionAssignment(activeWorker) == null) {

                //First we harvest.
                if(harvesting) {
                    boolean finished = handleHarvest(gs, pa);
                    if(finished)
                    {
                        harvesting = false;
                        buildingBarracks = true;
                    }

                    //After we have completed harvesting, we build barracks.
                }else if(buildingBarracks) {
                    boolean barracksBuilt = handleBuildBarracks(gs, pa, utt);
                    if(barracksBuilt)
                    {
                        buildingBarracks = false;

                        //After we have started building the barracks, we use the forward model to see the future.
                        handleRollState(gs, pa, player);

                        attacking = true;
                    }

                    //We send the worker to attack after the barracks are built.
                }else if(attacking && gs.getUnits().contains(activeWorker)){
                    handleAttack(gs, pa, player, pf);
                }
            }


            // General rules
            for (Unit u : gs.getUnits()) {
                if (u.getPlayer() == player && gs.getActionAssignment(u) == null && u != activeWorker && pa.getAction(u) == null) {
                    // Get available actions
                    List<UnitAction> availableActions = u.getUnitActions(gs);
                    UnitAction nextAction = null;

                    if (u.getType().canHarvest) { // Workers
                        nextAction = availableActions.get(new Random().nextInt(availableActions.size()));
                    }

                    boolean barracksExist = gs.getPhysicalGameState().getUnitAt(4,4) != null;
                    if ((u.getType().name.equals("Base") || u.getType().name.equals("Barracks")) && barracksExist) { // Buildings
                        nextAction = availableActions.get(0);
                    }

                    checkResourcesAndAddAction(nextAction, u, pa, gs);

                }
            }
        }

        return pa;
    }

    /**
     * Handles a harvest to a specific location. It first adds the required sequence of actions to a list, to
     * then select which is the next action to execute in the game.
     * @param gs Current game state
     * @param pa PlayerAction to assign player actions.
     * @return true if harvest finishes in this game cycle.
     */
    private boolean handleHarvest(GameState gs, PlayerAction pa) {

        if (harvestActions.size() == 0) {
            harvestActions.add(new UnitAction(TYPE_HARVEST, DIRECTION_LEFT));
            harvestActions.add(new UnitAction(TYPE_MOVE, DIRECTION_RIGHT));
            harvestActions.add(new UnitAction(TYPE_RETURN, DIRECTION_DOWN));
            harvestActions.add(new UnitAction(TYPE_MOVE, DIRECTION_LEFT));
        }

        if (harvestActions.size() != 0) { // && pa.getAction(activeWorker) == null) {
            // Continue our list of actions to build barracks
            UnitAction nextAction = harvestActions.remove(0);
            checkResourcesAndAddAction(nextAction, activeWorker, pa, gs);
        }

        if (harvestActions.size() == 0) {
            // Harvest is finished
            return true;
        }
        return false;
    }


    /**
     * Handles the building of a barracks in a specific location. It first adds the required sequence of actions
     * to a list, to then select which is the next action to execute in the game.
     * @param gs Current game state
     * @param pa PlayerAction to assign player actions.
     * @return true if all actions have been issued.
     */
    private boolean handleBuildBarracks(GameState gs, PlayerAction pa, UnitTypeTable utt) {

        // Create hardcoded action sequence for the active worker to harvest 1 resource and build Barracks at (4,4).
        if (buildBarracksActions.size() == 0 && gs.free(4, 4)) {
            buildBarracksActions.add(new UnitAction(TYPE_MOVE,DIRECTION_DOWN));
            buildBarracksActions.add(new UnitAction(TYPE_MOVE,DIRECTION_DOWN));
            buildBarracksActions.add(new UnitAction(TYPE_MOVE,DIRECTION_DOWN));
            buildBarracksActions.add(new UnitAction(TYPE_MOVE,DIRECTION_RIGHT));
            buildBarracksActions.add(new UnitAction(TYPE_MOVE,DIRECTION_RIGHT));
            buildBarracksActions.add(new UnitAction(TYPE_PRODUCE,DIRECTION_RIGHT,utt.getUnitType("Barracks")));
        }

        if (buildBarracksActions.size() != 0) {
            // Continue our list of actions to build barracks
            UnitAction nextAction = buildBarracksActions.remove(0);
            checkResourcesAndAddAction(nextAction, activeWorker, pa, gs);
        }

        if (buildBarracksActions.size() == 0)
            return true;

        return false;
    }

    /**
     * Rolls the state forward from the current state until the barracks are complete.
     * @param gs The current game state
     * @param pa The actions to be executed.
     * @param playerID This player ID.
     */
    private void handleRollState(GameState gs, PlayerAction pa, int playerID)
    {
        int numStepsForward = 200;
        // Just assigned build action. Roll state forward for 200 ticks.
        GameState copyState = gs.clone();
        copyState.issue(pa);
        for (int i = 0; i < numStepsForward; i++) {
            copyState.cycle();
        }

        // Print the number of barracks to confirm it was built.
        int barracksCountNow = 0;
        int barracksCountThen = 0;

        for (Unit u : gs.getUnits()) {
            if (u.getPlayer() == playerID && u.getType().name.equals("Barracks")) {
                barracksCountNow++;
            }
        }
        for (Unit u : copyState.getUnits()) {
            if (u.getPlayer() == playerID && u.getType().name.equals("Barracks")) {
                barracksCountThen++;
            }
        }
        System.out.println("Barracks count at game tick " + gs.getTime() + ": " + barracksCountNow);
        System.out.println("Barracks count at game tick " + copyState.getTime() + ": " + barracksCountThen);
    }

    /**
     * This function sends the active worker to attack the closest opponent unit.
     * @param gs Current game state
     * @param pa PlayerAction where actions are issued.
     * @param playerID The ID of this player.
     * @param pf Pathfinding object to find shortest paths.
     */
    public void handleAttack(GameState gs, PlayerAction pa, int playerID, PathFinding pf)
    {
        // It's time to attack the closest enemy. Find that unit
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2: gs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != playerID) {
                int d = manhattanDistance(u2,activeWorker);
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        // If we found an enemy, attack them!
        if (closestEnemy != null) {
            UnitAction nextAction;

            // Attack if in range
            if (closestDistance <= activeWorker.getAttackRange()) {
                nextAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemy.getX(), closestEnemy.getY());

            } else {
                // Move towards the unit otherwise
                int opponentFlatPosition = flattenedPosition(gs, closestEnemy.getX(), closestEnemy.getY());
                nextAction = pf.findPathToPositionInRange(activeWorker, opponentFlatPosition,
                        activeWorker.getAttackRange(), gs, new ResourceUsage());
            }

            checkResourcesAndAddAction(nextAction, activeWorker, pa, gs);
        }
    }

    /**
     * Returns the position (x,y) flattened. This is, the location of that cell in a vector instead of a matrix.
     * @param gs Game state, needed for the with of the board or map.
     * @param x x position
     * @param y y position
     * @return position in a vector.
     */
    private int flattenedPosition(GameState gs, int x, int y)
    {
        return x + y * gs.getPhysicalGameState().getWidth();
    }

    /**
     * Computes the Manhattan distance between two units.
     * @param a One unit.
     * @param b Another unit.
     * @return the Manhattan distance. As expected.
     */
    private int manhattanDistance(Unit a, Unit b)
    {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }


    public StrategyExercise5 clone() {
        StrategyExercise5 myClone = new StrategyExercise5();
        myClone.setLevel(myClone.getLevel());
        myClone.activeWorker = activeWorker.clone();
        myClone.buildBarracksActions.addAll(buildBarracksActions);
        return myClone;
    }

    /**
     * Method to check resources needed are available before adding an action assignment to the PlayerAction
     * @param nextAction    The action to be added to PlayerAction
     * @param u             The unit the action is assigned to
     * @param pa            The PlayerAction object to receive new unit assignment
     * @param gs            The current game state
     */
    private void checkResourcesAndAddAction(UnitAction nextAction, Unit u, PlayerAction pa, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //If there are enough resources and the action is allowed for this unit
        if (nextAction != null && nextAction.resourceUsage(u, pgs).consistentWith(pa.getResourceUsage(), gs) &&
                (gs.isUnitActionAllowed(u, nextAction)) ) {
            addActionWithResourceUsage(nextAction, u, pa, gs);
        }
    }

    /**
     * Method to add an action assignment and resource usage to the PlayerAction
     * @param nextAction    The action to be added to PlayerAction
     * @param u             The unit the action is assigned to
     * @param pa            The PlayerAction object to receive new unit assignment
     * @param gs            The current game state
     */
    private void addActionWithResourceUsage(UnitAction nextAction, Unit u, PlayerAction pa, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        ResourceUsage ru = nextAction.resourceUsage(u, pgs);
        pa.getResourceUsage().merge(ru);
        pa.addUnitAction(u, nextAction);
    }
}
