import de.livingmainframe.plugins.cobol.gnu.GnuCompilerExecutable;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class tests both implementations of CobolCompilerExecutable that currently exist because the class itself is
 * abstract
 * and shouldn't be tested:
 * IBMEnterpriseCompilerExecutable and GnuCompilerExecutable
 */
public class CobolCompilerExecutableTest {

    @TempDir
    File tempDirectory;
    Path buildFilePath;
    Path outputDirectoryPath;
    GnuCompilerExecutable gnuCompilerExecutable;

    @BeforeEach
    public void setup() throws IOException {
        buildFilePath = File.createTempFile("test", ".cbl").toPath();
        outputDirectoryPath = tempDirectory.toPath().resolve("output");

        gnuCompilerExecutable = new GnuCompilerExecutable(buildFilePath, outputDirectoryPath);
    }

    @Test
    public void testSimpleGnuBuildCommand() {
        List<String> command = gnuCompilerExecutable.getBuildCommand();

        // The compiler binary must always be the first argument to be fed into Gradle's ExecOperation class
        assertEquals("cobc", command.getFirst());
        // A simple GNU build command without any further options should contain 6 element
        assertEquals(6, command.size());
        assertEquals(FilenameUtils.getBaseName(buildFilePath.toString()), FilenameUtils.getBaseName(command.get(4)));
        // The file to be build must always be the last parameter
        assertEquals(buildFilePath.toString(), command.getLast());
    }

    @Test
    public void testCustomCompilerExecutablePath() {
        Path pathToCompilerExecutable = new File("/tmp/path/to/compiler").toPath();

        gnuCompilerExecutable.setPathOfCompilerExecutable(pathToCompilerExecutable);

        List<String> command = gnuCompilerExecutable.getBuildCommand();

        // The first parameter should include the pathToCompilerExecutable path as well as the executable itself
        assertEquals(pathToCompilerExecutable.resolve("cobc").toString(), command.getFirst());
    }

    @Test
    public void testSourceIncludePaths() {
        Path pathToSourceIncludeDirectory1 = new File("tmp/path/to/copybook1").toPath();
        Path pathToSourceIncludeDirectory2 = new File("tmp/path/to/copybook2").toPath();

        gnuCompilerExecutable.setSourceIncludePaths(List.of(pathToSourceIncludeDirectory2,
                pathToSourceIncludeDirectory1));
        List<String> command = gnuCompilerExecutable.getBuildCommand();
        List<String> includeArguments = command.stream().filter(s -> s.startsWith("-I")).toList();

        // When adding an include path there are (1 + n) more arguments for GNU Cobol
        assertEquals(9, command.size());
        // Make sure that pathToSourceIncludeDirectory2 is found first (order is important)
        // When we migrated to ConfigurableFileCollection we migrated from a List to a Set implementation. Sets don't
        // have any order so it is impossible to guarantee what is included first.
        // TODO: Discuss whether order is important. It can be compared to the Java ClassPath which also follows a
        //  certain order. For copybooks we are used to have the same mechanic so it may be important to keep this.
        assertEquals(String.format("-I%s", pathToSourceIncludeDirectory2), includeArguments.getFirst());
        assertEquals(String.format("-I%s", pathToSourceIncludeDirectory1), includeArguments.get(1));
    }

    @Test
    public void testNoSourceIncludePaths() {
        gnuCompilerExecutable.setSourceIncludePaths(List.of());
        List<String> command = gnuCompilerExecutable.getBuildCommand();
        // When adding an empty list, no additional arguments should be added
        assertEquals(6, command.size());
    }
}
