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

package com.github.rwsbillyang.githubbrowser.model.db


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.rwsbillyang.githubbrowser.ConstantsConfig
import com.github.rwsbillyang.githubbrowser.model.vo.Contributor
import com.github.rwsbillyang.githubbrowser.model.vo.Repo
import com.github.rwsbillyang.githubbrowser.model.vo.RepoSearchResult
import com.github.rwsbillyang.githubbrowser.model.vo.User

/**
 * Main database description.
 */
@Database(
    entities = [
        User::class,
        Repo::class,
        Contributor::class,
        RepoSearchResult::class],
    version = 3,
    exportSchema = false
)
abstract class GithubDb : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun repoDao(): RepoDao

    companion object {
        @Volatile private var INSTANCE: GithubDb? = null
        fun getInstance(context: Context): GithubDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                GithubDb::class.java, ConstantsConfig.DB_NAME)
                //.addMigrations(*GithubDb_Migrations.build())
                .build()
    }
}
