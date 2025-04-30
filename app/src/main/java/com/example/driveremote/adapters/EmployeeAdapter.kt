package com.example.driveremote.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.R
import com.example.driveremote.models.User
import kotlinx.coroutines.launch

class EmployeeAdapter(
    private var employees: List<User>,
    private val onEmployeeClick: (User) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

    inner class EmployeeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView = view.findViewById<TextView>(R.id.textName)
        val ageTextView = view.findViewById<TextView>(R.id.textAge)
        val emailTextView = view.findViewById<TextView>(R.id.textEmail)
        val iconInitials: TextView = view.findViewById(R.id.iconRole)

        fun bind(employee: User) {
            nameTextView.text = "${employee.surName} ${employee.firstName} ${employee.fatherName}"
            ageTextView.text = "${employee.age} год(а)/лет"
            emailTextView.text = employee.email
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_employee, parent, false)
        return EmployeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val employee = employees[position]
        holder.bind(employee)
        holder.itemView.setOnClickListener {
            onEmployeeClick(employee)
        }

        // Формируем инициалы
        val initials = "${employee.surName.firstOrNull() ?: ""}${employee.firstName.firstOrNull() ?: ""}".uppercase()

        // Генерируем насыщенный цвет для текста
        val textColor = generateRandomColor(saturation = 0.8f, brightness = 0.9f)
        // Генерируем бледный цвет для фона
        val backgroundColor = adjustAlpha(textColor, 0.15f)

        holder.iconInitials.text = initials
        holder.iconInitials.setBackgroundColor(backgroundColor)
        holder.iconInitials.setTextColor(textColor)
        holder.iconInitials.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    override fun getItemCount(): Int = employees.size

    fun updateList(newEmployees: List<User>) {
        employees = newEmployees
        notifyDataSetChanged()
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