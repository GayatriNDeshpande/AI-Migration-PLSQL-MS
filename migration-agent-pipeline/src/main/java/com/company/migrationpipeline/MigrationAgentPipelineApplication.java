package com.company.migrationpipeline;

import com.company.migrationpipeline.config.PipelineConfig;
import com.company.migrationpipeline.orchestrator.MigrationOrchestrator;

public class MigrationAgentPipelineApplication {
  public static void main(String[] args) {
    PipelineConfig config = PipelineConfig.fromArgs(args);
    MigrationOrchestrator orchestrator = new MigrationOrchestrator(config);
    orchestrator.run();
  }
}
