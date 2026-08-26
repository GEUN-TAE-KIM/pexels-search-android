package com.gtkim.pexelssearch.ui

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val MIN_LOADING_MILLIS = 400L

/**
 * ユーザーが押した再試行では、失敗が数msで返るとローディングが1フレームも描かれず、
 * 押したこと自体が伝わらない。[block] が早く終わってもこの時間までは表示を残す。
 *
 * 自動の読み込みには使わない — そちらは「一定時間を超えたときだけ出す」が定石で、向きが逆になる。
 */
suspend fun <T> withMinLoadingDuration(block: suspend () -> T): T = coroutineScope {
    val minDuration = launch { delay(MIN_LOADING_MILLIS.milliseconds) }
    val result = block()
    minDuration.join()
    result
}
