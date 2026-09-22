# Revia

紙の参考書・問題集の**目次を AI で読み取り**、章・節・問題ごとに「完了」「要復習」などの状態やメモを記録して、次に見直す箇所を管理する Android アプリ。

Kotlin + Jetpack Compose / minSdk 26 / targetSdk 36

> **開発中です。** 2026-09-23 時点で機能はまだ入っていません。CI が緑で回る骨組みだけがあります。

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

AI 解析の API キー、署名鍵、ユーザーが撮影した目次画像、個人の学習データ。詳細は [AGENTS.md の §3](AGENTS.md)。
