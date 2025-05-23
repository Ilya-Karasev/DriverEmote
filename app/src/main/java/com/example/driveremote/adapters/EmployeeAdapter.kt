package com.example.driveremote.adapters
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.R
import com.example.driveremote.api.Constants
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class EmployeeAdapter(
    private var employees: List<User>,
    private val context: Context,
    private val onEmployeeClick: (User) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {
    private val apiService = RetrofitClient.api
    inner class EmployeeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView = view.findViewById<TextView>(R.id.textName)
        val ageTextView = view.findViewById<TextView>(R.id.textAge)
        val emailTextView = view.findViewById<TextView>(R.id.textEmail)
        val iconInitials: TextView = view.findViewById(R.id.iconRole)
        val employeeStatus: TextView = view.findViewById(R.id.driver_status)
        fun bind(employee: User) {
            nameTextView.text = "${employee.surName} ${employee.firstName} ${employee.fatherName}"
            ageTextView.text = "${employee.age} год(а)/лет"
            emailTextView.text = employee.email
            val initials = "${employee.surName.firstOrNull() ?: ""}${employee.firstName.firstOrNull() ?: ""}".uppercase()
            val textColor = generateRandomColor(saturation = 0.8f, brightness = 0.9f)
            val backgroundColor = adjustAlpha(textColor, 0.15f)
            iconInitials.text = initials
            iconInitials.setBackgroundColor(backgroundColor)
            iconInitials.setTextColor(textColor)
            iconInitials.textAlignment = View.TEXT_ALIGNMENT_CENTER
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val driver = withContext(Dispatchers.IO) { apiService.getDriverById(employee.id) }
                    employeeStatus.text = driver.status
                    when (driver.status) {
                        "Норма" -> employeeStatus.setTextColor(Color.parseColor(Constants.STATUS_NORMAL))
                        "Внимание" -> employeeStatus.setTextColor(Color.parseColor(Constants.STATUS_WARNING))
                        "Критическое" -> employeeStatus.setTextColor(Color.parseColor(Constants.STATUS_CRITICAL))
                        else -> employeeStatus.setTextColor(Color.DKGRAY)
                    }
                } catch (e: Exception) {
                    employeeStatus.text = "Ошибка"
                    employeeStatus.setTextColor(Color.RED)
                }
            }
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