package de.livingmainframe.plugins.cobol.core;

import de.livingmainframe.plugins.cobol.core.dsl.ModuleGroup;
import de.livingmainframe.plugins.cobol.core.dsl.ModuleOptions;
import de.livingmainframe.plugins.cobol.core.tasks.DynamicBuildTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.problems.ProblemGroup;
import org.gradle.api.problems.ProblemId;
import org.gradle.api.problems.Problems;
import org.gradle.api.problems.Severity;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// The Gradle Problems API is still work in progress and thus marked as incubating / unstable.
// With the official release of the API the warnings should not be suppressed anymore.
public abstract class CobolPlugin implements Plugin<Project> {

    public static final ProblemGroup PROBLEM_GROUP = ProblemGroup.create("cobol-base-plugin", "COBOL Base Plugin");

    @Inject
    protected abstract Problems getProblems();

    @Override
    public void apply(@NotNull Project project) {
        NamedDomainObjectContainer<ModuleGroup> moduleGroups = project.container(ModuleGroup.class);
        project.getExtensions().add("moduleGroups", moduleGroups);

        project.afterEvaluate(target -> configureBuildTasks(target, moduleGroups));
    }

    private void configureBuildTasks(Project target, NamedDomainObjectContainer<ModuleGroup> moduleGroups) {
        CobolExtensionInterface cobolExtension = (CobolExtensionInterface) target.getExtensions().getByName("cobol");

        HashMap<String, ModuleOptions> moduleOptions = new HashMap<>();
        for (ModuleGroup moduleGroup : moduleGroups) {
            moduleOptions.putAll(moduleGroup.getFlatModules());
        }

        List<File> sourcePaths = cobolExtension.getSourcePaths().get().stream().map(Path::toFile).toList();
        for (File sourcePath : sourcePaths) {
            // It is required that the source path points to a relative directory inside the project directory
            // For further processing the full path is used
            Path projectSourcePath =
                    target.getLayout().getProjectDirectory().getAsFile().toPath().resolve(sourcePath.toPath());
            File projectSrcDirectory = projectSourcePath.toFile();
            if (!projectSrcDirectory.exists()) {
                ProblemId problemId = ProblemId.create("src-directory-existence", "The source directory doesn't " +
                        "exist", PROBLEM_GROUP);
                throw getProblems().getReporter().throwing(new FileNotFoundException("The source directory doesn't " + "exist"), problemId, problemSpec -> {
                    problemSpec.details(String.format("The source directory %s doesn't exist", projectSourcePath));
                    problemSpec.solution("Make sure that the source directory exists");
                    problemSpec.severity(Severity.ERROR);
                });
            }

            if (!projectSrcDirectory.isDirectory()) {
                ProblemId problemId = ProblemId.create("src-directory-is-file", "The source directory is a file",
                        PROBLEM_GROUP);
                throw getProblems().getReporter().throwing(new RuntimeException("The source directory is a file"),
                        problemId, problemSpec -> {
                    problemSpec.details(String.format("The source directory %s is a file", projectSourcePath));
                    problemSpec.solution("Only directories can be specified. Specifying individual files is not " +
                            "allowed");
                    problemSpec.severity(Severity.ERROR);
                });
            }

            File[] filesInSourceDirectory = projectSrcDirectory.listFiles();

            if (filesInSourceDirectory == null) {
                ProblemId problemId = ProblemId.create("src-directory-readability", "A severe error occurred when " +
                        "trying to read a source directory", PROBLEM_GROUP);
                throw getProblems().getReporter().throwing(new RuntimeException("A severe error occurred when trying " +
                        "to read a source directory"), problemId, problemSpec -> {
                    problemSpec.details(String.format("A severe error occurred when trying to read %s",
                            projectSourcePath));
                    problemSpec.solution("The directory exists but cannot be read. This shouldn't happen");
                    problemSpec.severity(Severity.ERROR);
                });
            }

            for (File buildFile : filesInSourceDirectory) {
                String buildFileName = getFileNameWithoutExtension(buildFile);
                File outputDirectory = target.getLayout().getBuildDirectory().dir(buildFileName).get().getAsFile();

                CobolSourceAnalyzer cobolSourceAnalyzer = new CobolSourceAnalyzer(buildFile);
                List<File> sourceIncludeDirectories =
                        new ArrayList<>(cobolExtension.getSourceIncludePaths().get().stream().map(Path::toFile).toList());
                final SourceCodeAnalysisResult analysisResult =
                        cobolSourceAnalyzer.analyzeSource(sourceIncludeDirectories);

                Logger logger = target.getLogger();
                logger.info("Identified {} to use Db2: {}, CICS: {}", buildFileName, analysisResult.isDb2(),
                        analysisResult.isCics());

                final ModuleOptions options;
                if (moduleOptions.get(buildFile.toString()) == null) {
                    options = new ModuleOptions(target.getObjects());
                } else {
                    options = moduleOptions.get(buildFile.toString());
                }

                options.setCics(analysisResult.isCics());
                options.setDb2(analysisResult.isDb2());

                target.getTasks().register(String.format("build-%s", buildFileName), DynamicBuildTask.class, task -> {
                    task.getBuildFile().set(buildFile);
                    task.getOutputDirectory().set(outputDirectory);
                    task.setCobolCompilerExecutable(cobolExtension.getCobolCompilerClass());
                    task.setEnvironmentVariables(cobolExtension.getEnvironmentVariables().get());
                    task.setModuleOptions(options);
                    task.getSourceIncludeDependencies().setFrom(analysisResult.getSourceDependencies());
                    task.setGroup(cobolExtension.getGradleTaskGroupName());
                });
                target.getLogger().info("Build file: {}, output directory: {}", buildFile, outputDirectory);
            }
        }
    }

    private @NotNull String getFileNameWithoutExtension(@NotNull File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}
