package de.livingmainframe.plugins.cobol.core.dsl;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ModuleOptions {
    private final Property<Boolean> db2;
    private final Property<Boolean> cics;
    private List<String> compilerOptions = new ArrayList<>();

    @Inject
    public ModuleOptions(ObjectFactory objectFactory) {
        this.db2 = objectFactory.property(Boolean.class);
        this.db2.convention(false);
        this.cics = objectFactory.property(Boolean.class);
        this.cics.convention(false);
    }

    public List<String> getCompilerOptions() {
        return compilerOptions;
    }

    public void setCompilerOptions(List<String> compilerOptions) {
        this.compilerOptions = compilerOptions;
    }

    public boolean getCics() {
        return cics.get();
    }

    public void setCics(boolean cics) {
        this.cics.set(cics);
    }

    public boolean getDb2() {
        return db2.get();
    }

    public void setDb2(boolean db2) {
        this.db2.set(db2);
    }

}
