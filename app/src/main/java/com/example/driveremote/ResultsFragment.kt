package com.example.driveremote

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
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentResultsBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Results
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ResultsFragment : Fragment() {
    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

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

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        binding.textTime.text = "Дата и время завершения:\n$currentTime"
        binding.textScore.text = "Вы набрали $totalScore баллов"
        binding.textEmotionalExhaustionScore.text = emotionalExhaustionScore.toString()
        binding.textDepersonalizationScore.text = depersonalizationScore.toString()
        binding.textPersonalAchievementScore.text = personalAchievementScore.toString()

        binding.iconEmotionalExhaustion.setOnClickListener {
            showTooltip("Эмоциональное истощение проявляется в снижении эмоционального тонуса, повышенной психической истощаемости и аффективной лабильности, равнодушием, неспособностью испытывать сильные эмоции, как положительные, так и отрицательные, утраты интереса и позитивных чувств к окружающим, ощущении «пресыщенности» работой, неудовлетворенностью жизнью в целом.")
        }
        binding.iconDepersonalization.setOnClickListener {
            showTooltip("Деперсонализация проявляется в эмоциональном отстранении и безразличии, формальном выполнении профессиональных обязанностей без личностной включенности и сопереживания, а в отдельных случаях – в раздражительности, негативизме и циничном отношении к коллегам и пациентам. На поведенческом уровне «деперсонализация» проявляется в высокомерном поведении, использовании профессионального сленга, юмора, ярлыков.")
        }
        binding.iconPersonalAchievement.setOnClickListener {
            showTooltip("Редукция профессиональных достижений проявляется в негативном оценивании себя, результатов своего труда и возможностей для профессионального развития. Высокое значение этого показателя отражает тенденцию к негативной оценке своей компетентности и продуктивности и, как следствие, снижение профессиональной мотивации, нарастание негативизма в отношении служебных обязанностей, в лимитировании своей вовлеченности в профессию за счет перекладывания обязанностей и ответственности на других людей, к изоляции от окружающих, отстраненность и неучастие, избегание работы сначала психологически, а затем физически.")
        }

        binding.buttonBackToMenu.setOnClickListener {
            if (userId != -1) {
                lifecycleScope.launch {
                    try {
                        val driver = RetrofitClient.api.getDriverByUserId(userId)

                        if (driver != null) {
                            // Обновить статус прохождения теста
                            val updatedDriver = driver.copy(isCompleted = true)
                            RetrofitClient.api.updateDriver(driver.id, updatedDriver)

                            // Сохранить результат
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

                            setFragmentResult("requestKey", bundleOf("refresh" to true))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(requireContext(), "Ошибка при сохранении результата", Toast.LENGTH_SHORT).show()
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