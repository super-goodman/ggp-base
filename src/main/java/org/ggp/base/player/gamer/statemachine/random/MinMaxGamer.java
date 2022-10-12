package org.ggp.base.player.gamer.statemachine.random;

import java.util.HashMap;
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

	private HashMap<MachineState, Integer> scoreCache;
	public MinMaxGamer() {
		super();
		scoreCache = new HashMap<MachineState, Integer>();
	}

	@Override
	public String getName() {
		return "MinMaxPlayer";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{

		long start = System.currentTimeMillis();
		long finishBy = timeout - timeoutThreshold;
		//算法第一部分
		//第一步首先获得所有合法移动
		StateMachine theMachine = getStateMachine();
		List<Move> myMoves = theMachine.getLegalMoves(getCurrentState(), getRole());
		//获得第一个合法移动
		Move bestMove = myMoves.get(0);
		//获得第二回合对手所有可能下子的可能性，并将其与第一个合法移动组成一个集合
		List<List<Move>> t = theMachine.getLegalJointMoves(getCurrentState(), getRole(), bestMove);
		//获得第一种可能性
		List<Move> jointMoves = t.get(0);
		//更新当前的游戏状态(已经模拟下了1子了)
		MachineState state = theMachine.getNextState(getCurrentState(), jointMoves);

		int bestMaxValue = getStateValue(state, finishBy);

		//算法第二部分
		//其实就是遍历所有的节点
		for (int i = 1; i < myMoves.size(); i++) {
			Move move = myMoves.get(i);
			jointMoves = theMachine.getLegalJointMoves(getCurrentState(), getRole(), move).get(0);
			int maxValue = getStateValue(theMachine.getNextState(getCurrentState(), jointMoves), finishBy);
			//移植循环直到获得一个最佳的move
			if (maxValue > bestMaxValue) {
				bestMove = move;
				bestMaxValue = maxValue;
			}
		}
		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(myMoves, bestMove, stop - start));
		return bestMove;
	}

	public boolean timedOut(long finishBy) {
		return System.currentTimeMillis() > finishBy;
	}

	public int getStateValue(MachineState state, long finishBy) {
		//时间超出就返回
		if (timedOut(finishBy)) return -1;
		//如果这个状态有了，就直接返回（怎么会直接有呢？应该只是保险措施？）
		//hashmap 存储着当前的状态与对应的score
		Integer cachedScore = scoreCache.get(state);
		if (cachedScore != null)
			return cachedScore;

		try {
			StateMachine theMachine = getStateMachine();

			//如果游戏结束就直接返回 hashmap
			if (theMachine.isTerminal(state)) {
				// TODO: Remove this line when done testing
				scoreCache.put(state, theMachine.getGoal(state, getRole()));
				return theMachine.getGoal(state, getRole());
			}
			//获得当前游戏状态中所有成对的可能性[(player1 mark),(player2 mark)]
			List<List<Move>> moves = theMachine.getLegalJointMoves(state);
			//刷新当前所有合法的移动，这个也会被在递归后用于检查是否是自己回合
			//getRole永远只返回服务器端当前可以行动的一方
			List<Move> myMoves = theMachine.getLegalMoves(state, getRole());
			//β = +∞  α = -∞
			int minScore = Integer.MAX_VALUE;
			int maxScore = Integer.MIN_VALUE;
			int score = 0;
			for (int i = 0; i < moves.size(); i++) {
				if (timedOut(finishBy)) return -1;
				MachineState next = theMachine.getNextState(state, moves.get(i));
				score = getStateValue(next, finishBy);
				//如果出错就返回-1错误码
				if (score == -1) {
					return -1;
				}
				//第一次运行到这里意味着第一个树干被递归到最下方的节点了
				//接下来是一层又一层的往上递归直到回到左树枝最上方的节点
				//永远都只保存 最小或者最大的score
				//标注【1】
				if (score < minScore)
					minScore = score;
				if (score > maxScore)
					maxScore = score;
			}
			//到这里，一个树节点的所有可能性就递归结束了，并获得了score
			//如果当前的合法移动数为1 代表是最后一步了，
			//最后一步可不就是min 选择 最下面一排的score
			//如果不是1，就判断全是max
			int temp = myMoves.size() ;
			// == 1代表是X掌控
			//之所以只有1的时候才返回min:
			//      o   max
			//x   x    x   x   min
			//在这个递归中，最后会运行到这里，计算的内容是最顶端到最底下的路线
			//因为标注【1】中min只会越来越小，max会越来越大
			//每一个树叶节点的值会随着往上递归会越来越大，
			//这个算法到最顶上其实只需要一个最大值就够了，所以只有在底层时返回min就够了
			if (temp == 1) {
				scoreCache.put(state, minScore);
				return minScore;
			} else {
				scoreCache.put(state, maxScore);
				return maxScore;
			}

		} catch (Exception e) {
			System.out.println("ERROR");
			return -1;
		}
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