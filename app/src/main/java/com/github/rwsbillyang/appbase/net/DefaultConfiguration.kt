package com.github.rwsbillyang.appbase.net

import android.app.Application
import java.io.InputStream

open class DefaultConfiguration(private var inputStreamList: List<InputStream>? = null): NetConfiguration {

    override fun cetrificatesInputStreamList(): List<InputStream>? {
       return inputStreamList
    }
    fun configureCetrificatesResources(application: Application, array: IntArray?){
        inputStreamList = convertCertificatesReources(application,array)
    }
}
