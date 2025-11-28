package de.livingmainframe.plugins.cobol.core;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;

import java.nio.file.Path;

public interface CobolExtensionInterface {

    /**
     * Specify the directories which contain COBOL modules that must be built
     *
     * @return All directories that include COBOL modules
     */
    ListProperty<Path> getSourcePaths();

    /**
     * Include files are assumed to be in src/copy. If the project requires a different
     * setup this can be overridden. In that case all paths must be specified (also the default)
     *
     * @return All paths that include source includes (copybooks)
     */
    ListProperty<Path> getSourceIncludePaths();

    /**
     * Allows the configuration of environment variables that are passed to the compiler
     *
     * @return A Map containing all the environment variables
     */
    //TODO: This property was created because I didn't know how I correctly pass the value for the COBOL compiler
    // data set that can be specified through a property into a Map (that is eventually passed to the ExecOperation).
    // It should be specific to the IbmEnterpriseCobolPlugin
    MapProperty<String, String> getEnvironmentVariables();

    /**
     * The group name for Gradle to group all the build tasks into
     *
     * @return String returning the name of the group to be created
     */
    String getGradleTaskGroupName();

    /**
     * The name of the class that represent the COBOL compiler which should extend CobolCompilerExecutable class
     *
     * @return Name of the compiler implementation class
     */
    Class<?> getCobolCompilerClass();
}
