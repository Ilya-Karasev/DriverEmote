package com.example.driveremote

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.adapters.EmployeeAdapter
import com.example.driveremote.databinding.FragmentManagerMenuBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Post
import kotlinx.coroutines.launch

class ManagerMenuFragment : Fragment() {
    private var _binding: FragmentManagerMenuBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    private lateinit var db: AppDatabase
    private lateinit var employeeAdapter: EmployeeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        db = AppDatabase.getDatabase(requireContext())
        val userDao = db.userDao()
        val managerDao = db.managerDao()

        // Initializing the adapter with an empty list
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

        // Retrieve the current user's id and post from SharedPreferences
        val sharedPrefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getInt("userId", -1)

        if (userId != -1) {
            lifecycleScope.launch {
                val currentUser = userDao.getUserById(userId)
                val currentManager = managerDao.getManagerById(userId)

                if (currentUser != null) {
                    binding.driverName.text = "${currentUser.surName} ${currentUser.firstName} ${currentUser.fatherName}"
                    binding.driverAge.text = "${currentUser.age} год(а)/лет"
                    binding.driverEmail.text = currentUser.email
                }

                if (currentUser?.post == Post.РУКОВОДИТЕЛЬ) {
                    val subordinates = currentManager?.let { managerDao.getUsersByIds(it.employeesList) }
                    if (subordinates != null) {
                        employeeAdapter.updateList(subordinates)
                    }
                }
            }
        }

        binding.viewSearch.setOnClickListener {
            findNavController().navigate(R.id.action_managerMenuFragment_to_searchFragment)
        }

        binding.viewRequests.setOnClickListener {
            findNavController().navigate(R.id.action_managerMenuFragment_to_requestsFragment)
        }

        binding.logoutButton.setOnClickListener {
            sharedPreferences.edit().clear().apply()
            findNavController().navigate(R.id.action_managerMenuFragment_to_signInFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}