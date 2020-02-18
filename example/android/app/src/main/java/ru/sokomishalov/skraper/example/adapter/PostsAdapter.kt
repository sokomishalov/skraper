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
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.Post

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
        val attachment = post.attachments.firstOrNull()

        if (attachment != null) {
            when (attachment.type) {
                IMAGE -> with(vh.image) {
                    Picasso
                        .get()
                        .load(attachment.url)
                        .into(this)
                }
                VIDEO -> with(vh.video) {
                    setVideoPath(attachment.url)
                    start()
                }
            }
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
    val tvTitle: TextView = view.findViewById(R.id.tvContent)
}