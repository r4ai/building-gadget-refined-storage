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

## テクスチャ配置

- ブロックのテクスチャは `src/main/resources/assets/buildinggadgetrefinedstorage/textures/block/` に置く
- アイテムアイコンは `src/main/resources/assets/buildinggadgetrefinedstorage/textures/item/` に置く
- 現在のモデルは vanilla の仮テクスチャを参照しているため、差し替える場合は `models/block/*.json` と `models/item/*.json` の `textures` を追加・変更する
- Minecraft の標準どおり PNG を使う
- ブロックは `16x16` のタイル前提。解像度を上げるなら `32x32` や `64x64` でもよいが、縦横比は正方形でそろえる
- アイテムは通常 `16x16` を基準にした透過 PNG。大きめに描く場合も正方形推奨
- `refined_storage_bridge` を独自化するなら、たとえば `textures/block/refined_storage_bridge.png` を作って `models/block/refined_storage_bridge_off.json` / `refined_storage_bridge_on.json` から参照する
- `fluid_proxy` を独自化するなら、たとえば `textures/item/fluid_proxy.png` を作って `models/item/fluid_proxy.json` の `layer0` に設定する
