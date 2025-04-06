package com.example.driveremote.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.R
import com.example.driveremote.models.Post
import com.example.driveremote.models.User

class UserAdapter(private var users: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconRole: ImageView = view.findViewById(R.id.iconRole)
        val textName: TextView = view.findViewById(R.id.textName)
        val textAge: TextView = view.findViewById(R.id.textAge)
        val textEmail: TextView = view.findViewById(R.id.textEmail)
        val buttonAdd: ImageView = view.findViewById(R.id.buttonAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.textName.text = "${user.surName} ${user.firstName} ${user.fatherName}"
        holder.textAge.text = "${user.age} год(а) / лет"
        holder.textEmail.text = user.email

        // Выбор иконки по должности
        holder.iconRole.setImageResource(
            if (user.post == Post.ВОДИТЕЛЬ) R.drawable.driver else R.drawable.manager
        )

        holder.buttonAdd.setOnClickListener {
            // Обработка добавления
        }
    }

    fun filterList(filteredUsers: List<User>) {
        users = filteredUsers
        notifyDataSetChanged()
    }
}