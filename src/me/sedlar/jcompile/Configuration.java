package me.sedlar.jcompile;

import me.sedlar.util.OperatingSystem;
import me.sedlar.util.io.ResourceLoader;

import java.io.File;

/**
 * @author Tyler Sedlar
 * @since 6/27/2015
 */
public class Configuration {

    public static final String APP_NAME = "JCompile";
    public static final String HOME = OperatingSystem.getHomeDirectory() + File.separator + APP_NAME + File.separator;
    public static final String PROJECT_DATA = HOME + "projects.dat";
    public static final String LOGS = HOME + "logs" + File.separator;

    public static final ResourceLoader IMAGES = new ResourceLoader("/me/sedlar/jcompile/res/images/");

    public static File getNextLog() {
        File dir = new File(LOGS);
        if (!dir.exists()) {
            if (!dir.mkdirs())
                throw new RuntimeException("Unable to create logs directory");
        }
        File[] files = dir.listFiles();
        int logIndex = (files != null ? files.length : 0) + 1;
        return new File(dir, "log-" + logIndex + ".txt");
    }
}
