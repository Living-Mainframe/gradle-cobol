package de.livingmainframe.plugins.cobol.gnu;

import de.livingmainframe.plugins.cobol.core.compiler.CobolCompilerExecutable;

import java.nio.file.Path;
import java.util.List;

public class GnuCompilerExecutable extends CobolCompilerExecutable {
    public GnuCompilerExecutable(Path buildFile, Path outputDirectory) {
        super(buildFile, outputDirectory);
    }

    @Override
    public String getNameOfCompilerExecutable() {
        return "cobc";
    }

    @Override
    public List<String> getSpecialSourceIncludeArguments() {
        return List.of("-ffold-copy=LOWER");
    }

    @Override
    public List<String> getCicsCompilerOptions() {
        return List.of();
    }

    @Override
    public List<String> getDb2CompilerOptions() {
        return List.of();
    }

    @Override
    public List<String> getCustomCompilerOptions() {
        return List.of("-x", "-std=ibm");
    }
}
