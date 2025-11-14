package com.sb.jini;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class JiniPlugin implements Plugin<@NotNull Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("jini", JiniExtension.class, project);
        project.getTasks().register("jini", JiniTask.class, task -> {
            task.setGroup("jini");
            task.setDescription("Create executable package");
        });
    }
}
