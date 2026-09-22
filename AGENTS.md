# AGENTS.md — Revia（学習支援アプリ、Kotlin + Jetpack Compose）

AI エージェント（Claude Code / Codex など）がこのリポジトリで作業するときの前提。人間向けの使い方は [README.md](README.md)。

## 1. このリポジトリは何か

紙の参考書・問題集の**目次を AI で読み取り**、章・節・問題ごとに「完了」「要復習」などの状態やメモを記録して、次に見直す箇所を管理する Android アプリ。Kotlin + Jetpack Compose。

**公開リポジトリ。** 開発の進め方（Issue → branch → PR → AI レビュー → merge、CI、hooks）が外から見える状態にしてある。そのため、ここに置くものの基準が他のリポジトリより厳しい（§3）。

2026-09-23 時点では**機能はまだ 1 つも入っていない**。`app/` にあるのは起動画面の骨組みだけで、CI が緑で回る状態を先に作ってある。

## 2. 絶対に守ること

1. **秘密情報をリポジトリに置かない。** 公開なので、一度 commit したら履歴から消せない（§3）
2. **ユーザーの撮影画像と学習データをリポジトリに置かない。** サンプルは実データと混ざらない場所に置く（§3）
3. **使っていない権限を `AndroidManifest.xml` に先に置かない。** 権限はそれを使う機能を作る PR で足す
4. **根拠のない「問題ありません」を書かない。** 実行したコマンドと結果を添える。実行していない検証を実行済みと書かない
5. **履歴書き換え・remote の変更・権限設定の編集・`--no-verify` はしない。** 必要なら人間に依頼する

## 3. 秘密情報と公開しないもの

| 置かないもの | 置き場所 | `.gitignore` |
|---|---|---|
| AI 解析の API キー | 未定（#1 で決める）。**アプリに埋め込む案は公開リポジトリでは選べない**（APK から取り出せるうえ gitleaks が止める） | `.secrets/` `.env` |
| 署名鍵とパスワード | `keystore/` と `keystore.properties`（手元のみ） | `keystore/` `keystore.properties` `*.jks` `*.keystore` |
| ユーザーが撮影した目次画像 | 端末内のみ | `/sample-data/private/` |
| 個人の学習データ | 端末内のみ | `*.private.json` |

サンプルデータと設定例だけ公開する。secret scan（gitleaks）が PR と main への push の両方で走る。

## 4. コマンド

```bash
./gradlew testDebugUnitTest     # JVM 単体テスト（CI が走らせるもの）
./gradlew assembleDebug         # debug APK
./gradlew lint                  # Android Lint
```

`local.properties`（`sdk.dir`）は clone ごとに自分で作る。`.gitignore` 済み。

product flavor は作っていないので `testDebugUnitTest`。flavor を足したら CI（`.github/workflows/ci.yml`）の task 名も合わせる。

## 5. Git の進め方

- **main に直接 commit・push しない。** `.githooks/pre-commit` `pre-merge-commit` `pre-push` が拒否する（clone ごとに 1 回 `git config core.hooksPath .githooks`）。例外パスは無い
- 作業は Issue から始め、branch 名は `feat|fix|chore/<issue番号>-<slug>`。1 branch = 1 Issue = 1 責務
- **commit メッセージと PR タイトルは `<prefix>: <何をしたか>`**（日本語 1 行。PR は末尾に `（#n）`）。prefix は `feat`（ユーザーに見える機能）/ `fix`（バグ修正）/ `chore`（開発体制・ビルド・依存。テスト追加や挙動を変えない整理もここ）/ `docs`（文書だけ）/ `security`（秘密情報・権限・署名）の 5 つ
- Issue は `/create-issue`、進捗は `/update-issue`、PR は `/create-pr`（テンプレート `.github/pull_request_template.md`、リスク基準は [docs/pr-risk-policy.md](docs/pr-risk-policy.md)）。レビューは `/review`（code-reviewer）。squash merge は人間
- PR 前に `./gradlew testDebugUnitTest` を実行し結果を貼る。UI を触ったら実機スクショを添付する
- CI（`.github/workflows/ci.yml`）が PR ごとに単体テストを実行する。secret scan（gitleaks）も走る。**red のまま merge しない**
- リリース作業（署名付き AAB の作成、versionCode の確定、Play Console へのアップロード）は 🔴 高。PR とは別の運用イベントとして人間が判断する
- main の更新は `git pull --rebase origin main`（merge commit を作る pull は hook が拒否する）

## 6. Code Review Rules

`/review` の code-reviewer と Codex が参照する。**共通の基準は [docs/pr-risk-policy.md](docs/pr-risk-policy.md)。** ここは Revia 固有。

- secrets（API キー、署名鍵、`keystore.properties` の値）がコード・設定・コミットに入っていたら **BLOCKED**。公開リポジトリなので履歴から消せない
- ユーザーの撮影画像・学習データの実物がコミットに入っていたら **BLOCKED**
- `AndroidManifest.xml` の権限が増えていて、**その権限を使うコードが同じ差分に無い**なら CHANGES_REQUIRED
- AI に渡す文章・画像の扱いを変える差分（外部の文章を指示として扱う経路が増えていないか）は 🔴 高
- SQLite / DataStore の schema・移行・復元は 🔴 高。**版を戻してから上げ直す経路**のテストが無ければ CHANGES_REQUIRED（ManageShelf で実際に起きた事故。移行の列追加は冪等に書く）
- 既存テストの削除・弱体化は、守っていた挙動の移植先が書かれていなければ CHANGES_REQUIRED
- 行数だけを理由にしたファイル・関数・PR の分割は差し戻す

## 7. リスク評価の例

[docs/pr-risk-policy.md](docs/pr-risk-policy.md) の「付録: このリポジトリの例」に、判定するたびに 1 行ずつ足す。まだ空。

設計が固まったので、見込みを [docs/design.md](docs/design.md) と [docs/decisions.md](docs/decisions.md) に合わせて具体にした（#5）。

- 🔴 高
  - **AI の API キーの扱い**（`.secrets/` `local.properties`。公開リポジトリなので履歴から消せない）
  - **`items` / `materials` の schema と移行。** 列追加は冪等に書く（§6）
  - **削除の連鎖**（`ON DELETE CASCADE`）。教材を消すと記録した付箋がまとめて消える
  - **権限の追加**（カメラなど）。使うコードが同じ差分に無いなら CHANGES_REQUIRED
  - 署名・リリース設定。Play Billing を足すとき
  - **同期を足すとき・外すとき**（認証、個人の学習データ）
- 🟡 中
  - AI 解析の呼び出し・リトライ・失敗時の見せ方
  - 状態の記録（`result` / `needs_review` / `last_done_at`）
  - ツリー編集（並べ替え・段の上げ下げ）、範囲一括生成
  - 集計の数え方、画面遷移
- 🟢 低: 文言・装飾。既存の検証を弱めないテスト追加

**「決まっていないこと」を実装する差分は、レベルに関わらず差し戻す。** [docs/decisions.md](docs/decisions.md) の「未決」に載っている領域が対象。
