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
public final class MinMaxGamer extends StateMachineGamer
{
	protected static final int timeoutThreshold = 2000;
	// our role's index
	Integer roleIndex = 0;

	public MinMaxGamer() {
		super();
	}

	@Override
	public String getName() {
		return "MinMaxPlayer";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move bestmove = moves.get(0);
		int score = 0;
		for (int i = 0; i < moves.size(); i++) {
			Move move = moves.get(i);
			int result = minscore(move, getCurrentState(), timeout);
			if (result > score)	{
				score = result;
				bestmove = move;
			}
			if(result==-1){
				return bestmove;
			}
		}
		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, bestmove, stop - start));
		return bestmove;
	}

	private int minscore(Move move, MachineState state, long timeout) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		// TODO:
        // 1) find a state after the move
        // 2) if the state is terminal, return result
        // 3) check all the possible moves
        // 4) find a move that gives as the minimum reward in the end
        // 5) return the best reward we can achieve from the resulting state
		if(System.currentTimeMillis()>timeout) {
			return -1;
		}
		List<List<Move>> moves = getStateMachine().getLegalJointMoves(state, getRole(), move);
		int score = 100;

		for(List<Move> mov : moves) {
			MachineState newstate = getStateMachine().getNextState(state, mov);
			int result = maxscore(newstate, timeout);
			if(result==-1)
				return -1;
			if (result < score)
				score = result;
		}
		return score;
	}

	private int maxscore(MachineState state, long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
	    // TODO:
        // 1) find a state after the move
        // 2) if the state is terminal, return result
        // 3) check all the possible moves
        // 4) find a move that gives as the highest reward in the end
        // 5) return the best reward we can achieve from the resulting state
		if(System.currentTimeMillis()>timeout) {
			return -1;
		}

		if (getStateMachine().isTerminal(state))
			return getStateMachine().getGoal(state, getRole());
		List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
		int maxscore = 0;
		for (int i = 0; i < moves.size(); i++) {
			int result = minscore(moves.get(i), state, timeout);
			if(result == -1)
				return -1;
			if (result > maxscore)
				maxscore = result;
		}
		return maxscore;
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