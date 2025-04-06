package com.example.driveremote

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.adapters.UserAdapter
import com.example.driveremote.databinding.FragmentSearchBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.User
import kotlinx.coroutines.launch
import java.util.Locale

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")
    private lateinit var adapter: UserAdapter
    private var allUsers: List<User> = emptyList()

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

        adapter = UserAdapter(emptyList())
        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewUsers.adapter = adapter

        // Инициализация остальной части экрана
        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        // Загружаем пользователей
        lifecycleScope.launch {
            allUsers = userDao.getAllUsers()
            adapter.filterList(allUsers)
        }

        // Поиск по тексту
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
            findNavController().navigate(R.id.action_mainMenuFragment_to_signInFragment)
        }

        binding.view1.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_mainMenuFragment)
        }

        binding.view2.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_searchFragment)
        }

        binding.view3.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_profileFragment)
        }
    }
}