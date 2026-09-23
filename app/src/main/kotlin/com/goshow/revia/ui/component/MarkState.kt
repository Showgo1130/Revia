package com.goshow.revia.ui.component

/**
 * 項目に付ける印。**このアプリの語彙そのもの**（決定 35）。
 *
 * 画面によって形や色を変えない。○・✓・● の 3 つだけ。
 *
 * ## データとの対応
 *
 * データは `result` と `needs_review` の 2 列に分けて持つ（決定 11）。**本来は別の概念**で、
 * 「間違えたが完全に理解した」（`wrong` だが `needs_review` は false）のような組み合わせがある。
 *
 * ただし**画面では 3 つを循環させるだけ**にする。紙は印を付けるのが 1 秒なので、
 * 操作量を増やすと紙に負ける。
 *
 * | 印 | `result` | `needs_review` |
 * |---|---|---|
 * | [Unstarted] | `unstarted` | false |
 * | [Done] | `correct` | false |
 * | [Review] | `wrong` | true |
 */
enum class MarkState {
    /** まだやっていない。薄い破線の円（決定 21） */
    Unstarted,

    /** やった。折れ線のチェック */
    Done,

    /** 見直したい。朱の塗りつぶした丸。**画面で唯一の彩度**（決定 24） */
    Review,
    ;

    /**
     * 1 タップで次に進む。`Unstarted → Done → Review → Unstarted` と回る。
     *
     * **`values()` の順に依存させていない。** 並び順を変えても循環が壊れないよう、
     * ここに明示してある。
     */
    fun next(): MarkState = when (this) {
        Unstarted -> Done
        Done -> Review
        Review -> Unstarted
    }
}
