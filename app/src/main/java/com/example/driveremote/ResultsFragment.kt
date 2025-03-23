package com.example.driveremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentResultsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultsFragment : Fragment() {
    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем переданный результат из TestFragment
        val score = arguments?.getInt("totalScore") ?: 0

        // Форматируем дату и время (год, месяц, день, часы, минуты, секунды)
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Сохраняем дату и время последнего теста в SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("TestResults", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("lastTestTime", currentTime).apply()

        // Устанавливаем текст в элементы
        binding.textTime.text = "Дата и время завершения: $currentTime"
        binding.textScore.text = "Вы набрали $score баллов"

        // Обработчик кнопки "Вернуться в меню"
        binding.buttonBackToMenu.setOnClickListener {
            findNavController().navigate(R.id.action_resultsFragment_to_mainMenuFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}