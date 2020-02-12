package ru.sokomishalov.skraper.example.presenter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.provider.ninegag.NinegagSkraper
import ru.sokomishalov.skraper.provider.ninegag.getHotPosts


/**
 * @author sokomishalov
 */

class PostsPresenter {
    private var favoritesView: FavoritesView? = null
    private var job: Job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    fun getPosts() {
        scope.launch {
            favoritesView?.showFavorites(NinegagSkraper().getHotPosts(limit = 2))
        }
    }

    fun attachView(view: FavoritesView) {
        this.favoritesView = view
    }

    fun detachView() {
        job.cancel()
        favoritesView = null
    }

    interface FavoritesView {
        fun showFavorites(products: List<Post>)
    }
}