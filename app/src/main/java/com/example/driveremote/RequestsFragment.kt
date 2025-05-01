package com.example.driveremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.adapters.RequestAdapter
import com.example.driveremote.adapters.UserAdapter
import com.example.driveremote.databinding.FragmentRequestsBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Post
import com.example.driveremote.models.Request
import com.example.driveremote.models.RequestDao
import com.example.driveremote.models.User
import com.example.driveremote.models.UserDao
import kotlinx.coroutines.launch

class RequestsFragment : Fragment() {
    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!

    private lateinit var Radapter: RequestAdapter
    private lateinit var currentUserPost: Post
    private var userId: Int = -1

    private lateinit var requestDao: RequestDao
    private lateinit var userDao: UserDao

    private var isIncomingSelected = true // состояние фильтра

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val db = AppDatabase.getDatabase(requireContext())
        userDao = db.userDao()
        requestDao = db.requestDao()

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("userId", -1)

        Radapter = RequestAdapter(emptyList(), requireContext(), { refreshRequests() }, userId)

        binding.recyclerViewRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewRequests.adapter = Radapter

        lifecycleScope.launch {
            val currentUser = userDao.getUserById(userId)
            if (currentUser != null) {
                currentUserPost = currentUser.post
                refreshRequests()
            }
        }

        // Переключение кнопок
        binding.buttonIncoming.setOnClickListener {
            isIncomingSelected = true
            updateButtonStates()
            refreshRequests()
        }

        binding.buttonOutcoming.setOnClickListener {
            isIncomingSelected = false
            updateButtonStates()
            refreshRequests()
        }

        updateButtonStates()

        // Навигация
        binding.viewMenu.setOnClickListener {
            val destination = if (currentUserPost == Post.РУКОВОДИТЕЛЬ)
                R.id.action_requestsFragment_to_managerMenuFragment
            else R.id.action_requestsFragment_to_mainMenuFragment
            findNavController().navigate(destination)
        }

        binding.viewSearch.setOnClickListener {
            findNavController().navigate(R.id.action_requestsFragment_to_searchFragment)
        }

        binding.settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_settingsFragment)
        }
    }

    private fun refreshRequests() {
        lifecycleScope.launch {
            val allRequests = requestDao.getAllRequests()
            val allUsers = userDao.getAllUsers()

            val filtered = allRequests.filter {
                if (isIncomingSelected) it.receiverId == userId else it.senderId == userId
            }.mapNotNull { request ->
                val otherUserId = if (isIncomingSelected) request.senderId else request.receiverId
                val user = allUsers.find { it.id == otherUserId }
                user?.let { Pair(it, request) }
            }

            Radapter.updateRequests(filtered)
        }
    }

    private fun updateButtonStates() {
        if (isIncomingSelected) {
            binding.buttonIncoming.isEnabled = false
            binding.buttonIncoming.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_green))
            binding.buttonIncoming.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            binding.buttonOutcoming.isEnabled = true
            binding.buttonOutcoming.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.buttonOutcoming.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green))
        } else {
            binding.buttonIncoming.isEnabled = true
            binding.buttonIncoming.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.buttonIncoming.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green))

            binding.buttonOutcoming.isEnabled = false
            binding.buttonOutcoming.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_green))
            binding.buttonOutcoming.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
