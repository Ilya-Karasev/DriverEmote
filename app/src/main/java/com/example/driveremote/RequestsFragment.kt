package com.example.driveremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.adapters.RequestAdapter
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentRequestsBinding
import com.example.driveremote.models.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RequestsFragment : Fragment() {
    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    private lateinit var Radapter: RequestAdapter
    private lateinit var currentUserPost: Post
    private var userId: Int = -1
    private var isIncomingSelected = true

    private val apiService = RetrofitClient.api

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userId = requireActivity()
            .getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            .getInt("userId", -1)

        Radapter = RequestAdapter(emptyList(), requireContext(), { refreshRequests() }, userId, apiService)

        binding.recyclerViewRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewRequests.adapter = Radapter

        lifecycleScope.launch {
            try {
                val currentUser = withContext(Dispatchers.IO) {
                    apiService.getUserById(userId)
                }
                currentUserPost = currentUser.post
                refreshRequests()
            } catch (e: Exception) {
                showToast("Ошибка при загрузке пользователя")
            }
        }

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

        binding.viewMenu.setOnClickListener {
            val destination = if (this::currentUserPost.isInitialized && currentUserPost == Post.РУКОВОДИТЕЛЬ)
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

        updateButtonStates()
    }

    private fun refreshRequests() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewRequests.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val requests = withContext(Dispatchers.IO) {
                    if (isIncomingSelected)
                        apiService.getRequestsByReceiver(userId)
                    else
                        apiService.getRequestsBySender(userId)
                }

                val userIds = requests.map {
                    if (isIncomingSelected) it.sender else it.receiver
                }.distinct()

                val users = withContext(Dispatchers.IO) {
                    userIds.mapNotNull { id ->
                        try {
                            apiService.getUserById(id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                val pairs = requests.mapNotNull { request ->
                    val relatedUserId = if (isIncomingSelected) request.sender else request.receiver
                    val user = users.find { it.id == relatedUserId }
                    user?.let { it to request }
                }

                Radapter.updateRequests(pairs)
            } catch (e: Exception) {
                showToast("Ошибка при загрузке запросов")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.recyclerViewRequests.visibility = View.VISIBLE
            }
        }
    }

    private fun updateButtonStates() {
        val context = requireContext()
        val darkGreen = ContextCompat.getColor(context, R.color.dark_green)
        val white = ContextCompat.getColor(context, R.color.white)

        binding.buttonIncoming.apply {
            isEnabled = !isIncomingSelected
            setBackgroundColor(if (isIncomingSelected) darkGreen else white)
            setTextColor(if (isIncomingSelected) white else darkGreen)
        }

        binding.buttonOutcoming.apply {
            isEnabled = isIncomingSelected
            setBackgroundColor(if (!isIncomingSelected) darkGreen else white)
            setTextColor(if (!isIncomingSelected) white else darkGreen)
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}