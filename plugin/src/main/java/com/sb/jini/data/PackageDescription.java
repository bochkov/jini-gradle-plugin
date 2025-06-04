package com.sb.jini.data;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Builder
@Getter
public final class PackageDescription {

    @SerializedName("main.class")
    private final String mainClass;

    @SerializedName("class.path")
    private final List<String> classPath;

    @SerializedName("vm.args")
    private final Set<String> vmArgs;

    @SerializedName("args")
    private final Set<String> args;

}
