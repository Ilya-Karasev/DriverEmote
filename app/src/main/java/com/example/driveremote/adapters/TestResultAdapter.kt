package com.example.driveremote
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.driveremote.api.Constants
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.ItemTestResultBinding
import com.example.driveremote.models.Results
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
class TestResultAdapter(
    private val context: Context,
    private val userId: Int,
    private val onResultsLoaded: (List<Results>) -> Unit
) : RecyclerView.Adapter<TestResultAdapter.ResultsViewHolder>() {
    private val results = mutableListOf<Results>()
    init {
        fetchResults()
    }
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
                "Норма" -> Color.parseColor(Constants.STATUS_NORMAL)
                "Внимание" -> Color.parseColor(Constants.STATUS_WARNING)
                "Критическое" -> Color.parseColor(Constants.STATUS_CRITICAL)
                else -> Color.BLACK
            }
            binding.textStatusTest.setTextColor(statusColor)
        }
        private fun formatDate(original: String): String {
            return try {
                val parser = SimpleDateFormat("dd.MM.yyyy — HH:mm", Locale.getDefault())
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
    private fun fetchResults() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.getResultsByUser(userId)
                withContext(Dispatchers.Main) {
                    results.clear()
                    results.addAll(response)
                    notifyDataSetChanged()
                    onResultsLoaded(response)
                    val prefs = context.getSharedPreferences("ResultsSession", Context.MODE_PRIVATE)
                    val editor = prefs.edit()
                    val json = Gson().toJson(response)
                    editor.putString("results_user_$userId", json)
                    editor.apply()
                }
            } catch (e: Exception) {
                Log.e("TestResultAdapter", "Ошибка при получении результатов: ${e.message}")
                val prefs = context.getSharedPreferences("ResultsSession", Context.MODE_PRIVATE)
                val json = prefs.getString("results_user_$userId", null)
                if (json != null) {
                    val type = object : TypeToken<List<Results>>() {}.type
                    val cachedResults: List<Results> = Gson().fromJson(json, type)
                    withContext(Dispatchers.Main) {
                        results.clear()
                        results.addAll(cachedResults)
                        notifyDataSetChanged()
                        onResultsLoaded(cachedResults)
                        Toast.makeText(context, "Загружены сохранённые результаты", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка загрузки результатов", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}