# EdgeSurveillanceBenchmark: Thermal & Latency Profiling for Edge AI

[![Android CI](https://github.com/Zeeshndev/EdgeSurveillanceBenchmark/actions/workflows/android-build.yml/badge.svg)](https://github.com/Zeeshndev/EdgeSurveillanceBenchmark/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%2012%2B%20%7C%20HyperOS-orange.svg)](https://developer.android.com)
[![Engine](https://img.shields.io/badge/TFLite-v2.9.0%20Task--Vision-green.svg)](https://www.tensorflow.org/lite)

An empirical benchmarking harness engineered to evaluate thermal degradation, battery drain, and latency drift of nano-scale object detectors (YOLOv10n and YOLOv11n) on resource-constrained Android edge hardware under sustained 20-hour inference workloads.

This repository serves as the official mobile evaluation artifact for our research paper submitted to *Transactions on Machine Learning Research (TMLR)* and *Journal of Open Source Software (JOSS)*.

---

## 1. Research Scope & Architectural Focus

Deploying real-time vision algorithms on edge surveillance systems demands continuous inference over hours of operation. Standard mobile ML benchmarks typically execute brief, unthrottled bursts (10 to 60 seconds), masking critical system-level behaviors:
* **Thermal Throttling & Clock Clamping:** Degradation of frame latencies as SoC junction temperatures reach thermal trip points.
* **Hardware Delegate Graph Partitioning:** Friction points where non-supported operator layers (e.g., Dual-Focus / Non-Maximum Suppression post-processing) force silent fallbacks to the CPU.
* **Driver Layer Fallbacks:** Transparent delegation re-routing (e.g., OpenCL to OpenGL) across proprietary OEM Android distributions.

`EdgeSurveillanceBenchmark` replaces volatile UI-driven camera evaluation loops with a hardened, deterministic orchestrator running headless in a dedicated thread pool with real-time hardware telemetry logging.

---

## 2. Core Harness Architecture

```text
                       ┌─────────────────────────────┐
                       │    BenchmarkOrchestrator    │
                       └──────────────┬──────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        ▼                             ▼                             ▼
┌───────────────┐             ┌───────────────┐             ┌───────────────┐
│ DatasetFeeder │             │ In-Memory TFL │             │   Telemetry   │
│ (Pre-decoded  │ ──────────► │  Interpreter  │ ──────────► │   Collector   │
│   Bitmaps)    │             │  (CPU / GPU)  │             │ (PSS/mA/°C/Th)│
└───────────────┘             └───────┬───────┘             └───────┬───────┘
                                      │                             │
                                      ▼                             ▼
                       ┌─────────────────────────────┐
                       │       MetricsExporter       │
                       │   (CSV 14-Column Schemas)   │
                       └─────────────────────────────┘


                       The system is decoupled into five primary components:

BenchmarkOrchestrator.kt: Controls trial life cycles, dynamic warm-up convergence loops (CV < 0.05), fail-loud execution gates, and process wake-lock management.

DatasetFeeder.kt: Feeds standardized input frames (pre-scaled from the UA-DETRAC surveillance benchmark) directly into memory buffers, isolating neural inference from camera HAL acquisition noise.

TelemetryCollector.kt: Samples system health at 1 Hz via Android OS hooks: Proportional Set Size (PSS memory), raw battery current draw (BatteryManager.BATTERY_PROPERTY_CURRENT_NOW in µA), battery percentage, thermal status, and battery temperature.

ObjectDetectorHelper.kt: Wraps the underlying native TensorFlow Lite runtime, managing delegate attachment, model binary initialization, and bounding box decoding.

MetricsExporter.kt: Emits isolated, atomic per-trial CSV logs and appends normalized run vectors to a consolidated master rollup dataset.

3. Physical Gatekeeping & Environmental Controls
To prevent noisy data contamination from transient OS state or battery hysteresis, the harness strictly enforces the pre-flight checks outlined in PHYSICAL_CONTROLS.md:

State-of-Charge Boundary (85%–95%): Inference will not trigger outside this window to prevent internal battery cell resistance spikes that skew voltage and current telemetry at low charges.

Unplugged State Verification: USB cable connection is verified. Running while tethered to a charger introduces battery charge current vectors that distort power telemetry and alter heat dissipation.

Xiaomi HyperOS Battery Health Quirk: On HyperOS, BatteryManager.EXTRA_HEALTH returns 1 (BATTERY_HEALTH_UNKNOWN) rather than standard 2 (BATTERY_HEALTH_GOOD). The orchestrator tags this as UNKNOWN_1 and maps it directly against native dialer diagnostic checks (*#*#6485#*#* parameter MB_06: Good).

OEM OS Mitigation Triad: To eliminate background process interference on Xiaomi/HyperOS devices, three platform settings must be applied:

Autostart: Enabled.

Battery Saver Profile: Configured to "No Restrictions" (prevents background thread scheduling clamps).

Recent Apps Task Lock: Pinned in Recents to prevent aggressive OS memory reclamation.

4. Delegate Forensics & Graph Partitioning
When targeting hardware delegates on edge platforms, standard runtime implementations can silently fall back to CPU execution without throwing runtime exceptions. This harness incorporates native logcat stream parsing to guarantee execution integrity:

[UI Trigger] ──► Force Delegate Rebuild ──► Verify Kernel Allocation (N > 0)
                                                    │
                                  ┌─────────────────┴─────────────────┐
                                  ▼                                   ▼
                            Valid (>0)                            Zero (==0)
                     Tag Backend & Launch               ABORT (Prevent Silent CPU Run)

                     Observed Runtime Profile:
Graph Partition Split (63 / 1): For YOLOv11n, 63 neural network operations execute on the Adreno GPU via TFLiteGpuDelegateV2. The final post-processing operation (CUSTOM TFLite_Detection_PostProcess) is rejected by the delegate due to sequential NMS branching logic and runs on the CPU.

Driver Fallback (OpenCL -> OpenGL): The test environment lacks an accessible vendor driver path for libOpenCL.so. As is consistent with documented Android ecosystem behavior across multiple OEMs, the TFLite runtime falls back to its OpenGL compute shader backend (Initialized OpenGL-based API).

Automated Kernel Gate: The orchestrator reads its own PID's logcat stream post-initialization. If Created 0 GPU delegate kernels is detected, the run aborts immediately, preventing disguised CPU execution from contaminating GPU trials. All CSV artifacts dynamically record GPU (OpenGL) rather than an unqualified GPU label.

5. Pinned Build Environment & Dependencies
This codebase depends on legacy TensorFlow Lite C++ bindings before the ecosystem migration to LiteRT. Mixing artifact namespaces causes binary symbol incompatibility and runtime dlopen failures.

Ensure the following dependencies remain pinned in app/build.gradle:

dependencies {
    // Core Engine Dependencies (Unified Namespace)
    implementation 'org.tensorflow:tensorflow-lite:2.9.0'
    implementation 'org.tensorflow:tensorflow-lite-gpu:2.9.0'
    implementation 'org.tensorflow:tensorflow-lite-support:0.4.0'
    implementation 'org.tensorflow:tensorflow-lite-task-vision:0.4.0'
    
    // Pinned native GPU delegate plugin matching task-vision v0.4.0 ABI
    implementation 'org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.0'
}

Target SDK: 32 (Android 12L)

Minimum SDK: 21 (Android 5.0 Lollipop)

JDK Version: Java 8 / Java 11 compatible toolchain

Gradle Wrapper: 7.1.2

## 6. Telemetry & Metrics Schema

Every benchmark run produces three structured CSV artifacts stored under the application's external storage directory (`/sdcard/Android/data/org.tensorflow.lite.examples.objectdetection/files/`):

### 1. `raw_latencies.csv` (Per-Trial)

Contains monotonic inference measurements for all completed frame cycles:

| Header | Type | Description |
| :--- | :--- | :--- |
| `InferenceIndex` | Integer | Monotonically increasing step index (0 to N). |
| `LatencyMs` | Float | Frame inference time in milliseconds (precision: 3 decimal places). |

### 2. `telemetry.csv` (Per-Trial)

Sampled continuously at 1 Hz during the trial:

| Header | Type | Description |
| :--- | :--- | :--- |
| `TimestampMs` | Long | UNIX epoch timestamp of measurement. |
| `PssKb` | Long | Proportional Set Size total memory consumption in kilobytes. |
| `RawCurrentUa` | Long | Instantaneous battery current draw in microamperes (µA). |
| `BatteryPct` | Integer | Battery state-of-charge percentage (0–100). |
| `TemperatureC` | Float | Battery and system junction temperature in degrees Celsius. |
| `ThermalStatus` | Integer | OS thermal status code (`PowerManager.THERMAL_STATUS_*`). |

### 3. `master_benchmark_results.csv` (Global Rollup)

Consolidated ledger containing the 14-column summary metric vector across all models and execution delegates:

```csv
ModelName,Delegate,Threads,TotalInferences,AvgLatencyMs,MedianLatencyMs,MaxLatencyMs,OSInterferences,PeakMemKb,StartBattery,EndBattery,StartTempC,MaxTempC,BatteryHealth