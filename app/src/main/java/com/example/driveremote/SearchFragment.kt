package com.example.driveremote

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.adapters.UserAdapter
import com.example.driveremote.databinding.FragmentSearchBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Post
import com.example.driveremote.models.Request
import com.example.driveremote.models.User
import kotlinx.coroutines.launch
import java.util.Locale

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    private lateinit var adapter: UserAdapter
    private var allUsers: List<User> = emptyList()
    private var currentUserId: Int = -1
    private lateinit var currentUserPost: Post
    private var allRequests: List<Request> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val db = AppDatabase.getDatabase(requireContext())
        val userDao = db.userDao()
        val requestDao = db.requestDao()
        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPreferences.getInt("userId", -1)

        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            val currentUser = userDao.getUserById(currentUserId)
            if (currentUser != null) {
                currentUserPost = currentUser.post
                // Получаем список всех пользователей, кроме текущего
                allUsers = userDao.getAllUsers().filter { it.id != currentUserId }
                // Получаем все Request из базы
                allRequests = requestDao.getAllRequests()
                setupAdapter()

                if (currentUserPost == Post.ВОДИТЕЛЬ) {
                    binding.view1Driver.visibility = View.VISIBLE
                    binding.view1Manager.visibility = View.GONE
                    binding.view1Driver.setOnClickListener {
                        findNavController().navigate(R.id.action_searchFragment_to_mainMenuFragment)
                    }
                } else if (currentUserPost == Post.РУКОВОДИТЕЛЬ) {
                    binding.view1Manager.visibility = View.VISIBLE
                    binding.view1Driver.visibility = View.GONE
                    binding.view1Manager.setOnClickListener {
                        findNavController().navigate(R.id.action_searchFragment_to_managerMenuFragment)
                    }
                }
            }
        }

        binding.editTextSearch.addTextChangedListener { text ->
            val query = text.toString().lowercase(Locale.getDefault())
            val filtered = allUsers.filter {
                "${it.surName} ${it.firstName} ${it.fatherName}".lowercase(Locale.getDefault()).contains(query)
                        || it.email.lowercase(Locale.getDefault()).contains(query)
            }
            adapter.filterList(filtered)
        }

        binding.iconLeft.setOnClickListener {
            requireActivity().finish()
        }

        binding.iconRight.setOnClickListener {
            sharedPreferences.edit().clear().apply()
            findNavController().navigate(R.id.action_searchFragment_to_signInFragment)
        }

        binding.view3.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_profileFragment)
        }
    }

    private fun setupAdapter() {
        lifecycleScope.launch {
            val employeesList = getEmployeesList()

            adapter = UserAdapter(
                users = allUsers,
                currentUserId = currentUserId,
                currentUserPost = currentUserPost,
                requests = allRequests,
                employeesList = employeesList
            ) { selectedUser ->
                val newRequest = Request(senderId = currentUserId, receiverId = selectedUser.id)
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).requestDao().insertRequest(newRequest)
                    allRequests = AppDatabase.getDatabase(requireContext()).requestDao().getAllRequests()
                    setupAdapter()
                    Toast.makeText(requireContext(), "Запрос отправлен", Toast.LENGTH_SHORT).show()
                }
            }

            binding.recyclerViewUsers.adapter = adapter
        }
    }

    private suspend fun getEmployeesList(): List<Int> {
        return if (currentUserPost == Post.РУКОВОДИТЕЛЬ) {
            val managerDao = AppDatabase.getDatabase(requireContext()).managerDao()
            val manager = managerDao.getManagerById(currentUserId)
            manager?.employeesList ?: emptyList()
        } else {
            emptyList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}