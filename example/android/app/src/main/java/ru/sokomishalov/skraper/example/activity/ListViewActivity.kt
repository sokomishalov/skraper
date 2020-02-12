package ru.sokomishalov.skraper.example.activity

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import ru.sokomishalov.skraper.example.R
import ru.sokomishalov.skraper.example.adapter.ListViewModelAdapter
import ru.sokomishalov.skraper.example.utils.VIEW_MODELS

class ListViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)

        val listView = findViewById<ListView>(R.id.sample_listVw)

        val listViewAdapter = ListViewModelAdapter(this, VIEW_MODELS)

        listView.adapter = listViewAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ -> }
    }
}
