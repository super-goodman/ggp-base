package org.ggp.base.player.gamer.statemachine.random;

import java.util.Arrays;
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

/**
 * RandomGamer is a very simple state-machine-based Gamer that will always
 * pick randomly from the legal moves it finds at any state in the game.
 */
public final class NashGamer extends StateMachineGamer
{
	int payoff [][] = new int[101][101];


	@Override
	public String getName() {
		return "NashGamer";
	}
	public NashGamer(){

	}
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{

		long start = System.currentTimeMillis();
		List<Move> move = getStateMachine().getLegalMoves(getCurrentState(),getRole());
		List<List<Move>> moves = getStateMachine().getLegalJointMoves(getCurrentState());
		Move selection = moves.get(0).get(0);
		for(List<Move> mov : moves) {
			MachineState currentState = getStateMachine().findNext(mov, getCurrentState());
			int score = getStateMachine().findReward(getRole(), currentState);
			int playerA = Integer.parseInt(mov.get(0).getContents().toSentence().get(0).toString());
			int playerB = Integer.parseInt(mov.get(1).getContents().toSentence().get(0).toString());
			if(score ==  100)
				score = 2;
			else if(score == 0)
				score = 0;
			else
				score = 1;
			payoff[playerA][playerB] = score;
		}

		for(int i = 0;i<101;i++){

			System.out.println(Arrays.toString(payoff[i]));

		}
		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(move, selection, stop - start));
		return selection;
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