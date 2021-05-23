package Utils;

import static java.lang.System.nanoTime;

public class Timer {
    private static long beginning = 0L;
    private static long end = 0L;

    public static void start() {
        end = 0L; beginning = nanoTime();
    }

    public static double stop() {
        end = nanoTime();
        long elapsedTime = end - beginning;
        // seconds
        return elapsedTime / 1.0E09;
    }

    public static String getTime() {
        return "" + stop();
    }


    public static String getTimeString() {
        return "Elapsed Time: " +getTime() + " s";
    }
}
