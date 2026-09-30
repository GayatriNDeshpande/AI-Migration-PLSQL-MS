package com.company.migrationpipeline.agent;

import com.company.migrationpipeline.model.PipelineContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class DiscoveryAgent implements PipelineAgent {
  @Override
  public String name() {
    return "discovery";
  }

  @Override
  public void execute(PipelineContext context) {
    try {
      List<Path> sqlFiles = Files.walk(context.getInputPath())
          .filter(path -> path.toString().endsWith(".sql"))
          .collect(Collectors.toList());
      context.setSqlFiles(sqlFiles);
      context.setDiscoverySummary("Discovered " + sqlFiles.size() + " SQL files.");
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to discover SQL files", ex);
    }
  }
}
