package com.gtkim.pexelssearch.ui.search

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gtkim.pexelssearch.R
import com.gtkim.pexelssearch.domain.error.PhotoError
import com.gtkim.pexelssearch.ui.theme.PexelsSearchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScaffoldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `読み込み中はローディングインジケーターが表示される`() {
        setScaffold(SearchUiState(query = "猫", isLoading = true))

        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            )
            .assertIsDisplayed()
    }

    @Test
    fun `検索結果が空のとき空状態メッセージが表示される`() {
        setScaffold(SearchUiState(query = "猫"))

        composeRule
            .onNodeWithText(getString(R.string.search_empty_message))
            .assertIsDisplayed()
    }

    @Test
    fun `エラー発生時はエラーメッセージと再試行ボタンが表示される`() {
        setScaffold(SearchUiState(query = "猫", error = PhotoError.Network))

        composeRule.onNodeWithText(getString(R.string.error_network)).assertIsDisplayed()
        composeRule.onNodeWithText(getString(R.string.retry)).assertIsDisplayed()
    }

    private fun setScaffold(state: SearchUiState) {
        composeRule.setContent {
            PexelsSearchTheme {
                SearchScaffold(state = state, onIntent = {})
            }
        }
    }

    private fun getString(@StringRes resId: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(resId)
}
