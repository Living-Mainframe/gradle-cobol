package de.livingmainframe.plugins.cobol.ibmenterprise;

import de.livingmainframe.plugins.cobol.core.CobolPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class IbmEnterpriseCobolPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project project) {
        project.getExtensions().create("cobol", IbmEnterpriseCobolExtension.class);

        project.getPluginManager().apply(CobolPlugin.class);
    }
}
