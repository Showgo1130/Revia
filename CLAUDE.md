@AGENTS.md

# Claude Code 向けの補足

- 上の AGENTS.md がこのリポジトリの正本。Codex も同じファイルを読む
- 横断の作法は `~/.claude/CLAUDE.md`。共有の Skill（`/create-issue` `/update-issue` `/create-pr` `/review` `/address-review`）と Subagent（`code-reviewer` `security-reviewer`）は `~/.claude/` に配布済み
- **このリポジトリは公開。** 開発機のディレクトリ構成が外から見えるので、**手元の絶対パスをコミットするファイルに書かない**（他のリポジトリの規約を参照するときも、リポジトリ内の写しを指す）。リスク判定の基準は `docs/pr-risk-policy.md`
- 自動モードで拒否される操作（履歴書き換え・remote 変更・権限設定の編集・削除を伴う git rm）は回避せず人間に依頼する
