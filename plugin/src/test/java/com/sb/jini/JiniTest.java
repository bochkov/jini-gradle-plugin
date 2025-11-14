package com.sb.jini;

import com.google.gson.Gson;
import com.sb.jini.data.PackageDescription;
import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

class JiniTest {

    @TempDir
    File projectDir;

    private File getSettingsFile() {
        return new File(projectDir, "settings.gradle");
    }

    private File getBuildFile() {
        return new File(projectDir, "build.gradle");
    }

    @Test
    void testRun() throws IOException {
        try (FileWriter fw = new FileWriter(getSettingsFile())) {
            fw.write(""); // single-project build
        }

        File resourcesDir = new File(projectDir, "src/main/resources");
        Files.createDirectories(resourcesDir.toPath());
        Files.copy(
                Objects.requireNonNull(JiniTest.class.getResourceAsStream("/jini-test.ico")),
                new File(resourcesDir, "jini-test.ico").toPath()
        );

        try (FileWriter fw = new FileWriter(getBuildFile())) {
            fw.write("""
                    plugins {
                        id 'java'
                        id 'application'
                        id 'com.sb.jini'
                    }
                    
                    jini {
                        mainClassName = 'com.sb.Test'
                        exeFilename = 'jini-test.exe'
                        vmArgs = ['-Xmx64m']
                        icon = "${projectDir}/src/main/resources/jini-test.ico"
                        fileVersion = "1.0.0.959"
                        productVersion = "1.0.0.959"
                        comments = "Jini Test GUI"
                        fileDescription = "Jini Test GUI"
                        productName = "Jini Test GUI"
                        copyright = "Bochkov Sergey"
                    }
                    """);
        }

        BuildResult jini = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("jini")
                .forwardOutput()
                .build();
        BuildTask task = jini.task(":jini");

        Assertions.assertThat(task).isNotNull();
        Assertions.assertThat(task.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

        File jiniDir = new File(projectDir, "build/jini");
        File execFile = new File(jiniDir, "jini-test.exe");
        Assertions.assertThat(execFile).exists().isFile();
        File libDir = new File(jiniDir, "lib");
        Assertions.assertThat(libDir).exists().isDirectory();
        File packageInfo = new File(libDir, "package.json");
        Assertions.assertThat(packageInfo).exists().isFile();

        Gson gson = new Gson();
        PackageDescription desc;
        try (FileReader infoReader = new FileReader(packageInfo)) {
            desc = gson.fromJson(infoReader, PackageDescription.class);
        }
        Assertions.assertThat(desc.getMainClass()).isEqualTo("com.sb.Test");
        Assertions.assertThat(desc.getClassPath()).isNotEmpty();
        Assertions.assertThat(desc.getArgs()).isEmpty();
        Assertions.assertThat(desc.getVmArgs()).contains("-Xmx64m");
    }

}
