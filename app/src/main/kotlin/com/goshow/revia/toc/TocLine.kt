package com.goshow.revia.toc

/**
 * 読み取った 1 行。テキストと、画像の中での外接矩形を持つ。
 *
 * **ML Kit の型を持ち込まない。** ここを端末非依存にしておくと、階層を組み立てる処理を
 * JVM のテストで詰められる（端末も実画像も要らない）。読み取りの実装を差し替えるときも、
 * この型に詰め直すだけで済む。
 *
 * 座標は画像のピクセル。左上が原点で、右と下が正。
 */
data class TocLine(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerY: Float get() = (top + bottom) / 2f

    /** 縦にどれだけ重なっているか。0 なら重なっていない */
    fun verticalOverlap(other: TocLine): Float =
        (minOf(bottom, other.bottom) - maxOf(top, other.top)).coerceAtLeast(0f)
}

/** 読み取った 1 ページぶん。 */
data class TocPage(
    val lines: List<TocLine>,
    val imageWidth: Float,
    val imageHeight: Float,
)

/**
 * 組み立てた目次の 1 項目。
 *
 * [kind] は `items.kind`（chapter / section / question）に対応する。
 * 階層は [children] で持つので、[level] は組み立ての過程を見るための情報。
 */
data class TocNode(
    val label: String,
    val kind: TocKind,
    val level: Int,
    val pageNumber: Int? = null,
    val children: List<TocNode> = emptyList(),
) {
    /** 自分を含めた総数 */
    fun count(): Int = 1 + children.sumOf { it.count() }

    /** 子を持たない項目（集計の対象。決定 13） */
    fun leafCount(): Int = if (children.isEmpty()) 1 else children.sumOf { it.leafCount() }
}

enum class TocKind { CHAPTER, SECTION, QUESTION }
