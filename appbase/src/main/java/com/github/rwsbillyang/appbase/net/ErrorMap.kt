package com.github.rwsbillyang.appbase.net


typealias OnErrHandler = (Int,String?) -> Unit

/**
 *
 * @Deprecated
 *
 * 出现诸如404，500之内的错误的异常
 * refer to https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
 */
val ErrorMap = mapOf(
        400 to "Bad Request",
        401 to "Unauthorized",
        402 to "invalid parameter type",
        403 to "Forbidden",
        404 to "Not found",
        405 to "Method Not Allowed",
        406 to "Not Acceptable",
        407 to "Proxy Authentication Required",
        408 to "Request Timeout",
        444 to "No Response",
        494 to "Request header too large",
        495 to "SSL Certificate Error",
        496 to "SSL Certificate Required",
        497 to "HTTP Request Sent to HTTPS Port",
        499 to "Client Closed Request",
        409 to "Conflict",
        410 to "Gone",
        411 to "Length Required",
        500 to "Internal Server Error",
        501 to "Not Implemented",
        502 to "Bad Gateway",
        503 to "Service Unavailable",
        504 to "Gateway Timeout",
        505 to "HTTP Version Not Supported"
)

val ErrorCodes = ErrorMap.keys
