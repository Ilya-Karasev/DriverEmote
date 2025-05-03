package com.example.driveremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentEnterBinding
import com.example.driveremote.models.Post
import kotlinx.coroutines.launch

class EnterFragment : Fragment() {
    private var _binding: FragmentEnterBinding? = null
    private val binding get() = _binding ?: error("Binding is null")

    private val sharedPreferences by lazy {
        requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageEnterIcon.setOnClickListener {
            val userId = sharedPreferences.getInt("userId", -1)
            if (userId == -1) {
                findNavController().navigate(R.id.action_enterFragment_to_signInFragment)
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val user = RetrofitClient.api.getUserById(userId)
                    when (user.post) {
                        Post.РУКОВОДИТЕЛЬ -> findNavController().navigate(R.id.action_enterFragment_to_managerMenuFragment)
                        Post.ВОДИТЕЛЬ -> findNavController().navigate(R.id.action_enterFragment_to_mainMenuFragment)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    sharedPreferences.edit().clear().apply()
                    findNavController().navigate(R.id.action_enterFragment_to_signInFragment)
                }
            }
        }

        // ⚠️ Удалена вставка тестовых пользователей из локальной БД
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}