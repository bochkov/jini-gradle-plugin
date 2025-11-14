package com.sb.jini;

import lombok.Getter;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class JiniExtension {

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
    @Optional
    private final SetProperty<@NotNull String> vmArgs;

    @Input
    @Getter
    @Optional
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

    public JiniExtension(Project project) {
        mainClassName = project.getObjects().property(String.class);
        exeFilename = project.getObjects().property(String.class);
        cliFilename = project.getObjects().property(String.class);
        cliFilename.convention((String) null);
        vmArgs = project.getObjects().setProperty(String.class);
        vmArgs.convention(Collections.emptyList());
        args = project.getObjects().setProperty(String.class);
        args.convention(Collections.emptyList());
        icon = project.getObjects().property(String.class);
        icon.convention((String) null);
        fileVersion = project.getObjects().property(String.class);
        fileVersion.convention((String) null);
        productVersion = project.getObjects().property(String.class);
        productVersion.convention((String) null);
        comments = project.getObjects().property(String.class);
        comments.convention((String) null);
        fileDescription = project.getObjects().property(String.class);
        fileDescription.convention((String) null);
        productName = project.getObjects().property(String.class);
        productName.convention((String) null);
        copyright = project.getObjects().property(String.class);
        copyright.convention((String) null);
    }

}
