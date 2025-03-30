package com.example.driveremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        val surname = sharedPreferences.getString("surName", "Неизвестно") ?: "Неизвестно"
        val firstName = sharedPreferences.getString("firstName", "Неизвестно") ?: "Неизвестно"
        val fatherName = sharedPreferences.getString("fatherName", "Неизвестно") ?: "Неизвестно"
        val age = sharedPreferences.getInt("age", 0)
        val email = sharedPreferences.getString("email", "Не указан") ?: "Не указан"
        val post = sharedPreferences.getString("post", "Неизвестно") ?: "Неизвестно"

        // Установка ФИО над иконкой
        binding.profileFullName.text = "$surname $firstName $fatherName"

        // Установка иконки в зависимости от должности
        val iconResId = if (post == "РУКОВОДИТЕЛЬ") R.drawable.manager else R.drawable.driver
        binding.profileIcon.setImageResource(iconResId)

        // Установка дополнительной информации
        binding.profileInfo.text = """
            Возраст: $age год(а) / лет
            Эл. почта: $email
        """.trimIndent()

        binding.iconLeft.setOnClickListener {
            requireActivity().finish()
        }

        binding.iconRight.setOnClickListener {
            sharedPreferences.edit().clear().apply()
            findNavController().navigate(R.id.action_profileFragment_to_signInFragment)
        }

        binding.view1.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_mainMenuFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}