---
name: codeowners-maintenance
description: Maintain CODEOWNERS and reviewer ownership for this HMCTS repository.
---

# CODEOWNERS Maintenance

Use this skill when updating `.github/CODEOWNERS`, GitHub Actions workflows, build files, dependency configuration, Helm charts, Jenkins files, Docker files, or infrastructure in this repository.

## Current Owners

The repository code owner is `@hmcts/cdm`.

Ownership evidence:
- `cnp-flux-config/CODEOWNERS` maps CCD, AAC, CPO, and TS product areas to `@hmcts/cdm`.
- Local Helm chart maintainer metadata identifies these services as HMCTS CCD, CDM, CPO, or TS maintained services.

## Maintenance Rules

- Keep `.github/workflows/` explicitly owned by `@hmcts/cdm`.
- Keep `.github/`, deployment configuration, runtime configuration, build files, and dependency control files owned by `@hmcts/cdm`.
- Prefer GitHub teams over individual users.
- Do not add placeholder users or teams. If ownership changes, update CODEOWNERS only after confirming the GitHub team from existing HMCTS repository or organisation evidence.
- When adding new sensitive paths, add explicit CODEOWNERS patterns for them even when the default `*` entry already covers them.

## Rationale

GitHub Actions workflows can change CI/CD execution, credentials, publishing, and deployment behaviour. Explicit workflow ownership ensures sensitive CI/CD changes require review from the maintainers responsible for this service.
