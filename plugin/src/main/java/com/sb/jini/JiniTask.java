package com.sb.jini;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sb.jini.data.PackageDescription;
import lombok.Getter;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class JiniTask extends DefaultTask {

    private static final String LIB_DIR = "lib";
    private static final String PACKAGE_DESC_FILE = "package.json";

    private final FileCollection classFiles;
    private final Property<FileCollection> jarFiles;

    @Input
    @Getter
    private final Property<String> mainClassName;
    @Input
    @Getter
    private final Property<String> exeFilename;
    @Input
    @Getter
    @Optional
    private final Property<String> cliFilename;
    @Input
    @Getter
    private final SetProperty<String> vmArgs;
    @Input
    @Getter
    private final SetProperty<String> args;

    @OutputDirectory
    @Getter
    private final DirectoryProperty outputDirectory;
    @OutputDirectory
    @Getter
    private final Provider<Directory> cpDirectory;

    public JiniTask() {
        JiniExtension ext = getProject().getExtensions().getByType(JiniExtension.class);
        classFiles = getProject().getConfigurations().getByName("runtimeClasspath");
        jarFiles = getProject().getObjects().property(FileCollection.class);
        getProject().getPluginManager().withPlugin("java", ap -> {
            TaskProvider<Task> named = getProject().getTasks().named(JavaPlugin.JAR_TASK_NAME);
            jarFiles.convention(named.map(it -> it.getOutputs().getFiles()));
        });

        mainClassName = getProject().getObjects().property(String.class);
        mainClassName.convention(ext.getMainClassName());
        exeFilename = getProject().getObjects().property(String.class);
        exeFilename.convention(ext.getExeFilename());
        cliFilename = getProject().getObjects().property(String.class);
        cliFilename.convention(ext.getCliFilename());

        vmArgs = getProject().getObjects().setProperty(String.class);
        vmArgs.convention(ext.getVmArgs());
        args = getProject().getObjects().setProperty(String.class);
        args.convention(ext.getArgs());

        ProjectLayout layout = getProject().getLayout();

        outputDirectory = getProject().getObjects()
                .directoryProperty()
                .convention(layout.getBuildDirectory().dir("jini"));
        cpDirectory = outputDirectory.dir(LIB_DIR);
    }

    @TaskAction
    public void run() throws IOException {
        Files.createDirectories(cpDirectory.get().getAsFile().toPath());

        // copy libs to out directory
        Stream.concat(
                jarFiles.get().getFiles().stream(),
                classFiles.getFiles().stream()
        ).forEach(f -> {
            try {
                Files.copy(
                        f.toPath(),
                        cpDirectory.get().file(f.getName()).getAsFile().toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (Exception ex) {
                getLogger().error(ex.getMessage());
            }
        });

        // generate and copy package
        List<String> classpath = Stream.concat(
                jarFiles.get().getFiles().stream(),
                classFiles.getFiles().stream()
        ).map(f -> LIB_DIR + File.separator + f.getName()).sorted().toList();
        PackageDescription pd = PackageDescription.builder()
                .mainClass(mainClassName.get())
                .classPath(classpath)
                .vmArgs(vmArgs.get())
                .args(args.get())
                .build();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = cpDirectory.get().file(PACKAGE_DESC_FILE).getAsFile();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(pd, writer);
        }

        // write executables
        Files.copy(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream("executables/jini.exe")
                ),
                outputDirectory.get().file(exeFilename.get()).getAsFile().toPath(),
                StandardCopyOption.REPLACE_EXISTING
        );
        if (cliFilename.isPresent()) {
            Files.copy(
                    Objects.requireNonNull(
                            getClass().getClassLoader().getResourceAsStream("executables/jini-cli.exe")
                    ),
                    outputDirectory.get().file(cliFilename.get()).getAsFile().toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }
}
