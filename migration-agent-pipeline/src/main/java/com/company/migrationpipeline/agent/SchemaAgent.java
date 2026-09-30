package com.company.migrationpipeline.agent;

import com.company.migrationpipeline.model.PipelineContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SchemaAgent implements PipelineAgent {
  private static final Pattern TABLE_PATTERN =
      Pattern.compile("\\b(?:FROM|INTO|UPDATE)\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

  @Override
  public String name() {
    return "schema";
  }

  @Override
  public void execute(PipelineContext context) {
    List<Path> sqlFiles = context.getSqlFiles();
    if (sqlFiles == null || sqlFiles.isEmpty()) {
      context.setSchemaSummary("No SQL files to derive schema artifacts.");
      return;
    }

    Set<String> tables = new HashSet<>();
    for (Path sqlFile : sqlFiles) {
      String contents = readSql(sqlFile);
      Matcher matcher = TABLE_PATTERN.matcher(contents);
      while (matcher.find()) {
        String table = matcher.group(1).toLowerCase();
        if (table.equals("nowait") || table.startsWith("v_") || table.startsWith("p_")) {
          continue;
        }
        tables.add(table);
      }
    }

    String tableList = tables.stream()
        .sorted()
        .collect(Collectors.joining(", "));

    context.setSchemaSummary(
        "Identified " + tables.size() + " tables: " + tableList + ".");
  }

  private String readSql(Path sqlFile) {
    try {
      return Files.readString(sqlFile);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read SQL file: " + sqlFile, ex);
    }
  }
}
