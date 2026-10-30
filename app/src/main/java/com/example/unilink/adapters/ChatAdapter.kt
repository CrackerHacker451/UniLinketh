package com.example.unilink.adapters

import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.unilink.R
import com.example.unilink.databinding.*
import com.example.unilink.models.ChatMessage
import com.example.unilink.utils.ThemeUtils
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: String,
    private val onMessageClick: (ChatMessage, View) -> Unit,
    private val onMessageDoubleTap: (ChatMessage) -> Unit,
    private val onPollVote: (ChatMessage, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
        private const val TYPE_POLL = 3
        private const val TYPE_DOUBT = 4
        private const val TYPE_TOKEN = 5
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when (msg.type) {
            "poll" -> TYPE_POLL
            "doubt" -> TYPE_DOUBT
            "token" -> TYPE_TOKEN
            else -> if (msg.senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SENT -> SentViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
            TYPE_RECEIVED -> ReceivedViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
            TYPE_POLL -> PollViewHolder(ItemMessagePollBinding.inflate(inflater, parent, false))
            TYPE_DOUBT -> DoubtViewHolder(ItemMessageDoubtBinding.inflate(inflater, parent, false))
            TYPE_TOKEN -> TokenViewHolder(ItemMessageTokenBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = if (msg.timestamp != null) timeFormat.format(msg.timestamp) else ""

        setupCommonListeners(holder.itemView, msg)

        when (holder) {
            is SentViewHolder -> {
                holder.binding.tvMessage.text = msg.messageText
                holder.binding.tvTimestamp.text = time
                
                val context = holder.itemView.context
                val isLightTheme = ThemeUtils.getSelectedTheme(context) == "WhatsApp Light"
                
                // --- Reply UI ---
                if (msg.replyToText != null) {
                    holder.binding.llReplyQuote.visibility = View.VISIBLE
                    holder.binding.tvReplyText.text = msg.replyToText
                    if (isLightTheme) {
                        holder.binding.llReplyQuote.setBackgroundColor(android.graphics.Color.parseColor("#E1F5FE"))
                        holder.binding.tvReplyText.setTextColor(android.graphics.Color.parseColor("#111B21"))
                    } else {
                        holder.binding.llReplyQuote.setBackgroundColor(android.graphics.Color.parseColor("#1AFFFFFF"))
                        holder.binding.tvReplyText.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                    }
                } else {
                    holder.binding.llReplyQuote.visibility = View.GONE
                }

                // --- Edited UI ---
                holder.binding.tvEdited.visibility = if (msg.isEdited) View.VISIBLE else View.GONE

                // Bubble colors
                if (isLightTheme) {
                    holder.binding.llMessageBubble.setBackgroundResource(R.drawable.bg_message_sent)
                    holder.binding.llMessageBubble.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D9FDD3"))
                    holder.binding.tvMessage.setTextColor(android.graphics.Color.parseColor("#111B21"))
                    holder.binding.tvTimestamp.setTextColor(android.graphics.Color.parseColor("#667781"))
                } else {
                    holder.binding.llMessageBubble.setBackgroundResource(if (msg.status == "seen") R.drawable.bg_message_seen else R.drawable.bg_message_sent)
                    holder.binding.llMessageBubble.backgroundTintList = null
                    holder.binding.tvMessage.setTextColor(android.graphics.Color.WHITE)
                    holder.binding.tvTimestamp.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                }

                // Seen Status
                if (msg.status == "seen") {
                    holder.binding.ivStatus.setImageResource(android.R.drawable.presence_online) 
                    holder.binding.ivStatus.setColorFilter(android.graphics.Color.parseColor(if (isLightTheme) "#34B7F1" else "#EC4899"))
                } else {
                    holder.binding.ivStatus.setImageResource(android.R.drawable.checkbox_on_background)
                    holder.binding.ivStatus.setColorFilter(android.graphics.Color.parseColor("#667781"))
                }

                holder.binding.ivStatus.visibility = View.VISIBLE
                renderReactions(holder.binding.llReactions, msg.reactions)
            }
            is ReceivedViewHolder -> {
                holder.binding.tvMessage.text = msg.messageText
                holder.binding.tvTimestamp.text = time
                holder.binding.tvSenderUsername.text = msg.senderUsername
                holder.binding.tvSenderUsername.visibility = if (msg.senderUsername.isNotEmpty()) View.VISIBLE else View.GONE

                val context = holder.itemView.context
                val isLightTheme = ThemeUtils.getSelectedTheme(context) == "WhatsApp Light"

                // --- Reply UI ---
                if (msg.replyToText != null) {
                    holder.binding.llReplyQuote.visibility = View.VISIBLE
                    holder.binding.tvReplyText.text = msg.replyToText
                    if (isLightTheme) {
                        holder.binding.llReplyQuote.setBackgroundColor(android.graphics.Color.parseColor("#F0F0F0"))
                        holder.binding.tvReplyText.setTextColor(android.graphics.Color.parseColor("#111B21"))
                    } else {
                        holder.binding.llReplyQuote.setBackgroundColor(android.graphics.Color.parseColor("#1AFFFFFF"))
                        holder.binding.tvReplyText.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                    }
                } else {
                    holder.binding.llReplyQuote.visibility = View.GONE
                }

                if (isLightTheme) {
                    holder.binding.llMessageBubble.setBackgroundResource(R.drawable.bg_message_received)
                    holder.binding.llMessageBubble.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                    holder.binding.tvMessage.setTextColor(android.graphics.Color.parseColor("#111B21"))
                    holder.binding.tvTimestamp.setTextColor(android.graphics.Color.parseColor("#667781"))
                    holder.binding.tvSenderUsername.setTextColor(android.graphics.Color.parseColor("#075E54"))
                } else {
                    holder.binding.llMessageBubble.setBackgroundResource(R.drawable.bg_message_received)
                    holder.binding.llMessageBubble.backgroundTintList = null
                    holder.binding.tvMessage.setTextColor(android.graphics.Color.WHITE)
                    holder.binding.tvTimestamp.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                    holder.binding.tvSenderUsername.setTextColor(android.graphics.Color.parseColor("#EC4899"))
                }

                // --- Edited UI ---
                holder.binding.tvEdited.visibility = if (msg.isEdited) View.VISIBLE else View.GONE
                renderReactions(holder.binding.llReactions, msg.reactions)
            }
            is PollViewHolder -> {
                holder.binding.tvPollQuestion.text = msg.pollQuestion
                renderPollOptions(holder.binding.pollOptionsContainer, msg)
            }
            is DoubtViewHolder -> {
                holder.binding.tvDoubtText.text = msg.messageText
                holder.binding.tvTokenReward.text = "💰 ${msg.doubtReward} UniCoins Reward"
                holder.binding.btnAnswer.setOnClickListener { 
                    // Answer logic
                }
            }
            is TokenViewHolder -> {
                holder.binding.tvTokenAmount.text = "${msg.uniCoinAmount} UNICOINS"
                holder.binding.tvTokenNote.text = msg.tokenReason
            }
        }
    }

    private fun setupCommonListeners(view: View, msg: ChatMessage) {
        val detector = GestureDetector(view.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onMessageClick(msg, view)
                return true
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onMessageDoubleTap(msg)
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onMessageClick(msg, view)
            }
        })
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.performClick()
            }
            detector.onTouchEvent(event)
            true
        }
    }

    private fun renderReactions(container: LinearLayout, reactions: Map<String, List<String>>) {
        if (reactions.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        container.removeAllViews()
        
        reactions.forEach { (emoji, userIds) ->
            if (userIds.isNotEmpty()) {
                val tv = android.widget.TextView(container.context)
                tv.text = "$emoji ${userIds.size}"
                tv.textSize = 10f
                tv.setPadding(8, 0, 8, 0)
                tv.setTextColor(android.graphics.Color.WHITE)
                container.addView(tv)
            }
        }
    }

    private fun renderPollOptions(container: LinearLayout, msg: ChatMessage) {
        container.removeAllViews()
        val totalVotes = msg.pollOptions.values.sum().coerceAtLeast(1)
        
        msg.pollOptions.forEach { (option, count) ->
            val optionBinding = ItemPollOptionBinding.inflate(LayoutInflater.from(container.context), container, false)
            optionBinding.tvOptionText.text = option
            val percent = (count * 100) / totalVotes
            optionBinding.tvOptionPercent.text = "$percent%"
            
            // Progress width setting
            optionBinding.optionProgress.post {
                val params = optionBinding.optionProgress.layoutParams as FrameLayout.LayoutParams
                params.width = (optionBinding.root.width * percent) / 100
                optionBinding.optionProgress.layoutParams = params
            }

            optionBinding.root.setOnClickListener { onPollVote(msg, option) }
            container.addView(optionBinding.root)
        }
    }

    override fun getItemCount(): Int = messages.size

    class SentViewHolder(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root)
    class ReceivedViewHolder(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root)
    class PollViewHolder(val binding: ItemMessagePollBinding) : RecyclerView.ViewHolder(binding.root)
    class DoubtViewHolder(val binding: ItemMessageDoubtBinding) : RecyclerView.ViewHolder(binding.root)
    class TokenViewHolder(val binding: ItemMessageTokenBinding) : RecyclerView.ViewHolder(binding.root)
}
