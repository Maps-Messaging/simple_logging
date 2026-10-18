# Simple Logging 2.1.3 release notes

**Status:** Release candidate; not yet published.  
**Previous release:** 2.1.2  
**Maven coordinates:** `io.mapsmessaging:simple_logging:2.1.3`  
**Source comparison:** [main...development](https://github.com/Maps-Messaging/simple_logging/compare/main...development)

## Highlights

Version 2.1.3 introduces an optional, file-backed audit journal alongside the existing enum-based SLF4J logging API. It also corrects exception-argument handling and strengthens audit-log persistence, verification, and test coverage.

### Added

- An append-only JSON Lines audit journal with monotonic record sequence numbers, SHA-256 hash chaining, configurable journal rotation by date and file size, and recovery of existing journal state.
- Optional Ed25519 signing of audit records, signature and hash-chain verification support, and key persistence utilities.
- Audit records, context, outcomes, manifests, manifest verification/writing, and file-backed payload references with SHA-256 digests.
- Configuration for signing and verification keys, clock, maximum journal size, rotation, and handling of invalid existing journals.
- Unit and regression coverage for journal lifecycle, recovery, cryptographic checks, manifests, payload persistence, and failure handling.

### Fixed

- Treat a trailing `Throwable` as an exception argument instead of counting it as a message-format placeholder. The explicit exception overload now uses this same handling path.
- Ensure audit payload, journal, and manifest writes finish writing their buffers and flush data as intended.
- Make audit payload dates and manifest/journal behaviour deterministic where controlled by an injected clock.
- Improve audit-journal verification, persistence error handling, and signature/key test coverage.

### Build and dependencies

- Upgrade `slf4j-api` from 2.0.18 to 2.0.20.
- Add `gson` 2.14.0 as a direct dependency for audit JSON serialization.
- Upgrade test-only `logback-classic` from 1.5.32 to 1.6.5, alongside updates to JUnit, Lombok, Maven plugins, and security/coverage tooling.
- Improve SonarCloud analysis, dependency-check caching, and non-interactive GPG artifact signing in Buildkite.

## Compatibility and operational notes

- The existing `io.mapsmessaging.logging.Logger` API remains available; the audit API is an additional `io.mapsmessaging.audit` package.
- The library targets Java 17.
- Audit signing is optional. Applications requiring cryptographic authenticity should configure both signing and verification keys and define their policy for unsigned records.
- The experimental audit-viewer module added during development was subsequently removed and **is not included** in 2.1.3.
- The new Gson runtime dependency should be considered when updating dependency-management or shaded-jar configurations.

## Release verification

These notes describe the changes in `development`; they do not assert that a Maven Central release has occurred.

- [ ] Complete the final snapshot tests and review the approved release commit.
- [ ] Change the Maven project version from `2.1.3-SNAPSHOT` to `2.1.3` on the release revision.
- [ ] Build and sign the main JAR, sources, Javadoc and POM; verify the `.asc` signatures.
- [ ] Publish with the configured Sonatype Central release profile and verify the artifact is resolvable as `io.mapsmessaging:simple_logging:2.1.3`.
- [ ] Publish Simple Logging before the dependent JMS Selector ML release.

## Related work

- [MSG-312](https://mapsmessaging.atlassian.net/browse/MSG-312) — logging exception handling.
- [MSG-342](https://mapsmessaging.atlassian.net/browse/MSG-342) — SonarCloud remediation and audit regression coverage.
- [MSG-95](https://mapsmessaging.atlassian.net/browse/MSG-95) — release notes.
