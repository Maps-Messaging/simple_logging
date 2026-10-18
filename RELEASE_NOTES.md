# Simple Logging 2.2.0

Version 2.2.0 adds a tamper-evident audit journal, fixes exception handling in the logging API, and improves reliability when writing and verifying audit records.

## New: audit journal

The new `io.mapsmessaging.audit` package provides:

- An append-only JSON Lines journal with sequential record numbers and SHA-256 hash chaining to detect changes to recorded events.
- Optional Ed25519 record signing and verification.
- Journal rotation by date or size, with recovery of existing journal state.
- Audit records with contextual information and outcomes, plus manifest and verification utilities.
- File-backed payload storage with SHA-256 digests and payload references.
- Configuration for journal location, rotation, cryptographic keys, and handling invalid existing journals.

The audit journal is optional; applications using the existing logging API do not need to adopt it.

## Fixes and improvements

- **Exception handling:** A trailing `Throwable` is treated as an exception rather than as a message-format argument. This also corrects argument-count checks for calls using the explicit exception overload.
- **Reliable persistence:** Journal, payload, and manifest writers now complete pending writes and flush data before returning.
- **Audit consistency:** Improved journal verification, recovery, and deterministic date handling for audit payloads and manifests.

## Compatibility and dependencies

- The existing `io.mapsmessaging.logging.Logger` API remains available. No intentional breaking changes to that API are included.
- Minimum Java version remains **17**.
- `slf4j-api` has been updated from **2.0.18** to **2.0.20**.
- `gson` **2.14.0** is now a direct runtime dependency for audit JSON serialization.

Applications adopting signed audit records should configure the appropriate signing and verification keys.
