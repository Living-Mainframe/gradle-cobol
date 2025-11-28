package de.livingmainframe.plugins.cobol.core.dsl;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ModuleGroup implements Named {

    private final String name;
    private final ModuleOptions options;
    private ConfigurableFileCollection modules;

    @Inject
    public ModuleGroup(Project project, ObjectFactory objectFactory, String name) {
        this.name = name;
        this.modules = project.files();
        this.options = new ModuleOptions(objectFactory);
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    public ConfigurableFileCollection getModules() {
        return modules;
    }

    public void setModules(ConfigurableFileCollection modules) {
        this.modules = modules;
    }

    public ModuleOptions getOptions() {
        return options;
    }

    public void options(Action<? super ModuleOptions> action) {
        action.execute(options);
    }

    public HashMap<String, ModuleOptions> getFlatModules() {
        HashMap<String, ModuleOptions> flatModules = new HashMap<>();
        for (File file : this.modules.getFiles()) {
            flatModules.put(file.toString(), this.options);
        }
        return flatModules;
    }
}
