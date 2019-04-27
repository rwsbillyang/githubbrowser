package com.github.rwsbillyang.appbase.net


import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

object NetManager {
    private val map: MutableMap<String, RetrofitTriple> = HashMap<String, RetrofitTriple>() //site baseUrl -> RetrofitTriple
    /**
     * 若提供全局的Http错误状态码处理器，则注册全局处理器
     * */
    var errHandler: OnErrHandler? = null

    var defaultConfig: NetConfiguration = DefaultConfiguration()

    @JvmOverloads
    fun getRetrofit(baseUrl: String, netConfig: NetConfiguration? = null): Retrofit {

        if (NetManager.empty(baseUrl)) {
            throw IllegalStateException("baseUrl can not be null")
        }

        var triple = map[baseUrl]
        if (triple?.retrofit != null) {
            return triple.retrofit!!
        }

        var config = netConfig
        if (config == null) {
            config = triple?.config
            if (config == null)
                config = defaultConfig
        }
        checkProvider(config)

        val gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create()

        val client = getClient(baseUrl, config)

        val builder = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                //.addCallAdapterFactory(CoroutineCallAdapterFactory())
                //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))

        var retrofit = builder.build()
        if (triple == null)
            map[baseUrl] = RetrofitTriple(config, retrofit, client)
        else {
            triple.retrofit = retrofit
            triple.client = client
        }


        return retrofit
    }

    private fun getClient(baseUrl: String, provider: NetConfiguration): OkHttpClient {
        if (empty(baseUrl)) {
            throw IllegalStateException("baseUrl can not be null")
        }
        var client = map[baseUrl]?.client
        if (client != null) {
            return client
        }
        checkProvider(provider)

        val builder = OkHttpClient.Builder()

        builder.connectTimeout(if (provider.connectTimeoutMs() != 0L)
            provider.connectTimeoutMs()
        else
            defaultConfig.connectTimeoutMs(), TimeUnit.MILLISECONDS)
        builder.readTimeout(if (provider.readTimeoutMs() != 0L)
            provider.readTimeoutMs()
        else
            defaultConfig.readTimeoutMs(), TimeUnit.MILLISECONDS)

        builder.writeTimeout(if (provider.writeTimeoutMs() != 0L)
            provider.writeTimeoutMs()
        else
            defaultConfig.writeTimeoutMs(), TimeUnit.MILLISECONDS)

        val cookieJar = provider.cookie()
        if (cookieJar != null) {
            builder.cookieJar(cookieJar)
        }

        provider.configHttps(builder)

        val interceptors = provider.interceptors()

        if (!empty(interceptors)) {
            for (interceptor in interceptors!!) {
                builder.addInterceptor(interceptor)
            }
        }

        val netWorkinterceptors = provider.networkInterceptors()
        if (!empty(netWorkinterceptors)) {
            for (interceptor in netWorkinterceptors!!) {
                builder.addNetworkInterceptor(interceptor)
            }
        }

        if (provider.logEnable()) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(loggingInterceptor)
        }

        if (provider.gzipRequestEnable()) {
            builder.addNetworkInterceptor(GzipRequestInterceptor())
        }

        val headerMap = provider.requestHeaders()
        if (headerMap != null && headerMap.isNotEmpty()) {
            builder.addNetworkInterceptor(HeaderInterceptor(headerMap))
        }

        if(errHandler != null)
        {
            builder.addNetworkInterceptor(ErrorResponseInterceptor(errHandler!!))
        }

        return builder.build()
    }

    fun regiseterDefaultConfiguration(config: NetConfiguration) {
        defaultConfig = config
    }

    fun regiseterConfiguration(baseUrl: String, config: NetConfiguration) {
        var triple = map[baseUrl]
        if (triple != null) {
            triple.config = config
        } else {
            triple = RetrofitTriple(config, null, null)
            map[baseUrl] = triple
        }
    }
    /**
     * 若提供全局的Http错误状态码处理器，则注册全局处理器
     * */
    fun registerGlobalErrHandler(errHandler: OnErrHandler)
    {
        this.errHandler = errHandler
    }

    fun clearCache() {
        map.clear()
    }

    private fun empty(baseUrl: String?): Boolean {
        return baseUrl?.isEmpty()?:true
        //return baseUrl == null || baseUrl.isEmpty()
    }

    private fun empty(interceptors: Array<Interceptor>?): Boolean {
        return interceptors?.isEmpty()?:true
    }

    private fun checkProvider(provider: NetConfiguration?) {
        if (provider == null) {
            throw IllegalStateException("must register provider first")
        }
    }
}
