import de.livingmainframe.plugins.cobol.core.SourceCodeAnalysisResult;
import de.livingmainframe.plugins.cobol.core.CobolSourceAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CobolSourceAnalyzerTest {

    private final List<File> includeDirectories = new ArrayList<>();
    private final List<File> copybookFiles = new ArrayList<>();
    @TempDir
    public File testRootDirectory;
    private File sourceFile;

    @BeforeEach
    public void setup() throws IOException {
        sourceFile = File.createTempFile("cobol_test", ".cbl");
        sourceFile.deleteOnExit();

        boolean created;

        File copybookDirectory1 = new File(testRootDirectory, "copy1");
        created = copybookDirectory1.mkdir();
        if (created) {
            includeDirectories.add(copybookDirectory1);
        } else {
            throw new IOException(String.format("Couldn't create %s", copybookDirectory1));
        }
        File copybookDirectory2 = new File(testRootDirectory, "copy2");
        created = copybookDirectory2.mkdir();
        if (created) {
            includeDirectories.add(copybookDirectory2);
        } else {
            throw new IOException(String.format("Couldn't create %s", copybookDirectory2));
        }
        File copybook1 = new File(copybookDirectory2, "testcopy.cpy");
        created = copybook1.createNewFile();
        if (created) {
            copybookFiles.add(copybook1);
        } else {
            throw new IOException(String.format("Couldn't create %s", copybook1));
        }
        File copybook2 = new File(copybookDirectory1, "another1.cpy");
        created = copybook2.createNewFile();
        if (created) {
            copybookFiles.add(copybook2);
        } else {
            throw new IOException(String.format("Couldn't create %s", copybook2));
        }
        File copybook3 = new File(copybookDirectory2, "another1.cpy");
        created = copybook3.createNewFile();
        if (created) {
            copybookFiles.add(copybook3);
        } else {
            throw new IOException(String.format("Couldn't create %s", copybook3));
        }
    }

    @Test
    public void testFindCopyDependencies() throws IOException{
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(sourceFile))) {
            writer.write("       COPY TESTCOPY\n"); // Valid line
            writer.write("       COPY INVALID-NAME\n"); // Invalid line for IBM
            writer.write("      * This is a comment line\n"); // Comment line
            writer.write("       COPY ANOTHER1\n"); // Another valid line
            writer.write("      *COPY ANOTHER2\n"); // Comment line, containing a COPY statement
        } catch (IOException e) {
            throw new IOException(String.format("Couldn't write to %s", sourceFile));
        }

        CobolSourceAnalyzer cobolSourceAnalyzer = new CobolSourceAnalyzer(sourceFile);
        SourceCodeAnalysisResult analysisResult = cobolSourceAnalyzer.analyzeSource(includeDirectories);
        List<File> sourceDependencies = analysisResult.getSourceDependencies();
        assertNotNull(sourceDependencies);

        assertTrue(sourceDependencies.containsAll(List.of(copybookFiles.get(0), copybookFiles.get(1))));
        // Make sure that another1.cpy is not taken from the second directory
        assertFalse(sourceDependencies.contains(copybookFiles.get(2)));
        assertEquals(2, sourceDependencies.size());
    }

    @Test
    public void testFindExecSqlDependencies() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(sourceFile))) {
            writer.write("       EXEC SQL INCLUDE TESTCOPY END-EXEC.\n"); // Valid line
            writer.write("       EXEC SQL INCLUDE ANOTHER1 END-EXEC\n"); // Valid line
            writer.write("       EXEC SQL INCLUDE INVALID-NAME END-EXEC\n"); // Invalid line for IBM
            writer.write("      *EXEC SQL INCLUDE TESTCOP1 END-EXEC\n"); // Comment line

        } catch (IOException e) {
            throw new IOException(String.format("Couldn't write to %s", sourceFile));
        }
        CobolSourceAnalyzer cobolSourceAnalyzer = new CobolSourceAnalyzer(sourceFile);
        SourceCodeAnalysisResult analysisResult = cobolSourceAnalyzer.analyzeSource(includeDirectories);
        List<File> sourceDependencies = analysisResult.getSourceDependencies();
        assertNotNull(sourceDependencies);

        assertTrue(sourceDependencies.containsAll(List.of(copybookFiles.get(0), copybookFiles.get(1))));
        // Make sure that another1.cpy is not taken from the second directory
        assertFalse(sourceDependencies.contains(copybookFiles.get(2)));
        assertEquals(2, sourceDependencies.size());
    }
}
