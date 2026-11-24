package de.livingmainframe.plugins.cobol.core;

import de.livingmainframe.plugins.cobol.core.dsl.ModuleGroup;
import de.livingmainframe.plugins.cobol.core.dsl.ModuleOptions;
import de.livingmainframe.plugins.cobol.core.tasks.DynamicBuildTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.problems.ProblemId;
import org.gradle.api.problems.Problems;
import org.gradle.api.problems.SecondLevelProblemGroup;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public abstract class CobolPlugin implements Plugin<Project> {

    @Inject
    protected abstract Problems getProblems();

    @Override
    public void apply(@NotNull Project project) {
        NamedDomainObjectContainer<ModuleGroup> moduleGroups = project.getObjects()
                .domainObjectContainer(ModuleGroup.class);
        project.getExtensions().add("moduleGroups", moduleGroups);

        project.afterEvaluate(target -> configureBuildTasks(target, moduleGroups));
    }

    private void configureBuildTasks(Project target, NamedDomainObjectContainer<ModuleGroup> moduleGroups) {
        SecondLevelProblemGroup problemGroup = getProblems().getGroups().getCompilation().group("COBOL Base Plugin");
        CobolExtensionInterface cobolExtension = (CobolExtensionInterface) target.getExtensions().getByName("cobol");

        HashMap<String, ModuleOptions> moduleOptions = new HashMap<>();
        for (ModuleGroup moduleGroup : moduleGroups) {
            moduleOptions.putAll(moduleGroup.getFlatModules());
        }

        List<File> sourceIncludeDirectories = new ArrayList<>(cobolExtension.getSourceIncludePaths().get().stream()
                .map(path -> resolve(target, path).toFile()).distinct().toList());

        for (Path sourcePath : cobolExtension.getSourcePaths().get()) {
            File projectSrcDirectory = resolve(target, sourcePath).toFile();
            if (!projectSrcDirectory.exists()) {
                ProblemId problemId = problemGroup.problemId("The source directory doesn't exist");
                throw getProblems().getReporter().throwing(
                        new FileNotFoundException("The source directory doesn't " + "exist"), problemId,
                        problemSpec -> {
                            problemSpec
                                    .details(String.format("The source directory %s doesn't exist", projectSrcDirectory.getPath()));
                            problemSpec.solution("Make sure that the source directory exists");
                        });
            }

            if (!projectSrcDirectory.isDirectory()) {
                ProblemId problemId = problemGroup.problemId("The source directory is a file");
                throw getProblems().getReporter().throwing(new RuntimeException("The source directory is a file"),
                        problemId, problemSpec -> {
                            problemSpec.details(String.format("The source directory %s is a file", projectSrcDirectory.getPath()));
                            problemSpec
                                    .solution("Only directories can be specified. Specifying individual files is not " +
                                            "allowed");
                        });
            }

            Set<File> filesInSourceDirectory = new TreeSet<>(Comparator.comparing(File::getAbsolutePath));
            filesInSourceDirectory.addAll(target.fileTree(projectSrcDirectory).getFiles().stream().filter(file -> {
                String name = file.getName();
                int dot = name.lastIndexOf('.');
                return dot >= 0 && cobolExtension.getSourceExtensions().get().stream()
                        .anyMatch(ext -> name.substring(dot + 1).equalsIgnoreCase(ext));
            }).toList());

            Map<String, File> programNames = new HashMap<>();
            for (File sourceFile : filesInSourceDirectory) {
                String programName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.'));

                File previous = programNames.putIfAbsent(programName.toLowerCase(Locale.ROOT), sourceFile);
                if (previous != null) {
                    ProblemId problemId = problemGroup.problemId("Duplicate program names");
                    throw getProblems().getReporter().throwing(
                            new RuntimeException("Duplicate program names are not allowed"), problemId, problemSpec -> {
                                problemSpec.details(String.format(
                                        "The program name %s exists twice which isn't supported. Program names must be distinct. Locations: %s, %s",
                                        programName, previous.getAbsolutePath(), sourceFile.getAbsolutePath()));
                                problemSpec.solution("Make sure that program names are distinct inside the repository");
                            });
                }

                CobolSourceAnalyzer cobolSourceAnalyzer = new CobolSourceAnalyzer(sourceFile,
                        Charset.forName(cobolExtension.getSourceEncoding().get()));

                final SourceCodeAnalysisResult analysisResult = cobolSourceAnalyzer
                        .analyzeSource(sourceIncludeDirectories);

                Logger logger = target.getLogger();
                logger.info("Identified {} to use Db2: {}, CICS: {}", programName, analysisResult.isDb2(),
                        analysisResult.isCics());

                final ModuleOptions options;
                if (moduleOptions.get(sourceFile.toString()) == null) {
                    options = new ModuleOptions(target.getObjects());
                } else {
                    options = moduleOptions.get(sourceFile.toString());
                }

                options.setCics(analysisResult.isCics());
                options.setDb2(analysisResult.isDb2());

                target.getTasks().register(String.format("build-%s", programName), DynamicBuildTask.class, task -> {
                    task.getBuildFile().set(sourceFile);
                    task.getOutputDirectory().convention(target.getLayout().getBuildDirectory().dir(programName));
                    task.setCobolCompilerExecutable(cobolExtension.getCobolCompilerClass());
                    task.setEnvironmentVariables(cobolExtension.getEnvironmentVariables().get());
                    task.setModuleOptions(options);
                    task.getSourceIncludeDependencies().setFrom(analysisResult.getSourceDependencies());
                    task.setGroup(cobolExtension.getGradleTaskGroupName());
                });
                target.getLogger().info("Build file: {}: {}", sourceFile);
            }
        }
    }

    private Path resolve(Project project, Path path) {
        return project.getLayout().getProjectDirectory().getAsFile().toPath().resolve(path).toAbsolutePath()
                .normalize();
    }
}
