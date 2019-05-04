package com.github.rwsbillyang.appbase.apiresponse

interface ApiResponseInterceptor{
    /**
     * 对apiResponse进行处理,返回true则无需再处理，返回false表示继续
     * 若返回true，则表示已消费掉，不再处理，包括不更新LiveData<Resouce<T>>
     * */
    fun intercept(apiResponse: ApiResponse<*>):Boolean  = false
}