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

package com.github.rwsbillyang.githubbrowser.ui.search

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.rwsbillyang.appbase.apiresponse.Resource
import com.github.rwsbillyang.appbase.apiresponse.Status
import com.github.rwsbillyang.appbase.util.ToastType
import com.github.rwsbillyang.appbase.util.autoCleared
import com.github.rwsbillyang.appbase.util.setVisible
import com.github.rwsbillyang.appbase.util.toast
import com.github.rwsbillyang.githubbrowser.R
import com.github.rwsbillyang.githubbrowser.model.vo.Repo
import com.github.rwsbillyang.githubbrowser.ui.base.LoadingFragment
import com.github.rwsbillyang.githubbrowser.ui.user.RepoListAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.github_search_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class SearchFragment : LoadingFragment() {

    val searchViewModel: SearchViewModel  by viewModel()


    var adapter by autoCleared<RepoListAdapter>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.github_search_fragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchInputListener()
    }

    override fun retry() {
        searchViewModel.refresh()
    }

    private fun initSearchInputListener() {
        input.setOnEditorActionListener { view: View, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(view)
                true
            } else {
                false
            }
        }
        input.setOnKeyListener { view: View, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                doSearch(view)
                true
            } else {
                false
            }
        }
    }

    private fun doSearch(v: View) {
        val query = input.text.toString()
        // Dismiss keyboard
        dismissKeyboard(v.windowToken)
        searchViewModel.setQuery(query)
    }

    private fun initRecyclerView() {
        adapter = RepoListAdapter(showFullName = true) { repo ->
            findNavController().navigate(
                SearchFragmentDirections.showRepo(repo.owner.login, repo.name)
            )
        }

        repo_list.adapter = adapter
        repo_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastPosition = layoutManager.findLastVisibleItemPosition()
                if (lastPosition == adapter.itemCount - 1) {
                    searchViewModel.loadNextPage()
                }
            }
        })

        searchViewModel.apply {
            results.observe(viewLifecycleOwner, Observer { renderResult(it)})
            loadMoreStatus.observe(viewLifecycleOwner,Observer{ renderLoadingMore(it)} )
        }

    }

    private fun dismissKeyboard(windowToken: IBinder) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }

    private  fun renderResult(result: Resource<List<Repo>>)
    {
        renderLoading(result)

        val noResult = result?.status == Status.OK && result.data?.size == 0
        no_results_text.setVisible(noResult)
        if(noResult)
        {
            no_results_text.text = resources.getString(
                R.string.empty_search_result,
                searchViewModel.query.value
            )
            toast(no_results_text.text,3,ToastType.NORMAL)
        }
        if(result.status == Status.LOADING){
            adapter.setList(result?.data)
        }else{
            adapter.submitList(result?.data)
        }
    }

    private fun renderLoadingMore(loadingMore: SearchViewModel.LoadMoreState)
    {
        if (loadingMore == null) {
            load_more_bar.setVisible(false)
        } else {
            load_more_bar.setVisible(loadingMore.isRunning)
            val error = loadingMore.errorMessageIfNotHandled
            if (error != null) {
                Snackbar.make(load_more_bar, error, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
