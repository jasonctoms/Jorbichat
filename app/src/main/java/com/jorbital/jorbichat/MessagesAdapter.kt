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
        internal fun bind(viewHolder: MessageViewHolder, jorbichatMessage: JorbichatMessage) {
            progressBar.visibility = ProgressBar.INVISIBLE

            //if text isn't null, it is a normal message
            if (jorbichatMessage.text != null) {
                viewHolder.itemView.messageBody.text = jorbichatMessage.text
                viewHolder.itemView.messageBody.visibility = TextView.VISIBLE
                viewHolder.itemView.messageImage.visibility = ImageView.GONE
            }
            //if the image is not downloaded locally
            else if (jorbichatMessage.imageUrl != null) {
                val imageUrl = jorbichatMessage.imageUrl
                if (imageUrl!!.startsWith("gs://")) {
                    val storageReference = FirebaseStorage.getInstance()
                            .getReferenceFromUrl(imageUrl)
                    storageReference.downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val downloadUrl = task.result!!.toString()
                            Glide.with(viewHolder.itemView.messageImage.context)
                                    .load(downloadUrl)
                                    .into(viewHolder.itemView.messageImage)
                        } else {
                            Log.w(TAG, "Getting download url was not successful.",
                                    task.exception)
                        }
                    }
                } else {
                    Glide.with(viewHolder.itemView.messageImage.context)
                            .load(jorbichatMessage.imageUrl)
                            .into(viewHolder.itemView.messageImage)
                }
                viewHolder.itemView.messageImage.visibility = ImageView.VISIBLE
                viewHolder.itemView.messageBody.visibility = TextView.GONE
            }

            viewHolder.itemView.messageFrom.text = jorbichatMessage.name
            if (jorbichatMessage.photoUrl != null) {
                Glide.with(viewHolder.itemView.messageAvatar.context)
                        .load(jorbichatMessage.photoUrl)
                        .into(viewHolder.itemView.messageAvatar)
            }
        }
    }
}
