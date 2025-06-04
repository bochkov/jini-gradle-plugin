package com.sb.jini;

import lombok.Getter;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.util.Collections;

public class JiniExtension {

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
    @Optional
    private final SetProperty<String> vmArgs;
    @Input
    @Getter
    @Optional
    private final SetProperty<String> args;

    public JiniExtension(Project project) {
        mainClassName = project.getObjects().property(String.class);
        exeFilename = project.getObjects().property(String.class);
        cliFilename = project.getObjects().property(String.class);
        cliFilename.convention((String) null);
        vmArgs = project.getObjects().setProperty(String.class);
        vmArgs.convention(Collections.emptyList());
        args = project.getObjects().setProperty(String.class);
        args.convention(Collections.emptyList());
    }

}
