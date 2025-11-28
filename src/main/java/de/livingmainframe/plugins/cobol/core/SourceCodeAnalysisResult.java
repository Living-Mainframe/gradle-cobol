package de.livingmainframe.plugins.cobol.core;

import java.io.File;
import java.util.List;

public class SourceCodeAnalysisResult {
    public void setCics(boolean cics) {
        this.cics = cics;
    }

    public void setDb2(boolean db2) {
        this.db2 = db2;
    }

    public void setSourceDependencies(List<File> sourceDependencies) {
        this.sourceDependencies = sourceDependencies;
    }

    private boolean cics;
    private boolean db2;
    private List<File> sourceDependencies;

    public SourceCodeAnalysisResult() {
        this.cics = false;
        this.db2 = false;
        this.sourceDependencies = List.of();
    }

    public SourceCodeAnalysisResult(boolean cics, boolean db2, List<File> sourceDependencies) {
        this.cics = cics;
        this.db2 = db2;
        this.sourceDependencies = sourceDependencies;
    }

    public boolean isCics() {
        return cics;
    }

    public boolean isDb2() {
        return db2;
    }

    public List<File> getSourceDependencies() {
        return sourceDependencies;
    }
}
