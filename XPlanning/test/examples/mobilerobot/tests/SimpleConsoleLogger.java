package examples.mobilerobot.tests;

public class SimpleConsoleLogger {

	public static void log(String message) {
		System.out.println(message);
	}

	public static void log(String header, Object message, boolean sameLine) {
		if (sameLine) {
			System.out.print(header + ": ");
		} else {
			System.out.println(header + ":");
		}
		System.out.println(message);
	}

	public static void newLine() {
		System.out.println();
	}
}
