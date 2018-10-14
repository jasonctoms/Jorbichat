package com.jorbital.jorbichat

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.message_item.view.*

class MessagesAdapter internal constructor(options: FirebaseRecyclerOptions<JorbichatMessage>, private val progressBar: View) :
        FirebaseRecyclerAdapter<JorbichatMessage, MessagesAdapter.MessageViewHolder>(options) {
    companion object {
        private const val TAG = "MessagesAdapter"
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MessageViewHolder {
        val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.message_item, viewGroup, false)
        return MessageViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: MessageViewHolder,
                                  position: Int,
                                  jorbichatMessage: JorbichatMessage) {
        viewHolder.bind(viewHolder, jorbichatMessage)
    }

    inner class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        internal fun bind(vh: MessageViewHolder, message: JorbichatMessage) {
            progressBar.visibility = ProgressBar.INVISIBLE

            //if text isn't null, it is a normal message
            if (message.text != null) {
                vh.itemView.messageBody.text = message.text
                vh.itemView.messageBody.visibility = TextView.VISIBLE
                vh.itemView.messageImage.visibility = ImageView.GONE
            }
            //if the image is not downloaded locally
            else if (message.imageUrl != null) {
                val imageUrl = message.imageUrl
                if (imageUrl!!.startsWith("gs://")) {
                    val storageReference = FirebaseStorage.getInstance()
                            .getReferenceFromUrl(imageUrl)
                    storageReference.downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val downloadUrl = task.result!!.toString()
                            Glide.with(vh.itemView.messageImage.context)
                                    .load(downloadUrl)
                                    .into(vh.itemView.messageImage)
                        } else {
                            Log.w(TAG, "Getting download url was not successful.",
                                    task.exception)
                        }
                    }
                } else {
                    Glide.with(vh.itemView.messageImage.context)
                            .load(message.imageUrl)
                            .into(vh.itemView.messageImage)
                }
                vh.itemView.messageImage.visibility = ImageView.VISIBLE
                vh.itemView.messageBody.visibility = TextView.GONE
            }

            vh.itemView.messageFrom.text = message.name
            if (message.photoUrl != null) {
                Glide.with(vh.itemView.messageAvatar.context)
                        .load(message.photoUrl)
                        .into(vh.itemView.messageAvatar)
            }
        }
    }
}
