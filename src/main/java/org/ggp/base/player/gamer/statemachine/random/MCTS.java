package org.ggp.base.player.gamer.statemachine.random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;


public final class MCTS extends SampleGamer
{
	private int[] depth = new int[1];
	private Map<MachineState, Integer> attemptsOnNode;
	private Map<MachineState, Integer> nodeValue;
	//exploration constant
	private static final double Exploration = 4;
	private static final int MctsTimeOut = 2500;
	private static final int MctsTimeOutShort = 500;


	@Override
	public String getName() {
		return "MCTS";
	}


	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
	    StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();
		long stop = System.currentTimeMillis();

		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = startGameTree(getRole(), getCurrentState(), timeout);

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}



	private Move startGameTree(Role role, MachineState currentState,long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		int score = 0;
		StateMachine stateMachine = getStateMachine();
		attemptsOnNode = new HashMap<MachineState, Integer>();
		nodeValue = new HashMap<MachineState, Integer>();
		attemptsOnNode.put(currentState, 0);
		nodeValue.put(currentState, 0);

		while(true) {

			ArrayList<MachineState> path = new ArrayList<MachineState>();
			//Selection
			MachineState selectedState = select(currentState, role, path, timeout);

			if (stateMachine.isTerminal(selectedState))  {
				attemptsOnNode.put(selectedState, attemptsOnNode.get(selectedState) + 1);
				nodeValue.put(selectedState, nodeValue.get(selectedState) + stateMachine.getGoal(selectedState, role));
				continue;
			}
			//Expansion
			expand(selectedState, role, timeout);
			//Simulation
			score += stateMachine.getGoal(stateMachine.performDepthCharge(selectedState, depth), role);
			if (timeout - System.currentTimeMillis() <= MctsTimeOut){
				break;
			}
			//Backpropagate
			backpropagate(score, path, timeout);

		}
		List<Move> legalMoves = stateMachine.getLegalMoves(currentState, role);
		double bestScore = 0;
		Move bestMove  = legalMoves.get(0);
		for (int i = 0; i < legalMoves.size(); i++) {
			List<List<Move>> legalJointMoves = stateMachine.getLegalJointMoves(currentState, role, legalMoves.get(i));
			for(int j = 0; j < legalJointMoves.size(); j++) {
				MachineState subNode = stateMachine.getNextState(currentState, legalJointMoves.get(j));
				if (attemptsOnNode.get(subNode) == 0) {
					continue;
				}
				//Evaluation
				double currentScore = nodeValue.get(subNode)/(double)attemptsOnNode.get(subNode);
				if(currentScore > bestScore) {
					bestScore = currentScore;
					bestMove = legalMoves.get(i);
				}
				if (timeout - System.currentTimeMillis() <= MctsTimeOutShort){
					return bestMove;
				}
			}
		}
		return bestMove;
	}


	private MachineState select(MachineState state, Role role, ArrayList<MachineState> path, long timeout) throws MoveDefinitionException, TransitionDefinitionException {
		StateMachine stateMachine = getStateMachine();
		MachineState result = state;
		int score = 0;
		while (true) {
			path.add(state);
			if ((timeout - System.currentTimeMillis() <= MctsTimeOut) || attemptsOnNode.get(state) == 0 ){
				return state;
			}
			List<Move> legalMoves = stateMachine.getLegalMoves(state, role);
			for (int i=0; i < legalMoves.size(); i++) {
				List<List<Move>> legalJointMoves = stateMachine.getLegalJointMoves(state, role, legalMoves.get(i));
				for(int j=0; j< legalJointMoves.size(); j++) {
					MachineState subNode = stateMachine.getNextState(state, legalJointMoves.get(j));
					if(attemptsOnNode.get(subNode) != null && attemptsOnNode.get(subNode) == 0) {
						path.add(subNode);
						return subNode;
					}
				}
			}

			for (int i=0; i < legalMoves.size(); i++) {
				List<List<Move>> legalJointMoves = stateMachine.getLegalJointMoves(state, role, legalMoves.get(i));
				for(int j=0; j< legalJointMoves.size(); j++) {
					MachineState subNode = getStateMachine().getNextState(state, legalJointMoves.get(j));
					if(attemptsOnNode.get(subNode) == null) {
						continue;
					}
					int currentScore = decideNodeToSimulate(state, subNode);
					if (currentScore> score) {
						result = subNode;
						score = currentScore;
					}
				}
			}
			state = result;
		}
	}


	private int decideNodeToSimulate(MachineState parentNode, MachineState subNode) {
		double utility = nodeValue.get(subNode)/ (double)attemptsOnNode.get(subNode);
		//UCT with ASM
		return (int) (utility + Exploration*Math.sqrt(Math.log(attemptsOnNode.get(parentNode))/attemptsOnNode.get(subNode))) ;
	}

	private void expand(MachineState state, Role role, long timeout) throws MoveDefinitionException, TransitionDefinitionException {
		StateMachine stateMachine = getStateMachine();
		List<Move> legalMoves = stateMachine.getLegalMoves(state, role);
		for (int i=0; i < legalMoves.size(); i++) {
			List<List<Move>> legalJointMoves = stateMachine.getLegalJointMoves(state, role, legalMoves.get(i));
			for(int j=0; j< legalJointMoves.size(); j++) {
				MachineState subNode = stateMachine.getNextState(state, legalJointMoves.get(j));
				if(attemptsOnNode.get(subNode) == null) {
					attemptsOnNode.put(subNode, 0);
					nodeValue.put(subNode, 0);
				}
				if (timeout - System.currentTimeMillis() <= MctsTimeOut){
					return;
				}
			}
		}
	}


	private void backpropagate(int score,
			ArrayList<MachineState> path, long timeout) throws GoalDefinitionException {
		for(int i=0; i< path.size(); i++) {
			MachineState currentState = path.get(i);
			attemptsOnNode.put(currentState, attemptsOnNode.get(currentState));
			nodeValue.put(currentState, nodeValue.get(currentState) + score);
			if (timeout - System.currentTimeMillis() <= MctsTimeOut){
				return ;
			}

		}

	}



	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{

	}

	@Override
	public void stateMachineStop() {

	}

	@Override
	public void stateMachineAbort() {

	}

}