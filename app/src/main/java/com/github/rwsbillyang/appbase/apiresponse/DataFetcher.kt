package com.github.rwsbillyang.appbase.apiresponse

import androidx.lifecycle.*
import com.github.rwsbillyang.githubbrowser.MainApplication
import com.orhanobut.logger.Logger
import kotlinx.coroutines.*
import retrofit2.Call
import java.io.IOException

/**
 * 从远程返回的payload数据类型，从本地存储返回的数据类型，以及返回给调用者的数据类型均为T
 *
 * 这是最常用的情景
 * */
fun <T> dataFetcher(init: DataFetcher<T,T>.() -> Unit): LiveData<Resource<T>> {
    val fetcher = DataFetcher<T,T>()
    fetcher.init()
    fetcher.loadData()
    return fetcher.asLiveData()
}

/**
 * RequestType表示从远程请求的payload数据类型，然后直接送往本地存储，故传递的数据类型也是RequestType
 * ResultType表示从本地存储中所取出来的payload类型，
 *
 * 最终DataFetcher返回的给调用者的payload数据类型是ResultType
 *
 * 当从远程返回的payload数据类型，需要变换时，用此函数
 * */
fun <RequestType,ResultType> dataFetcher2(init: DataFetcher<RequestType,ResultType>.() -> Unit): LiveData<Resource<ResultType>> {
    val fetcher = DataFetcher<RequestType,ResultType>()
    fetcher.init()
    fetcher.loadData()
    return fetcher.asLiveData()
}
/**
 * RequestType表示从远程请求的payload数据类型，然后直接送往本地存储，故传递的数据类型也是RequestType
 * ResultType表示从本地存储中所取出来的payload类型，
 *
 * 最终DataFetcher返回的给调用者的payload数据类型是ResultType
 * */
class DataFetcher<RequestType,ResultType>
{
    /**
     * 请求得到的数据储存于此，被viewModel中的LiveData观察
     * */
    private val result = MediatorLiveData<Resource<ResultType>>()
    private var remoteBlock: (() -> Call<RequestType>)? = null
    private var localBlock: (() -> ResultType?)? = null
    private var saveBlock: ((RequestType) -> Unit)? = null
    private var onFetchFailBlock: ((code:Int?,msg:String?) -> Unit)? = null

    private var converter:((RequestType?) -> ResultType?)? = {if(it!=null) it as ResultType else null }
    private var processResponseBlock:((ApiSuccessResponse<RequestType>) -> RequestType) = {it.body}
    /**
     * 是否强制刷新，即强制从网络获取数据
     * */
    private var isForceRefreshBlock:  ((ResultType?) -> Boolean)? = {true}


    fun asLiveData() = result as LiveData<Resource<ResultType>>
    private fun setValue(newValue: Resource<ResultType>?) {
        if(newValue != null && result.value != newValue)
        {
            result.postValue(newValue)
        }
    }
     /**
      * 是否异步请求, true表示同步，默认为false表示异步请求
      * */
   //  var sync = false

    /**
     * 从本地加载的代码块，返回的payload数据是ResultType，即最终返回给最终调用者所需要的数据类型
     *
     * 默认为空，表示不从本地获取数据，若激活，从网络请求的数据也将更新到本地
     * */
    fun fromLocal(block: () -> ResultType?) {
        localBlock = block
    }

     /**
      * 从远程加载的代码块，返回的是一个Call调用，payload数据为RequestType
      * */
     fun fromRemote(block: () -> Call<RequestType>) {
         remoteBlock = block
     }

    /**
     * 存入本地storage的代码块，将从远程网络返回的数据（类型为RequestType，若需转换请自行转换）传递给存储代码块
     * 默认为空不执行任何操作
     * */
    fun save(block: (RequestType) -> Unit) {
        saveBlock = block
    }

    /**
     * 从网络返回数据失败时的回调，默认为空不执行任何操作
     * */
    fun onFetchFail(block:(code:Int?,msg:String?) -> Unit){
        onFetchFailBlock = block
    }

    /**
     * 是否强制刷新，即是否强制从远程获取最新数据
     *
     * 传递的数据通常是从本地存储中获取的数据，然后对其进行判断是否该获取最新的数据
     * */
    fun isForceRefresh(block: (ResultType?) -> Boolean) {
        isForceRefreshBlock = block
    }


     /**
      * 远程加载的代码块到实际所使用数据的转换器
      * 将远程加载获取的payload数据（RequestType类型）转换成ResultType，目的最终的调用者需求的是ResultType
      *
      * 默认的转换器是类型转换返回自身
      * */
     fun converter(block: (RequestType?) -> ResultType?) {
         converter = block
     }

