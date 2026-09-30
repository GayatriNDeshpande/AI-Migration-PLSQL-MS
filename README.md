# AI-Migration-PLSQL-MS
Create agentic pipeline for Migration of PL/SQL to microservices

## Prerequisites
- JDK 17+ installed and available on PATH (`java` and `javac`).

## Project layout
- `input/` — PL/SQL files to process
- `output/` — pipeline output summaries
- `agent-generated-service/` — generated Spring Boot service output
- `migration-agent-pipeline/` — Java agentic pipeline source

## Run the agentic pipeline (no Maven required)
From PowerShell:

1) Compile
```powershell
cd D:\AI-Migration-PLSQL-MS\migration-agent-pipeline
$sources = Get-ChildItem -Recurse -Filter *.java src\main\java | ForEach-Object { $_.FullName }
javac -d target\classes $sources
```

2) Run with input/output paths
```powershell
java -cp target\classes com.company.migrationpipeline.MigrationAgentPipelineApplication "D:\AI-Migration-PLSQL-MS\input" "D:\AI-Migration-PLSQL-MS\agent-generated-service"
```

3) View pipeline output
```powershell
Get-ChildItem D:\AI-Migration-PLSQL-MS\agent-generated-service
Get-Content D:\AI-Migration-PLSQL-MS\agent-generated-service\discovery-summary.txt
Get-Content D:\AI-Migration-PLSQL-MS\agent-generated-service\analysis-summary.txt
Get-Content D:\AI-Migration-PLSQL-MS\agent-generated-service\schema-summary.txt
Get-Content D:\AI-Migration-PLSQL-MS\agent-generated-service\migration-summary.txt
```

4) Inspect generated service
```powershell
Get-ChildItem D:\AI-Migration-PLSQL-MS\agent-generated-service\src\main\java
```

## Demo checklist
- Confirm `input\order_management_pkg.sql` exists.
- Run compile + pipeline commands above.
- Show output summary files under `output\`.
