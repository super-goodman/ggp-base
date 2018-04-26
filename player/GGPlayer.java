import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/***
 * This class contains all the methods needed to complete the class (as based on the notes)
 * at a minimum level. To implement further extensions to your player,
 * you are free to dig into the code base and change whatever you want to optimize
 * and improve your player.
 *
 * You are free to use this class as much or as little as you want. It is here to provide
 * an interface to the code base that makes implementing ideas from the notes easier. It
 * is up to you to improve your player beyond this. Good luck!
 *
 * @author Jeffrey Barratt
 */
public abstract class GGPlayer extends StateMachineGamer {

	/**
	 * Finds the next state after the provided state, given the list of moves was played
	 * by the players in the game.
	 */
	public MachineState findNext(List<Move> actions, MachineState s, StateMachine m)
			throws TransitionDefinitionException {
		return m.getNextState(s, actions);
	}

	/**
	 * Finds all legal moves for the given role in the given state.
	 */
	public List<Move> findLegals(Role r, MachineState s, StateMachine m)
			throws MoveDefinitionException {
		return m.getLegalMoves(s, r);
	}

	/**
	 * Finds all combinations of moves that can be played, given that the role provided played
	 * the action provided. (e.g. if the tic tac toe board position was filled except for the
	 * top left and bottom right, and it was O to play, and this method was called with
	 * findLegalJoints([Role X], [noop], current, machine), it would return [[noop, (place 1 1)], [noop, (place 3 3)]],
	 * where noop, (place 1 1) and (place 3 3) are all moves.
	 *
	 * This method is useful for the minscore method in both MiniMax/AlphaBeta and MCTS
	 */
	public List<List<Move>> findLegalJoints(Role r, Move action, MachineState s, StateMachine m)
			throws MoveDefinitionException {
		return m.getLegalJointMoves(s, r, action);
	}

	/**
	 * Finds all possible actions that are available to the given role. This includes illegal
	 * moves as well as legal ones for any given state.
	 */
	public List<Move> findActions(Role r, StateMachine m)
			throws MoveDefinitionException {
		return m.findActions(r);
	}

	/**
	 * Returns true if the given state is a terminal state, false otherwise.
	 */
	public boolean findTerminalp(MachineState s, StateMachine m) {
		return m.findTerminalp(s);
	}

	/**
	 * Returns the current utility the given role has in the given state.
	 */
	public int findReward(Role r, MachineState s, StateMachine m)
			throws GoalDefinitionException {
		return m.getGoal(s, r);
	}

	/**
	 * Returns a (possibly empty) list of all players in the game that are not the given role.
	 */
	public List<Role> findOpponents(Role r, StateMachine m) {
		List<Role> roles = new ArrayList<Role>(m.getRoles());
		roles.remove(r);
		return roles;
	}

	/**
	 * Simulates a game with random moves until a terminal state is reached.
	 * Useful for Monte-Carlo implementations.
	 */
	Random gen = null;
	public int depthCharge(Role r, MachineState s, StateMachine m)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (gen == null) gen = new Random();
		MachineState current = s;
		while (!m.findTerminalp(current)) {
			List<List<Move>> moves = m.getLegalJointMoves(current);
			current = m.getNextState(current, moves.get(gen.nextInt(moves.size())));
		}
		return m.findReward(r, current);
	}


	/*
	 * Below are interfaces for the GGPlayer class to interact with the StateMachineGamer class
	 * in a much clearer and more consistent way. You will not need to use the code below.
	 */

	public abstract void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException;
	public abstract Move play(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException;
	@Override
	public abstract void abort();
	@Override
	public abstract void stop();

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		start(timeout);
	}
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		return play(timeout);
	}
	@Override
	public void stateMachineAbort() {abort();}
	@Override
	public void stateMachineStop() {stop();}

	/**
	 * Not currently used in the current GGP standard.
	 */
	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {}
}
