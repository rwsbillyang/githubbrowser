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

package com.github.rwsbillyang.githubbrowser.model.repository

import androidx.lifecycle.LiveData
import com.github.rwsbillyang.appbase.apiresponse.*
import com.github.rwsbillyang.githubbrowser.model.api.GithubService
import com.github.rwsbillyang.githubbrowser.model.db.GithubDb
import com.github.rwsbillyang.githubbrowser.model.db.RepoDao
import com.github.rwsbillyang.githubbrowser.model.vo.Contributor
import com.github.rwsbillyang.githubbrowser.model.vo.Repo
import com.github.rwsbillyang.githubbrowser.model.vo.RepoSearchResponse
import com.github.rwsbillyang.githubbrowser.model.vo.RepoSearchResult
import java.util.concurrent.TimeUnit

/**
 * Repository that handles Repo instances.
 *
 * unfortunate naming :/ .
 * Repo - value object name
 * Repository - type of this class.
 */
//@OpenForTesting
class RepoRepository  constructor(
    private val db: GithubDb,
    private val repoDao: RepoDao,
    private val githubService: GithubService
) {

    private val repoListRateLimit =
        RateLimiter<String>(10, TimeUnit.MINUTES)

    fun loadRepos(owner: String): LiveData<Resource<List<Repo>>> = dataFetcher<List<Repo>>
    {
        fromLocal { repoDao.loadRepositories(owner) }
        fromRemote { githubService.getRepos(owner) }
        converter { it }
        save { repoDao.insertRepos(it) }
        isForceRefresh { it == null || it.isEmpty() || repoListRateLimit.shouldFetch(owner) }
        onFetchFail {_,_ -> repoListRateLimit.reset(owner)}
    }

    fun loadRepo(owner: String, name: String): LiveData<Resource<Repo>> = dataFetcher<Repo>{
        fromLocal{repoDao.load(ownerLogin = owner,name = name)}
        fromRemote { githubService.getRepo(owner = owner, name = name) }
        converter { it }
        save { repoDao.insert(it)}
        isForceRefresh { it == null }
    }

    fun loadContributors(owner: String, name: String): LiveData<Resource<List<Contributor>>> = dataFetcher<List<Contributor>>
    {
        fromLocal { repoDao.loadContributors(owner, name) }
        fromRemote { githubService.getContributors(owner, name) }
        converter { it }
        isForceRefresh{it == null || it.isEmpty()}
        save {
            it.forEach {
                it.repoName = name
                it.repoOwner = owner
            }
            db.runInTransaction {
                repoDao.createRepoIfNotExists(
                    Repo(
                        id = Repo.UNKNOWN_ID,
                        name = name,
                        fullName = "$owner/$name",
                        description = "",
                        owner = Repo.Owner(owner, null),
                        stars = 0
                    )
                )
                repoDao.insertContributors(it)
            } }
    }



    fun searchNextPage(query: String): LiveData<Resource<Boolean>> {
        val fetchNextSearchPageTask = FetchNextSearchPageTask(
            query = query,
            githubService = githubService,
            db = db
        )

        Coroutines.io(true){
            fetchNextSearchPageTask.run()
        }

        return fetchNextSearchPageTask.liveData
    }

    fun search(query: String): LiveData<Resource<List<Repo>>> = dataFetcher2<RepoSearchResponse,List<Repo>>{
        fromLocal { repoDao.search(query)?.let {repoDao.loadOrdered(it.repoIds)} }
        fromRemote {githubService.searchRepos(query)}
        save {
            val repoIds = it.items.map { it.id }
            val repoSearchResult = RepoSearchResult(
                query = query,
                repoIds = repoIds,
                totalCount = it.total,
                next = it.nextPage
            )
            db.beginTransaction()
            try {
                repoDao.insertRepos(it.items)
                repoDao.insert(repoSearchResult)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            } }
        converter { it?.items }
        isForceRefresh { it == null }
        processResponse {
            val body: RepoSearchResponse = it.body
            body.nextPage = it.nextPage
            body
        }
    }


}
