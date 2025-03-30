package com.example.driveremote

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentTestBinding

class TestFragment : Fragment() {
    private var _binding: FragmentTestBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    private val scoreMap = mutableMapOf<Int, Int>()

    private var totalScore = 0
    private var emotionalExhaustionScore = 0
    private var depersonalizationScore = 0
    private var personalAchievementScore = 0
    private var answeredQuestions = 0
    private val totalQuestions = 22

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        for (i in 1..totalQuestions) {
            scoreMap[resources.getIdentifier("radio_never_q$i", "id", requireContext().packageName)] = 0
            scoreMap[resources.getIdentifier("radio_very_rarely_q$i", "id", requireContext().packageName)] = 1
            scoreMap[resources.getIdentifier("radio_sometimes_q$i", "id", requireContext().packageName)] = 3
            scoreMap[resources.getIdentifier("radio_often_q$i", "id", requireContext().packageName)] = 4
            scoreMap[resources.getIdentifier("radio_very_often_q$i", "id", requireContext().packageName)] = 5
            scoreMap[resources.getIdentifier("radio_every_day_q$i", "id", requireContext().packageName)] = 6
        }

        val radioGroups = listOf(
            binding.question1, binding.question2, binding.question3, binding.question4, binding.question5,
            binding.question6, binding.question7, binding.question8, binding.question9, binding.question10,
            binding.question11, binding.question12, binding.question13, binding.question14, binding.question15,
            binding.question16, binding.question17, binding.question18, binding.question19, binding.question20,
            binding.question21, binding.question22
        )

        setResultsButtonState(false)
        radioGroups.forEach { setupRadioGroupListener(it) }

        binding.textExit.setOnClickListener {
            if (answeredQuestions == totalQuestions) {
                val bundle = Bundle().apply {
                    putInt("totalScore", totalScore)
                    putInt("emotionalExhaustionScore", emotionalExhaustionScore)
                    putInt("depersonalizationScore", depersonalizationScore)
                    putInt("personalAchievementScore", personalAchievementScore)
                }
                findNavController().navigate(R.id.action_testFragment_to_resultsFragment, bundle)
            }
        }

    }

    private fun setupRadioGroupListener(radioGroup: RadioGroup) {
        radioGroup.setOnCheckedChangeListener { _, _ ->
            updateScore()
        }
    }

    private fun updateScore() {
        totalScore = 0
        emotionalExhaustionScore = 0
        depersonalizationScore = 0
        personalAchievementScore = 0
        answeredQuestions = 0

        val emotionalExhaustionQuestions = setOf(1, 2, 3, 6, 8, 13, 14, 16, 20)
        val depersonalizationQuestions = setOf(5, 10, 11, 15, 22)
        val personalAchievementQuestions = setOf(4, 7, 9, 12, 17, 18, 19, 21)

        val radioGroups = listOf(
            binding.question1, binding.question2, binding.question3, binding.question4, binding.question5,
            binding.question6, binding.question7, binding.question8, binding.question9, binding.question10,
            binding.question11, binding.question12, binding.question13, binding.question14, binding.question15,
            binding.question16, binding.question17, binding.question18, binding.question19, binding.question20,
            binding.question21, binding.question22
        )

        radioGroups.forEachIndexed { index, radioGroup ->
            val questionNumber = index + 1
            val selectedId = radioGroup.checkedRadioButtonId
            val score = scoreMap[selectedId] ?: 0

            if (selectedId != -1) {
                totalScore += score
                answeredQuestions++

                when (questionNumber) {
                    in emotionalExhaustionQuestions -> emotionalExhaustionScore += score
                    in depersonalizationQuestions -> depersonalizationScore += score
                    in personalAchievementQuestions -> personalAchievementScore += score
                }
            }
        }

        binding.textAppName.text = "Отвечено вопросов: $answeredQuestions/$totalQuestions"
        setResultsButtonState(answeredQuestions == totalQuestions)
    }

    private fun setResultsButtonState(isEnabled: Boolean) {
        binding.bottomBar.isEnabled = isEnabled
        binding.bottomBar.setBackgroundColor(if (isEnabled) Color.rgb(76, 175, 80) else Color.GRAY)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}