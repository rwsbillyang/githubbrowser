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
import com.github.rwsbillyang.appbase.apiresponse.Resource
import com.github.rwsbillyang.appbase.apiresponse.dataFetcher
import com.github.rwsbillyang.githubbrowser.model.api.GithubService
import com.github.rwsbillyang.githubbrowser.model.db.UserDao
import com.github.rwsbillyang.githubbrowser.model.vo.User

/**
 * Repository that handles User objects.
 */
//@OpenForTesting
//@Singleton
class UserRepository(
    private val local: UserDao,
    private val remote: GithubService
) {

    fun loadUser(login: String): LiveData<Resource<User>> = dataFetcher<User>
    {
        fromLocal { local.findByLogin(login) }
        fromRemote { remote.getUser(login) }
        converter { it }
        save{user -> local.insert(user)}
    }
}
