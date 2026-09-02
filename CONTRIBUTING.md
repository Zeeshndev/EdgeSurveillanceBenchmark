# Contributing to EdgeSurveillanceBenchmark

Thank you for your interest in contributing! This repository serves as the
official mobile evaluation artifact for research submitted to *Transactions
on Machine Learning Research (TMLR)* and the *Journal of Open Source
Software (JOSS)*.

We welcome community contributions, particularly those that expand hardware
delegate support, improve telemetry precision, or enhance data
visualization.

**A note on timing**: this project is currently mid-way through an active,
multi-day physical device data-collection campaign for the associated
research paper. Structural or behavioral changes to the trial-execution
pipeline (`BenchmarkOrchestrator.kt`, `MetricsExporter.kt`,
`TelemetryCollector.kt`) will not be merged until the current campaign
completes, to guarantee every trial in the resulting dataset ran under
identical software behavior. Bug reports, discussion, and non-pipeline
contributions (documentation, tooling, visualization scripts) are welcome
at any time.

---

## 1. Reporting Bugs & Hardware Anomalies

Because this harness tests low-level hardware delegates (e.g., OpenCL/OpenGL
drivers, NPU acceleration) and OEM-specific OS behaviors, bug reports must
include strict environmental context.

Before filing, please review [`PHYSICAL_CONTROLS.md`](./PHYSICAL_CONTROLS.md)
to confirm the anomaly isn't explained by an unmet physical testing
precondition (battery state of charge, thermal status, charging state).

When opening an issue on GitHub, please include:

- **Device specifications**: OEM, exact model, SoC, and GPU (e.g., Xiaomi
  POCO, Snapdragon [chipset], Adreno [model]).
- **OS version**: Android version and OEM skin (e.g., Android 12, HyperOS
  1.0).
- **The logcat trace**: crucially, include the startup trace showing
  delegate kernel allocation:
  ```bash
  adb logcat --pid=<PID> | grep -iE "tflite|TFLiteDelegate_PROOF|Orchestrator"
  ```
- **Reproduction steps**: the exact `.tflite` model used and the battery
  state-of-charge at the time of the anomaly.

## 2. Seeking Support

If you have a usage question rather than a bug report or code contribution
-- for example, help adapting the harness to a different model or device --
please open a GitHub Discussion, or a GitHub Issue tagged `question`, rather
than a standard bug report. This keeps hardware-anomaly reports (which
require the environmental detail above) separate from general usage help.

## 3. Development Setup

To modify the codebase, you will need:

- **Android Studio**: Giraffe or newer (or equivalent IntelliJ/VS Code
  setup).
- **Toolchain**: JDK 11 (see the main README for the full Java 8/11
  compatibility note) and Android SDK 32.
- **Physical hardware**: emulators cannot simulate thermal degradation,
  battery drain, or accurate GPU compute shader behavior. All development
  and testing must be performed on a physical Android device (API 29+).

## 4. Pull Request Process

1. **Fork the repository** and create a branch from `main` (e.g.,
   `feature/npu-delegate-support` or `fix/telemetry-memory-leak`).
2. **Preserve the metrics schema.** If your PR modifies
   `MetricsExporter.kt`, do not alter or reorder the existing 14-column
   schema in `master_benchmark_results.csv` unless explicitly discussed in
   an issue first -- academic reproducibility depends on this schema
   remaining stable.
3. **Run local verification** before submitting:
   ```bash
   ./gradlew assembleDebug --stacktrace
   ```
   A regression test suite covering known failure modes (delegate
   mislabeling, latency-statistics correctness, CSV schema integrity) is
   planned; once available, running it will be a required step here.
4. **Submit the PR** with a clear description of the problem solved, links
   to any relevant issues, and a screenshot of the `logcat` output showing
   the orchestrator passing the hardware verification gate on your test
   device.
5. By submitting a contribution, you agree it is licensed under this
   project's Apache License 2.0, consistent with the rest of the codebase.

## 5. Code Style & Architecture

- **Language**: Kotlin.
- **Format**: standard Android Kotlin style guidelines.
- **Architecture**: keep UI logic (`CameraFragment`) strictly decoupled from
  benchmarking logic (`BenchmarkOrchestrator`). The orchestrator must remain
  capable of running headlessly.

## 6. Code of Conduct

By participating in this project, you agree to maintain a respectful,
inclusive, and harassment-free environment for everyone, regardless of
experience level or background. Constructive academic and engineering
debate is encouraged. (Consider adopting the
[Contributor Covenant](https://www.contributor-covenant.org/) as a
standalone `CODE_OF_CONDUCT.md` if the project grows beyond its current
contributor base.)
