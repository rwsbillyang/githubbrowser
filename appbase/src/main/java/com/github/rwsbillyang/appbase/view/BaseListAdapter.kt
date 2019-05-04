package com.github.rwsbillyang.appbase.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.orhanobut.logger.Logger

/**
 * 简化RecyclerView.Adapter的使用
 */

abstract class BaseListAdapter<ItemType> : RecyclerView.Adapter<BaseListAdapter.BaseViewHolder<ItemType>>() {

    private var list: List<ItemType> = ArrayList()


    /**
     * 子类listAdapter需要指定item的layout，如R.layout.item_xxx
     */
    abstract fun getItemLayout(): Int

    /**
     * 子类listAdapter需要创建子类的viewHolder，如使用 XxxViewHolder();
     */
    abstract fun createViewHolder(itemView: View): BaseViewHolder<ItemType>

    override fun getItemCount() = list.size ?: 0

    /**
     * 添加新数据，通常用于加载更多
     * */
    fun submitList(newList: List<ItemType>?) {
        if (newList == null || newList.isEmpty()) {
            Logger.w("the list is null or empty")
        } else {
            val position = list.size
            this.list =  this.list.plus(newList)
            this.notifyItemRangeInserted(position,newList.size)
        }
    }

    /**
     * 指定新数据
     * */
    fun setList(newList: List<ItemType>?)
    {
        if (newList == null || newList.isEmpty()) {
            this.list = ArrayList()
        } else {
            val size = if (list.size > newList.size) list.size else newList.size
            this.list = newList
            this.notifyItemRangeChanged(0, size)
        }
    }
    /**
     * 重置
     * */
    fun resetList() {
        this.list = ArrayList()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ItemType> {
        val view = LayoutInflater.from(parent.context)
            .inflate(getItemLayout(), parent, false)
        return createViewHolder(view)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ItemType>, position: Int) {
        list?.let {
            holder.bindToItem(list!![position])
        }
    }

    /**
     * 子类viewHolder需要使用ButterKnife进行view的绑定
     *
     * <pre>{@code
     * @BindView(R.id.item_iv_repo_name)
     * TextView mIvRepoName;
     * @BindView(R.id.item_iv_repo_detail)
     * TextView mIvRepoDetail;
     * </pre>
     */
    abstract class BaseViewHolder<ItemType>(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        init {
//            ButterKnife.bind(this, itemView)
//        }

        /**
         * 子类viewHolder需要将数据与ItemView关联起来
         * <pre>{@code
         * mIvRepoName.setText(repo.name );
         * mIvRepoDetail.setText(String.valueOf(repo.description + "(" + repo.language + ")"));
         * </pre>
         * */
        abstract fun bindToItem(item: ItemType?)

    }

}
