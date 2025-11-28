package de.livingmainframe.plugins.cobol.core.tasks;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

/**
 * This task copies and renames all dependent source includes into a temporary folder. This is a workaround because the
 * COBOL compiler (at least from IBM) doesn't support finding lower-case copybooks. This isn't ideal but a valid
 * workaround because it uses Gradle's dependency tracking system. This task is only executed if any of the
 * dependencies change
 */
public abstract class RenameSourceIncludesTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getLowerCaseSourceIncludes();

    @OutputFiles
    public abstract ConfigurableFileCollection getUpperCaseTemporarySourceIncludes();

    @TaskAction
    public void renameSourceIncludes() {

        for (int i = 0; i < getLowerCaseSourceIncludes().getFiles().size(); i++) {
            try {
                FileUtils.copyFile(getLowerCaseSourceIncludes().getFiles().stream().toList().get(i),
                        getUpperCaseTemporarySourceIncludes().getFiles().stream().toList().get(i));
                getLogger().info("Copied {} to {}", getLowerCaseSourceIncludes().getFiles().stream().toList().get(i),
                        getUpperCaseTemporarySourceIncludes().getFiles().stream().toList().get(i));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
