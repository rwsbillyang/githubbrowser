package com.github.rwsbillyang.githubbrowser

import com.github.rwsbillyang.appbase.net.DefaultConfiguration

class GithubConfiguration: DefaultConfiguration() {
    //override fun cetrificatesInputStreamList(): List<InputStream>? = null

    companion object {
        const val CONNECT_TIME_OUT: Long = 20 * 1000L
        const val READ_TIME_OUT: Long = 180 * 1000L
        const val WRITE_TIME_OUT: Long = 30 * 1000L
    }
    override fun connectTimeoutMs(): Long = CONNECT_TIME_OUT

    override fun readTimeoutMs(): Long = READ_TIME_OUT

    override fun writeTimeoutMs(): Long = WRITE_TIME_OUT

    override fun logEnable(): Boolean = BuildConfig.DEBUG
}