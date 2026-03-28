# Building Gadget Refined Storage Bridge

NeoForge `1.21.1` 向けの Kotlin 製 mod です。`refined_storage_bridge` ブロックを Building Gadgets 2 の `bind to inventory` 先にし、Refined Storage 2 のネットワークからアイテムと流体を出し入れできます。

## セットアップ

- Java 21
- `./gradlew test`
- `./gradlew runClient`

`runClient` には Building Gadgets 2 `1.3.9` と Refined Storage 2 `2.0.1` を runtime 依存として含めています。

## 対応範囲

- アイテム
- 流体
- 専用ブロック `refined_storage_bridge`
- GUI なし
- 素手右クリックで `接続中 / 非接続 / 無効` を表示

## 制限

- Building Gadgets 2 とは `ItemHandler.BLOCK` のみで接続します
- Refined Storage 2 とは公開 API を使い、resource 表示変換のみ runtime reflection を使います
- owner 制限や RS セキュリティ連携は入れていません

## 動作確認

1. `./gradlew runClient` を実行する。
2. Refined Storage 2 のネットワークを作り、`refined_storage_bridge` を cable に接続する。
3. Building Gadget を bridge に `bind to inventory` する。
4. `build` / `exchange` でアイテム消費と返却が RS ネットワークへ反映されることを確認する。
5. 水または溶岩の source を使い、設置と回収が RS ネットワークへ反映されることを確認する。
6. cable を切るか電力を止め、bridge が fail-closed で動作することを確認する。

## パフォーマンス計測

`BridgeItemHandler` のスロットレイアウト構築コストを、アイテム種類数を変えながら定量的に計測できます。

### 実行

```bash
./gradlew test --rerun-tasks --tests "*.BridgePerformanceBenchmark"
```

### 計測シナリオ

| シナリオ | 内容 |
|---|---|
| **Scenario 1** 強制再構築 | backend revision を毎回進め、内容変更直後の cold-path を測る。 |
| **Scenario 2** キャッシュヒット | ティックが変わらない場合。キャッシュ済みレイアウトを返すだけなので N に依存しない定数時間。 |
| **Scenario 3** フルティックシミュレーション | 変更のない steady-state で、キャッシュ済みレイアウトに対する全スロットへの `getStackInSlot()` 呼び出し。正式 KPI。 |
| **Scenario 4** `ProjectionBuilder.build()` 単体 | ハンドラのオーバーヘッドを除いたビルドコスト。 |

各シナリオをアイテム種類数 10 / 100 / 1000 / 5000 で実行し、統計値を表示します。

### 出力列の意味

| 列 | 意味 |
|---|---|
| `(N=…)` | テストに使ったアイテムの **種類数**。Refined Storage ネットワークに何種類格納されているかを模擬する。 |
| `n=…` | **サンプル数**（ウォームアップ後に実際に計測した回数）。 |
| `min` | 計測した中で最も速かった1回の時間。JIT が最もうまく働いた状態の下限。 |
| `mean` | 全サンプルの平均時間。外れ値の影響を受けやすい。 |
| `p50` | **中央値**。サンプルの 50% がこの値以下。典型的なスループットを表す。 |
| `p95` | サンプルの 95% がこの値以下。負荷が高めのときの代表値。 |
| `p99` | サンプルの 99% がこの値以下。**「ほぼ最悪に近いケース」**。閾値チェックにはこの値を使う。 |
| `max` | 計測した中で最も遅かった1回。GC ポーズなど偶発的な外れ値が出やすい。 |

時間の単位は `ns`（ナノ秒）・`us`（マイクロ秒）・`ms`（ミリ秒）で自動切り替えされます。

### パフォーマンス目標

サーバーの 1 ティック予算は 50ms。正式な合格条件は **Scenario 3 / N=5000 の p99 < 1ms**。

### 閾値付き実行（CI 等での回帰検知）

```bash
./gradlew test --rerun-tasks --tests "*.BridgePerformanceBenchmark" -Dbenchmark.fail.threshold.ms=1
```

`-Dbenchmark.fail.threshold.ms` を指定すると、Scenario 3 / `N=5000` の p99 がその値（ミリ秒）を超えたときにテストを失敗させます。省略時はタイミングを出力するだけで常に成功します。

### 出力例

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

## テクスチャ配置

- ブロックのテクスチャは `src/main/resources/assets/buildinggadgetrefinedstorage/textures/block/` に置く
- アイテムアイコンは `src/main/resources/assets/buildinggadgetrefinedstorage/textures/item/` に置く
- 現在のモデルは vanilla の仮テクスチャを参照しているため、差し替える場合は `models/block/*.json` と `models/item/*.json` の `textures` を追加・変更する
- Minecraft の標準どおり PNG を使う
- ブロックは `16x16` のタイル前提。解像度を上げるなら `32x32` や `64x64` でもよいが、縦横比は正方形でそろえる
- アイテムは通常 `16x16` を基準にした透過 PNG。大きめに描く場合も正方形推奨
- `refined_storage_bridge` を独自化するなら、たとえば `textures/block/refined_storage_bridge.png` を作って `models/block/refined_storage_bridge_off.json` / `refined_storage_bridge_on.json` から参照する
- `fluid_proxy` を独自化するなら、たとえば `textures/item/fluid_proxy.png` を作って `models/item/fluid_proxy.json` の `layer0` に設定する
