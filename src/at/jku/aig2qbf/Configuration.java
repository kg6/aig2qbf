package at.jku.aig2qbf;



public class Configuration {
	public static boolean FAST = false;
	public static boolean SANTIY = true;
	public static boolean VERBOSE = true;
	public static boolean VERBOSETIMES = false;
	
	private static long startTimer = 0;
	public static void timerStart() {
		timerStart("");
	}
	public static void timerStart(String text) {
		if (text.compareTo("") != 0) {
			System.err.println(text);
		}

		startTimer = System.currentTimeMillis();
	}
	public static void timerEnd() {
		timerEnd("");
	}
	public static long timerEnd(String text) {
		long time = System.currentTimeMillis() - startTimer;

		if (text.compareTo("") != 0) {
			System.err.println(text + " " + time + "ms");
		}

		return time;
	}
}
