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

package com.github.rwsbillyang.githubbrowser.ui.repo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.github.rwsbillyang.appbase.apiresponse.Resource
import com.github.rwsbillyang.appbase.util.autoCleared
import com.github.rwsbillyang.appbase.util.setVisible
import com.github.rwsbillyang.appbase.view.LoadingFragment
import com.github.rwsbillyang.githubbrowser.R
import com.github.rwsbillyang.githubbrowser.model.vo.Contributor
import com.github.rwsbillyang.githubbrowser.model.vo.Repo
import kotlinx.android.synthetic.main.github_repo_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * The UI Controller for displaying a Github Repo's information with its contributors.
 */

class RepoFragment : LoadingFragment() {
    val repoViewModel: RepoViewModel by viewModel()

    //private val params by navArgs<RepoFragmentArgs>()
    private var adapter by autoCleared<ContributorAdapter>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.move)
        }
        return inflater.inflate(R.layout.github_repo_fragment, container, false)
    }
    override fun retry(){
        repoViewModel.retry()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            this.adapter = ContributorAdapter{
                    contributor, imageView ->
                val extras = FragmentNavigatorExtras(
                    imageView to contributor.login
                )
                findNavController().navigate(
                    RepoFragmentDirections.showUser(contributor.login, contributor.avatarUrl),
                    extras
                )
            }
        }else
        {
            this.adapter = ContributorAdapter{
                contributor, imageView ->
            findNavController().navigate(RepoFragmentDirections.showUser(contributor.login, contributor.avatarUrl))
            }
        }

        contributor_list.adapter = adapter

        postponeEnterTransition()
        contributor_list.viewTreeObserver
            .addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }


        val params = RepoFragmentArgs.fromBundle(arguments!!)


        repoViewModel.apply {
            //如果RepoId变化，将引起Repo的装载，从而contributors中的值会变化，此处代码得到执行
            repo.observe(viewLifecycleOwner,Observer<Resource<Repo>>{
                renderLoading(it)
                renderRepo(it)
            })

            //如果RepoId变化，将引起List<Contributor>的装载，从而contributors中的值会变化，此处代码得到执行
            contributors.observe(viewLifecycleOwner, Observer<Resource<List<Contributor>>> {
                renderLoading(it)
                it?.data?.run { adapter.submitList(this) }
            })

            setId(params.owner, params.name)
        }
    }

    private fun renderRepo(resource:Resource<Repo>?)
    {
        val visible = resource?.data == null

        name.setVisible(!visible)
        name.text = resources.getString(R.string.repo_full_name,
            resource?.data?.owner?.login,resource?.data?.name)

        description.setVisible(!visible)
        description.text = resource?.data?.description
    }


}