    /**
     * 对远程返回的响应结果，进行额外处理，通常是response.body就是所需的payload数据，
     * 但如果响应头中还有额外的payload数据，可以在此做额外的操作
     * */
    fun processResponse(block: (ApiSuccessResponse<RequestType>) -> RequestType) {
        processResponseBlock = block
    }


    fun loadData(){
        CoroutineScope(Dispatchers.IO).launch(newSingleThreadContext("loadData")){
            localBlock?.let {
                //setValue(Resource.loading(null))
                val localResult = CoroutineScope(Dispatchers.IO).async {
                    return@async it()
                }.await()
                setValue(Resource.success(localResult))
            }

            if(MainApplication.Instance?.isNetworkAvailable() ?: false)
            {
                remoteBlock?.let {
                    result.postValue(Resource.loading(null))
                    Coroutines.ioThenMain({call2ApiResponse(it())}){
                        notify(it)
                        this@DataFetcher.setValue(it.toResource())
                    }
                }
            }else{
                setValue(Resource.err("No Network,please enable Wifi or Mobile data"))
            }
        }
    }


     private fun  call2ApiResponse(call: Call<RequestType>): ApiResponse<RequestType> {
         return try {
             call.execute().let {
                 if(it.isSuccessful)
                     ApiResponse.create(it)
                 else
                     ApiErrorResponse(it.message(),it.code())
             }
             }catch (ioe: IOException){
                  ApiErrorResponse<RequestType>("IOException: " + ioe.message)
             }catch (e: RuntimeException)
             {
                  ApiErrorResponse<RequestType>("RuntimeException: " + e.message)
             }catch(e:Exception){
                  ApiErrorResponse<RequestType>("unknown exeption: " + e.message)
             }
     }

    /**
     * 返回null则不更新liveData中的值，支持ResponseBox封装的值的提取处理
     * */
     private fun ApiResponse<RequestType>.toResource(): Resource<ResultType>?{
        if(consumed) return null
        when(this){
            is ApiEmptyResponse -> {
                return Resource.err("return nothing", null)
            }
            is ApiErrorResponse -> {
                onFetchFailBlock?.let { it(this.code,this.errorMessage) }
                return Resource.err(this.errorMessage, null, this.code)
            }
            is ApiSuccessResponse -> {
                val apiResponse = this.body

                if(apiResponse is ResponseBox<*>)
                {
                    val resultData: ResultType? = if(converter != null)
                        converter!!(apiResponse.data as RequestType)
                    else
                        apiResponse.data as ResultType

                    if(apiResponse.isOK()){
                        Coroutines.io(true){
                            saveBlock?.let { it(processResponseBlock(this)) }
                        }

                        return Resource.success(resultData)
                    }else
                    {
                        return Resource.bizErr(apiResponse.ret, apiResponse.msg, resultData)
                    }
                }else{
                    Coroutines.io(true){ saveBlock?.let { it(processResponseBlock(this)) } }

                    return Resource.success( if(converter != null) converter!!(apiResponse)
                                            else apiResponse as ResultType)
                }
            }
        }
    }


    companion object {
        /**
         * 当有ApiResponse到来时，更新此liveData
         * 当为其添加observer时，可实现对ApiResponse的直接处理
         * */
        private val responseLiveData = MutableLiveData<ApiResponse<*>>()
        private val map: MutableMap<ApiResponseInterceptor,Observer<ApiResponse<*>>> = HashMap()
        /**
         * 为某种错误状态添加一个interceptor，用于直接对apiResponse的处理
         *
         * 若intercepter处理完返回true，表示已消耗掉，viewModel中的liveData将接受不到数据更新
         * */
        fun addInterceptor(owner: LifecycleOwner, status: ApiResponseStatus, interceptor: ApiResponseInterceptor)
        {
            map[interceptor]?.let {
                Logger.w("interceptor exsit")
                return
            }

            val observer = Observer<ApiResponse<*>> {
                if(it.status == status ){
                    it.consumed = interceptor.intercept(it)
                }
            }
            map[interceptor] = observer
            responseLiveData.observe(owner,  observer)
        }

        /**
         * 取消一个interceptor
         * */
        fun removeInterceptor(interceptor: ApiResponseInterceptor)
        {
            map[interceptor]?.let {
                responseLiveData.removeObserver(it)
                map.remove(interceptor)
            }
        }

        fun notify(r: ApiResponse<*>){
            responseLiveData.value = r
        }
    }

}