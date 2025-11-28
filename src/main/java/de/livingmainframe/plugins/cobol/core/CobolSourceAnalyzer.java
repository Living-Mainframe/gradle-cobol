package de.livingmainframe.plugins.cobol.core;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CobolSourceAnalyzer {

    /*
    This regular expression is flawed in the following ways:
    1) It accepts a hyphen as the first and last character which isn't allowed with IBM
    2) It only accepts 1-8 characters instead of 1-30
    3) It doesn't make any distinction between IBM and GNU Cobol
     */
    private static final String copyRegex = "^.{6}(?!\\*).\\s*COPY ([A-Za-z0-9-]{1,8})(?:\\.|\\s|$)";
    private static final Pattern copyRegexPattern = Pattern.compile(copyRegex);

    /*
    This is regular expression also has flaws. It currently covers the following cases:
    1) The EXEC SQL INCLUDE statement is on one line
    2) There are line breaks between EXEC SQL, INCLUDE, the dependency name and END-EXEC
    In case the source code contains characters in the suffix or prefix area the regular
    won't find a result
    TODO: Enhance RegEx to ignore text in the suffix and prefix area of the program
     */
    private static final String execSqlIncludeRegex = "^.{6}(?!\\*).\\s*EXEC SQL\\s*INCLUDE\\s*([A-Za-z0-9-]{1,8})" +
            "\\s*END-EXEC(?:\\.|\\s|$)";
    private static final Pattern execSqlIncludeRegexPattern = Pattern.compile(execSqlIncludeRegex);

    /*
    This regular expression searches for EXEC SQL commands which can be anywhere in the source but cannot be comment
    lines
     */
    private static final String execSqlRegex = "^.{6}(?!\\*).\\s*EXEC SQL";
    private static final Pattern execSqlRegexPattern = Pattern.compile(execSqlRegex);

    /*
    Similarly to EXEC SQL we search for EXEC CICS statements
     */
    private static final String execCicsRegex = "^.{6}(?!\\*).\\s*EXEC CICS";
    private static final Pattern execCicsRegexPattern = Pattern.compile(execCicsRegex);

    private final File buildFile;
    private boolean analyzeDb2Usage = true;
    private boolean analyzeCicsUsage = true;

    /**
     * Utility class that scan source files and returns information like dependencies and whether the program uses
     * Db2 or CICS
     *
     * @param buildFile The file handle pointing to the source file
     */
    public CobolSourceAnalyzer(File buildFile) {
        this.buildFile = buildFile;
    }

    public SourceCodeAnalysisResult analyzeSource(List<File> sourceIncludeDirectories) {
        SourceCodeAnalysisResult analysisResult = new SourceCodeAnalysisResult();

        List<String> sourceDependencyNames = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(buildFile))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Matcher copyRegexMatcher = copyRegexPattern.matcher(line);
                while (copyRegexMatcher.find()) {
                    String includeName = copyRegexMatcher.group(1);
                    sourceDependencyNames.add(includeName);
                }
                if (analyzeDb2Usage) {

                    // First, we check if the current line includes any EXEC SQL
                    // After the first occurrence we don't check again
                    if (!analysisResult.isDb2()) {
                        Matcher execSqlRegexMatcher = execSqlRegexPattern.matcher(line);
                        while (execSqlRegexMatcher.find()) {
                            analysisResult.setDb2(true);
                        }
                    }

                    // We only need to execute this in case we have identified that the program uses Db2
                    // This avoids unnecessary line reads because the previous 'if' will find the first line that
                    // identifies a program as Db2
                    if (analysisResult.isDb2()) {
                        // TODO Change to buffered reading because EXEC SQL INCLUDE statements that span multiple rows
                        //  cannot be detected
                        Matcher execSqlIncludeRegexMatcher = execSqlIncludeRegexPattern.matcher(line);
                        while (execSqlIncludeRegexMatcher.find()) {
                            String includeName = execSqlIncludeRegexMatcher.group(1);
                            sourceDependencyNames.add(includeName);
                        }
                    }
                }

                if (analyzeCicsUsage && !analysisResult.isCics()) {
                    Matcher execCicsRegexMatcher = execCicsRegexPattern.matcher(line);
                    while (execCicsRegexMatcher.find()) {
                        analysisResult.setCics(true);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("An error occurred reading the file: " + e);
        }

        List<File> sourceDependencies = findSourceDependencies(sourceDependencyNames, sourceIncludeDirectories);
        analysisResult.setSourceDependencies(sourceDependencies);

        return analysisResult;
    }

    private List<File> findSourceDependencies(List<String> sourceDependencyNames, List<File> sourceIncludeDirectories) {
        List<File> sourceDependencies = new ArrayList<>();

        Map<String, File> cacheDirectoryListing = cacheDirectoryListing(sourceIncludeDirectories);
        for (String sourceDependencyName : sourceDependencyNames) {
            sourceDependencyName = sourceDependencyName.toLowerCase();
            if (cacheDirectoryListing.containsKey(sourceDependencyName)) {
                sourceDependencies.add(cacheDirectoryListing.get(sourceDependencyName));
            }
        }

        return sourceDependencies;
    }

    private @NotNull Map<String, File> cacheDirectoryListing(@NotNull List<File> sourceIncludeDirectories) {
        Map<String, File> cacheDirectoryListing = new LinkedHashMap<>();
        for (File sourceIncludeDirectory : sourceIncludeDirectories) {
            File[] directoryListing = sourceIncludeDirectory.listFiles();
            if (directoryListing != null) {
                for (File file : directoryListing) {
                    if (file.isFile()) {
                        //TODO: Make the file extension configurable
                        //TODO: Evaluate whether toLowerCase() is actually desired
                        cacheDirectoryListing.putIfAbsent(file.getName().replace(".cpy", "").toLowerCase(), file);
                    }
                }
            }
        }
        return cacheDirectoryListing;
    }

    public void setAnalyzeDb2Usage(boolean analyzeDb2Usage) {
        this.analyzeDb2Usage = analyzeDb2Usage;
    }

    public void setAnalyzeCicsUsage(boolean analyzeCicsUsage) {
        this.analyzeCicsUsage = analyzeCicsUsage;
    }
}
