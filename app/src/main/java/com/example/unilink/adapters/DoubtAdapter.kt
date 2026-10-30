package com.example.unilink.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.unilink.databinding.ItemDoubtBinding
import com.example.unilink.models.Doubt
import com.example.unilink.web3.HederaX402Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoubtAdapter(private val doubts: List<Doubt>) : RecyclerView.Adapter<DoubtAdapter.DoubtViewHolder>() {

    class DoubtViewHolder(val binding: ItemDoubtBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoubtViewHolder {
        val binding = ItemDoubtBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoubtViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoubtViewHolder, position: Int) {
        val doubt = doubts[position]

        holder.binding.tvUserName.text = "${doubt.userName} · x402 (0.05 USDC)"
        holder.binding.tvQuestion.text = doubt.question
        holder.binding.tvRewardCoins.text = doubt.rewardCoins.toString()
        holder.binding.tvAnswerCount.text = if (doubt.answerCount > 0) "✅ ${doubt.answerCount} answers" else "No answers yet"
        holder.binding.viewAccent.setBackgroundColor(Color.parseColor(doubt.accentColor))

        holder.binding.viewOnlineStatus.visibility = if (doubt.isOnline) View.VISIBLE else View.GONE

        if (doubt.tags.isNotEmpty()) {
            holder.binding.tvTag1.text = doubt.tags[0]
            holder.binding.tvTag1.visibility = View.VISIBLE
            holder.binding.tvTag1.setTextColor(Color.parseColor(doubt.accentColor))
        } else {
            holder.binding.tvTag1.visibility = View.GONE
        }

        if (doubt.tags.size > 1) {
            holder.binding.tvTag2.text = doubt.tags[1]
            holder.binding.tvTag2.visibility = View.VISIBLE
            holder.binding.tvTag2.setTextColor(Color.parseColor(doubt.accentColor))
        } else {
            holder.binding.tvTag2.visibility = View.GONE
        }

        holder.binding.btnAnswer.setOnClickListener { view ->
            CoroutineScope(Dispatchers.Main).launch {
                val receipt = HederaX402Client.settleX402Request(
                    serviceName = "Doubt Answer Bounty (${doubt.question.take(24)})",
                    priceUsdcMicro = doubt.x402PriceUsdcMicro
                )
                if (receipt.settled) {
                    Toast.makeText(
                        view.context,
                        "⚡ Hedera x402 Settled (${receipt.amountUsdcFormatted})! Tx: ${receipt.hederaTxId} · HCS #${receipt.hcsSequenceNumber}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(view.context, receipt.errorMessage ?: "Payment blocked", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun getItemCount(): Int = doubts.size
}
