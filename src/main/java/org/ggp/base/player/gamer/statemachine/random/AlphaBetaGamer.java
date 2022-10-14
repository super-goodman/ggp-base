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

/**
 * RandomGamer is a very simple state-machine-based Gamer that will always
 * pick randomly from the legal moves it finds at any state in the game.
 */
public final class AlphaBetaGamer extends StateMachineGamer
{
	protected static final int timeoutThreshold = 2000;

	Integer roleIndex = 0;

	public AlphaBetaGamer() {
		super();
	}

	@Override
	public String getName() {
		return "MinMaxPlayer";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException	{
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move bestmove = moves.get(0);
		int score = 0;
		for (int i = 0; i < moves.size(); i++) {
			Move move = moves.get(i);
			int result = minscore(move, getCurrentState(), 0, 100, timeout);
			if(result==-1) return bestmove;
			if (result > score)		{
				score = result;
				bestmove = move;
			}
		}

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, bestmove, stop - start));
		return bestmove;
	}

	private int minscore(Move move, MachineState state, int alpha, int beta, long timeout) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if(System.currentTimeMillis() > timeout) {
			return -1;
		}

		List<List<Move>> moves = getStateMachine().getLegalJointMoves(state, getRole(), move);
		int score = 100;

		for(List<Move> mov : moves) {
			MachineState newstate = getStateMachine().getNextState(state, mov);

			int result = maxscore(newstate, alpha, beta, timeout);
			if(result==-1)
				return -1;
			beta = min(beta, result);
			if (beta <= alpha)
				return alpha;
		}
		return beta;
	}

	private int maxscore(MachineState state, int alpha, int beta, long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if(System.currentTimeMillis() > timeout) {
			return -1;
		}

		if (getStateMachine().isTerminal(state))
			return getStateMachine().getGoal(state, getRole());
		List<Move> my_moves = getStateMachine().getLegalMoves(state, getRole());
		for (int i = 0; i < my_moves.size(); i++) {
			int result = minscore(my_moves.get(i), state, alpha, beta, timeout);
			if(result==-1)
				return -1;
			alpha = max(alpha, result);
			if (alpha >= beta)
				return beta;
		}
		return alpha;
	}


	private int min(int x, int y) {
		if (x < y) return x;
		return y;
	}
	private int max(int x, int y) {
		if (x > y) return x;
		return y;
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