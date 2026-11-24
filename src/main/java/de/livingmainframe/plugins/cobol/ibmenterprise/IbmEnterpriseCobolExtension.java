package de.livingmainframe.plugins.cobol.ibmenterprise;

import de.livingmainframe.plugins.cobol.core.CobolExtension;

public abstract class IbmEnterpriseCobolExtension extends CobolExtension {

    private static final String gradleTaskGroupName = "ibm-cobol";

    @Override
    public String getGradleTaskGroupName() {
        return gradleTaskGroupName;
    }

    @Override
    public Class<?> getCobolCompilerClass() {
        return IbmEnterpriseCompilerExecutable.class;
    }
}
