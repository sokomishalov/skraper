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
@file:Suppress(
        "MoveVariableDeclarationIntoWhen"
)

package ru.sokomishalov.skraper.example.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import com.squareup.picasso.Picasso
import ru.sokomishalov.skraper.example.R
import ru.sokomishalov.skraper.model.Audio
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.model.Video

class PostsAdapter(
        private val context: Context,
        private val data: MutableList<Post>
) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val (view, vh) = when (convertView) {
            null -> {
                val view = LayoutInflater.from(context).inflate(R.layout.view_item, parent, false)
                view to ViewHolder(view)
            }
            else -> convertView to convertView.tag as ViewHolder
        }

        view.tag = vh

        val post = data[position]
        val attachment = post.media.firstOrNull()

        when (attachment) {
            is Image -> with(vh.image) {
                Picasso
                        .get()
                        .load(attachment.url)
                        .into(this)
            }
            is Video -> with(vh.video) {
                setVideoPath(attachment.url)
                start()
            }
            is Audio,
            null -> Unit
        }

        vh.tvTitle.text = data[position].text

        return view
    }

    override fun getItem(position: Int): Any = data[position]
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getCount(): Int = data.size

    operator fun plus(posts: List<Post>) {
        data += posts
        notifyDataSetChanged()
    }
}

private class ViewHolder(view: View) {
    val video: VideoView = view.findViewById(R.id.video_attachment)
    val image: ImageView = view.findViewById(R.id.image_attachment)
    val tvTitle: TextView = view.findViewById(R.id.post_text)
}