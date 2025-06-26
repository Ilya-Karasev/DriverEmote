package com.example.driveremote.fragments
import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.R
import com.example.driveremote.api.Constants
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentResultsBinding
import com.example.driveremote.models.Driver
import com.example.driveremote.models.Results
import com.example.driveremote.sessionManagers.DriverSession
import com.example.driveremote.sessionManagers.ResultsSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
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
        val totalScore = arguments?.getInt("totalScore") ?: 0
        val emotionalExhaustionScore = arguments?.getInt("emotionalExhaustionScore") ?: 0
        val depersonalizationScore = arguments?.getInt("depersonalizationScore") ?: 0
        val personalAchievementScore = arguments?.getInt("personalAchievementScore") ?: 0
        val currentTime = SimpleDateFormat("dd.MM.yyyy — HH:mm", Locale.getDefault()).format(Date())
        val sharedPreferences =
            requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)
        binding.textTime.text = "Дата и время завершения:\n$currentTime"
        binding.textScore.text = "Вы набрали $totalScore баллов"
        binding.textEmotionalExhaustionScore.text = emotionalExhaustionScore.toString()
        binding.textDepersonalizationScore.text = depersonalizationScore.toString()
        binding.textPersonalAchievementScore.text = personalAchievementScore.toString()
        binding.iconEmotionalExhaustion.setOnClickListener {
            showTooltip(Constants.EmotionalExhaustion)
        }
        binding.iconDepersonalization.setOnClickListener {
            showTooltip(Constants.Depersonalization)
        }
        binding.iconPersonalAchievement.setOnClickListener {
            showTooltip(Constants.PersonalAchievement)
        }
        binding.buttonBackToMenu.setOnClickListener {
            if (userId != -1) {
                lifecycleScope.launch {
                    try {
                        val driver = RetrofitClient.api.getDriverByUserId(userId)
                        if (driver != null) {
                            val updatedDriver = driver.copy(isCompleted = true)
                            RetrofitClient.api.updateDriver(driver.id, updatedDriver)
                            val result = Results(
                                userId = userId,
                                testDate = currentTime,
                                emotionalExhaustionScore = emotionalExhaustionScore,
                                depersonalizationScore = depersonalizationScore,
                                personalAchievementScore = personalAchievementScore,
                                totalScore = totalScore
                            )
                            RetrofitClient.api.addResult(result)
                            delay(100)
                            val allResults = RetrofitClient.api.getResultsByUser(userId)
                            val maxScore = 132
                            val averageScore = allResults.map { it.totalScore }.average()
                            val testsCount = allResults.size
                            val newStatus = when {
                                testsCount >= 7 && averageScore > maxScore * 0.75 -> "Критическое"
                                testsCount >= 1 && averageScore > maxScore * 0.5 -> "Внимание"
                                else -> "Норма"
                            }
                            val updatedDriverWithStatus = updatedDriver.copy(status = newStatus)
                            RetrofitClient.api.updateDriver(driver.id, updatedDriverWithStatus)
                            DriverSession.saveDriver(requireContext(), updatedDriverWithStatus)
                            ResultsSession.saveResults(requireContext(), allResults)
                            setFragmentResult("requestKey", bundleOf("refresh" to true))
                            Toast.makeText(
                                requireContext(),
                                "Результаты успешно сохранены",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val localResults =
                            ResultsSession.loadResults(requireContext()).toMutableList()
                        val localResult = Results(
                            id = 0,
                            userId = userId,
                            testDate = currentTime,
                            emotionalExhaustionScore = emotionalExhaustionScore,
                            depersonalizationScore = depersonalizationScore,
                            personalAchievementScore = personalAchievementScore,
                            totalScore = totalScore
                        )
                        localResults.add(localResult)
                        ResultsSession.saveResults(requireContext(), localResults)
                        val savedDriver = DriverSession.loadDriver(requireContext())
                        val updatedDriver = savedDriver?.copy(isCompleted = true) ?: Driver(
                            id = -1,
                            isCompleted = true,
                            quantity = 1,
                            testingTime = null,
                            status = "Неизвестно"
                        )
                        DriverSession.saveDriver(requireContext(), updatedDriver)
                        Toast.makeText(
                            requireContext(),
                            "Нет подключения. Результаты сохранены локально",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    findNavController().navigate(R.id.action_resultsFragment_to_mainMenuFragment)
                }
            } else {
                findNavController().navigate(R.id.action_resultsFragment_to_mainMenuFragment)
            }
        }
    }
    private fun showTooltip(message: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.tooltip_layout)
        val tooltipText = dialog.findViewById<TextView>(R.id.tooltip_text)
        val spannableMessage = SpannableString(message)
        val termsToHighlight = listOf(
            "Эмоциональное истощение",
            "Деперсонализация",
            "Редукция профессиональных достижений"
        )
        for (term in termsToHighlight) {
            val startIndex = message.indexOf(term)
            if (startIndex != -1) {
                spannableMessage.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex,
                    startIndex + term.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        tooltipText.text = spannableMessage
        dialog.show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}