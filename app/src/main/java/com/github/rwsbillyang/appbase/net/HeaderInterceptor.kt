package com.github.rwsbillyang.appbase.net

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class HeaderInterceptor(private val headers: Map<String, String>): Interceptor {


    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {

        val builder = chain.request()
                .newBuilder()
        if (headers.isNotEmpty()) {
            val keys = headers.keys
            for (headerKey in keys) {
                headers[headerKey]?.let { builder.addHeader(headerKey, it).build() }
            }
        }
        return chain.proceed(builder.build())
    }
}