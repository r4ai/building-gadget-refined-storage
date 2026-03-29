# Building Gadget Refined Storage Bridge

> English version is available at [README.md](./README.md).

NeoForge `1.21.1` 向けの Kotlin 製 mod です。

`refined_storage_bridge` ブロックを追加します。このブロックを Building Gadgets 2 の `bind to inventory` 先に指定することで、Building Gadget が Refined Storage 2 のネットワークに対してアイテム・流体を出し入れできるようになります。

## 前提 Mod

- [Building Gadgets 2](https://www.curseforge.com/minecraft/mc-mods/building-gadgets-2) `1.3.9`
- [Refined Storage 2](https://www.curseforge.com/minecraft/mc-mods/refined-storage) `2.0.1`

## インストール

1. `./gradlew build` を実行する。
2. `build/libs/` に生成された jar を Minecraft の `mods/` フォルダに入れる。

```
build/libs/
├── refined_storage_bridge-<version>.jar         ← mods/ に入れるもの
└── refined_storage_bridge-<version>-sources.jar ← ソース（任意）
```

## 使い方

1. Refined Storage 2 のネットワークを構築し、`refined_storage_bridge` ブロックを cable に接続する。
2. Building Gadget を持った状態で、bridge ブロックを右クリックして `bind to inventory` する。
3. Gadget でブロックを設置・交換すると、アイテムが RS ネットワークから引き出される。
4. 撤去したブロックは RS ネットワークに返却される。

### ステータス確認

素手で bridge ブロックを右クリックすると、現在の接続状態を確認できます。

| 表示   | 状態                                                 |
| ------ | ---------------------------------------------------- |
| 接続中 | RS ネットワークに正常に接続されている                |
| 非接続 | cable が切断されているか、ネットワークが見つからない |
| 無効   | ネットワークの電力が不足している                     |

電力が止まるか cable が切れると、bridge は即座に動作を停止します（fail-closed）。

在庫表示のレイアウト再構築は、ネットワーク更新が連続しても最大で約 1 秒ごとにまとめて行われます。実際の入出力はその間も通常どおり処理されますが、GUI 上の種類数や表示順の反映には少し遅延が入ります。

## 制限事項

- GUI はありません。
- Building Gadgets 2 との接続には `ItemHandler.BLOCK` のみを使用します（他のインタフェースは未対応）。
- Refined Storage 2 とは公式 API を通じて接続します。ただし resource 名の表示変換にのみ runtime reflection を使用します。
- owner 制限や RS のセキュリティ連携は実装していません。

---

開発者向けの情報は [docs/development.md](./docs/development.md) を参照してください。
