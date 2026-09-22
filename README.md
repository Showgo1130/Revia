# Revia

紙の参考書・問題集の**目次を AI で読み取り**、章・節・問題ごとに「完了」「要復習」などの状態やメモを記録して、次に見直す箇所を管理する Android アプリ。

Kotlin + Jetpack Compose / minSdk 26 / targetSdk 36

> **開発中です。** 2026-09-23 時点で機能はまだ入っていません。開発の進め方（下）を先に通してあり、CI が緑で回る骨組みだけがあります。

## なぜ公開しているか

このリポジトリは、**アプリそのものと同じくらい「どう作っているか」を見てもらうため**に公開しています。Issue の立て方、PR 本文の構成、AI によるレビュー、CI と git hooks による守り——それらが実際に動いている様子が履歴に残ります。

## 開発の進め方

```
Issue を立てる  →  branch を切る  →  実装  →  PR  →  AI レビュー  →  人間が merge
```

| 決まり | 場所 |
|---|---|
| 何をしてよくて何をしてはいけないか（AI 向けの前提） | [AGENTS.md](AGENTS.md) |
| PR のリスク判定（🔴 高 / 🟡 中 / 🟢 低） | [docs/pr-risk-policy.md](docs/pr-risk-policy.md) |
| PR 本文の構成 | [.github/pull_request_template.md](.github/pull_request_template.md) |

- **main には直接 commit できません。** `.githooks/` の 3 本が拒否します（clone ごとに 1 回 `git config core.hooksPath .githooks`）
- **すべての PR で secret scan（gitleaks）が走ります。** 公開リポジトリなので、一度入った秘密情報は履歴から消せません
- **merge は必ず人間が行います。** AI は merge しません

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
