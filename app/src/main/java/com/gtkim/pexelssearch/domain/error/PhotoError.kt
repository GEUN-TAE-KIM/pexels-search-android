package com.gtkim.pexelssearch.domain.error

sealed interface PhotoError {
    data object Network : PhotoError
    data object RateLimit : PhotoError
    data object Server : PhotoError
    /** Pexelsは両エンドポイントでキーを強制しないため実際には到達しない。防御的に残す。 */
    data object Unauthorized : PhotoError

    /** 未分類の400やJSONパース失敗がここに集まるため、原因を捨てずに保持する。 */
    data class Unknown(val cause: Throwable) : PhotoError
}
