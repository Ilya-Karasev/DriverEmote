package com.example.driveremote.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.R
import com.example.driveremote.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RequestAdapter(
    private var requestList: List<Pair<User, Request>>,
    private val context: Context,
    private val onDataChanged: () -> Unit // Колбэк для обновления фрагмента
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    private val db = AppDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val requestDao = db.requestDao()
    private val managerDao = db.managerDao()

    inner class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iconRole)
        val name: TextView = view.findViewById(R.id.textName)
        val yesButton: ImageView = view.findViewById(R.id.buttonYes)
        val noButton: ImageView = view.findViewById(R.id.buttonNo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val (senderUser, request) = requestList[position]

        holder.name.text = "${senderUser.surName}\n${senderUser.firstName}\n${senderUser.fatherName}"

        holder.icon.setImageResource(
            if (senderUser.post.name == "ВОДИТЕЛЬ") R.drawable.driver else R.drawable.manager
        )

        // ❌ Отклонение запроса
        holder.noButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                requestDao.deleteRequest(request)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Вы отклонили запрос", Toast.LENGTH_SHORT).show()
                    reloadRequests()
                }
            }
        }

        // ✅ Принятие запроса
        holder.yesButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val sender = userDao.getUserById(request.senderId)
                val receiver = userDao.getUserById(request.receiverId)

                if (sender != null && receiver != null) {
                    val (managerUser, subordinateId) = when {
                        sender.post.name == "РУКОВОДИТЕЛЬ" -> sender to receiver.id
                        receiver.post.name == "РУКОВОДИТЕЛЬ" -> receiver to sender.id
                        else -> null to -1
                    }

                    managerUser?.let { manager ->
                        val existingManager = managerDao.getManagerById(manager.id)
                        val updatedList = (existingManager?.employeesList ?: emptyList()).toMutableList()
                        if (!updatedList.contains(subordinateId)) {
                            updatedList.add(subordinateId)
                        }

                        managerDao.insertManager(
                            Manager(manager.id, updatedList)
                        )
                        requestDao.deleteRequest(request)

                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Вы приняли запрос", Toast.LENGTH_SHORT).show()
                            reloadRequests()
                        }
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
}