package com.example.driveremote
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
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
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentSearchBinding
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
    private var employeesList: List<Int> = emptyList()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext())
        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPreferences.getInt("userId", -1)
        loadInitialData()
        binding.editTextSearch.addTextChangedListener { text ->
            val query = text.toString().lowercase(Locale.getDefault())
            val filtered = allUsers.filter {
                "${it.surName} ${it.firstName} ${it.fatherName}".lowercase(Locale.getDefault()).contains(query)
                        || it.email.lowercase(Locale.getDefault()).contains(query)
            }
            adapter.filterList(filtered)
        }
        binding.viewMenu.setOnClickListener {
            if (currentUserPost == Post.ВОДИТЕЛЬ) {
                findNavController().navigate(R.id.action_searchFragment_to_mainMenuFragment)
            } else if (currentUserPost == Post.РУКОВОДИТЕЛЬ) {
                findNavController().navigate(R.id.action_searchFragment_to_managerMenuFragment)
            }
        }
        binding.viewRequests.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_requestsFragment)
        }
        binding.settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_settingsFragment)
        }
    }
    private fun loadInitialData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewUsers.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.api
                val currentUser = api.getUserById(currentUserId)
                currentUserPost = currentUser.post
                val prefs = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                prefs.edit().putString("post", currentUserPost.name).apply()
                if (currentUserPost == Post.ВОДИТЕЛЬ) {
                    binding.settingsIcon.visibility = View.VISIBLE
                    binding.settingsIcon.isClickable = true
                } else {
                    binding.settingsIcon.visibility = View.GONE
                    binding.settingsIcon.isClickable = false
                }
                allUsers = api.getAllUsers().filter { it.id != currentUserId }
                employeesList = if (currentUserPost == Post.РУКОВОДИТЕЛЬ) {
                    api.getManagerById(currentUserId)?.employeesList ?: emptyList()
                } else {
                    emptyList()
                }
                setupAdapter()
            } catch (e: Exception) {
                Log.e("SearchFragment", "Ошибка загрузки данных", e)
                Toast.makeText(requireContext(), "Ошибка загрузки данных: ${e.message}", Toast.LENGTH_SHORT).show()
                val prefs = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                val savedPost = prefs.getString("post", null)
                currentUserPost = savedPost?.let { Post.valueOf(it) } ?: Post.ВОДИТЕЛЬ
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.recyclerViewUsers.visibility = View.VISIBLE
            }
        }
    }
    private fun setupAdapter() {
        adapter = UserAdapter(
            apiService = RetrofitClient.api,
            currentUserId = currentUserId,
            currentUserPost = currentUserPost,
            employeesList = employeesList
        ) { selectedUser ->
            lifecycleScope.launch {
                try {
                    val request = Request(sender = currentUserId, receiver = selectedUser.id)
                    RetrofitClient.api.createRequest(request)
                    Toast.makeText(requireContext(), "Запрос отправлен", Toast.LENGTH_SHORT).show()
                    loadInitialData()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка при отправке запроса", Toast.LENGTH_SHORT).show()
                }
            }
        }
        adapter.filterList(allUsers)
        binding.recyclerViewUsers.adapter = adapter
    }
    override fun onDestroyView() {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onDestroyView()
        _binding = null
    }
}