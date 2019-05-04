package com.github.rwsbillyang.githubbrowser.ui.user

import android.view.View
import com.github.rwsbillyang.appbase.view.BaseListAdapter
import com.github.rwsbillyang.githubbrowser.R
import com.github.rwsbillyang.githubbrowser.model.vo.Repo
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.github_repo_item.*

//import kotlinx.android.synthetic.main.github_repo_item.view.*

class RepoListAdapter(private val showFullName: Boolean = true, private val repoClickCallback: ((Repo) -> Unit)?): BaseListAdapter<Repo>(){
    override fun getItemLayout() = R.layout.github_repo_item

    override fun createViewHolder(itemView: View): BaseViewHolder<Repo> = RepoItemViewHolder(itemView,showFullName,repoClickCallback)



    class RepoItemViewHolder (override val containerView: View, private val showFullName:Boolean = true,
                              private val callback: ((Repo) -> Unit)?):
        BaseListAdapter.BaseViewHolder<Repo>(containerView), LayoutContainer {

        override fun bindToItem(item: Repo?) {
            name.text = if(showFullName) item?.fullName else item?.name
            desc.text = item?.description
            stars.text = item?.stars.toString()

            containerView.setOnClickListener { item?.let { callback?.invoke(item) } }
        }
    }
}