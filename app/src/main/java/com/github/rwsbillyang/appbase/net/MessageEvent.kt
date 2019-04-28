package com.github.rwsbillyang.appbase.net

object EventChannel {
    const val CHANNEL = "EVENT"
}


enum class EventType{
    TYPE_LOADING_START,
    TYPE_LOADING_FINISH,
    TYPE_HTTP_STATUS_CODE_ERR,
    TYPE_RESULT_ERR

}
/**
 * @Deprecated
 * */
data class MessageEvent(var type: EventType, var code: Int? = null, var ret: String? = null, var msg: String? = null)