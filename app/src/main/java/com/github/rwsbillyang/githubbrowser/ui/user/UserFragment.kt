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

package com.github.rwsbillyang.githubbrowser.ui.user

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.postDelayed
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.rwsbillyang.appbase.apiresponse.Resource
import com.github.rwsbillyang.appbase.util.autoCleared
import com.github.rwsbillyang.appbase.util.loadImg
import com.github.rwsbillyang.appbase.util.setVisible
import com.github.rwsbillyang.githubbrowser.R
import com.github.rwsbillyang.githubbrowser.model.vo.Repo
import com.github.rwsbillyang.githubbrowser.model.vo.User
import com.github.rwsbillyang.githubbrowser.ui.base.LoadingFragment
import kotlinx.android.synthetic.main.github_user_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class UserFragment : LoadingFragment() {
    private val userViewModel: UserViewModel by viewModel()

    private val params by navArgs<UserFragmentArgs>()
    private var adapter by autoCleared<RepoListAdapter>()
    private var handler = Handler(Looper.getMainLooper())

    val imageRequestListener = object: RequestListener<Drawable> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
            startPostponedEnterTransition()
            return false
        }

        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            startPostponedEnterTransition()
            return false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.move)

        // Animation Watchdog - Make sure we don't wait longer than a second for the Glide image
        handler.postDelayed(1000) {
            startPostponedEnterTransition()
        }
        postponeEnterTransition()

        return inflater.inflate(R.layout.github_user_fragment, container, false)
    }

    override fun retry(){
        userViewModel.retry()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = RepoListAdapter(showFullName = false) { repo ->
            findNavController().navigate(UserFragmentDirections.showRepo(repo.owner.login, repo.name))
        }
        repo_list.adapter = adapter

        userViewModel.apply {
            user.observe(viewLifecycleOwner,Observer<Resource<User>>{
                renderLoading(it)
                renderUser(it)
            })

            repositories.observe(viewLifecycleOwner, Observer<Resource<List<Repo>>> {
                renderLoading(it)
                adapter.submitList(it?.data)
            })

            setLogin(params.login)
        }

    }

    private fun renderUser(resource: Resource<User>)
    {
        avatar.loadImg(resource.data?.avatarUrl,null,imageRequestListener)

        name.setVisible(resource.data != null)
        name.text = resource.data?.name ?: resource.data?.login
    }

}
