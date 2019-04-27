

### 技术栈

- MVVM架构 涉及到Lifecycle/ViewModel/LiveData等android架构核心组件；
- Navigation  单Activity下的多Fragment，跟前端的单页应用SPA是不是有点类似？
- AndroidX 全新支持库；
- View Binding  使用Kotlin的Android扩展实现，DataBinding让xml中夹杂着数据，还需要导入冗长的包名，经常会出拼写错误；
- Retrofit2/OkHttp3 网络请求，提供了多种拦截器，支持多站点配置 ；
- Kotlin 主要是lambda、DSL、扩展、let/run等库函数， 大大缩减缩累赘代码；
- Kotlin Coroutine  用协程代替多线程，更简便；
- Room 数据库存储，可更换成NoSQL数据库；
- Koin 依赖注入，使用更方便，不再使用Dagger；
- Glide 加载图片；



### 主要修改更新

基于
https://github.com/googlesamples/android-architecture-components/tree/master/GithubBrowserSample

做了以下几方面修改：


#### 依赖注入由Dagger改为Koin

使用起来更加简单。

#### Respository层使用DSL进行配置

不再使用原来的继承方式，改用DSL实现，更加简洁优雅，示例如下：
```
 fun loadRepos(owner: String): LiveData<Resource<List<Repo>>> = dataFetcher<List<Repo>>
    {
        fromLocal { repoDao.loadRepositories(owner) }
        fromRemote { githubService.getRepos(owner) }
        converter { it }
        save { repoDao.insertRepos(it) }
        isForceRefresh { it == null || it.isEmpty() || repoListRateLimit.shouldFetch(owner) }
        onFetchFail {code,msg -> repoListRateLimit.reset(owner)}
    }
```

fromLocal 从本地加载数据

fromRemote 从远程加载数据

converter 将远程加载的数据变换成本地需要的数据

save 网络请求的数据保存到本地

isForceRefresh 意是否强制刷新

onFetchFail 从远程取得的数据出错时的回调


如果远端返回的数据需要变换，示例如下：
```
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
            }
        }
        converter { it.items }
        isForceRefresh { it == null }
        processResponse {
            val body: RepoSearchResponse = it.body
            body.nextPage = it.nextPage
            body
        }
    }
```
processResponse是远程api调用响应的结果进行变换。

其中 Retrofit的接口类直接返回Call<RepoSearchResponse>, DB接口直接返回List<Repo>，liveData中保存的值的类型是Resource<List<Repo>>；


#### 网络请求改由利用Kotlin的coroutine实现

协程依赖kotlin1.3版本。

由于Room被限制在非主线程，协程切换到一个新线程中,参见DataFetcher：
```
    fun loadData(){
        CoroutineScope(Dispatchers.IO).launch(newSingleThreadContext("loadData")){
            localBlock?.let {
                setValue(Resource.loading(null))
                val localResult = CoroutineScope(Dispatchers.IO).async {
                    return@async it()
                }.await()
                setValue(Resource.success(localResult))
            }

            remoteBlock?.let {
                result.postValue(Resource.loading(null))
                Coroutines.ioThenMain({call2ApiResponse(it())}){
                    notify(it)
                    this@DataFetcher.setValue(it.toResource())
                }
            }
        }
    }
```

#### 添加了interceptor机制

可以优先直接拦截处理来自网络的请求结果，处理完若返回true，则不再发送更新给前端UI所观察的Resource<T>所在liveData。
主要用于某些业务逻辑错误的统一处理，而无需影响大上层UI。


#### 使用View Binding(Kotlin Android Extensions)替换掉DataBinding

在Activity或Fragment中直接操作UI控件，不再在xml中夹杂着绑定的数据变量。

改用在代码中使用liveData进行observe，observer的工作主要renderView；

如repoFragment中，当结果数据有变化时，更新repo数据：
```
repoViewModel.repo.observe(viewLifecycleOwner,Observer<Resource<Repo>>{
            renderRepo(it)
        })
```

searchFragment中，观察加载更多的状态变化：
```
searchViewModel.loadMoreStatus.observe(viewLifecycleOwner,Observer{updateLoadingMore(it)})
```

注意：xml中的其它控件，配置好初始状态，然后在onViewCreated向上面配置好数据监察响应即可。

#### 其它
- 来自网络数据具有最高优先级，本地存储只作为备用；
- 调整了部分依赖库版本，调整了目录结构，并将一些可作为公共部分的代码提取到appbase中；


### 特别鸣谢：
https://github.com/googlesamples/android-architecture-components/tree/master/GithubBrowserSample

改写过程中还参考了下列项目，一并感谢：

https://github.com/ditclear/PaoNet

https://github.com/githubwing/GankClient-Kotlin

https://github.com/shkschneider/android_viewmodel_livedata_coroutines/