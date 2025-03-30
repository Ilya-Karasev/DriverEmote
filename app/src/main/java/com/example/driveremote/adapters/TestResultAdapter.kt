package com.example.driveremote

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.databinding.ItemTestResultBinding
import com.example.driveremote.models.Results

class TestResultAdapter(private val results: List<Results>) : RecyclerView.Adapter<TestResultAdapter.ResultsViewHolder>() {

    inner class ResultsViewHolder(private val binding: ItemTestResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(result: Results) {
            binding.textTestDate.text = result.testDate
            binding.textEmotionalExhaustionScore.text = "Эмоциональное выгорание: ${result.emotionalExhaustionScore}"
            binding.textDepersonalizationScore.text = "Деперсонализация: ${result.depersonalizationScore}"
            binding.textPersonalAchievementScore.text = "Редукция личных достижений: ${result.personalAchievementScore}"
            binding.textTotalScore.text = "Общий балл: ${result.totalScore}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultsViewHolder {
        val binding = ItemTestResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResultsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultsViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int {
        return results.size
    }
}