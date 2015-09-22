package me.sedlar.jcompile.project;

import me.sedlar.jcompile.Configuration;
import me.sedlar.jcompile.JCompile;

import java.io.*;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Tyler Sedlar
 * @since 6/27/2015
 */
public class Projects {

    private static final Set<Project> PROJECTS = new TreeSet<>();

    public static Set<Project> getAll() {
        return PROJECTS;
    }

    public static void readData() {
        File projectData = new File(Configuration.PROJECT_DATA);
        if (projectData.exists()) {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(projectData))) {
                int projectCount = dis.readInt();
                for (int i = 0; i < projectCount; i++) {
                    String projectDir = dis.readUTF();
                    String classpath = dis.readUTF();
                    PROJECTS.add(new Project(new File(projectDir), classpath));
                }
                JCompile.updateProjects();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeData() {
        File projectData = new File(Configuration.PROJECT_DATA);
        if (!projectData.exists()) {
            try {
                if (!projectData.createNewFile())
                    throw new RuntimeException("Unable to create project data");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(projectData))) {
            dos.writeInt(PROJECTS.size());
            for (Project project : PROJECTS) {
                String projectDir = project.dir.getAbsolutePath();
                dos.writeUTF(projectDir);
                dos.writeUTF(project.classpath);
            }
            JCompile.updateProjects();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void create(File dir) {
        PROJECTS.add(new Project(dir));
        writeData();
    }

    public static Project find(String dir) {
        for (Project project : PROJECTS) {
            if (project.dir.getAbsolutePath().equals(dir))
                return project;
        }
        return null;
    }
}
