# Architecture

Building Gadget Refined Storage Bridge は、Building Gadgets mod と Refined Storage 2 を統合する Minecraft NeoForge mod（Kotlin）。
ブリッジブロックを介してストレージネットワークへの遠隔アクセスを提供する。

## ディレクトリ構成

```
src/main/kotlin/io/github/r4ai/buildinggadgetrefinedstorage/
├── BuildingGadgetRefinedStorageMod.kt           # エントリポイント & Capability 登録
├── content/
│   ├── ModContent.kt                            # ブロック・アイテム・コンポーネントのレジストリ
│   └── FluidProxyItem.kt                        # 流体プロキシアイテム実装
└── bridge/
    ├── core/                                    # プラットフォーム非依存のブリッジロジック
    │   ├── BridgeBackend.kt                    # コア抽象インターフェース
    │   ├── BridgeSnapshot.kt                   # リソーススナップショットのデータクラス群
    │   ├── BridgeItemHandler.kt                # キャッシュ付き IItemHandler 実装
    │   ├── ProjectedSlot.kt                    # スロットレイアウトと射影型の定義
    │   ├── ProjectionBuilder.kt                # スナップショットから UI スロット配列を構築
    │   ├── FluidDescriptor.kt                  # 流体の同一性（type + components）
    │   ├── FluidProxyRef.kt                    # 流体プロキシへの参照 + モード
    │   ├── FluidProxyMode.kt                   # SPECIFIC / WILDCARD_INPUT モード
    │   ├── FluidProxyFluidHandler.kt           # IFluidHandlerItem 実装
    │   └── StackHelpers.kt                     # ユーティリティ拡張関数
    └── platform/                                # NeoForge / Refined Storage 統合層
        ├── RefinedStorageBridgeBackend.kt      # BridgeBackend の本番実装
        ├── RefinedStorageBridgeBlock.kt        # ブロックエンティティコンテナ
        ├── RefinedStorageBridgeBlockEntity.kt  # ティック処理 & 状態管理
        ├── RefinedStorageBridgeNetworkNode.kt  # Refined Storage ネットワークノード
        ├── RefinedStorageReflectionAdapter.kt  # RS 内部への Reflection ブリッジ
        ├── RefinedStorageBridgeStatus.kt       # ステータス列挙型
        └── BridgeLookup.kt                     # 座標から BridgeBackend を解決
```

**レイヤー分離の設計意図**: `core/` は Minecraft API のみに依存し、`platform/` は Refined Storage や NeoForge の具体実装に依存する。これにより `BridgeBackend` インターフェースを通じたテスタビリティが確保されている（`InMemoryBridgeBackend` をテスト時に差し替え可能）。

---

## コアデータ構造

### スナップショット型（不変）

```
BridgeSnapshot
├── itemResources: List<ItemResourceSnapshot>
└── fluidResources: List<FluidResourceSnapshot>

ItemResourceSnapshot
├── stableKey: String   ← ソートの安定キー（UI順序の決定論的保証）
├── prototype: ItemStack  ← count=1 のアイテム（同一性のみ保持）
└── amount: Long         ← ストレージ内の総量

FluidResourceSnapshot
├── stableKey: String
├── descriptor: FluidDescriptor
└── amount: Long
```

`BridgeSnapshot.EMPTY` は再利用可能な不変シングルトンとして定義されており、不活性状態での余分なアロケーションを防いでいる。

### ProjectedSlot（sealed interface）

```
ProjectedSlot
├── ItemResourceProjectedSlot   ← アイテムリソース + 表示用スタック
├── FluidResourceProjectedSlot  ← 流体リソース + FluidProxyRef
├── ItemInputProjectedSlot      ← 汎用アイテム入力スロット（シングルトン）
└── FluidInputProjectedSlot     ← ワイルドカード流体入力 + FluidProxyRef
```

sealed interface による網羅的な `when` 式でコンパイル時の型安全性を保証。インスタンスチェックは O(1)。

### SlotLayout

```kotlin
class SlotLayout(entries: List<ProjectedSlot>) {
    val entries: Array<ProjectedSlot>  // 配列バッキング（キャッシュ効率）
    val size: Int
    val itemInputIndex: Int?           // O(1) 直接参照
    val fluidInputIndex: Int?
    operator fun get(index: Int): ProjectedSlot?  // 境界安全アクセス
}
```

