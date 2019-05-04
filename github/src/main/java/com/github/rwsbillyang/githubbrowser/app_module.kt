package com.github.rwsbillyang.githubbrowser

import com.github.rwsbillyang.appbase.net.NetManager
import com.github.rwsbillyang.githubbrowser.model.api.GithubService
import com.github.rwsbillyang.githubbrowser.model.db.GithubDb
import com.github.rwsbillyang.githubbrowser.model.db.RepoDao
import com.github.rwsbillyang.githubbrowser.model.db.UserDao
import com.github.rwsbillyang.githubbrowser.model.repository.RepoRepository
import com.github.rwsbillyang.githubbrowser.model.repository.UserRepository
import com.github.rwsbillyang.githubbrowser.ui.repo.RepoViewModel
import com.github.rwsbillyang.githubbrowser.ui.search.SearchViewModel
import com.github.rwsbillyang.githubbrowser.ui.user.UserViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit


val commponModule = module {
    single<Retrofit> { NetManager.getRetrofit(ConstantsConfig.GITHUB_HOST_API) }
}


val githubModule = module {
    viewModel { RepoViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { UserViewModel(get(), get()) }

    //repository
    single { RepoRepository(get(),get(),get()) }
    single { UserRepository(get(), get()) }
    single<GithubService> { get<Retrofit>().create(GithubService::class.java) }

    //local storage
    single<GithubDb> { GithubDb.getInstance(androidApplication()) }
    single<UserDao> { get<GithubDb>().userDao() }
    single<RepoDao> { get<GithubDb>().repoDao() }
}


val appModule = listOf(commponModule,githubModule)