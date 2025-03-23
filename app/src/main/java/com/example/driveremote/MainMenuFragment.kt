package com.example.driveremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentMainMenuBinding

class MainMenuFragment : Fragment() {
    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Обработчик нажатия на левую иконку (закрытие приложения)
        binding.iconLeft.setOnClickListener {
            requireActivity().finish()
        }

        // Обработчик нажатия на правую иконку (выход из аккаунта)
        binding.iconRight.setOnClickListener {
            val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply() // Очистка данных пользователя

            findNavController().navigate(R.id.action_mainMenuFragment_to_signInFragment) // Переход на экран входа
        }

        binding.buttonTest.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.mainMenuFragment) {
                navController.navigate(R.id.action_mainMenuFragment_to_testFragment)
            }
        }

        // Загружаем дату и время последнего прохождения теста
        val sharedPreferences = requireActivity().getSharedPreferences("TestResults", Context.MODE_PRIVATE)
        val lastTestTime = sharedPreferences.getString("lastTestTime", null)

        // Устанавливаем текст
        binding.textTest.text = if (lastTestTime == null) {
            "Вы ещё не проходили тестирование"
        } else {
            "Последний раз вы проходили тестирование $lastTestTime"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}