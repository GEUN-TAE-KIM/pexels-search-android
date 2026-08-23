package com.gtkim.pexelssearch.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 全画面ローディングとページング中のインライン表示を兼ねる。どちらになるかは呼び出し側の [modifier] が決める。
 */
@Composable
fun AppLoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
