package com.example.driveremote

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.api.ApiService
import com.example.driveremote.api.Constants
import com.example.driveremote.databinding.ItemTestResultBinding
import com.example.driveremote.models.Results
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class TestResultAdapter(private val userId: Int, private val apiService: ApiService) : RecyclerView.Adapter<TestResultAdapter.ResultsViewHolder>() {
    private var results: List<Results> = emptyList()

    inner class ResultsViewHolder(private val binding: ItemTestResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(result: Results) {
            val formattedDate = formatDate(result.testDate)
            binding.textTestDate.text = formattedDate
            binding.EmotionalExhaustionScore.text = "${result.emotionalExhaustionScore}"
            binding.DepersonalizationScore.text = "${result.depersonalizationScore}"
            binding.PersonalAchievementScore.text = "${result.personalAchievementScore}"
            binding.TotalScore.text = "${result.totalScore}"
            binding.textStatusTest.text = result.status

            val statusColor = when (result.status) {
                "Норма" -> Color.parseColor(Constants.STATUS_NORMAL)
                "Внимание" -> Color.parseColor(Constants.STATUS_WARNING)
                "Критическое" -> Color.parseColor(Constants.STATUS_CRITICAL)
                else -> Color.BLACK
            }
            binding.textStatusTest.setTextColor(statusColor)
        }

        private fun formatDate(original: String): String {
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) // Пример: 2025-04-23T14:30:00
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

    // Метод для загрузки результатов с сервера через Retrofit
    fun loadResults(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Получаем результаты пользователя через API
                val fetchedResults = apiService.getResultsByUser(userId)

                // Обновляем данные в главном потоке
                withContext(Dispatchers.Main) {
                    results = fetchedResults
                    notifyDataSetChanged() // Обновляем адаптер после получения данных
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Обработка ошибок сети или сервера
                    Toast.makeText(context, "Ошибка при загрузке результатов", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}