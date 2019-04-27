基于https://github.com/googlesamples/android-architecture-components/tree/master/GithubBrowserSample

主要做了以下修改

### 依赖注入由Dagger改为Koin

使用起来更加简单明了；

### Respository层使用DSL进行配置

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

fromLocal 是从本地加载数据

fromRemote 是从远程加载数据

converter 将远程加载的数据变换成本地需要的数据

save是保存到本地

isForceRefresh 意味着是否强制刷新

onFetchFail 若从远程取得的数据出错的回调


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


### 网络请求改由利用Kotlin的coroutine实现

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

### 添加了interceptor机制

可以优先直接拦截处理来自网络的请求结果，处理完若返回true，则不再发送更新给前端UI所观察的Resource<T>所在liveData


### 使用View Binding(Kotlin Android Extensions)替换掉DataBinding

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

### 其它
- 来自网络数据具有最高优先级，本地存储只作为备用；
- 调整了部分依赖库版本，调整了目录结构，并将一些可作为公共部分的代码提取到appbase中；
