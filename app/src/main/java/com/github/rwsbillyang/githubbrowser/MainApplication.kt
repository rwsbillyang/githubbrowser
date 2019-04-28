/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.githubbrowser

import android.app.Application
import android.net.ConnectivityManager
import com.github.rwsbillyang.appbase.net.NetManager
import com.orhanobut.logger.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin


class MainApplication : Application() {

    var CONNECTIVITY_MANAGER: ConnectivityManager? = null

    override fun onCreate() {
        super.onCreate()

        setupLog()

        startKoin {
            // Android context
            androidContext(this@MainApplication)
            androidLogger()
            // modules
            modules(appModule)
        }

        val config = GithubConfiguration()
        //config.configureCetrificatesResources(this, certificatesResourcesArray)
        NetManager.regiseterConfiguration(ConstantsConfig.GITHUB_HOST_API, config)

        Instance = this

        CONNECTIVITY_MANAGER = this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private fun setupLog() {

        val formatStrategy = PrettyFormatStrategy.newBuilder()
            .showThreadInfo(false)  // (Optional) Whether to show thread info or not. Default true
            .methodCount(1)         // (Optional) How many method line to show. Default 2
            //.methodOffset(7)        // (Optional) Hides internal method calls up to offset. Default 5
            //.logStrategy(customLog) // (Optional) Changes the log strategy to print out. Default LogCat
            .tag(ConstantsConfig.AppName)   // (Optional) Global tag for every log. Default PRETTY_LOGGER
            .build()

        Logger.addLogAdapter(object : AndroidLogAdapter(formatStrategy) {
            fun isLoggable(tag: String) = BuildConfig.DEBUG
        })

        val csvFormatStrategy = CsvFormatStrategy.newBuilder()
            .tag(ConstantsConfig.AppName)
            .build()

        Logger.addLogAdapter(object : DiskLogAdapter(csvFormatStrategy) {
            fun isLoggable(priority: Int) = priority >= Logger.WARN
        })
    }

    companion object {
        var Instance: MainApplication? = null
    }


    fun isNetworkAvailable(): Boolean = CONNECTIVITY_MANAGER?.activeNetworkInfo?.isConnected ?: false
//    {
//        if(CONNECTIVITY_MANAGER == null) {
//            Logger.w("CONNECTIVITY_MANAGER is null")
//            return false
//        }
//        if(CONNECTIVITY_MANAGER!!.activeNetworkInfo == null)
//        {
//            Logger.w("CONNECTIVITY_MANAGER.activeNetworkInfo is null")
//            return false
//        }
//        Logger.i("state= $(CONNECTIVITY_MANAGER!!.activeNetworkInfo!!.detailedState)")
//       return CONNECTIVITY_MANAGER!!.activeNetworkInfo!!.isConnected
//    }




/*


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    var callback: ConnectivityManager.NetworkCallback? = null

    private fun listenNetwork()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            callback = object: ConnectivityManager.NetworkCallback(){
                override fun onAvailable(network: Network){
                    super.onAvailable(network!!)
                    Logger.d("network available")
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Logger.w("network unavailable")
                }

                override fun onLosing(network: Network?, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    Logger.w("network is losing $maxMsToLive")
                }

                override fun onLost(network: Network?) {
                    super.onLost(network)
                    Logger.w("network lost")
                }
            }
            CONNECTIVITY_MANAGER.requestNetwork(NetworkRequest.Builder().build(),callback )
        }else{

        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            callback?.let {  CONNECTIVITY_MANAGER.unregisterNetworkCallback(it) }

        }
    }
    */


}
