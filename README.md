# Revia

紙の参考書や問題集の目次を撮って取り込み、章・節・問題ごとに「できた」「もう一度」を記録するアプリです。**本に書き込む代わり**になることを目指しています。

Android アプリ / Kotlin + Jetpack Compose / minSdk 26 / targetSdk 36

[![Revia の画面デザイン。左から教材一覧・教材詳細・復習一覧](docs/ui/screenshot.png)](https://showgo1130.github.io/Revia/ui/)

**[14 画面のデザインを見る](https://showgo1130.github.io/Revia/ui/)** — ブラウザで触れます。実装前のモックです。

> **開発中です。** 2026-09-23 時点で、設計と画面デザインが決まり、CI が回る骨組みだけがあります。機能はまだ入っていません。

## 設計

作る前に決めたことを `docs/` に残しています。

- **[コンセプト](docs/concept.md)** — 何を解決したいのか。紙と比べて何で勝てて、何を諦めるか
- **[デザイン](docs/design.md)** — 14 画面の仕様と、色・寸法・書体の値
- **[決定の記録](docs/decisions.md)** — そう決めた理由と、捨てた案（38 件）
- **[規約と権利の調査](docs/research/ai-terms.md)** — 目次を AI に読ませることの整理

## ビルド

```bash
# clone ごとに 1 回
git config core.hooksPath .githooks
echo "sdk.dir=<Android SDK のパス>" > local.properties

./gradlew testDebugUnitTest   # 単体テスト
./gradlew assembleDebug       # debug APK
```

JDK 17 以上が要ります。

## 公開していないもの

AI 解析の API キー、署名鍵、撮影した目次の画像、個人の学習データ。詳細は [AGENTS.md](AGENTS.md) に書いています。
