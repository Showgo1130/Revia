# Revia Wireframes — Figma プラグイン

[docs/design.md](../../docs/design.md) のワイヤーフレームを Figma に生成する（[Issue #9](https://github.com/Showgo1130/Revia/issues/9)）。

実行すると**新しいページ**を作り、そこに 360 × 800 の 4 画面を横並びで置く。**既存のページは触らない。**

1. 01 教材一覧
2. 02 教材追加 — 確認・修正
3. 03 教材詳細
4. 04 復習一覧

## 使い方

```bash
cd tools/figma
npm install
npm run build      # code.ts → code.js
```

Figma デスクトップアプリで:

1. 任意のデザインファイルを開く
2. **Plugins → Development → Import plugin from manifest...**
3. このフォルダの `manifest.json` を選ぶ
4. **Plugins → Development → Revia Wireframes** で実行

`code.js` は `.gitignore` 済み。**clone したら `npm run build` が要る。**

## 直すとき

**見た目の値（色・文字サイズ・余白）は `code.ts` の先頭の `M3` と `TYPE`** にまとまっている。

**それ以外は勝手に変えない。** 状態の印・ナビの構成・字下げの上限などは [docs/decisions.md](../../docs/decisions.md) で決まっていて、ここはその実装。変えたいときは先に decisions を直す。

| 変えてよい | `code.ts` の `M3` / `TYPE` / `SCREEN` / `GAP` / `PAD` |
|---|---|
| **変えてはいけない** | 状態の印（空白の破線の円 / `✓` / `🔖`）、ナビが `教材` `復習` の 2 つ、4 段目以降の字下げ、復習一覧にだけ日付を出すこと |

直したら必ず:

```bash
npm run typecheck   # tsc --noEmit
npm run build
```

**型チェックは実行時の落ちを全部は防がない。** バリアント名の取り違えのような間違いは `instanceOf()` が例外にするが、それ以外は Figma で実行して見るしかない。

## 制約

- **Figma の中でしか動かない。** CI では回せない（公式にバックグラウンド実行は無い）
- **フォントは環境依存。** 日本語が出る候補を 5 つ試して、無ければ止まる
- 実在の教材の目次を使っていない。データは `code.ts` 内の架空のもの
