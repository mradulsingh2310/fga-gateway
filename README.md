# fga-gateway

Java/Spring OpenFGA authorization gateway for agent tool calls.

`fga-gateway` is planned as a standalone OSS service that enforces per-user and
per-agent authorization before LangGraph, MCP, or HTTP tools execute. It follows
the same broad pattern as the Zeus/Hades authorization stack: annotations or
decorators declare intent, OpenFGA models live in version-controlled modules,
tests validate authorization behavior, and every allow/deny is auditable.

## Current Status

This repository currently contains product requirements documents, an initial
OpenFGA model layout, and the initial OSS project shell. Implementation has not
started yet.

## PRDs

- [fga-gateway PRD](docs/prd/fga-gateway.md)

## License

Apache-2.0
