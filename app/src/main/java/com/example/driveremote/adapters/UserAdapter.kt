package com.example.driveremote.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.R
import com.example.driveremote.models.Post
import com.example.driveremote.models.Request
import com.example.driveremote.models.User

class UserAdapter(
    private var users: List<User>,
    private val currentUserId: Int,
    private val currentUserPost: Post,
    private val requests: List<Request>,
    private val employeesList: List<Int>, // <--- новый параметр
    private val onAddClicked: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconRole: ImageView = view.findViewById(R.id.iconRole)
        val textName: TextView = view.findViewById(R.id.textName)
        val buttonAdd: ImageView = view.findViewById(R.id.buttonAdd)
        val borderline: View = view.findViewById(R.id.borderline2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val isAlreadyEmployee = employeesList.contains(user.id)
        holder.textName.text = "${user.surName}\n${user.firstName}\n${user.fatherName}"

        holder.iconRole.setImageResource(
            if (user.post == Post.ВОДИТЕЛЬ) R.drawable.driver else R.drawable.manager
        )

        val shouldShowAddButton =
            ((currentUserPost == Post.ВОДИТЕЛЬ && user.post == Post.РУКОВОДИТЕЛЬ) ||
                    (currentUserPost == Post.РУКОВОДИТЕЛЬ && user.post == Post.ВОДИТЕЛЬ)) &&
                    !isAlreadyEmployee

        if (shouldShowAddButton) {
            holder.borderline.visibility = View.VISIBLE
            holder.buttonAdd.visibility = View.VISIBLE

            val requestExists = requests.any {
                (it.senderId == currentUserId && it.receiverId == user.id) ||
                        (it.senderId == user.id && it.receiverId == currentUserId)
            }

            if (requestExists) {
                holder.buttonAdd.setImageResource(R.drawable.waiting)
                holder.buttonAdd.background = null
                holder.buttonAdd.isClickable = true
                holder.buttonAdd.setOnClickListener {
                    Toast.makeText(
                        holder.itemView.context,
                        "Запрос уже отправлен, ожидается ответ от получателя",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                holder.buttonAdd.setImageResource(android.R.drawable.ic_input_add)
                holder.buttonAdd.setBackgroundResource(R.drawable.small_circle_border)
                holder.buttonAdd.isClickable = true
                holder.buttonAdd.setOnClickListener {
                    onAddClicked(user)
                }
            }
        } else {
            holder.borderline.visibility = View.GONE
            holder.buttonAdd.visibility = View.GONE
        }
    }

    fun filterList(filteredUsers: List<User>) {
        users = filteredUsers
        notifyDataSetChanged()
    }
}