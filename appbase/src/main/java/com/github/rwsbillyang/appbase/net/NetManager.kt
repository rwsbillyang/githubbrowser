package com.github.rwsbillyang.appbase.net


import android.os.Build
import com.google.gson.GsonBuilder
import com.orhanobut.logger.Logger
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.io.InputStream
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


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

        val netWorkInterceptors = provider.networkInterceptors()
        if (!empty(netWorkInterceptors)) {
            for (interceptor in netWorkInterceptors!!) {
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


        //Android5.0版本以上以及一个类似先进网路服务器
        if (provider.enableMordenTLS()) {
            if (Build.VERSION.SDK_INT >= 22) {
                val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .cipherSuites(
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
                    ).build()
                builder.connectionSpecs(Collections.singletonList(spec))

            } else if (Build.VERSION.SDK_INT >= 16) {
                try {
                    val sc = SSLContext.getInstance("TLSv1.2")
                    sc.init(null, null, null)

                    builder.sslSocketFactory(Tls12SocketFactory(sc.socketFactory),getDefaultTrustManager())//socketFactory


                    val cs = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build()

                    val specs = ArrayList<ConnectionSpec>()
                    specs.add(cs)
                    specs.add(ConnectionSpec.COMPATIBLE_TLS)
                    specs.add(ConnectionSpec.CLEARTEXT)

                    builder.connectionSpecs(specs)
                } catch (exc: Exception) {
                    Logger.e("Error while setting TLS 1.2, $exc.message")
                }

            } else {
                Logger.e("Build.VERSION.SDK_INT=$(Build.VERSION.SDK_INT) , Not Support TLS")
            }

        }

        if (provider.enableCustomTrust()) {
            if(provider.cetrificatesInputStreamList().isNullOrEmpty())
            {
               Logger.w("forget to configure your certificates resources?")
            }else
            {
                builder.socketFactory(getSSLSocketFactory( provider.cetrificatesInputStreamList()!!))
            }
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


    /**
     * 比如现在我们有个证书media.bks，首先需要将其放在res/raw目录下，当然你可以可以放在assets目录下。
     * Java本身支持的证书格式jks，但是遗憾的是在android当中并不支持jks格式正式，而是需要bks格式的证书。
     * 因此我们需要将jks证书转换成bks格式证书
     *
     * http://blog.csdn.net/dd864140130/article/details/52625666
     * https://blog.csdn.net/lmj623565791/article/details/48129405
     * */
    private fun getSSLSocketFactory(inputStreamArray: List<InputStream>): SSLSocketFactory? {
        //CertificateFactory用来证书生成
        val certificateFactory: CertificateFactory
        try {
            certificateFactory = CertificateFactory.getInstance("X.509")
            //Create a KeyStore containing our trusted CAs
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)

            var `is`: InputStream?
            for (i in 0 until inputStreamArray.size) {
                `is` = inputStreamArray.get(i)
                keyStore.setCertificateEntry(i.toString(), certificateFactory.generateCertificate(`is`))
                if (`is` != null) {
                    `is`!!.close()
                }
            }
            //Create a TrustManager that trusts the CAs in our keyStore
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)

            //Create an SSLContext that uses our TrustManager
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagerFactory.trustManagers, SecureRandom())
            return sslContext.socketFactory

        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }

        return null
    }

    private fun getDefaultTrustManager(): X509TrustManager {
        try {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers
            if (trustManagers.size != 1 || trustManagers[0] !is X509TrustManager) {
                throw IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers))
            }
            return trustManagers[0] as X509TrustManager
        } catch (e: GeneralSecurityException) {
            throw AssertionError() // The system has no TLS. Just give up.
        }

    }
}
