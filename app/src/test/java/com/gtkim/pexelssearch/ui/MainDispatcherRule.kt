package com.gtkim.pexelssearch.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * 各 ViewModel が init で viewModelScope の処理を始めるため、Main を [TestDispatcher] に差し替えて
 * runTest の仮想時間で debounce やロードを進められるようにする。
 *
 * 既定を [UnconfinedTestDispatcher] にしているのは、購読が生成と同時に始まらないと Retry の emit が
 * 購読者なしで捨てられるため。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
