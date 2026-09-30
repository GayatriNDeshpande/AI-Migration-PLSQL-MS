package com.company.migrationpipeline.agent;

import com.company.migrationpipeline.model.PipelineContext;

public interface PipelineAgent {
  String name();
  void execute(PipelineContext context);
}
