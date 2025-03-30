package com.example.driveremote

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.databinding.FragmentMainMenuBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Results
import kotlinx.coroutines.launch
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainMenuFragment : Fragment() {
    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    private lateinit var resultsAdapter: TestResultAdapter
    private var resultsList: List<Results> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        if (userId != -1) {
            loadResults(userId)
            loadLastTestDate(userId)
        } else {
            binding.textTest.text = "Вы ещё не проходили тестирование"
        }

        binding.iconLeft.setOnClickListener {
            requireActivity().finish()
        }

        binding.iconRight.setOnClickListener {
            sharedPreferences.edit().clear().apply()
            findNavController().navigate(R.id.action_mainMenuFragment_to_signInFragment)
        }

        binding.buttonTest.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.mainMenuFragment) {
                navController.navigate(R.id.action_mainMenuFragment_to_testFragment)
            }
        }

        binding.view3.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_profileFragment)
        }
    }

    private fun loadLastTestDate(userId: Int) {
        val db = AppDatabase.getDatabase(requireContext())
        val resultsDao = db.resultsDao()

        lifecycleScope.launch {
            val lastResult = resultsDao.getResultsByUser(userId).firstOrNull()
            binding.textTest.text = if (lastResult != null) {
                "Последний раз вы проходили тестирование ${lastResult.testDate}"
            } else {
                "Вы ещё не проходили тестирование"
            }
        }
    }

    private fun setupChart() {
        val chart = binding.lineChart

        // Настройки оси X (дата тестирования)
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        // Настройки оси Y
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.setPinchZoom(true)

        // Проверка наличия данных
        if (resultsList.isEmpty()) {
            chart.clear()
            return
        }

        val entriesBurnout = ArrayList<Entry>()
        val entriesDepersonalization = ArrayList<Entry>()
        val entriesReduction = ArrayList<Entry>()
        val dateLabels = ArrayList<String>()

        // Сортировка результатов по дате тестирования
        val sortedResultsList = resultsList.sortedBy { it.testDate }

        sortedResultsList.forEachIndexed { index, result ->
            // Форматирование даты в формат "dd.MM"
            val date = result.testDate.split(" ")[0] // Отделяем дату от времени
            val formattedDate = SimpleDateFormat("dd.MM", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd").parse(date) ?: Date())
            dateLabels.add(formattedDate)

            // Добавляем данные для графика
            entriesBurnout.add(Entry(index.toFloat(), result.emotionalExhaustionScore.toFloat()))
            entriesDepersonalization.add(Entry(index.toFloat(), result.depersonalizationScore.toFloat()))
            entriesReduction.add(Entry(index.toFloat(), result.personalAchievementScore.toFloat()))
        }

        // Настройка меток на оси X
        xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)

        val datasetBurnout = LineDataSet(entriesBurnout, "Эмоц-ое истощение").apply {
            color = Color.RED
            valueTextColor = Color.BLACK
        }

        val datasetDepersonalization = LineDataSet(entriesDepersonalization, "Деперсон-ция").apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
        }

        val datasetReduction = LineDataSet(entriesReduction, "Редукция достижений").apply {
            color = Color.GREEN
            valueTextColor = Color.BLACK
        }

        chart.data = LineData(datasetBurnout, datasetDepersonalization, datasetReduction)
        chart.invalidate()
    }

    // Вызов setupChart() после загрузки данных
    private fun loadResults(userId: Int) {
        val db = AppDatabase.getDatabase(requireContext())
        val resultsDao = db.resultsDao()

        lifecycleScope.launch {
            resultsList = resultsDao.getResultsByUser(userId)
            resultsAdapter = TestResultAdapter(resultsList)
            binding.recyclerViewResults.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewResults.adapter = resultsAdapter
            resultsAdapter.notifyDataSetChanged() // Уведомляем адаптер об изменениях

            // Отображаем график
            setupChart()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}