`List` ではなく `Array` でバッキングしている理由：GUI ティックごとに `getStackInSlot(slot)` がランダムアクセスされるため、配列の連続メモリレイアウトによるキャッシュ効率が重要になる。

### FluidDescriptor

```kotlin
data class FluidDescriptor(
    val fluidId: ResourceLocation,
    val components: DataComponentPatch = DataComponentPatch.EMPTY
)
```

流体の同一性を `type + components` で表現。モッド追加の流体データも `DataComponentPatch` を通じて正確に一致判定できる。

---

## アルゴリズムとパフォーマンス特性

### 1. バージョンベースキャッシュ（最重要最適化）

`BridgeItemHandler` はストレージ状態のバージョン番号でキャッシュ無効化を行う。

```
GUI ティック
    ↓ getSlots() / getStackInSlot(slot)
    ↓
currentLayout()
    ├─ backend.stateVersion() == cachedVersion? → O(1) 配列返却（キャッシュヒット）
    └─ バージョン不一致? → ProjectionBuilder.build() で再構築（キャッシュミス）
```

```kotlin
fun currentLayout(): SlotLayout {
    if (!backend.isActive()) {
        cachedLayout = SlotLayout.EMPTY
        cachedVersion = Long.MIN_VALUE
        return SlotLayout.EMPTY
    }
    val currentVersion = backend.stateVersion()
    if (currentVersion != cachedVersion) {
        cachedVersion = currentVersion
        cachedLayout = ProjectionBuilder.build(backend.snapshot(), position)
    }
    return cachedLayout
}
```

- **キャッシュヒット（定常状態）**: O(1) — Long の比較のみ
- **キャッシュミス（ストレージ変更時）**: O((n+m) log(n+m)) — ソート支配
- GUI 描画中はティックごとに呼ばれるが、ストレージ変更がなければ常に O(1)

### 2. ProjectionBuilder: ソート→射影の2フェーズ構築

`snapshot` を受け取り `SlotLayout` を構築する。

```
フェーズ1: アイテムリソースを stableKey でソート  O(n log n)
フェーズ2: 流体リソースを stableKey でソート      O(m log m)
フェーズ3: アイテムスロット生成（線形走査）        O(n)
フェーズ4: 流体スロット生成（線形走査）            O(m)
フェーズ5: ArrayList → Array 変換               O(n+m)
```

**全体計算量**: O((n+m) log(n+m))  ← ソートが支配的

`stableKey` でソートすることで UI のスロット順序を決定論的に保証する（ストレージの内部順序に依存しない）。

表示スタック数のクランプ:
```kotlin
displayCount = amount.coerceAtMost(maxStackSize.toLong()).coerceAtLeast(1)
```
`coerceAtLeast(1)` により、大量在庫でも `count=0` の「見えないスタック」が生まれない。

### 3. スナップショット取得: Reflection ベースの変換

Refined Storage の内部型（`ItemResource`, `FluidResource`）を直接使わず、Reflection でアクセスする。これはコンパイル時の依存を切り離しつつ、実行時に型情報を取得するためのトレードオフ。

```
storage.all の線形走査  O(n)
    ↓ 各リソースについて
    ├─ isInstance() チェック        O(1)
    ├─ toItemSnapshot() 呼び出し    O(1)（キャッシュ済みメソッドハンドル）
    └─ toFluidSnapshot() 呼び出し   O(1)
```

**全体計算量**: O(n) — Reflection のコストは Class 初回ロード時のみ。`lazy { }` で遅延初期化されたメソッドハンドルは 2 回目以降は O(1)。

### 4. ストレージバインディング同期: 参照等価性ベースの差分検出

```kotlin
private fun syncStorageBinding() {
    val binding = storageBindingProvider()
    if (binding?.identity === attachedStorageIdentity) return  // ===（参照等価）

    attachedStorage?.removeListener(storageListener)
    attachedStorageIdentity = binding?.identity
    attachedStorage = binding?.storage
    attachedStorage?.addListener(storageListener)
    stateVersion += 1
}
```

`==`（値等価）でなく `===`（参照等価）を使うことで、ストレージオブジェクトが実際に入れ替わったかどうかを O(1) で判定。`equals()` の重い計算を避けている。

### 5. リスナーによるバージョンインクリメント

```
Refined Storage 内部でアイテム増減
    ↓ RootStorageListener.changed() コールバック（同一スレッド・同期呼び出し）
    ↓ stateVersion += 1
    ↓ 次回 currentLayout() でキャッシュミス → 再構築トリガー
```

