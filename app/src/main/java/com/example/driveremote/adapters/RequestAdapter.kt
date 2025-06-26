package com.example.driveremote.adapters
import android.content.Context
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
import com.example.driveremote.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class RequestAdapter(
    private var requestList: List<Pair<User, Request>>,
    private val context: Context,
    private val onDataChanged: () -> Unit,
    private val currentUserId: Int,
    private val apiService: ApiService
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {
    inner class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconInitials: TextView = view.findViewById(R.id.iconRole)
        val name: TextView = view.findViewById(R.id.textName)
        val yesButton: ImageView = view.findViewById(R.id.buttonYes)
        val noButton: ImageView = view.findViewById(R.id.buttonNo)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }
    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val (user, request) = requestList[position]
        val context = holder.itemView.context
        val initials = "${user.surName.firstOrNull() ?: ""}${user.firstName.firstOrNull() ?: ""}".uppercase()
        val textColor = generateRandomColor(0.8f, 0.9f)
        val backgroundColor = adjustAlpha(textColor, 0.15f)
        holder.iconInitials.text = initials
        holder.iconInitials.setBackgroundColor(backgroundColor)
        holder.iconInitials.setTextColor(textColor)
        holder.iconInitials.textAlignment = View.TEXT_ALIGNMENT_CENTER
        holder.name.text = "${user.surName} ${user.firstName} ${user.fatherName}"
        if (request.sender == currentUserId) {
            holder.yesButton.visibility = View.GONE
        } else {
            holder.yesButton.visibility = View.VISIBLE
        }
        holder.noButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    apiService.deleteRequest(request.id)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Вы удалили / отклонили запрос", Toast.LENGTH_SHORT).show()
                        reloadRequests()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при удалении запроса", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        holder.yesButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val sender = apiService.getUserById(request.sender)
                    val receiver = apiService.getUserById(request.receiver)
                    if (sender != null && receiver != null) {
                        val (managerUser, subordinateId) = when {
                            sender.post.name == "РУКОВОДИТЕЛЬ" -> sender to receiver.id
                            receiver.post.name == "РУКОВОДИТЕЛЬ" -> receiver to sender.id
                            else -> null to -1
                        }
                        managerUser?.let { manager ->
                            val existingManager = apiService.getManagerById(manager.id)
                            val updatedList = (existingManager?.employeesList ?: emptyList()).toMutableList()
                            if (!updatedList.contains(subordinateId)) {
                                updatedList.add(subordinateId)
                            }
                            apiService.updateEmployeesList(manager.id, updatedList)
                            apiService.deleteRequest(request.id)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Вы приняли запрос", Toast.LENGTH_SHORT).show()
                                reloadRequests()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при обработке запроса", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = requestList.size
    fun updateRequests(newList: List<Pair<User, Request>>) {
        requestList = newList
        notifyDataSetChanged()
    }
    private fun reloadRequests() {
        CoroutineScope(Dispatchers.Main).launch {
            onDataChanged()
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
}