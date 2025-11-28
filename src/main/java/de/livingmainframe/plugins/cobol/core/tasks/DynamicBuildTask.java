package de.livingmainframe.plugins.cobol.core.tasks;

import de.livingmainframe.plugins.cobol.core.compiler.CobolCompilerExecutable;
import de.livingmainframe.plugins.cobol.core.dsl.ModuleOptions;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public abstract class DynamicBuildTask extends DefaultTask {

    private final ExecOperations execOperations;
    private Class<?> cobolCompilerExecutableClass;
    private Map<String, String> environmentVariables;
    private ModuleOptions moduleOptions;

    @Inject
    public DynamicBuildTask(ExecOperations execOperations) {
        this.execOperations = execOperations;
    }

    @InputFile
    abstract public RegularFileProperty getBuildFile();

    @InputFiles
    public abstract ConfigurableFileCollection getSourceIncludeDependencies();

    @OutputDirectory
    abstract public RegularFileProperty getOutputDirectory();

    public void setCobolCompilerExecutable(Class<?> cobolCompilerExecutableClass) {
        this.cobolCompilerExecutableClass = cobolCompilerExecutableClass;
    }

    @TaskAction
    public void compileCobol() {
        Logger logger = getLogger();
        logger.info("Building {}", getBuildFile().get().getAsFile().getAbsolutePath());

        CobolCompilerExecutable compilerExecutable;

        //TODO: I have no idea if this is the proper way of keeping this plugin modular or if this adds to much
        // unmaintainable code. It would also be possible to sub-class DynamicBuildTask and instantiate it with the
        // correct CompilerExecutable class. It needs to be modular (to easily implement other compilers) but also
        // should be maintainable.
        try {
            compilerExecutable =
                    (CobolCompilerExecutable) cobolCompilerExecutableClass.getDeclaredConstructor(Path.class,
                            Path.class).newInstance(getBuildFile().get().getAsFile().toPath(),
                            getOutputDirectory().get().getAsFile().toPath());
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }

        // Calculate the paths that we want included for the compiler based on the dependencies that we found, use
        // the parent directories
        // Make sure this list is distinct to avoid passing the same directory multiple times to the compiler
        List<Path> sourceIncludePaths =
                getSourceIncludeDependencies().getFiles().stream().map(file -> file.getParentFile().toPath()).distinct().toList();

        compilerExecutable.setSourceIncludePaths(sourceIncludePaths);
        compilerExecutable.setEnvironmentVariables(environmentVariables);
        compilerExecutable.setModuleOptions(moduleOptions);
        List<String> command = compilerExecutable.getBuildCommand();

        execOperations.exec(execSpec -> {
            execSpec.commandLine(command);
            // Make sure to not use setEnvironment() as it will override all other variables that are set automatically
            execSpec.environment(compilerExecutable.getEnvironmentVariables());
            execSpec.setStandardOutput(System.out);
            execSpec.setErrorOutput(System.err);
        });
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public void setModuleOptions(ModuleOptions moduleOptions) {
        this.moduleOptions = moduleOptions;
    }
}
