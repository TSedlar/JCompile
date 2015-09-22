package me.sedlar.jcompile;

import me.sedlar.jcompile.project.Project;
import me.sedlar.jcompile.project.Projects;
import me.sedlar.util.OperatingSystem;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyAdapter;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tyler Sedlar
 * @since 6/27/2015
 */
public class JCompile {

    private static final TrayIcon TRAY_ICON = new TrayIcon(Configuration.IMAGES.getImage("icon-16x16.png"));

    private static final Menu PROJECTS = new Menu("Set Project");
    private static final Menu PROJECT = new Menu("Project");
    private static final Map<String, CheckboxMenuItem> PROJECT_MAP = new TreeMap<>();

    private static Project currentProject;
    private static File currentMain;

    public static void updateProjects() {
        PROJECTS.removeAll();
        PROJECT_MAP.clear();
        for (Project project : Projects.getAll()) {
            CheckboxMenuItem projectMenuItem = new CheckboxMenuItem(project.getName(), false);
            projectMenuItem.addItemListener(e -> {
                PROJECT_MAP.values().forEach(cmi -> cmi.setState(false));
                PROJECT_MAP.get(projectMenuItem.getLabel()).setState(true);
                currentProject = project;
                currentMain = null;
                PROJECT.setEnabled(true);
            });
            PROJECTS.add(projectMenuItem);
            PROJECT_MAP.put(project.getName(), projectMenuItem);
        }
    }

    public static void main(String[] args) throws Exception {
        EventQueue.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Projects.readData();
        PopupMenu menu = new PopupMenu();
        menu.add(PROJECTS);
        PROJECT.setEnabled(false);
        MenuItem compile = new MenuItem("Compile (Ctrl+Shift+C)");
        compile.addActionListener(e -> {
            if (currentProject != null) {
                if (currentProject.compile())
                    TRAY_ICON.displayMessage("Compilation", "Success!", TrayIcon.MessageType.INFO);
            }
        });
        PROJECT.add(compile);
        MenuItem run = new MenuItem("Run (Ctrl+Shift+R)");
        run.addActionListener(e -> {
            if (currentMain == null) {
                JFileChooser chooser = new JFileChooser(currentProject.classes);
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                chooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().endsWith(".class");
                    }
                    public String getDescription() {
                        return "*.class";
                    }
                });
                int result = chooser.showDialog(null, "Run class");
                if (result == 0) {
                    currentMain = chooser.getSelectedFile();
                }
            }
            if (currentMain != null) {
                String file = currentMain.getAbsolutePath().replace(currentProject.classes.getAbsolutePath() +
                        File.separator, "");
                try {
                    file = file.replace(File.separatorChar, '.').replaceAll(".class", "");
                    List<String> pArgs = new LinkedList<>();
                    if (OperatingSystem.get() == OperatingSystem.WINDOWS) {
                        pArgs.add("cmd.exe");
                        pArgs.add("/C");
                        pArgs.add("start");
                        pArgs.add("cmd");
                        pArgs.add("/k");
                    } else {
                        pArgs.add("xterm");
                        pArgs.add("-hold");
                        pArgs.add("-e");
                    }
                    pArgs.add("java");
                    pArgs.add("-cp");
                    String cp = currentProject.classes.getAbsolutePath();
                    if (currentProject.classpath != null && !currentProject.classpath.isEmpty())
                        cp += (File.pathSeparator + currentProject.classpath);
                    pArgs.add(cp);
                    pArgs.add(file);
                    ProcessBuilder builder = new ProcessBuilder(pArgs);
                    builder.directory(currentProject.classes);
                    builder.start();
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        });
        PROJECT.add(run);
        MenuItem clear = new MenuItem("Clear Run");
        clear.addActionListener(e -> currentMain = null);
        PROJECT.add(clear);
        MenuItem classpath = new MenuItem("Set Classpath");
        classpath.addActionListener(e -> {
            String cp = JOptionPane.showInputDialog("Classpath:");
            if (cp != null && !cp.isEmpty()) {
                currentProject.classpath = cp;
                Projects.writeData();
            }
        });
        PROJECT.add(classpath);
        MenuItem delete = new MenuItem("Delete Entry");
        delete.addActionListener((e) -> {
            if (currentProject != null) {
                Projects.getAll().remove(currentProject);
                Projects.writeData();
            }
        });
        PROJECT.add(delete);
        menu.add(PROJECT);
        MenuItem create = new MenuItem("Create Project");
        create.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(OperatingSystem.getHomeDirectory());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showDialog(null, "Create Project");
            if (result == 0) {
                File dir = chooser.getSelectedFile();
                Projects.create(dir);
                PROJECT_MAP.get(dir.getName()).setState(true);
            }
        });
        menu.add(create);
        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        menu.add(exit);
        TRAY_ICON.setPopupMenu(menu);
        SystemTray.getSystemTray().add(TRAY_ICON);
        try {
            Handler[] handlers = Logger.getLogger("").getHandlers();
            for (Handler handler : handlers)
                handler.setLevel(Level.OFF);
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
            throw new RuntimeException("There was a problem registering the native hook.");
        }
        NativeKeyListener listener = new NativeKeyAdapter() {
            public void nativeKeyPressed(NativeKeyEvent e) {
                String key = NativeKeyEvent.getKeyText(e.getKeyCode()).toLowerCase();
                String modifiers = NativeKeyEvent.getModifiersText(e.getModifiers()).toLowerCase();
                if (currentProject != null) {
                    if (modifiers.equals("shift+ctrl")) {
                        if (key.equals("c")) {
                            compile.getActionListeners()[0].actionPerformed(null);
                        } else if (key.equals("r")) {
                            run.getActionListeners()[0].actionPerformed(null);
                        }
                    }
                }
            }
        };
        GlobalScreen.addNativeKeyListener(listener);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                GlobalScreen.removeNativeKeyListener(listener);
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException e) {
                e.printStackTrace();
            }
        }));
    }
}
