package de.livingmainframe.plugins.cobol.gnu;

import de.livingmainframe.plugins.cobol.core.CobolExtensionInterface;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class GnuCobolExtension implements CobolExtensionInterface {

    private static final String gradleTaskGroupName = "gnu-cobol";

    private final ListProperty<Path> sourcePaths;
    private final ListProperty<Path> sourceIncludePaths;

    @Inject
    public GnuCobolExtension(ObjectFactory objectFactory) {
        this.sourcePaths = objectFactory.listProperty(Path.class);
        List<Path> defaultSourcePaths = Collections.singletonList(new File("src/cobol").toPath());
        this.sourcePaths.convention(defaultSourcePaths);

        this.sourceIncludePaths = objectFactory.listProperty(Path.class);
        Collection<Path> defaultSourceIncludePaths = Collections.singletonList(new File("src/copy").toPath());
        this.sourceIncludePaths.convention(defaultSourceIncludePaths);
    }

    @Override
    public String getGradleTaskGroupName() {
        return gradleTaskGroupName;
    }

    @Override
    public ListProperty<Path> getSourceIncludePaths() {
        return sourceIncludePaths;
    }

    @Override
    public ListProperty<Path> getSourcePaths() {
        return sourcePaths;
    }

    @Override
    public Class<?> getCobolCompilerClass() {
        return GnuCompilerExecutable.class;
    }
}
