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
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class JiniTask extends DefaultTask {

    private static final String LIB_DIR = "lib";
    private static final String PACKAGE_DESC_FILE = "package.json";
    private static final String RC_EDIT = "rcedit.exe";

    private final FileCollection classFiles;
    private final Property<@NotNull FileCollection> jarFiles;

    @Input
    @Getter
    private final Property<@NotNull String> mainClassName;
    @Input
    @Getter
    private final Property<@NotNull String> exeFilename;
    @Input
    @Getter
    @Optional
    private final Property<@NotNull String> cliFilename;
    @Input
    @Getter
    private final SetProperty<@NotNull String> vmArgs;
    @Input
    @Getter
    private final SetProperty<@NotNull String> args;
    @Input
    @Getter
    @Optional
    private final Property<@NotNull String> icon;
    @Input
    @Getter
    @Optional
    private final Property<@NotNull String> fileVersion;
    @Input
    @Getter
    @Optional
    private final Property<@NotNull String> productVersion;
    @Input
    @Getter
    @Optional
    private final Property<@NotNull String> comments;
    @Input
    @Getter
    @Optional
    private final Property<@NotNull String> fileDescription;
    @Input
    @Getter
    @Optional
    private final Property<@NotNull String> productName;
    @Input
    @Getter
    @Optional
    private final Property<@NotNull String> copyright;

    @Getter
    @OutputDirectory
    private final DirectoryProperty outputDirectory;
    @Getter
    @OutputDirectory
    private final Provider<@NotNull Directory> cpDirectory;

    public JiniTask() {
        JiniExtension ext = getProject().getExtensions().getByType(JiniExtension.class);
        classFiles = getProject().getConfigurations().getByName("runtimeClasspath");
        jarFiles = getProject().getObjects().property(FileCollection.class);
        getProject().getPluginManager().withPlugin("java", ap -> {
            TaskProvider<@NotNull Task> named = getProject().getTasks().named(JavaPlugin.JAR_TASK_NAME);
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

        icon = getProject().getObjects().property(String.class);
        icon.convention(ext.getIcon());
        fileVersion = getProject().getObjects().property(String.class);
        fileVersion.convention(ext.getFileVersion());
        productVersion = getProject().getObjects().property(String.class);
        productVersion.convention(ext.getProductVersion());
        comments = getProject().getObjects().property(String.class);
        comments.convention(ext.getComments());
        fileDescription = getProject().getObjects().property(String.class);
        fileDescription.convention(ext.getFileDescription());
        productName = getProject().getObjects().property(String.class);
        productName.convention(ext.getProductName());
        copyright = getProject().getObjects().property(String.class);
        copyright.convention(ext.getCopyright());

        ProjectLayout layout = getProject().getLayout();

        outputDirectory = getProject().getObjects()
                .directoryProperty()
                .convention(layout.getBuildDirectory().dir("jini"));
        cpDirectory = outputDirectory.dir(LIB_DIR);
    }

    @TaskAction
    public void run() throws IOException {
        Files.createDirectories(cpDirectory.get().getAsFile().toPath());
        copyLibs();
        genPackageDesc();
        writeExec();
        setExecResources();
        cleanup();
    }

    /**
     * copy libs to out directory
     */
    private void copyLibs() {
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
    }

    /**
     * generate and copy package description
     */
    private void genPackageDesc() throws IOException {
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
    }

    /**
     * write executables
     */
    private void writeExec() throws IOException {
        Files.copy(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream("executables/jini.exe")
                ),
                outputDirectory.get().file(exeFilename.get()).getAsFile().toPath(),
                StandardCopyOption.REPLACE_EXISTING
        );
        Files.copy(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream("executables/" + RC_EDIT)
                ),
                new File(outputDirectory.get().getAsFile(), RC_EDIT).toPath(),
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

    /**
     * set exec resources - version, description, icon, etc.
     */
    private void setExecResources() throws IOException {
        List<String> args = new ArrayList<>();
        if (fileVersion.isPresent()) {
            args.add("--set-file-version");
            args.add(String.format("\"%s\"", fileVersion.get()));
        }
        if (productVersion.isPresent()) {
            args.add("--set-product-version");
            args.add(String.format("\"%s\"", productVersion.get()));
        }
        if (productName.isPresent()) {
            args.add("--set-version-string");
            args.add("ProductName");
            args.add(String.format("\"%s\"", productName.get()));
        }
        if (fileDescription.isPresent()) {
            args.add("--set-version-string");
            args.add("\"FileDescription\"");
            args.add(String.format("\"%s\"", fileDescription.get()));
        }
        if (comments.isPresent()) {
            args.add("--set-version-string");
            args.add("\"Comments\"");
            args.add(String.format("\"%s\"", comments.get()));
        }
        if (copyright.isPresent()) {
            args.add("--set-version-string");
            args.add("\"LegalCopyright\"");
            args.add(String.format("\"%s\"", copyright.get()));
        }
        if (icon.isPresent()) {
            args.add("--set-icon");
            args.add(String.format("\"%s\"", icon.get()));
        }
        if (!args.isEmpty()) {
            args.add(0, RC_EDIT);
            args.add(1, exeFilename.get());
            System.out.println(String.join(" ", args));
            ProcessBuilder pcb = new ProcessBuilder(args);
            Process p = pcb
                    .directory(outputDirectory.get().getAsFile())
                    .start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                int res = p.waitFor();
                if (res != 0) {
                    throw new IOException(output.toString());
                }
            } catch (InterruptedException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }
    }

    private void cleanup() throws IOException {
        Files.deleteIfExists(outputDirectory.get().file(RC_EDIT).getAsFile().toPath());
    }
}
