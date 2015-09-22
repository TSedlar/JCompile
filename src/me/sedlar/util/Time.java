package me.sedlar.util;

/**
 * @author Tyler Sedlar
 * @since 7/8/2015
 */
public class Time {

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
