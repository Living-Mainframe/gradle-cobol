package de.livingmainframe.plugins.cobol.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Common conventions; concrete variants select their executable and task type. */
public abstract class CobolExtension implements CobolExtensionInterface {
    public CobolExtension() {
        getSourcePaths().set(List.of(Path.of("src/cobol")));
        getSourceIncludePaths().set(List.of(Path.of("src/copy")));
        getSourceExtensions().set(List.of("cbl", "CBL", "cob", "COB"));
        getCopybookExtensions().set(List.of("", "cpy", "CPY"));
        getSourceEncoding().convention("UTF-8");
        getEnvironmentVariables().convention(Map.of());
    }
}
