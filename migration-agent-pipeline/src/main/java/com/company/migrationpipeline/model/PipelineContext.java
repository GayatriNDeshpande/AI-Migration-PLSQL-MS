package com.company.migrationpipeline.model;

import java.nio.file.Path;
import java.util.List;

public class PipelineContext {
  private final Path inputPath;
  private final Path outputPath;
  private List<Path> sqlFiles;
  private String discoverySummary;
  private String analysisSummary;
  private String schemaSummary;
  private String migrationSummary;

  public PipelineContext(Path inputPath, Path outputPath) {
    this.inputPath = inputPath;
    this.outputPath = outputPath;
  }

  public Path getInputPath() {
    return inputPath;
  }

  public Path getOutputPath() {
    return outputPath;
  }

  public List<Path> getSqlFiles() {
    return sqlFiles;
  }

  public void setSqlFiles(List<Path> sqlFiles) {
    this.sqlFiles = sqlFiles;
  }

  public String getDiscoverySummary() {
    return discoverySummary;
  }

  public void setDiscoverySummary(String discoverySummary) {
    this.discoverySummary = discoverySummary;
  }

  public String getAnalysisSummary() {
    return analysisSummary;
  }

  public void setAnalysisSummary(String analysisSummary) {
    this.analysisSummary = analysisSummary;
  }

  public String getSchemaSummary() {
    return schemaSummary;
  }

  public void setSchemaSummary(String schemaSummary) {
    this.schemaSummary = schemaSummary;
  }

  public String getMigrationSummary() {
    return migrationSummary;
  }

  public void setMigrationSummary(String migrationSummary) {
    this.migrationSummary = migrationSummary;
  }
}
