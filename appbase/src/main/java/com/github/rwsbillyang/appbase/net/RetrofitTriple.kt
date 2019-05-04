package com.github.rwsbillyang.appbase.net

import okhttp3.OkHttpClient
import retrofit2.Retrofit

data class RetrofitTriple(var config: NetConfiguration,
                          var retrofit: Retrofit?,
                          var client: OkHttpClient?)