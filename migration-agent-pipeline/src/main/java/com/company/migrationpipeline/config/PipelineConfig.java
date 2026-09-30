package com.company.migrationpipeline.config;

import java.nio.file.Path;

public record PipelineConfig(Path inputPath, Path outputPath) {
  public static PipelineConfig fromArgs(String[] args) {
    Path input = Path.of(args.length > 0 ? args[0] : "input");
    Path output = Path.of(args.length > 1 ? args[1] : "output");
    return new PipelineConfig(input, output);
  }
}
