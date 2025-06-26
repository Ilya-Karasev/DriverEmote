package com.example.driveremote.fragments
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.R
import com.example.driveremote.adapters.EmployeeAdapter
import com.example.driveremote.api.ApiService
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentManagerMenuBinding
import com.example.driveremote.models.Post
import com.example.driveremote.models.User
import kotlinx.coroutines.launch
class ManagerMenuFragment : Fragment() {
    private var _binding: FragmentManagerMenuBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")
    private lateinit var employeeAdapter: EmployeeAdapter
    private lateinit var apiService: ApiService
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerMenuBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        apiService = RetrofitClient.api
        employeeAdapter = EmployeeAdapter(emptyList(), requireContext()) { employee ->
            val bundle = Bundle().apply {
                putString("fullName", "${employee.surName} ${employee.firstName} ${employee.fatherName}")
                putInt("age", employee.age)
                putString("email", employee.email)
                putString("post", employee.post.toString())
                putInt("userId", employee.id)
            }
            findNavController().navigate(R.id.action_managerMenuFragment_to_employeeFragment, bundle)
        }
        binding.recyclerViewSubordinates.adapter = employeeAdapter
        binding.recyclerViewSubordinates.layoutManager = LinearLayoutManager(requireContext())
        val sharedPrefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getInt("userId", -1)
        setLoadingState(true)
        if (userId != -1) {
            lifecycleScope.launch {
                try {
                    val currentUser = apiService.getUserById(userId)
                    binding.driverName.text = "${currentUser.surName} ${currentUser.firstName} ${currentUser.fatherName}"
                    binding.driverAge.text = "${currentUser.age} год(а)/лет"
                    binding.driverEmail.text = currentUser.email
                    if (currentUser.post == Post.РУКОВОДИТЕЛЬ) {
                        val currentManager = apiService.getManagerById(userId)
                        if (currentManager != null) {
                            val subordinates = getSubordinatesFromIds(currentManager.employeesList)
                            employeeAdapter.updateList(subordinates)
                        }
                    }
                    setLoadingState(false)
                } catch (e: Exception) {
                    Log.e("ManagerMenuFragment", "Ошибка получения данных: ${e.message}")
                    setLoadingState(false)
                }
            }
        } else {
            setLoadingState(false)
        }
        binding.viewSearch.setOnClickListener {
            findNavController().navigate(R.id.action_managerMenuFragment_to_searchFragment)
        }
        binding.viewRequests.setOnClickListener {
            findNavController().navigate(R.id.action_managerMenuFragment_to_requestsFragment)
        }
        binding.logoutButton.setOnClickListener {
            sharedPrefs.edit().clear().apply()
            findNavController().navigate(R.id.action_managerMenuFragment_to_signInFragment)
        }
    }
    private fun setLoadingState(isLoading: Boolean) {
        binding.userInfoProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.driverName.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.driverAge.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.driverEmail.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewSubordinates.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.textSubordinates.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
    private suspend fun getSubordinatesFromIds(employeeIds: List<Int>): List<User> {
        val subordinates = mutableListOf<User>()
        for (employeeId in employeeIds) {
            try {
                val user = apiService.getUserById(employeeId)
                subordinates.add(user)
            } catch (e: Exception) {
                Log.e("ManagerMenuFragment", "Ошибка получения подчинённого с id $employeeId: ${e.message}")
            }
        }
        return subordinates
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}