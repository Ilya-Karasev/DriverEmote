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
import com.example.driveremote.models.Post
import com.example.driveremote.models.TestUsers
import kotlinx.coroutines.launch

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
        val managerDao = db.managerDao() // Получаем managerDao

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        binding.imageEnterIcon.setOnClickListener {
            val userId = sharedPreferences.getInt("userId", -1)

            lifecycleScope.launch {
                val user = userDao.getUserById(userId)
                if (user != null) {
                    if (user.post == Post.РУКОВОДИТЕЛЬ) {
                        findNavController().navigate(R.id.action_enterFragment_to_managerMenuFragment)
                    } else if (user.post == Post.ВОДИТЕЛЬ) {
                        findNavController().navigate(R.id.action_enterFragment_to_mainMenuFragment)
                    }
                } else {
                    sharedPreferences.edit().clear().apply()
                    findNavController().navigate(R.id.action_enterFragment_to_signInFragment)
                }
            }
        }

        lifecycleScope.launch {
            val driverDao = db.driverDao() // Получаем driverDao
            TestUsers.insertTestUsers(requireContext(), userDao, driverDao, managerDao)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}