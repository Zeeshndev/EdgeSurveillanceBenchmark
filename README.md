# EdgeSurveillanceBenchmark: Thermal & Latency Profiling for Edge AI

[![Android CI](https://github.com/Zeeshndev/EdgeSurveillanceBenchmark/actions/workflows/android-build.yml/badge.svg)](https://github.com/Zeeshndev/EdgeSurveillanceBenchmark/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%2012%2B%20%7C%20HyperOS-orange.svg)](https://developer.android.com)
[![TFLite Core](https://img.shields.io/badge/TFLite%20Core-2.9.0-green.svg)](https://www.tensorflow.org/lite)
[![Task Vision](https://img.shields.io/badge/Task--Vision-0.4.0-green.svg)](https://www.tensorflow.org/lite)

An empirical benchmarking harness engineered to evaluate thermal degradation,
battery drain, and latency drift of nano-scale object detectors (YOLOv10n and
YOLOv11n) on resource-constrained Android edge hardware under sustained
inference workloads.

This repository is the mobile evaluation artifact for the empirical study
*"Beyond Simulated Latency: An Empirical Study of Battery, Thermal, and
Memory Costs of INT8-Quantized YOLOv10 and YOLOv11 for On-Device Vehicle
Surveillance"* (Transactions on Machine Learning Research, in preparation),
and is submitted independently to the Journal of Open Source Software (JOSS)
as a standalone reusable tool.

Companion research repository (dataset pipeline, model training, manuscript):
`[link to edge-vehicle-surveillance-benchmark]`

---

## 1. Research Scope & Architectural Focus

Deploying real-time vision algorithms on edge surveillance systems demands
continuous inference over hours of operation. Standard mobile ML benchmarks
typically execute brief, unthrottled bursts (10 to 60 seconds), masking
critical system-level behaviors:

- **Thermal throttling and clock clamping**: degradation of frame latencies
  as SoC junction temperatures reach thermal trip points.
- **Hardware delegate graph partitioning**: friction points where
  unsupported operator layers -- for example, the Distribution Focal Loss
  (DFL) coordinate-normalization step and Non-Maximum Suppression
  post-processing -- force silent fallbacks to the CPU.
- **Driver-layer fallbacks**: transparent delegation re-routing (for
  example, OpenCL to OpenGL) across proprietary OEM Android distributions.

`EdgeSurveillanceBenchmark` replaces volatile, UI-driven camera evaluation
loops with a hardened, deterministic orchestrator running headless with
continuous, real-time hardware telemetry logging.

---

## 2. Core Harness Architecture

```
                        BenchmarkOrchestrator
                                 |
              ---------------------------------------
              |                    |                  |
       DatasetFeeder      TFLite Interpreter    TelemetryCollector
     (pre-decoded    -->    (CPU / GPU)     -->   (PSS / mA / C /
       bitmaps)                                    thermal status)
              |                    |                  |
              ---------------------------------------
                                 |
                          MetricsExporter
                       (14-column CSV schema)
```

The system is decoupled into five primary components:

- **`BenchmarkOrchestrator.kt`**: controls trial life cycles, dynamic
  warmup-convergence detection (CV < 0.05 over a rolling window), fail-loud
  delegate initialization, and process wake-lock management.
- **`DatasetFeeder.kt`**: feeds standardized input frames (pre-scaled from
  the UA-DETRAC surveillance dataset) directly into memory buffers,
  isolating inference from live camera HAL acquisition noise.
- **`TelemetryCollector.kt`**: samples system health at 1 Hz -- Proportional
  Set Size (PSS) memory, raw battery current draw
  (`BatteryManager.BATTERY_PROPERTY_CURRENT_NOW` in microamperes), battery
  percentage, thermal status, and battery temperature.
- **`ObjectDetectorHelper.kt`**: wraps the native TensorFlow Lite runtime,
  managing delegate attachment, model initialization, and output decoding.
- **`MetricsExporter.kt`**: emits per-trial CSV logs and appends normalized
  run vectors to a consolidated master rollup dataset.

---

## 3. Physical Gatekeeping & Environmental Controls

Full protocol in [`PHYSICAL_CONTROLS.md`](./PHYSICAL_CONTROLS.md). Summary:

- **State-of-charge boundary (85-95%)**: inference will not trigger outside
  this window, avoiding internal battery cell resistance effects that skew
  voltage/current telemetry at low charge levels.
- **Unplugged-state verification**: USB power connection is checked; running
  while charging introduces a charge-current vector that distorts both power
  telemetry and heat dissipation behavior.
- **Xiaomi HyperOS battery health quirk**: on this device,
  `BatteryManager.EXTRA_HEALTH` returns `1` (`BATTERY_HEALTH_UNKNOWN`) rather
  than the standard `2` (`BATTERY_HEALTH_GOOD`). The orchestrator tags this
  as `UNKNOWN_1` and permits the run to proceed. This was independently
  cross-checked against the device's hidden battery diagnostic menu
  (accessed via `*#*#6485#*#*`), which reported the `MB_06` health parameter
  as "Good," corroborating that the `UNKNOWN_1` reading reflects a healthy
  battery rather than a genuine fault. (See `/evidence` for a screenshot of
  this diagnostic screen.)
- **OEM OS mitigation triad** (required together; any one alone is
  insufficient on HyperOS): Autostart enabled; Battery Saver set to "No
  restrictions"; app locked in the Recents tray.

---

## 4. Delegate Forensics & Graph Partitioning

Hardware delegate runtimes can silently fall back to CPU execution without
raising an exception. This project treats that as a first-class risk to
guard against, not an edge case, and validates it through two complementary
checks:

```
UI delegate selection --> force delegate rebuild --> verify kernel count (N > 0)
                                                            |
                                    ------------------------------------
                                    |                                  |
                              valid (N > 0)                     zero (N == 0)
                          tag backend, proceed          FLAG / ABORT -- do not
                                                          report as this delegate
```

**In-app self-monitoring**: the orchestrator spawns
`Runtime.getRuntime().exec("logcat -d -t 1500 --pid=${Process.myPid()}")` to
inspect its own recent log output at runtime. Android's log daemon restricts
log visibility by the reading process's UID, which permits an app to read
its own log entries without the `READ_LOGS` permission (that permission
governs access to *other* processes' logs). This has been verified working
on the tested POCO/HyperOS configuration; note that SELinux policy
enforcement varies across Android versions and OEM skins, so this specific
self-monitoring approach should be re-validated if deployed on a
substantially different device or OS build.

**External verification**: Section 7 additionally documents the manual
`adb logcat --pid=<PID>` workflow used by the researcher to visually confirm
delegate behavior from a connected workstation during development and
pre-flight validation. The two serve complementary purposes -- automated
in-app gating during unattended trial runs, and manual visual confirmation
during setup and debugging.

**Observed runtime profile (FP32 models, this device):**

- **Graph partition split (63 / 1)**: for YOLOv11n, 63 operations execute on
  the Adreno GPU via `TFLiteGpuDelegateV2`. The final post-processing
  operation (`CUSTOM TFLite_Detection_PostProcess`, the NMS step) is
  rejected by the delegate due to its sequential branching logic and runs
  on CPU.
- **Driver fallback (OpenCL to OpenGL)**: the test environment does not
  expose `libOpenCL.so`. This is consistent with widely reported TFLite/
  Android ecosystem behavior across multiple OEMs, not a device-specific
  anomaly. The runtime falls back to its OpenGL compute shader backend.
  Reported GPU results in this study reflect GPU-via-OpenGL performance,
  not OpenCL-backend performance, and are labeled accordingly.
- **Kernel-count gate**: a `Created 0 GPU delegate kernels` result is
  treated as a failed GPU trial and excluded, preventing disguised CPU
  execution from being reported as GPU data.

---

## 5. Pinned Build Environment & Dependencies

This codebase intentionally targets the pre-LiteRT-migration TensorFlow Lite
namespace throughout. Mixing artifact namespaces (legacy `org.tensorflow`
alongside the newer `com.google.ai.edge.litert`) has been observed to cause
binary symbol incompatibility and native `dlopen` failures on this device --
keep all TFLite-related dependencies below on the legacy namespace together.

```groovy
dependencies {
    // Core engine dependencies (legacy org.tensorflow namespace, unified)
    implementation 'org.tensorflow:tensorflow-lite:2.9.0'
    implementation 'org.tensorflow:tensorflow-lite-gpu:2.9.0'
    implementation 'org.tensorflow:tensorflow-lite-support:0.4.0'
    implementation 'org.tensorflow:tensorflow-lite-task-vision:0.4.0'

    // Pinned native GPU delegate plugin, version-matched to task-vision 0.4.0.
    // This exact pairing was arrived at empirically after a native dlopen
    // failure with an unmatched version -- do not upgrade independently.
    implementation 'org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.0'
}
```

- Target SDK: 32 (Android 12L)
- Minimum SDK: 29 (Android 10) -- required by
  `PowerManager.getCurrentThermalStatus()`, used in `TelemetryCollector.kt`
- JDK: Java 8 / 11 compatible toolchain
- Gradle Wrapper: 7.1.2

**Package identifier**: this project retains the original fork's application
ID, `org.tensorflow.lite.examples.objectdetection`. This has not yet been
tested for rebrand safety; renaming `applicationId` is a routine operation
for forked Android apps and is not generally expected to affect a prebuilt
AAR dependency's native JNI bindings, but this has not been independently
verified for this specific dependency set. Left unchanged pending a
low-stakes rename test on a separate branch.

---

## 6. Telemetry & Metrics Schema

Every benchmark run produces structured CSV artifacts.

### `raw_latencies.csv` (per trial)

| Column | Type | Description |
|---|---|---|
| `InferenceIndex` | Integer | Monotonically increasing step index |
| `LatencyMs` | Float | Per-inference latency (nanosecond-resolution timer, ms) |

### `telemetry.csv` (per trial, sampled at 1 Hz)

| Column | Type | Description |
|---|---|---|
| `TimestampMs` | Long | Unix epoch timestamp of the sample |
| `PssKb` | Long | Proportional Set Size memory (KB) |
| `RawCurrentUa` | Long | Instantaneous battery current draw (microamperes) |
| `BatteryPct` | Integer | Battery state of charge (0-100) |
| `TemperatureC` | Float | Battery temperature (Celsius) |
| `ThermalStatus` | Integer | `PowerManager.THERMAL_STATUS_*` code |

### `master_benchmark_results.csv` (global rollup)

```
ModelName,Delegate,Threads,TotalInferences,AvgLatencyMs,MedianLatencyMs,MaxLatencyMs,OSInterferences,PeakMemKb,StartBattery,EndBattery,StartTempC,MaxTempC,BatteryHealth
```

`AvgLatencyMs` and `MedianLatencyMs` are computed from the cleaned latency
array only, excluding any single inference exceeding the 500ms
`OSInterferences` anomaly threshold.

---

## 7. Build & Execution Workflow

### Step 1: Model asset placement
```bash
cp /path/to/yolo11n.tflite app/src/main/assets/yolo11n.tflite
```

### Step 2: Compile and deploy
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Manually monitor a trial via logcat (setup/debugging)
```bash
adb shell am force-stop org.tensorflow.lite.examples.objectdetection
adb logcat -c
adb shell pidof org.tensorflow.lite.examples.objectdetection

# Unix/macOS:
adb logcat --pid=<PID> | grep -iE "tflite|TFLiteDelegate_PROOF|Orchestrator"

# Windows:
adb logcat --pid=<PID> | findstr /I "tflite TFLiteDelegate_PROOF Orchestrator"
```

Note: this manual workflow is for setup and debugging visibility. During
unattended trial runs, the orchestrator's in-app self-monitoring (Section 4)
performs the equivalent check automatically.

---

## 8. Citation

If you use this harness or the accompanying study, please cite both:

```bibtex
@software{edge_surveillance_benchmark,
  title  = {EdgeSurveillanceBenchmark: An Android Telemetry Harness for
            On-Device ML Inference Profiling},
  author = {Ali, Zeeshan},
  year   = {2026},
  url    = {https://github.com/Zeeshndev/EdgeSurveillanceBenchmark}
}

@article{beyond_simulated_latency,
  title   = {Beyond Simulated Latency: An Empirical Study of Battery,
             Thermal, and Memory Costs of INT8-Quantized YOLOv10 and
             YOLOv11 for On-Device Vehicle Surveillance},
  author  = {Ali, Zeeshan},
  journal = {Transactions on Machine Learning Research},
  year    = {2026},
  note    = {In preparation}
}
```

---

## Contributing

See [`CONTRIBUTING.md`](./CONTRIBUTING.md).

## License

Apache License 2.0. See [`LICENSE`](./LICENSE) for full text.
