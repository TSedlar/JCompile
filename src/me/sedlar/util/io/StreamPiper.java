package me.sedlar.util.io;

import java.io.InputStream;
import java.util.Scanner;

public class StreamPiper extends Thread {

    public final InputStream stream;
    public final Object lock = new Object();
    private StringBuilder output = new StringBuilder();

    public StreamPiper(InputStream stream) {
        this.stream = stream;
    }

    public String output() {
        return output.toString();
    }

    @Override
    public synchronized void run() {
        synchronized (lock) {
            try {
                Scanner scanner = new Scanner(stream);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (output.length() > 0)
                        output.append(System.getProperty("line.separator"));
                    output.append(line);
                }
                lock.notify();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}