# 開発者向け

## 環境構築

- Java 21 が必要です。

```bash
# テスト実行
./gradlew test

# クライアント起動（Building Gadgets 2 と RS 2 を同梱した開発環境）
./gradlew runClient
```

## 動作確認手順

1. `./gradlew runClient` を実行する。
2. Refined Storage 2 のネットワークを作り、`refined_storage_bridge` を cable に接続する。
3. Building Gadget を bridge に `bind to inventory` する。
4. `build` / `exchange` でアイテム消費と返却が RS ネットワークに反映されることを確認する。
5. 水または溶岩の source を使い、設置・回収が RS ネットワークに反映されることを確認する。
6. cable を切るか電力を止め、bridge が fail-closed で動作することを確認する。

## パフォーマンス計測

`BridgeItemHandler` のスロットレイアウト構築コストを、ネットワーク内のアイテム種類数を変えながら計測できます。

```bash
./gradlew test --rerun-tasks --tests "*.BridgePerformanceBenchmark"
```

### 計測シナリオ

| シナリオ                                        | 内容                                                                                        |
| ----------------------------------------------- | ------------------------------------------------------------------------------------------- |
| **Scenario 1** 強制再構築                       | backend revision を毎回進め、キャッシュなし（cold-path）のコストを測る。                    |
| **Scenario 2** キャッシュヒット                 | ティックをまたがない場合。キャッシュ済みレイアウトを返すだけなので N に依存しない定数時間。 |
| **Scenario 3** フルティックシミュレーション     | 変更のない steady-state で、全スロットに `getStackInSlot()` を呼び出す。**正式な KPI**。    |
| **Scenario 4** `ProjectionBuilder.build()` 単体 | ハンドラのオーバーヘッドを除いた純粋なビルドコスト。                                        |

各シナリオをアイテム種類数 10 / 100 / 1000 / 5000 で実行し、統計値を表示します。

### 出力列の意味

| 列      | 意味                                                         |
| ------- | ------------------------------------------------------------ |
| `(N=…)` | ネットワーク内のアイテム種類数（模擬値）。                   |
| `n=…`   | ウォームアップ後に実際に計測したサンプル数。                 |
| `min`   | 最速の1回。JIT が最も効いた状態の下限。                      |
| `mean`  | 全サンプルの平均。外れ値の影響を受けやすい。                 |
| `p50`   | 中央値。典型的なスループットを表す。                         |
| `p95`   | 負荷が高いときの代表値。                                     |
| `p99`   | 「ほぼ最悪に近いケース」。**閾値チェックにはこの値を使う。** |
| `max`   | 最遅の1回。GC ポーズなど偶発的な外れ値が出やすい。           |

時間の単位は `ns`（ナノ秒）・`us`（マイクロ秒）・`ms`（ミリ秒）で自動切り替えされます。

### パフォーマンス目標

サーバーの 1 ティック予算は 50ms。合格条件は **Scenario 3 / N=5000 の p99 < 1ms**。

### CI での回帰検知

```bash
./gradlew test --rerun-tasks --tests "*.BridgePerformanceBenchmark" -Dbenchmark.fail.threshold.ms=1
```

`-Dbenchmark.fail.threshold.ms` を指定すると、Scenario 3 / N=5000 の p99 がその値（ミリ秒）を超えたときにテストが失敗します。省略時は結果を表示するだけで常に成功します。

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

## テクスチャのカスタマイズ

| 対象               | 配置先                                                                   |
| ------------------ | ------------------------------------------------------------------------ |
| ブロックテクスチャ | `src/main/resources/assets/buildinggadgetrefinedstorage/textures/block/` |
| アイテムアイコン   | `src/main/resources/assets/buildinggadgetrefinedstorage/textures/item/`  |

テクスチャを変更する場合は、`models/block/*.json` または `models/item/*.json` の `textures` フィールドも合わせて更新してください。

- フォーマット：PNG（透過可）
- ブロック：`16x16`（高解像度にする場合は `32x32` や `64x64` でも可。縦横比は正方形を維持）
- アイテム：`16x16` の透過 PNG（正方形推奨）

**例：`refined_storage_bridge` を独自テクスチャに差し替える場合**

1. `textures/block/refined_storage_bridge.png` を作成する。
2. `models/block/refined_storage_bridge_off.json` と `refined_storage_bridge_on.json` の `textures` からそのファイルを参照する。

**例：`fluid_proxy` アイテムを独自テクスチャに差し替える場合**

1. `textures/item/fluid_proxy.png` を作成する。
2. `models/item/fluid_proxy.json` の `layer0` にそのファイルを設定する。
