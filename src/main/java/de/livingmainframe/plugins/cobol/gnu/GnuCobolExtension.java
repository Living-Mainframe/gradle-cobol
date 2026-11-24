package de.livingmainframe.plugins.cobol.gnu;

import de.livingmainframe.plugins.cobol.core.CobolExtension;

public abstract class GnuCobolExtension extends CobolExtension {

    private static final String gradleTaskGroupName = "gnu-cobol";

    @Override
    public String getGradleTaskGroupName() {
        return gradleTaskGroupName;
    }

    @Override
    public Class<?> getCobolCompilerClass() {
        return GnuCompilerExecutable.class;
    }
}
