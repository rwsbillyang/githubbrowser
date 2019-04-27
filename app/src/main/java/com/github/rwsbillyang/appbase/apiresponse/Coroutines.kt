package com.github.rwsbillyang.appbase.apiresponse

import kotlinx.coroutines.*

//https://github.com/shkschneider/android_viewmodel_livedata_coroutines/blob/master/app/src/main/kotlin/me/shkschneider/viewmodellivedatacoroutines/Coroutines.kt
object Coroutines {

    /**
     * Room会要求在新线程中进行存储操作，createNewThread需设置为true
     * */
    fun <T : Any> io(createNewThread: Boolean = false,work: suspend (() -> T?)): Job =
        if (createNewThread) {
            CoroutineScope(Dispatchers.IO).launch(newSingleThreadContext("io")) { work()}
        } else {
            CoroutineScope(Dispatchers.IO).launch { work()}
        }




    fun <T: Any> ioThenMain(work: suspend (() -> T), callback: ((T) -> Unit)? = null): Job =
            CoroutineScope(Dispatchers.Main).launch {
                val data = CoroutineScope(Dispatchers.IO).async {
                    return@async work()
                }.await()
                callback?.let {
                    it(data)
                }
            }
    fun <T: Any> ioThenMain2(work: suspend (() -> T?), callback: ((T?) -> Unit)? = null): Job =
            CoroutineScope(Dispatchers.Main).launch {
                val data = CoroutineScope(Dispatchers.IO).async {
                    return@async work()
                }.await()
                callback?.let {
                    it(data)
                }
            }

}