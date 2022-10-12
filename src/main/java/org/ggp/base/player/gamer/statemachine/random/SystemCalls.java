package org.ggp.base.player.gamer.statemachine.random;

public class SystemCalls {
	public static final double garbageCollectionThreshold = 0.3;
	public static final double stopFillingPrimaryCacheThreshold = 0.3;
	public static final double stopFillingSecondaryCacheThreshold = 0.2;

	public static long getFreeMemoryBytes() {
		return Runtime.getRuntime().freeMemory();
	}

	public static double getFreeMemoryRatio() {
		return Runtime.getRuntime().freeMemory() / (double)Runtime.getRuntime().totalMemory();
	}

	public static long getUsedMemoryBytes() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	public static double getUsedMemoryRatio() {
		double totalMemory = Runtime.getRuntime().totalMemory();
		return (totalMemory - Runtime.getRuntime().freeMemory()) / totalMemory;
	}

	public static boolean isMemoryAvailable() {
		return getFreeMemoryRatio() > garbageCollectionThreshold;
	}

	public static boolean passedTime(long finishBy) {
		return System.currentTimeMillis() > finishBy;
	}
}