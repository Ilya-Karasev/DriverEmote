package com.example.driveremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentEnterBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Results
import com.example.driveremote.models.TestUsers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EnterFragment : Fragment() {
    private var _binding: FragmentEnterBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val userDao = db.userDao()
        val resultsDao = db.resultsDao()

        binding.imageEnterIcon.setOnClickListener {
            val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val userEmail = sharedPreferences.getString("email", null)

            if (userEmail != null) {
                findNavController().navigate(R.id.action_enterFragment_to_mainMenuFragment)
            } else {
                findNavController().navigate(R.id.action_enterFragment_to_signInFragment)
            }
        }

        lifecycleScope.launch {
            TestUsers.insertTestUsers(requireContext(), userDao)

            // Получаем первого пользователя (например, для создания теста)
            val user = userDao.getAllUsers().firstOrNull()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}