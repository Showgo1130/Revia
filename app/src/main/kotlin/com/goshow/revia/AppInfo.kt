package com.goshow.revia

/**
 * 画面とテストの両方から使う、アプリの名乗り。
 *
 * 文字列リソース（`strings.xml`）ではなくここに置いてあるのは、**JVM 単体テストから
 * Android のリソースを読まずに確かめられるようにする**ため。表示の言語を分けるときは
 * `strings.xml` へ移し、テストもそれに合わせて書き直す。
 */
const val APP_NAME: String = "Revia"

/** 何をするアプリかの 1 行。ストアの短い説明と揃える。 */
const val TAGLINE: String = "紙の参考書・問題集の復習を管理する"
