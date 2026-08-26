package com.gtkim.pexelssearch.domain.model

import com.gtkim.pexelssearch.domain.error.PhotoError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private const val VALUE = "猫の写真"

class OutcomeTest {

    @Test
    fun `成功した処理は Success に包まれる`() {
        val outcome = safeCall({ PhotoError.Network }) { VALUE }

        assertEquals(Outcome.Success(VALUE), outcome)
    }

    @Test
    fun `例外は onError が返したエラーを持つ Failure になる`() {
        val outcome = safeCall({ PhotoError.Network }) { throw IOException("通信失敗") }

        assertEquals(Outcome.Failure(PhotoError.Network), outcome)
    }

    @Test
    fun `CancellationException は握り潰さず再送出される`() {
        // ここで握り潰すと flatMapLatest による検索の打ち切りが機能しなくなる
        assertThrows(CancellationException::class.java) {
            safeCall({ PhotoError.Network }) { throw CancellationException("打ち切り") }
        }
    }
}
