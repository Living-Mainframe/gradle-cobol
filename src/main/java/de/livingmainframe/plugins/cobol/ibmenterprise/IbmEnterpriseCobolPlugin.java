package de.livingmainframe.plugins.cobol.ibmenterprise;

import de.livingmainframe.plugins.cobol.core.CobolPlugin;
import de.livingmainframe.plugins.cobol.core.tasks.DynamicBuildTask;
import de.livingmainframe.plugins.cobol.core.tasks.RenameSourceIncludesTask;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

@SuppressWarnings("unused")
public abstract class IbmEnterpriseCobolPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project project) {
        project.getExtensions().create("cobol", IbmEnterpriseCobolExtension.class);

        project.getPluginManager().apply(CobolPlugin.class);

        project.afterEvaluate(target -> {
            // This is the workaround we use to rename the source includes to uppercase. Two things are happening here
            // 1. Register the RenameSourceIncludesTask for each build task that handles the renaming
            // 2. Wire both tasks to be dependent on each other by changing the inputs of the build task to the outputs
            //    from the RenameSourceIncludeTask
            // TODO: Check if any of this introduces eager configuration and whether this can be improved
            for (DynamicBuildTask task : project.getTasks().withType(DynamicBuildTask.class)) {
                List<File> lowerCaseDependencyFiles = task.getSourceIncludeDependencies().getFiles().stream().toList();
                List<File> upperCaseDependencyFiles =
                        transformUppercaseDependencyPaths(project.getLayout().getBuildDirectory().getAsFile().get(),
                                lowerCaseDependencyFiles);
                TaskProvider<RenameSourceIncludesTask> renameTask = project.getTasks().register(String.format("rename"
                        + "-dependencies-%s", task.getName().substring(6)), RenameSourceIncludesTask.class,
                        renameSourceIncludesTask -> {
                    renameSourceIncludesTask.getLowerCaseSourceIncludes().setFrom(lowerCaseDependencyFiles);
                    renameSourceIncludesTask.getUpperCaseTemporarySourceIncludes().setFrom(upperCaseDependencyFiles);
                });

                // This wires the outputs from the RenameSourceIncludeTask to the DynamicBuildTask. I don't know
                // exactly why this works, but it was suggested over at the Gradle forums to me
                // (see: https://discuss.gradle.org/t/gradle-doesnt-recognize-files-as-dependencies/50428/)
                // I thought it should simply be possible by declaring my ConfigurableFileCollections as @InputFiles
                // and @OutputFiles. This doesn't work and an issue was opened over at GitHub which may implement
                // this in the future: https://github.com/gradle/gradle/issues/32311
                task.getSourceIncludeDependencies().setFrom(renameTask.map(RenameSourceIncludesTask::getUpperCaseTemporarySourceIncludes));
            }
        });
    }

    /**
     * Changes the list of files to a temporary location in Gradle's build directory. This is necessary because the
     * IBM COBOL compiler can't be told to search for lowercase copybooks
     *
     * @param buildDirectory Gradle's base build directory
     * @param originalFiles  The list of files that are still lowercase and include the extension
     * @return A transformed list of files with a different path, uppercase filenames, removed extension
     */
    private List<File> transformUppercaseDependencyPaths(File buildDirectory, @NotNull List<File> originalFiles) {

        return originalFiles.stream().map(file -> {
            String newName = FilenameUtils.getBaseName(file.getAbsolutePath());
            newName = newName.toUpperCase();
            String parentDirectory = file.getParentFile().getName();
            File temporarySourceIncludeDirectory = new File(buildDirectory.getAbsolutePath(),
                    "temporarySourceIncludes");
            return new File(temporarySourceIncludeDirectory, new File(parentDirectory, newName).toString());
        }).toList();
    }
}
