# PRD: fga-gateway

Status: Draft v0.1  
Owner: Mradul Singh  
Date: 2026-07-07  
Repo: `github.com/mradulsingh2310/fga-gateway`

## 1. Product Summary

`fga-gateway` is a Java/Spring OpenFGA authorization gateway for agent tool
calls. It enforces that an agent may call a tool only when both the end user and
the agent/tool grant allow that action on the target resource.

The product is separate from `agentctl`. `agentctl` orchestrates durable agent
runs; `fga-gateway` decides whether protected tool calls are allowed and records
allow/deny audit events.

## 2. Goals

- Protect LangGraph, MCP, and HTTP tools with OpenFGA-backed authorization.
- Follow the Zeus/Hades pattern: annotation/decorator declarations, modular FGA
  model files, `.fga.yaml` tests, validation before deployment.
- Support both preflight API mode and proxy enforcement mode in v1.
- Support Python decorators that generate a versioned tool-auth manifest.
- Enforce tenant/org as a hard security boundary.
- Fail closed for unknown tools, missing manifests, unavailable FGA checks, and
  malformed resource identifiers.
- Emit audit records, OTel spans, and allow/deny metrics for every decision.

## 3. Non-Goals

- No full identity provider in v1.
- No hosted authorization SaaS.
- No broad API gateway replacement.
- No authorization model hidden in application code only; OpenFGA model stays
  version-controlled and tested.
- No backward compatibility guarantees before stable release unless explicitly requested.

## 4. Target Users

- Agent platform engineers protecting tool calls.
- Backend engineers exposing product tools to agents.
- Security engineers reviewing agent authorization policy.
- OSS users who need a self-hosted ReBAC enforcement layer for agent systems.

## 5. Authorization Model

Authorization requires both:

1. User can access the target resource.
2. Agent/tool is allowed to act in that context.

Example:

```text
allow = user_can_edit_ticket AND agent_can_call_ticket_update
```

Core entity types:

- `tenant`
- `user`
- `agent`
- `tool`
- `tool_group`
- `resource`
- `session`

Core permission relations:

- `can_invoke`
- `can_view`
- `can_edit`
- `can_create`
- `can_delete`
- `can_proxy`

The exact model must be implemented with modular OpenFGA files under `/fga`.

## 6. Repository Model Layout

Required layout:

```text
fga/
  fga.mod
  models/
    core.fga
    agent_tools.fga
  tests/
    tuples/
      common-tuples.yaml
    agent-tool-tests.fga.yaml
```

Requirements:

- model files use OpenFGA modular model style
- tests use `.fga.yaml`
- CI validates syntax and tests before release
- deployment workflow records model ID when configured

## 7. Python Decorator Manifest

Python tool authors declare authorization requirements with decorators.

Illustrative shape:

```python
@secure_tool(
    tool="github_issue.update",
    resource_type="github_issue",
    relation="can_edit",
    resource_id_arg="issue_id",
)
def update_issue(issue_id: str, body: dict):
    ...
```

The Python package scans decorated tools and emits a manifest.

Manifest requirements:

- versioned schema
- tool name
- tool version
- resource type
- relation
- resource id extraction rule
- tenant extraction rule
- whether proxy mode is supported
- whether approval is also required
- checksum/signature field for future hardening

The Java gateway validates the manifest at startup or registration time. Unknown
tools must be denied.

## 8. Enforcement Modes

### 8.1 Preflight API

Used for in-process LangGraph tools.

Request:

- tenant ID
- user ID
- agent ID
- session ID
- run ID
- tool name
- tool version
- resource type
- resource ID
- requested relation/action
- correlation ID

Response:

- allow/deny
- decision ID
- reason code
- OpenFGA checks performed
- cache status
- audit status

### 8.2 Proxy Mode

Used for HTTP/MCP tools.

Behavior:

- receives incoming tool request
- extracts auth context and resource metadata
- performs OpenFGA checks
- forwards allowed request to upstream tool
- blocks denied request
- records audit event
- propagates trace context

Proxy mode must fail closed for extraction failures, missing manifests, unknown
tools, or OpenFGA errors.

## 9. Java/Spring Service Requirements

Modules:

- API controller for preflight decisions.
- Proxy controller/filter for HTTP tool forwarding.
- Manifest loader and validator.
- OpenFGA client service.
- Decision cache.
- Audit writer.
- OTel instrumentation.
- Local dev auth/service-token validator.

Build:

- Maven.
- Java/Spring Boot.

## 10. Cache Strategy

V1 may cache allow decisions for a short TTL.

Requirements:

- cache key includes tenant, user, agent, tool, relation, resource type, resource ID, and manifest version
- deny decisions may be cached only if explicitly configured
- cache hit/miss metrics are emitted
- cache cannot bypass tenant boundary
- cache is invalidated by manifest version changes

## 11. Audit Requirements

Every decision records:

