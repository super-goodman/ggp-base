package org.ggp.base.player.gamer.statemachine.random;

import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;


public final class MCTS extends StateMachineGamer
{
	protected static final int timeoutThreshold = 2000;
	int[] howDeep = new int[1];
	@Override
	public String getName() {
		return "MCTS";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();
		long stop = System.currentTimeMillis();
		long allowedSearchTime = timeout - 500;

		StateMachine theMachine = getStateMachine();
		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		int branchQuantity = moves.size();
		Move selection = moves.get(0);

		if (branchQuantity > 1) {
			int[] branchScore = new int[branchQuantity];
			int[] branchAttempts = new int[branchQuantity];
			int currentBranch = 0;
			int simulationCount = 0;

			while (true) {
				if (currentBranch == branchQuantity)
					currentBranch = 0;
				int randomExploitResult = getDepthCharge(getCurrentState(), moves.get(currentBranch));
				branchScore[currentBranch] += randomExploitResult;
				branchAttempts[currentBranch] += 1;
				simulationCount += 1;
				if (System.currentTimeMillis() > allowedSearchTime)
    		        break;
				currentBranch += 1;
			}

			double moveValue[] = new double[branchQuantity];

			for (int i = 0; i < branchQuantity; i++) {
			    moveValue[i] = (double)branchScore[i] / branchAttempts[i] + 1.414 * Math.sqrt(Math.log(simulationCount)/branchAttempts[i]);
			}

			int choosenMove = findHighestValueMove(moveValue, branchQuantity);
			selection = moves.get(choosenMove);
		}

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	public static int findHighestValueMove (double moveValue[], int numberOfBranches)
	{
		int choosenMove = 0;
		double highestValue = moveValue[0];
		for (int i = 1; i < numberOfBranches; i++)
			if (moveValue[i] > highestValue) {
				choosenMove = i;
				highestValue = moveValue[i];
			}
		return choosenMove;
	}



	int getDepthCharge(MachineState currentState, Move move) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	    StateMachine stateMachine = getStateMachine();
        MachineState endBranch = stateMachine.performDepthCharge(stateMachine.getRandomNextState(currentState, getRole(), move), howDeep);
        return stateMachine.getGoal(endBranch, getRole());

	}
	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Random gamer does no game previewing.
	}

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Random gamer does no metagaming at the beginning of the match.
	}

	@Override
	public void stateMachineStop() {
		// Random gamer does no special cleanup when the match ends normally.
	}

	@Override
	public void stateMachineAbort() {
		// Random gamer does no special cleanup when the match ends abruptly.
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}
}