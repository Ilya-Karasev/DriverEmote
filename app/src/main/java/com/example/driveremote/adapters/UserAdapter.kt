package com.example.driveremote.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.R
import com.example.driveremote.api.ApiService
import com.example.driveremote.models.Post
import com.example.driveremote.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserAdapter(
    private val apiService: ApiService, // Параметр для Retrofit
    private val currentUserId: Int,
    private val currentUserPost: Post,
    private val employeesList: List<Int>,
    private val onAddClicked: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users: List<User> = emptyList()

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

        val initials = "${user.surName.firstOrNull() ?: ""}${user.firstName.firstOrNull() ?: ""}".uppercase()
        val textColor = generateRandomColor(saturation = 0.8f, brightness = 0.9f)
        val backgroundColor = adjustAlpha(textColor, 0.15f)

        holder.iconInitials.text = initials
        holder.iconInitials.setBackgroundColor(backgroundColor)
        holder.iconInitials.setTextColor(textColor)
        holder.iconInitials.textAlignment = View.TEXT_ALIGNMENT_CENTER

        val fullName = "${user.surName} ${user.firstName} ${user.fatherName}"
        holder.textName.text = fullName

        if (currentUserPost == Post.РУКОВОДИТЕЛЬ && user.post == Post.ВОДИТЕЛЬ) {
            val isAlreadyEmployee = employeesList.contains(user.id)
            if (isAlreadyEmployee) {
                holder.buttonAdd.visibility = View.GONE
            } else {
                holder.buttonAdd.visibility = View.VISIBLE
                checkRequestStatus(user, holder)
            }
        } else if (currentUserPost == Post.ВОДИТЕЛЬ && user.post == Post.РУКОВОДИТЕЛЬ) {
            holder.buttonAdd.visibility = View.GONE // по умолчанию
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val manager = apiService.getManagerById(user.id) // менеджер отображаемого user
                    val employees = manager?.employeesList ?: emptyList()
                    val isCurrentUserInList = employees.contains(currentUserId)
                    withContext(Dispatchers.Main) {
                        if (!isCurrentUserInList) {
                            holder.buttonAdd.visibility = View.VISIBLE
                            checkRequestStatus(user, holder)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при проверке подчинённости", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            holder.buttonAdd.visibility = View.GONE
        }
    }

    private fun checkRequestStatus(user: User, holder: UserViewHolder) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Получаем список запросов с сервера
                val requestsSender = apiService.getRequestsBySender(user.id)
                val requestsReceiver = apiService.getRequestsByReceiver(user.id)

                val requestExists = requestsSender.any {
                    (it.sender == currentUserId && it.receiver == user.id) ||
                            (it.sender == user.id && it.receiver == currentUserId)

                } or requestsReceiver.any {
                    (it.sender == currentUserId && it.receiver == user.id) ||
                            (it.sender == user.id && it.receiver == currentUserId)
                }

                withContext(Dispatchers.Main) {
                    if (requestExists) {
                        holder.buttonAdd.setImageResource(R.drawable.waiting)
                        holder.buttonAdd.background = null
                        holder.buttonAdd.setOnClickListener {
                            Toast.makeText(
                                holder.itemView.context,
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
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Обработка ошибок сети или сервера
                    Toast.makeText(holder.itemView.context, "Ошибка при проверке запросов", Toast.LENGTH_SHORT).show()
                }
            }
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

    // Метод для загрузки пользователей через Retrofit
    fun loadUsers(holder: UserViewHolder) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                users = apiService.getAllUsers() // Получаем всех пользователей с сервера
                withContext(Dispatchers.Main) {
                    notifyDataSetChanged() // Обновляем адаптер на главном потоке
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Обработка ошибок сети или сервера
                    Toast.makeText(holder.itemView.context, "Ошибка при загрузке пользователей", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}