package de.livingmainframe.plugins.cobol.core.compiler;

import de.livingmainframe.plugins.cobol.core.dsl.ModuleOptions;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class CobolCompilerExecutable {

    private final Path buildFile;
    private final Path outputDirectory;
    private Path pathOfCompilerExecutable;
    private List<Path> sourceIncludePaths;
    private Map<String, String> environmentVariables = new LinkedHashMap<>();
    private ModuleOptions moduleOptions;

    public CobolCompilerExecutable(Path buildFile, Path outputDirectory) {
        this.buildFile = buildFile;
        this.outputDirectory = outputDirectory;
    }

    private String getModuleName() {
        return FilenameUtils.getBaseName(buildFile.toString());
    }

    public void setSourceIncludePaths(List<Path> sourceIncludePaths) {
        this.sourceIncludePaths = sourceIncludePaths;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    /**
     * In case the compiler executable can't be found on the $PATH or should be called from a specific directory (e.g.,
     * to test new versions) the path can be specified through this setter method.
     *
     * @param pathOfCompilerExecutable The UNIX path where the compiler executable (the bin directory) resides
     */
    public void setPathOfCompilerExecutable(Path pathOfCompilerExecutable) {
        this.pathOfCompilerExecutable = pathOfCompilerExecutable;
    }

    public List<String> getBuildCommand() {
        List<String> command = new ArrayList<>();
        command.add(getFullyQualifiedCompilerExecutablePath());
        command.addAll(getCustomCompilerOptions());
        command.addAll(getDb2CompilerOptions());
        command.addAll(getCicsCompilerOptions());
        command.addAll(generateSourceIncludePathArguments());
        command.addAll(generateOutputArguments());
        command.add(buildFile.toString());
        return command;
    }

    private @NotNull List<String> generateSourceIncludePathArguments() {
        List<String> arguments = new ArrayList<>();
        if (sourceIncludePaths != null && !sourceIncludePaths.isEmpty()) {
            arguments.addAll(getSpecialSourceIncludeArguments());
            arguments.addAll(sourceIncludePaths.stream().map(item -> "-I" + item).toList());
        }
        return arguments;
    }

    private @NotNull List<String> generateOutputArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add("-o");
        arguments.add(outputDirectory.resolve(getModuleName()).toString());
        return arguments;
    }

    /**
     * Returns the full path to the compiler executable if a `pathOfCompilerExecutable` has been specified.
     * Else, it simply returns the name of the executable which means that it will be searched from the $PATH during
     * execution.
     *
     * @return Full path of the compiler executable if it isn't chosen from $PATH
     */
    private String getFullyQualifiedCompilerExecutablePath() {
        if (pathOfCompilerExecutable == null) {
            return getNameOfCompilerExecutable();
        } else {
            return pathOfCompilerExecutable.resolve(getNameOfCompilerExecutable()).toString();
        }
    }

    public abstract List<String> getCustomCompilerOptions();

    public abstract String getNameOfCompilerExecutable();

    public abstract List<String> getSpecialSourceIncludeArguments();

    public abstract List<String> getCicsCompilerOptions();

    public abstract List<String> getDb2CompilerOptions();

    public ModuleOptions getModuleOptions() {
        return moduleOptions;
    }

    public void setModuleOptions(ModuleOptions moduleOptions) {
        this.moduleOptions = moduleOptions;
    }
}
