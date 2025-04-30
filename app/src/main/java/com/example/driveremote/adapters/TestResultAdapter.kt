package com.example.driveremote

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.databinding.ItemTestResultBinding
import com.example.driveremote.models.Results
import java.text.SimpleDateFormat
import java.util.Locale

class TestResultAdapter(val results: List<Results>) : RecyclerView.Adapter<TestResultAdapter.ResultsViewHolder>() {

    inner class ResultsViewHolder(private val binding: ItemTestResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(result: Results) {
            val formattedDate = formatDate(result.testDate)
            binding.textTestDate.text = formattedDate
            binding.EmotionalExhaustionScore.text = "${result.emotionalExhaustionScore}"
            binding.DepersonalizationScore.text = "${result.depersonalizationScore}"
            binding.PersonalAchievementScore.text = "${result.personalAchievementScore}"
            binding.TotalScore.text = "${result.totalScore}"
            binding.textStatusTest.text = result.status

            val statusColor = when (result.status) {
                "Норма" -> Color.parseColor("#00B147")
                "Внимание" -> Color.parseColor("#FF7700")
                "Критическое" -> Color.parseColor("#FF0000")
                else -> Color.BLACK
            }
            binding.textStatusTest.setTextColor(statusColor)
        }

        private fun formatDate(original: String): String {
            return try {
                val parser = SimpleDateFormat("dd.MM.yyyy — HH:mm", Locale.getDefault()) // Пример: 2025-04-23T14:30:00
                val formatter = SimpleDateFormat("dd.MM.yyyy — HH:mm", Locale.getDefault())
                val date = parser.parse(original)
                date?.let { formatter.format(it) } ?: original
            } catch (e: Exception) {
                original
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTestResultBinding.inflate(inflater, parent, false)
        return ResultsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultsViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int = results.size
}