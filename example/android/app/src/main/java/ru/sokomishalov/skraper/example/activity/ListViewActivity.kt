package ru.sokomishalov.skraper.example.activity

import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.okhttp3.OkHttp3SkraperClient
import ru.sokomishalov.skraper.example.R
import ru.sokomishalov.skraper.example.adapter.ListViewModelAdapter
import ru.sokomishalov.skraper.provider.ninegag.NinegagSkraper
import ru.sokomishalov.skraper.provider.ninegag.getHotPosts
import kotlin.coroutines.CoroutineContext

class ListViewActivity : AppCompatActivity(), CoroutineScope {

    override val coroutineContext: CoroutineContext get() = job + Dispatchers.Main

    private val job: Job = Job()
    private val listViewAdapter: ListViewModelAdapter = ListViewModelAdapter(context = this, data = mutableListOf())

    private val client: SkraperClient = OkHttp3SkraperClient()
    private val limit: Int = 2
    private val skraper: NinegagSkraper = NinegagSkraper(client = client)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)
        fetchItems()
        val listView = findViewById<ListView>(R.id.postsListView)
        listView.adapter = listViewAdapter
    }

    private fun fetchItems() = launch {
        val items = withContext(Dispatchers.Default) {
            skraper.getHotPosts(limit = limit)
        }
        listViewAdapter + items
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

