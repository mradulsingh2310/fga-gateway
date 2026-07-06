# fga-gateway

Java/Spring OpenFGA authorization gateway for agent tool calls.

`fga-gateway` is planned as a standalone OSS service that enforces per-user and
per-agent authorization before LangGraph, MCP, or HTTP tools execute. It follows
the same broad pattern as the Zeus/Hades authorization stack: annotations or
decorators declare intent, OpenFGA models live in version-controlled modules,
tests validate authorization behavior, and every allow/deny is auditable.

## Current Status

This repository contains the initial Spring Boot gateway skeleton:

- `POST /v1/preflight` authorization decision API
- fail-closed default FGA check client for unconfigured local startup
- OpenFGA modular model and `.fga.yaml` tests
- focused unit, MVC, and startup tests

## PRDs

- [fga-gateway PRD](docs/prd/fga-gateway.md)

## Development Checks

```bash
mvn test
fga model validate --file fga/fga.mod
fga model test --tests fga/tests/agent-tool-tests.fga.yaml
```

## License

Apache-2.0