- decision ID
- timestamp
- tenant ID
- user ID
- agent ID
- session ID
- run ID
- tool name
- action/relation
- resource type
- resource ID
- allow/deny
- reason code
- OpenFGA model ID
- manifest version
- cache status
- trace ID

Audit records must be queryable by tenant, user, agent, tool, resource, and time range.

## 12. Observability

Required spans:

- `fga_gateway.preflight`
- `fga_gateway.proxy`
- `fga_gateway.openfga.check`
- `fga_gateway.manifest.validate`
- `fga_gateway.audit.write`

Required metrics:

- decisions total by allow/deny
- OpenFGA latency
- cache hit/miss
- proxy upstream latency
- manifest validation failures
- fail-closed decisions

## 13. agentctl Integration

`agentctl` uses `fga-gateway` for:

- protected support-ticket tools
- GitHub Issues workflow tools
- future incident remediation tools
- future GitHub ticket-to-PR tools

Integration requirements:

- service token auth between agentctl and fga-gateway
- trace context propagation
- run ID and session ID in every decision
- fail-closed behavior for protected tools
- decision ID stored in agentctl tool-call projection

## 14. Current Implementation Gap Audit

As of 2026-07-07, the repository contains:

- Spring Boot project shell.
- `POST /v1/preflight` controller.
- Preflight decision service that checks user access and agent `can_invoke`.
- Fail-closed default check client for unconfigured local startup.
- Modular OpenFGA model files under `fga/`.
- A minimal `.fga.yaml` model test.
- Unit, MVC, and startup tests for the current preflight skeleton.

The following PRD requirements are still missing and must be planned before
claiming v1 completeness:

### 14.1 OpenFGA Model Gaps

- Add `tool_group` and `session` types.
- Add `can_create`, `can_delete`, and `can_proxy` permissions.
- Model tenant boundary checks for user, agent, tool, and target resource in one
  authorization story.
- Add negative `.fga.yaml` cases for wrong tenant, wrong agent, unknown tool,
  denied relation, and proxy denial.
- Add CI workflow steps for `fga model validate --file fga/fga.mod` and
  `fga model test --tests fga/tests/agent-tool-tests.fga.yaml`.

### 14.2 Preflight API Gaps

- Request is missing `sessionId`, `toolVersion`, and `correlationId`.
- Response is missing `decisionId`, checks performed, cache status, audit status,
  model ID, and manifest version.
- No request validation for malformed tenant, resource type, resource ID, tool
  name, relation, or cross-tenant identifiers.
- Unknown tools are not denied through a manifest because no manifest is loaded.
- Current check client is only a fail-closed stub, not a real OpenFGA client.

### 14.3 Manifest Gaps

- No Python decorator package.
- No manifest schema.
- No manifest generation.
- No Java manifest loader.
- No Java manifest validator.
- No startup/registration path that denies unknown or malformed tools.
- No manifest version in cache keys, audit rows, or decision responses.

### 14.4 Proxy Mode Gaps

- No HTTP/MCP proxy controller or filter.
- No upstream forwarding.
- No request metadata extraction.
- No trace propagation through proxy mode.
- No fail-closed behavior for proxy extraction failures because proxy mode does
  not exist yet.

### 14.5 Audit, Cache, Auth, And Observability Gaps

- No audit table or audit writer.
- No persisted allow/deny decisions.
- No decision query API.
- No decision cache.
- No cache invalidation on manifest version changes.
- No service-token validation for local dev or service-to-service calls.
- No OTel spans or metrics for decisions, checks, cache, audit, or proxy.
- No OpenFGA model ID capture.

### 14.6 agentctl Integration Gaps

- `agentctl` does not call `fga-gateway` yet.
- `agentctl` has no `fga_decision_id` populated from real gateway responses.
- Support-ticket fake/GitHub tools are not protected by preflight checks yet.
- There is no shared contract fixture between `agentctl` tool calls and
  `fga-gateway` preflight requests.

## 15. Milestones

M0: Repo and PRD

- Create repo shell, PRD, license, seed FGA model layout.

M1: OpenFGA Model

- Implement core model, agent tools model, tuples, and tests.

M2: Java/Spring Preflight API

- Implement manifest validation, OpenFGA checks, audit rows, OTel spans.

M3: Proxy Mode

- Implement HTTP proxy enforcement and upstream forwarding.

M4: Python Decorator Package

- Implement decorators and manifest generation.

M5: agentctl Integration

- Authorize support-ticket fake and GitHub issue tools.

## 16. Acceptance Criteria

- Unauthorized tool call is denied before execution.
- Deny decision is persisted in audit store.
- Preflight and proxy modes both work in v1.
- OpenFGA model validates and tests pass in CI.
- Tenant boundary is enforced in all decisions.
- Unknown tool or missing manifest fails closed.
- agentctl can store fga-gateway decision IDs on tool calls.
