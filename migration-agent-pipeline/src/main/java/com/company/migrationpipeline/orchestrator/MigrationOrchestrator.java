package com.company.migrationpipeline.orchestrator;

import com.company.migrationpipeline.agent.AnalysisAgent;
import com.company.migrationpipeline.agent.DiscoveryAgent;
import com.company.migrationpipeline.agent.MigrationAgent;
import com.company.migrationpipeline.agent.PipelineAgent;
import com.company.migrationpipeline.agent.SchemaAgent;
import com.company.migrationpipeline.config.PipelineConfig;
import com.company.migrationpipeline.model.PipelineContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class MigrationOrchestrator {
  private final PipelineConfig config;
  private final List<PipelineAgent> agents;

  public MigrationOrchestrator(PipelineConfig config) {
    this.config = config;
    this.agents = List.of(
        new DiscoveryAgent(),
        new AnalysisAgent(),
        new SchemaAgent(),
        new MigrationAgent());
  }

  public void run() {
    PipelineContext context = new PipelineContext(config.inputPath(), config.outputPath());
    System.out.println("Starting migration pipeline");
    System.out.println("Input: " + context.getInputPath());
    System.out.println("Output: " + context.getOutputPath());

    ensureOutputDirectory(context.getOutputPath());

    for (PipelineAgent agent : agents) {
      System.out.println("Running agent: " + agent.name());
      agent.execute(context);
      String summary = summaryFor(agent, context);
      if (summary != null && !summary.isBlank()) {
        System.out.println("Summary (" + agent.name() + "): " + summary);
        writeSummary(context.getOutputPath(), agent.name(), summary);
      }
    }

    System.out.println("Pipeline complete");
  }

  private void ensureOutputDirectory(Path outputPath) {
    try {
      Files.createDirectories(outputPath);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to create output directory: " + outputPath, ex);
    }
  }

  private void writeSummary(Path outputPath, String agentName, String summary) {
    Path summaryFile = outputPath.resolve(agentName + "-summary.txt");
    try {
      Files.writeString(
          summaryFile,
          summary + System.lineSeparator(),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to write summary: " + summaryFile, ex);
    }
  }

  private String summaryFor(PipelineAgent agent, PipelineContext context) {
    if (agent instanceof DiscoveryAgent) {
      return context.getDiscoverySummary();
    }
    if (agent instanceof AnalysisAgent) {
      return context.getAnalysisSummary();
    }
    if (agent instanceof SchemaAgent) {
      return context.getSchemaSummary();
    }
    if (agent instanceof MigrationAgent) {
      return context.getMigrationSummary();
    }
    return null;
  }
}

