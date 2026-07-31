# Sroot

Sroot is a community-maintained research fork for documenting and testing
device-specific adaptations around the CVE-2026-43499 research chain.

This repository is a secondary project. The original exploit research and
the earlier Samsung adaptation remain upstream projects; Sroot keeps the
device-specific work, reproducibility notes, and documentation separate.

## Current Target

| Field | Value |
| --- | --- |
| Device | Samsung Galaxy S25 Ultra |
| Model | SM-S9380 |
| Region | Hong Kong |
| CSC | ZHU |
| Firmware | S9380ZHU1AYA1 |
| Android | 15 |
| Build fingerprint | `samsung/pa3qzhx/pa3q:15/AP3A.240905.015/S9380ZHU1AYA1:user/release-keys` |

The current private laboratory validation reached `root=1` through the
ADB/shell test path on the target firmware. The public repository does not
include the operational payload, root helper, device offsets, or a no-ADB
APK payload.

## Branch Layout

The project keeps the APP work and the native SO work in separate branches:

| Branch | Purpose |
| --- | --- |
| `main` | Current SM-S9380 ZHU AYA1 validation status, adaptation notes, and project documentation |
| `app` | APK/JNI framework, process-context diagnostics, and authorized-service integration |
| `cve-so` | Native SO build/reproducibility notes and private-artifact checklist |

The `main` branch records the currently validated research milestone. The
`app` and `cve-so` branches are development tracks and must not be treated as
interchangeable build outputs.

## Upstream And References

Sroot is based on the following upstream research project:

- [NebuSec/CyberMeowfia - original CVE-2026-43499 research](https://github.com/NebuSec/CyberMeowfia/tree/main/IonStack/CVE-2026-43499/exploit)

Sroot is a secondary device-adaptation project built around that original
research. It is not an upstream replacement and does not claim ownership of
the original work.

For the APP-side project structure and application packaging flow, see:

- [BuSung-dev/Root-My-Galaxy - APP reference](https://github.com/BuSung-dev/Root-My-Galaxy)

The APP branch may reuse that project's application architecture, lifecycle,
native-library loading arrangement, and logging layout. Its native payload
and device adaptation remain separate Sroot work and are not copied from the
reference project's exploit path.

## Adaptation Model

The project separates generic research code from device-specific data:

```text
generic research chain
        |
        +-- target profile
        |     +-- exact build fingerprint
        |     +-- kernel image and symbol information
        |     +-- validated structure/function data
        |     +-- boot and userland constraints
        |
        +-- build and validation notes
```

For a new device or firmware:

1. Record the exact model, CSC, firmware build, Android version, and build
   fingerprint.
2. Keep the generic upstream code unchanged while collecting device-specific
   evidence.
3. Put target-specific values in a separate profile instead of scattering
   constants through the exploit logic.
4. Rebuild from a clean tree and record compiler, NDK, source revision, and
   artifact hashes.
5. Validate in stages: build/load, information disclosure, control-flow
   redirection, memory read/write primitive, and privileged handoff.
6. Treat ADB/shell validation and ordinary-app validation as separate
   environments. An APK process has different SELinux, filesystem, and
   process-launch constraints.

Detailed documentation is in
[`docs/ADAPTATION.md`](docs/ADAPTATION.md).

## Repository Policy

The public main branch is documentation-first. Do not commit:

- operational root payloads or APKs;
- root helpers or embedded privileged binaries;
- device-specific exploit offsets;
- boot images, firmware packages, or private logs containing sensitive data;
- generated `.o`, `.so`, APK, or test artifacts.

Private lab artifacts should stay in a separate, access-controlled workspace.
The repository may contain non-operational build examples, test stubs, and
environment diagnostics.

## Reproducibility

Every target adaptation should provide:

- a target profile name;
- source revision and upstream commit;
- exact compiler/NDK version;
- build command;
- input artifact hashes;
- a redacted validation log;
- a clear statement of the execution context.

Do not describe a build as APP-compatible unless it has been tested from an
ordinary application process. A successful ADB run alone is not evidence that
an APK can execute the same path.

## Legal And Safety Notice

Use this project only on devices you own or are explicitly authorized to
test. Samsung firmware, kernel images, and third-party source remain subject
to their respective licenses. This repository is for defensive research,
reproducibility, and device compatibility study.
