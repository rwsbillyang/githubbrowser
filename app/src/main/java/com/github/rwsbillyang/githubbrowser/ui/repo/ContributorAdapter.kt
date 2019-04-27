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

import android.view.View
import android.widget.ImageView
import com.github.rwsbillyang.appbase.util.loadImg
import com.github.rwsbillyang.appbase.view.BaseListAdapter
import com.github.rwsbillyang.githubbrowser.R
import com.github.rwsbillyang.githubbrowser.model.vo.Contributor
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.github_contributor_item.*
import kotlinx.android.synthetic.main.github_contributor_item.view.*

class ContributorAdapter(private val callback: ((Contributor, ImageView) -> Unit)?): BaseListAdapter<Contributor>() {
    override fun getItemLayout(): Int = R.layout.github_contributor_item

    override fun createViewHolder(itemView: View): BaseViewHolder<Contributor>
            = ContributorViewHolder(itemView,callback)

        //https://kotlinlang.org/docs/tutorials/android-plugin.html
        class ContributorViewHolder(override val containerView: View,private val callback: ((Contributor, ImageView) -> Unit)?):
            BaseListAdapter.BaseViewHolder<Contributor>(containerView), LayoutContainer {

            // private val imageView = itemView.findViewById<ImageView>(R.id.imageView)
            // private val textView = itemView.findViewById<TextView>(R.id.textView)

            override fun bindToItem(item: Contributor?) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
                    imageView.transitionName = item?.login
                }
                imageView.loadImg(item?.avatarUrl)
                textView.text = item?.login

                containerView.setOnClickListener {
                    item?.let{callback?.let { it.invoke(item,containerView.imageView) }}
                }
            }

        }

}

