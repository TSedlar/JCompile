package me.sedlar.jcompile.project;

import me.sedlar.jcompile.Configuration;
import me.sedlar.util.Filter;
import me.sedlar.util.OperatingSystem;
import me.sedlar.util.io.StreamPiper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Tyler Sedlar
 * @since 6/27/2015
 */
public class Project implements Comparable<Project> {

    private static final Filter<File> SOURCE_FILTER = (f -> f.getName().endsWith(".java"));
    private static final Filter<File> CLASS_FILTER = (f -> f.getName().endsWith(".class"));

    public final File dir, src, out, classes, artifacts;

    public String classpath = "";

    public Project(File dir, String classpath) {
        this.dir = dir;
        this.src = new File(dir, "src" + File.separator);
        this.out = new File(dir, "out" + File.separator);
        this.classes = new File(out, "classes" + File.separator);
        this.artifacts = new File(out, "artifacts" + File.separator);
        this.classpath = classpath;
        File[] dirs = {src, out, classes, artifacts};
        for (File file : dirs) {
            if (!file.exists()) {
                if (!file.mkdirs())
                    throw new RuntimeException("Failed to create dir " + file.getAbsolutePath());
            }
        }
    }

    public Project(File dir) {
        this(dir, "");
    }

    public String getName() {
        return dir.getName();
    }

    private void populateFileList(File dir, List<File> files, Filter<File> filter) {
        File[] sub = dir.listFiles();
        if (sub != null) {
            for (File file : sub) {
                if (file.isDirectory()) {
                    populateFileList(file, files, filter);
                } else if (filter.accept(file)) {
                    files.add(file);
                }
            }
        }
    }

    public File[] sources() {
        List<File> files = new LinkedList<>();
        populateFileList(src, files, SOURCE_FILTER);
        return files.toArray(new File[files.size()]);
    }

    public File[] classes() {
        List<File> files = new LinkedList<>();
        populateFileList(classes, files, CLASS_FILTER);
        return files.toArray(new File[files.size()]);
    }

    public boolean compile() {
        clean();
        List<String> args = new LinkedList<>();
        args.add("javac");
        if (classpath != null && !classpath.isEmpty()) {
            args.add("-cp");
            args.add(classpath);
        }
        args.add("-d");
        args.add(classes.getAbsolutePath());
        File[] sources = sources();
        for (File file : sources) {
            String canonical = file.getAbsolutePath().replace(src.getAbsolutePath() + File.separator, "");
            canonical = canonical.replace('\\', '/');
            args.add(canonical);
        }
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.directory(src);
        String output = null;
        try {
            Process process = builder.start();
            StreamPiper piper = new StreamPiper(process.getErrorStream());
            piper.start();
            synchronized (piper.lock) {
                piper.lock.wait();
                output = piper.output();
            }
            if (!output.isEmpty()) {
                File log = Configuration.getNextLog();
                Files.write(Paths.get(log.toURI()), output.getBytes());
                args = new LinkedList<>();
                if (OperatingSystem.get() == OperatingSystem.WINDOWS) {
                    args.add("cmd.exe");
                    args.add("/C");
                    args.add("start");
                    args.add("cmd");
                    args.add("/k");
                } else {
                    args.add("xterm");
                    args.add("-hold");
                    args.add("-e");
                }
                args.add("more");
                args.add(log.getAbsolutePath());
                new ProcessBuilder(args).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output == null || output.isEmpty();
    }

    private boolean deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete())
                            throw new RuntimeException("Unable to delete file " + file.getAbsolutePath());
                    }
                }
            }
        }
        return dir.delete();
    }

    public boolean clean() {
        for (File file : classes()) {
            if (!file.delete())
                throw new RuntimeException("Failed to delete file " + file.getAbsolutePath());
        }
        File[] sub = classes.listFiles();
        if (sub != null) {
            for (File file : sub) {
                if (file.isDirectory())
                    deleteDirectory(file);
            }
        }
        List<File> classes = new LinkedList<>();
        populateFileList(src, classes, CLASS_FILTER);
        for (File file : classes) {
            if (!file.delete())
                throw new RuntimeException("Failed to delete file " + file.getAbsolutePath());
        }
        return true;
    }

    @Override
    public int compareTo(Project p) {
        return p.getName().compareTo(getName());
    }
}
