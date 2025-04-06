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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentResultsBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Results
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

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        binding.textTime.text = "Дата и время завершения: $currentTime"
        binding.textScore.text = "Вы набрали $totalScore баллов"
        binding.textEmotionalExhaustion.text = "Эмоциональное истощение: $emotionalExhaustionScore"
        binding.textDepersonalization.text = "Деперсонализация: $depersonalizationScore"
        binding.textPersonalAchievement.text = "Редукция личных достижений: $personalAchievementScore"

        // Добавление обработчика нажатий на иконки подсказки
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
                // Получаем объект Driver для текущего пользователя
                val db = AppDatabase.getDatabase(requireContext())
                val driverDao = db.driverDao()

                lifecycleScope.launch {
                    val driver = driverDao.getDriverById(userId)
                    if (driver != null) {
                        // Обновляем значение isCompleted на true
                        driverDao.updateCompletionStatus(driver.id, true)

                        // Сохраняем результат теста
                        saveTestResult(userId, currentTime, emotionalExhaustionScore, depersonalizationScore, personalAchievementScore, totalScore)
                    }
                }
            }
            // Отправляем результат обратно в MainMenuFragment
            setFragmentResult("requestKey", bundleOf("refresh" to true))
            findNavController().navigate(R.id.action_resultsFragment_to_mainMenuFragment)
        }
    }

    private fun showTooltip(message: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.tooltip_layout)

        val tooltipText = dialog.findViewById<TextView>(R.id.tooltip_text)

        // Создаем SpannableString для выделения текста
        val spannableMessage = SpannableString(message)

        // Пример выделения слова "деперсонализация" жирным
        val startIndex1 = message.indexOf("Деперсонализация")
        val endIndex1 = startIndex1 + "Деперсонализация".length
        if (startIndex1 != -1) {
            spannableMessage.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex1,
                endIndex1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val startIndex2 = message.indexOf("Эмоциональное истощение")
        val endIndex2 = startIndex2 + "Эмоциональное истощение".length
        if (startIndex2 != -1) {
            spannableMessage.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex2,
                endIndex2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val startIndex3 = message.indexOf("Редукция профессиональных достижений")
        val endIndex3 = startIndex3 + "Редукция профессиональных достижений".length
        if (startIndex3 != -1) {
            spannableMessage.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex3,
                endIndex3,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        tooltipText.text = spannableMessage

        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    private fun saveTestResult(userId: Int, testDate: String, emotionalExhaustionScore: Int, depersonalizationScore: Int, personalAchievementScore: Int, totalScore: Int) {
        val db = AppDatabase.getDatabase(requireContext())
        val resultsDao = db.resultsDao()

        lifecycleScope.launch {
            val result = Results(
                userId = userId,
                testDate = testDate,
                emotionalExhaustionScore = emotionalExhaustionScore,
                depersonalizationScore = depersonalizationScore,
                personalAchievementScore = personalAchievementScore,
                totalScore = totalScore
            )
            resultsDao.insertResult(result)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}