package com.gtkim.pexelssearch.domain.error

sealed interface PhotoError {
    data object Network : PhotoError
    data object RateLimit : PhotoError
    data object Server : PhotoError
    /** キーを送らないリクエストが断続的に 401 を返すため、キー未設定のクローンでは到達し得る。 */
    data object Unauthorized : PhotoError

    /** 未分類の400やJSONパース失敗がここに集まるため、原因を捨てずに保持する。 */
    data class Unknown(val cause: Throwable) : PhotoError
}
