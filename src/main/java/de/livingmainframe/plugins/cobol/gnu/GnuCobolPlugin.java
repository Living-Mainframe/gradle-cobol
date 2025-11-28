package de.livingmainframe.plugins.cobol.gnu;

import de.livingmainframe.plugins.cobol.core.CobolPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public abstract class GnuCobolPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project project) {
        project.getExtensions().create("cobol", GnuCobolExtension.class);

        project.getPluginManager().apply(CobolPlugin.class);
    }
}
