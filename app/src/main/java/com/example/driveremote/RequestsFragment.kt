package com.example.driveremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.adapters.RequestAdapter
import com.example.driveremote.databinding.FragmentRequestsBinding
import com.example.driveremote.models.AppDatabase
import kotlinx.coroutines.launch

class RequestsFragment : Fragment() {
    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    private lateinit var adapter: RequestAdapter

    // Переменные класса
    private lateinit var requestDao: com.example.driveremote.models.RequestDao
    private lateinit var userDao: com.example.driveremote.models.UserDao
    private var userId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val db = AppDatabase.getDatabase(requireContext())
        requestDao = db.requestDao()
        userDao = db.userDao()

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("userId", -1)

        adapter = RequestAdapter(emptyList(), requireContext()) {
            refreshRequests()
        }
        binding.recyclerViewRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewRequests.adapter = adapter

        refreshRequests()

        binding.textExit.setOnClickListener {
            findNavController().navigate(R.id.action_requestsFragment_to_profileFragment)
        }

        binding.iconLeft.setOnClickListener {
            requireActivity().finish()
        }

        binding.iconRight.setOnClickListener {
            sharedPreferences.edit().clear().apply()
            findNavController().navigate(R.id.action_requestsFragment_to_signInFragment)
        }
    }

    private fun refreshRequests() {
        lifecycleScope.launch {
            val requests = requestDao.getAllRequests()
            val users = userDao.getAllUsers()
            val filtered = requests.filter { it.receiverId == userId }
                .mapNotNull { request ->
                    val sender = users.find { it.id == request.senderId }
                    sender?.let { Pair(it, request) }
                }
            adapter.updateRequests(filtered)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}