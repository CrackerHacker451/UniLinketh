package com.example.unilink.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.unilink.activities.ProfileActivity
import com.example.unilink.databinding.ItemPostBinding
import com.example.unilink.models.Post

class PostAdapter(private val posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val repository = com.example.unilink.repository.PostRepository()
    private val userRepository = com.example.unilink.repository.UserRepository()

    class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        
        holder.binding.tvPostAuthor.text = post.authorName
        holder.binding.tvPostContent.text = post.content
        holder.binding.tvPostLikes.text = "+${post.likes} likes"

        // Handle Like State UI
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val isLiked = post.likedBy.contains(currentUid)
        holder.binding.btnLike.setImageResource(if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)

        // Show VIP badge on post if author is premium
        userRepository.getUserById(post.authorId) { author ->
            if (author?.isPremium == true) {
                holder.binding.ivPostVipBadge.visibility = android.view.View.VISIBLE
            } else {
                holder.binding.ivPostVipBadge.visibility = android.view.View.GONE
            }
        }

        if (!post.imageUrl.isNullOrEmpty()) {
            holder.binding.ivPostImage.visibility = android.view.View.VISIBLE
            holder.binding.ivPostImage.setImageResource(com.example.unilink.R.drawable.bg_mesh_gradient)
        } else {
            holder.binding.ivPostImage.visibility = android.view.View.GONE
        }

        holder.binding.ivPostAvatar.setOnClickListener {
            val intent = Intent(holder.itemView.context, ProfileActivity::class.java)
            intent.putExtra("userId", post.authorId)
            holder.itemView.context.startActivity(intent)
        }
        
        holder.binding.tvPostAuthor.setOnClickListener {
            val intent = Intent(holder.itemView.context, ProfileActivity::class.java)
            intent.putExtra("userId", post.authorId)
            holder.itemView.context.startActivity(intent)
        }

        holder.binding.btnLike.setOnClickListener {
            repository.likePost(post.postId) { /* UI updates via SnapshotListener in Repository */ }
            // Haptic feedback or simple animation
            holder.binding.btnLike.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction {
                holder.binding.btnLike.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()
        }

        holder.binding.ivPostMenu.setOnClickListener {
            if (post.authorId == currentUid) {
                val popup = androidx.appcompat.widget.PopupMenu(holder.itemView.context, it)
                popup.menu.add("Delete Post")
                popup.setOnMenuItemClickListener { item ->
                    if (item.title == "Delete Post") {
                        repository.deletePost(post.postId) { success ->
                            if (success) android.widget.Toast.makeText(holder.itemView.context, "Post deleted", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                popup.show()
            }
        }
    }

    override fun getItemCount(): Int = posts.size
}
