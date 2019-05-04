package com.github.rwsbillyang.appbase

import android.app.Application
import android.net.ConnectivityManager

open class NetAwareApplication: Application() {

    companion object {
        var Instance: NetAwareApplication? = null

        /**
         * 如果没有网络，发送请求时的错误提示，可随时更改，影响整个App全局
         * */
        var ifNetWorkUnavailableString = "No Network,please enable Wifi or Mobile data"
    }

    var CONNECTIVITY_MANAGER: ConnectivityManager? = null
    override fun onCreate() {
        super.onCreate()

        CONNECTIVITY_MANAGER = this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        Instance = this
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