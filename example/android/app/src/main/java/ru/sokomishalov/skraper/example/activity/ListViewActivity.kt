/**
 * Copyright (c) 2019-present Mikhael Sokolov
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
package ru.sokomishalov.skraper.example.activity

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.okhttp3.OkHttp3SkraperClient
import ru.sokomishalov.skraper.example.R
import ru.sokomishalov.skraper.example.adapter.PostsAdapter
import ru.sokomishalov.skraper.provider.facebook.FacebookSkraper
import ru.sokomishalov.skraper.provider.facebook.getUserPosts
import ru.sokomishalov.skraper.provider.instagram.InstagramSkraper
import ru.sokomishalov.skraper.provider.instagram.getUserPosts
import ru.sokomishalov.skraper.provider.ninegag.NinegagSkraper
import ru.sokomishalov.skraper.provider.ninegag.getTagPosts
import ru.sokomishalov.skraper.provider.reddit.RedditSkraper
import ru.sokomishalov.skraper.provider.reddit.getCommunityHotPosts
import ru.sokomishalov.skraper.provider.twitter.TwitterSkraper
import ru.sokomishalov.skraper.provider.twitter.getUserPosts
import kotlin.coroutines.CoroutineContext

class ListViewActivity : AppCompatActivity(), CoroutineScope {

    override val coroutineContext: CoroutineContext get() = job + Dispatchers.Main

    private val job: Job = Job()
    private val postsAdapter: PostsAdapter = PostsAdapter(context = this, data = mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)
        fetchItems()
        findViewById<ListView>(R.id.posts_list_view).adapter = postsAdapter
    }

    private fun fetchItems() = launch {
        val items = withContext(Dispatchers.Default) {
            listOf(
                    RedditSkraper(client = DEFAULT_CLIENT).getCommunityHotPosts(community = "r/videos", limit = DEFAULT_LIMIT),
                    FacebookSkraper(client = DEFAULT_CLIENT).getUserPosts(username = "memes", limit = DEFAULT_LIMIT),
                    InstagramSkraper(client = DEFAULT_CLIENT).getUserPosts(username = "memes", limit = DEFAULT_LIMIT),
                    TwitterSkraper(client = DEFAULT_CLIENT).getUserPosts(username = "memes", limit = DEFAULT_LIMIT),
                    NinegagSkraper(client = DEFAULT_CLIENT).getTagPosts(tag = "meme", limit = DEFAULT_LIMIT)
            ).flatten().sortedByDescending { it.publishedAt }
        }

        findViewById<ProgressBar>(R.id.posts_list_view_progress_bar).visibility = View.GONE

        postsAdapter + items
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        private const val DEFAULT_LIMIT: Int = 5
        private val DEFAULT_CLIENT: SkraperClient = OkHttp3SkraperClient()
    }
}