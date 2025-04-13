package com.example.driveremote.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.R
import com.example.driveremote.models.User

class EmployeeAdapter(
    private var employees: List<User>,
    private val onEmployeeClick: (User) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

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

    class EmployeeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameTextView = view.findViewById<TextView>(R.id.textName)
        private val ageTextView = view.findViewById<TextView>(R.id.textAge)
        private val emailTextView = view.findViewById<TextView>(R.id.textEmail)

        fun bind(employee: User) {
            nameTextView.text = "${employee.surName} ${employee.firstName} ${employee.fatherName}"
            ageTextView.text = "${employee.age} год(а) / лет"
            emailTextView.text = employee.email
        }
    }
}