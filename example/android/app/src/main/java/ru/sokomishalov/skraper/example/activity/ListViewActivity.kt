package ru.sokomishalov.skraper.example.activity

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking
import ru.sokomishalov.skraper.example.R
import ru.sokomishalov.skraper.example.adapter.ListViewModelAdapter
import ru.sokomishalov.skraper.provider.ninegag.NinegagSkraper
import ru.sokomishalov.skraper.provider.ninegag.getHotPosts

class ListViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)

        val listView = findViewById<ListView>(R.id.sample_listVw)

        val items = runBlocking { NinegagSkraper().getHotPosts(limit = 2) }

        val listViewAdapter = ListViewModelAdapter(this, items)

        listView.adapter = listViewAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ -> }
    }
}