リスナーはブリッジ 1 つにつき 1 つだけアタッチされ、その処理は Long のインクリメントのみ。ストレージ操作のホットパスへの影響は無視できる。

---

## データフロー

```
Building Gadgets GUI
    │ IItemHandler.getSlots() / getStackInSlot(slot)
    ▼
BridgeItemHandler  ←──── キャッシュ層（バージョン番号で無効化）
    │ キャッシュミス時のみ
    ▼
ProjectionBuilder.build(snapshot, position)
    │ 入力スナップショット
    ▼
RefinedStorageBridgeBackend.snapshot()
    │ Reflection 変換
    ▼
StorageNetworkComponent.all  ←── Refined Storage ネットワーク全体


流体プロキシアイテム側（パイプ・ホッパー等から）:
IFluidHandlerItem.fill() / drain()
    ▼
FluidProxyFluidHandler  ←── ステートレス、backendResolver() でブリッジ解決
    │ SPECIFIC: 流体 ID + components を厳密一致
    │ WILDCARD_INPUT: 任意流体を受け入れ、抽出は不可
    ▼
RefinedStorageBridgeBackend.insertFluid() / extractFluid()
    ▼
StorageNetworkComponent.insert() / extract()
```

---

## サーバーティック処理

```kotlin
private fun serverTick(state: BlockState) {
    val network = mainNetworkNode.network
    val newOperational = if (network == null) {
        false
    } else {
        val energy = network.getComponent(EnergyNetworkComponent::class.java)
        if (energy.stored >= ENERGY_USAGE_PER_TICK) {
            energy.extract(ENERGY_USAGE_PER_TICK)  // 1 EU/tick 消費
            true
        } else {
            false
        }
    }
    mainNetworkNode.setActive(newOperational)
    if (newOperational != isOperational) {
        isOperational = newOperational
        // ブロック状態（発光プロパティ）を条件付き更新
        level?.setBlock(...)
    }
}
```

エネルギー不足でブリッジが停止すると `isActive() = false` → `currentLayout()` が即座に `SlotLayout.EMPTY` を返す。ブロック状態更新は実際に変化した時のみ発生（無駄な chunk dirty マークを防止）。

---

## パフォーマンスサマリー

| シナリオ | 計算量 | 備考 |
|---|---|---|
| GUI ティック（定常状態・キャッシュヒット） | **O(1)** | Long 比較のみ |
| スロット単体アクセス `getStackInSlot(slot)` | **O(1)** | 配列ランダムアクセス + when dispatch |
| ストレージ変更後の再構築 | **O((n+m) log(n+m))** | ソートが支配的 |
| スナップショット取得 `snapshot()` | **O(n)** | 線形走査 + キャッシュ済み Reflection |
| 流体プロキシ操作 `fill()` / `drain()` | **O(1)** | ステートレス、バックエンド委譲 |
| ストレージバインディング同期 | **O(1)** | 参照等価性チェックのみ |
| リスナーコールバック | **O(1)** | Long インクリメントのみ |

**ベンチマーク設計** (`BridgePerformanceBenchmark`): アイテム数 10 / 100 / 1,000 / 5,000 の各スケールで計測。キャッシュヒット時はアイテム数に依存しない O(1) 特性を検証する「Steady-State」シナリオと、バージョン変更後の再構築コストを測る「Forced Rebuild」シナリオの両方を含む。

---

## 設計上のトレードオフ

| 設計選択 | 理由 |
|---|---|
| Reflection で RS 内部型にアクセス | コンパイル時依存を排除し、RS のバイナリ互換性の影響を受けない |
| `Array` バッキングの `SlotLayout` | List より連続メモリレイアウトで GUI ティックのキャッシュ効率向上 |
| `stableKey` ソートによる UI 順序保証 | RS の内部コレクション順序は無保証なため、決定論的な表示に必須 |
| `===` 参照等価でのバインディング差分検出 | `equals()` の重い計算を避けつつ、ストレージスワップを O(1) で検出 |
| `BridgeSnapshot.EMPTY` / `SlotLayout.EMPTY` シングルトン | 非活性状態での不要アロケーションを防止 |
| `InMemoryBridgeBackend`（テスト用） | 本番 RS ネットワーク不要でユニットテスト・ベンチマーク実行可能 |
