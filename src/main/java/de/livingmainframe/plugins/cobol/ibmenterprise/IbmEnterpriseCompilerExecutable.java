package de.livingmainframe.plugins.cobol.ibmenterprise;

import de.livingmainframe.plugins.cobol.core.compiler.CobolCompilerExecutable;

import java.nio.file.Path;
import java.util.List;

public class IbmEnterpriseCompilerExecutable extends CobolCompilerExecutable {
    public IbmEnterpriseCompilerExecutable(Path buildFile, Path outputDirectory) {
        super(buildFile, outputDirectory);
    }

    @Override
    public List<String> getCustomCompilerOptions() {
        return List.of();
    }

    @Override
    public String getNameOfCompilerExecutable() {
        return "cob2";
    }

    @Override
    public List<String> getSpecialSourceIncludeArguments() {
        return List.of();
    }

    @Override
    public List<String> getCicsCompilerOptions() {
        if (getModuleOptions().getCics()) {
            return List.of("-qCICS", "-qCOPYLOC(SYSLIB,DSN(CICSTS61.CICS.SDFHCOB))", "-LCICSTS61.CICS.SDFHLOAD");
        } else {
            return List.of();
        }
    }

    @Override
    public List<String> getDb2CompilerOptions() {
        if (getModuleOptions().getDb2()) {
            return List.of("-qSQL", "-dbrmlib");
        } else {
            return List.of();
        }
    }
}
