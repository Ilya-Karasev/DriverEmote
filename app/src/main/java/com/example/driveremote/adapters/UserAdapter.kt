package com.example.driveremote.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
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
    private val employeesList: List<Int>,
    private val onAddClicked: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconInitials: TextView = view.findViewById(R.id.iconRole)
        val textName: TextView = view.findViewById(R.id.textName)
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
        val context = holder.itemView.context
        val isAlreadyEmployee = employeesList.contains(user.id)

        // Формируем инициалы
        val initials = "${user.surName.firstOrNull() ?: ""}${user.firstName.firstOrNull() ?: ""}".uppercase()

        // Генерируем насыщенный цвет для текста
        val textColor = generateRandomColor(saturation = 0.8f, brightness = 0.9f)
        // Генерируем бледный цвет для фона
        val backgroundColor = adjustAlpha(textColor, 0.15f)

        holder.iconInitials.text = initials
        holder.iconInitials.setBackgroundColor(backgroundColor)
        holder.iconInitials.setTextColor(textColor)
        holder.iconInitials.textAlignment = View.TEXT_ALIGNMENT_CENTER

        val fullName = "${user.surName} ${user.firstName} ${user.fatherName}"

        holder.textName.text = fullName

        val shouldShowAddButton =
            ((currentUserPost == Post.ВОДИТЕЛЬ && user.post == Post.РУКОВОДИТЕЛЬ) ||
                    (currentUserPost == Post.РУКОВОДИТЕЛЬ && user.post == Post.ВОДИТЕЛЬ)) &&
                    !isAlreadyEmployee

        if (shouldShowAddButton) {
            holder.buttonAdd.visibility = View.VISIBLE

            val requestExists = requests.any {
                (it.senderId == currentUserId && it.receiverId == user.id) ||
                        (it.senderId == user.id && it.receiverId == currentUserId)
            }

            if (requestExists) {
                holder.buttonAdd.setImageResource(R.drawable.waiting)
                holder.buttonAdd.background = null
                holder.buttonAdd.setOnClickListener {
                    Toast.makeText(
                        context,
                        "Запрос уже отправлен, ожидается ответ от получателя",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                holder.buttonAdd.setImageResource(R.drawable.add)
                holder.buttonAdd.setOnClickListener {
                    onAddClicked(user)
                }
            }
        } else {
            holder.buttonAdd.visibility = View.GONE
        }
    }

    private fun generateRandomColor(saturation: Float, brightness: Float): Int {
        val hue = (0..360).random().toFloat()
        val hsv = floatArrayOf(hue, saturation, brightness)
        return Color.HSVToColor(hsv)
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    fun filterList(filteredUsers: List<User>) {
        users = filteredUsers
        notifyDataSetChanged()
    }
}