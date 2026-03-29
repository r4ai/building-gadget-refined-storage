# Building Gadget Refined Storage Bridge

> 日本語版は [README_ja.md](./README_ja.md) を参照してください。

A Kotlin-based mod for NeoForge `1.21.1`.

Adds the `refined_storage_bridge` block. By specifying this block as the `bind to inventory` target in Building Gadgets 2, the Building Gadget can insert and extract items and fluids from a Refined Storage 2 network.

## Prerequisites

- [Building Gadgets 2](https://www.curseforge.com/minecraft/mc-mods/building-gadgets-2) `1.3.9`
- [Refined Storage 2](https://www.curseforge.com/minecraft/mc-mods/refined-storage) `2.0.1`

## Installation

1. Run `./gradlew build`.
2. Place the generated jar from `build/libs/` into the Minecraft `mods/` folder.

```
build/libs/
├── refined_storage_bridge-<version>.jar         ← place this in mods/
└── refined_storage_bridge-<version>-sources.jar ← sources (optional)
```

## Usage

1. Build a Refined Storage 2 network and connect the `refined_storage_bridge` block to a cable.
2. While holding a Building Gadget, right-click the bridge block to `bind to inventory`.
3. When placing or exchanging blocks with the Gadget, items will be pulled from the RS network.
4. Removed blocks are returned to the RS network.

### Checking Status

Right-clicking the bridge block with an empty hand shows the current connection state.

| Display       | State                                                            |
| ------------- | ---------------------------------------------------------------- |
| Connected     | Successfully connected to the RS network                         |
| Disconnected  | The cable is disconnected or no network was found                |
| Inactive      | The network does not have enough power                           |

When power stops or the cable is cut, the bridge immediately halts operation (fail-closed).

Inventory layout rebuilds are batched at most once per ~1 second, even during continuous network updates. Actual item insertion and extraction continue to work normally in the meantime, but there may be a slight delay before the item count and sort order are reflected in the GUI.

## Limitations

- No GUI.
- Only `ItemHandler.BLOCK` is used for the connection with Building Gadgets 2 (other interfaces are not supported).
- Connects to Refined Storage 2 via the official API. Runtime reflection is used only for display conversion of resource names.
- Owner restrictions and RS security integration are not implemented.

---

## For Developers

### Setting Up the Environment

- Java 21 is required.

```bash
# Run tests
./gradlew test

# Launch client (dev environment bundled with Building Gadgets 2 and RS 2)
./gradlew runClient
```

### Verification Steps

1. Run `./gradlew runClient`.
2. Build a Refined Storage 2 network and connect `refined_storage_bridge` to a cable.
3. `bind to inventory` the Building Gadget to the bridge.
4. Confirm that item consumption and returns during `build` / `exchange` are reflected in the RS network.
5. Use a water or lava source and confirm that placement and retrieval are reflected in the RS network.
6. Cut the cable or stop the power and confirm that the bridge operates fail-closed.

### Performance Benchmarking

You can measure the slot layout build cost of `BridgeItemHandler` while varying the number of item types in the network.

```bash
./gradlew test --rerun-tasks --tests "*.BridgePerformanceBenchmark"
```

#### Benchmark Scenarios

| Scenario                                           | Description                                                                                                          |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| **Scenario 1** Forced Rebuild                      | Advances the backend revision every time, measuring the cost of the cold-path (no cache).                            |
| **Scenario 2** Cache Hit                           | Does not cross a tick boundary. Returns the cached layout, so constant time regardless of N.                         |
| **Scenario 3** Full Tick Simulation                | Steady-state with no changes, calling `getStackInSlot()` on every slot. **The official KPI.**                        |
| **Scenario 4** `ProjectionBuilder.build()` alone   | Pure build cost excluding handler overhead.                                                                          |

Each scenario is run with 10 / 100 / 1000 / 5000 item types and statistics are displayed.

#### Output Column Descriptions

| Column  | Meaning                                                                       |
| ------- | ----------------------------------------------------------------------------- |
| `(N=…)` | Number of item types in the network (simulated).                              |
| `n=…`   | Number of samples actually measured after warm-up.                            |
| `min`   | Fastest single run. Lower bound when JIT is most effective.                   |
| `mean`  | Average of all samples. Susceptible to outliers.                              |
| `p50`   | Median. Represents typical throughput.                                        |
| `p95`   | Representative value under high load.                                         |
| `p99`   | "Near worst-case." **Use this value for threshold checks.**                   |
| `max`   | Slowest single run. Prone to incidental outliers such as GC pauses.           |

Time units are automatically switched between `ns` (nanoseconds), `us` (microseconds), and `ms` (milliseconds).

#### Performance Goal

The server's budget per tick is 50ms. The passing criterion is **Scenario 3 / N=5000 p99 < 1ms**.

#### Regression Detection in CI

```bash
./gradlew test --rerun-tasks --tests "*.BridgePerformanceBenchmark" -Dbenchmark.fail.threshold.ms=1
```

When `-Dbenchmark.fail.threshold.ms` is specified, the test fails if the p99 of Scenario 3 / N=5000 exceeds that value (in milliseconds). If omitted, results are only displayed and the test always passes.

#### Sample Output

```
=== Scenario 1: Cache Miss (layout rebuild per tick) ===
  cache-miss getSlots (N=10)     n=500  min=1.10us   mean=2.05us   p50=1.80us   p95=2.70us   p99=9.90us   max=65.60us
  cache-miss getSlots (N=100)    n=500  min=5.00us   mean=9.25us   p50=8.50us   p95=12.30us  p99=32.80us  max=100.40us
  cache-miss getSlots (N=1000)   n=500  min=44.20us  mean=96.47us  p50=73.60us  p95=254.40us p99=332.10us max=2.36ms
  cache-miss getSlots (N=5000)   n=200  min=207.30us mean=537.09us p50=463.20us p95=815.40us p99=3.18ms   max=5.01ms

=== Scenario 2: Cache Hit (cached layout returned) ===
  cache-hit  getSlots (N=10)     n=500  min=100ns    mean=232ns    p50=200ns    p95=200ns    p99=2.60us   max=9.40us
  cache-hit  getSlots (N=5000)   n=500  min=0ns      mean=53ns     p50=0ns      p95=100ns    p99=200ns    max=2.20us
```

### Customizing Textures

| Target          | Path                                                                     |
| --------------- | ------------------------------------------------------------------------ |
| Block textures  | `src/main/resources/assets/buildinggadgetrefinedstorage/textures/block/` |
| Item icons      | `src/main/resources/assets/buildinggadgetrefinedstorage/textures/item/`  |

When changing textures, also update the `textures` field in `models/block/*.json` or `models/item/*.json` accordingly.

- Format: PNG (transparency supported)
- Block: `16x16` (higher resolutions like `32x32` or `64x64` are also fine; keep the aspect ratio square)
- Item: `16x16` transparent PNG (square recommended)

**Example: Replacing `refined_storage_bridge` with a custom texture**

1. Create `textures/block/refined_storage_bridge.png`.
2. Reference the file from the `textures` field in `models/block/refined_storage_bridge_off.json` and `refined_storage_bridge_on.json`.

**Example: Replacing the `fluid_proxy` item with a custom texture**

1. Create `textures/item/fluid_proxy.png`.
2. Set the file in the `layer0` field of `models/item/fluid_proxy.json`.
