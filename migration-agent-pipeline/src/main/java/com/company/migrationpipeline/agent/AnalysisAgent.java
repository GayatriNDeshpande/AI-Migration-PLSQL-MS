package com.company.migrationpipeline.agent;

import com.company.migrationpipeline.model.PipelineContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalysisAgent implements PipelineAgent {
  private static final Pattern PROCEDURE_PATTERN =
      Pattern.compile("\\bPROCEDURE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern FUNCTION_PATTERN =
      Pattern.compile("\\bFUNCTION\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

  @Override
  public String name() {
    return "analysis";
  }

  @Override
  public void execute(PipelineContext context) {
    List<Path> sqlFiles = context.getSqlFiles();
    int fileCount = sqlFiles == null ? 0 : sqlFiles.size();
    int procedureCount = 0;
    int functionCount = 0;

    if (sqlFiles != null) {
      for (Path sqlFile : sqlFiles) {
        String contents = readSql(sqlFile);
        procedureCount += countMatches(PROCEDURE_PATTERN, contents);
        functionCount += countMatches(FUNCTION_PATTERN, contents);
      }
    }

    context.setAnalysisSummary(
        "Analyzed " + fileCount + " SQL files with " + procedureCount
            + " procedures and " + functionCount + " functions.");
  }

  private String readSql(Path sqlFile) {
    try {
      return Files.readString(sqlFile);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read SQL file: " + sqlFile, ex);
    }
  }

  private int countMatches(Pattern pattern, String input) {
    Matcher matcher = pattern.matcher(input);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }
}
