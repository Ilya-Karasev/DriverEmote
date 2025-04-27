package com.example.driveremote

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
            binding.textEmotionalExhaustionScore.text = "Эмоциональное выгорание: ${result.emotionalExhaustionScore}"
            binding.textDepersonalizationScore.text = "Деперсонализация: ${result.depersonalizationScore}"
            binding.textPersonalAchievementScore.text = "Редукция личных достижений: ${result.personalAchievementScore}"
            binding.textTotalScore.text = "Общий балл: ${result.totalScore}"
        }

        private fun formatDate(original: String): String {
            return try {
                val parser = SimpleDateFormat("dd.MM.yyyy — HH:mm", Locale.getDefault()) // Пример: 2025-04-23T14:30:00
                val formatter = SimpleDateFormat("dd.MM.yyyy — HH:mm", Locale.getDefault())
                val date = parser.parse(original)
                date?.let { formatter.format(it) } ?: original
            } catch (e: Exception) {
                original // если формат неожиданен, выводим как есть
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