# <img src="docs/images/icon.png" width="48" height="48" align="center"> Building Gadget Refined Storage Bridge

https://github.com/user-attachments/assets/5f76aac1-23f3-4aa1-92b2-9a51376dad63

> 日本語版は [README_ja.md](./README_ja.md) を参照してください。

A Kotlin-based mod for NeoForge `1.21.1`.

Adds the `refined_storage_bridge` block. By specifying this block as the `bind to inventory` target in Building Gadgets 2, the Building Gadget can insert and extract items and fluids from a Refined Storage 2 network.

## Prerequisites

- [Building Gadgets 2](https://www.curseforge.com/minecraft/mc-mods/building-gadgets) `1.3.9`
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
