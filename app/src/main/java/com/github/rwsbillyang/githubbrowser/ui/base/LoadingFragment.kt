package com.github.rwsbillyang.githubbrowser.ui.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.rwsbillyang.appbase.apiresponse.Resource
import com.github.rwsbillyang.appbase.apiresponse.Status
import com.github.rwsbillyang.appbase.util.setVisible
import com.github.rwsbillyang.githubbrowser.R
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.loading_state.*

/**
 * 绑定了loading_state.xml的Fragment
 * */
open class LoadingFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retry.setOnClickListener { retry() }
    }
    protected open fun retry(){}

    protected open fun updateLoading(resource: Resource<*>?)
    {
        Logger.i("updateLoading")

        loading.setVisible(resource?.status == Status.LOADING || resource?.status == Status.ERR)
        progress_bar.setVisible(resource?.status == Status.LOADING)

        val errVisible = resource?.status == Status.ERR
        retry.setVisible(errVisible)

        error_msg.setVisible(errVisible)
        error_msg.text = resource?.message ?:  resources.getString(R.string.unknown_error)
    }
}