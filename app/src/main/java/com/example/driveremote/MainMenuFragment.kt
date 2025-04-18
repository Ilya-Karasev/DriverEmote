package com.example.driveremote

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.databinding.FragmentMainMenuBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Driver
import com.example.driveremote.models.Results
import com.example.driveremote.utils.NotificationUtils
import kotlinx.coroutines.launch
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.work.*
import com.example.driveremote.utils.TestReminderWorker
import java.text.ParseException
import java.util.*
import java.util.concurrent.TimeUnit

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

        // Слушаем результат из ResultsFragment
        parentFragmentManager.setFragmentResultListener("requestKey", viewLifecycleOwner) { _, bundle ->
            val refresh = bundle.getBoolean("refresh", false)
            if (refresh) {
                val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                val userId = sharedPreferences.getInt("userId", -1)

                if (userId != -1) {
                    loadResults(userId)
                    loadLastTestDate(userId)
                    updateTestButtonState(userId)
                    scheduleTestReminders(userId)
                } else {
                    binding.textTest.text = "Вы ещё не проходили тестирование"
                }
            }
        }

        // Инициализация остальной части экрана
        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        if (userId != -1) {
            loadResults(userId)
            loadLastTestDate(userId)
            updateTestButtonState(userId)
            scheduleTestReminders(userId)
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

        binding.view2.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_searchFragment)
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

    private fun updateTestButtonState(userId: Int) {
        val db = AppDatabase.getDatabase(requireContext())
        val driverDao = db.driverDao()

        lifecycleScope.launch {
            val driver = driverDao.getDriverById(userId)
            if (driver != null) {
                if (driver.isCompleted) {
                    // Тест уже пройден — деактивируем кнопку
                    binding.buttonTest.isEnabled = false
                    binding.buttonTest.text = "Вы уже проходили тестирование"
                    binding.buttonTest.setBackgroundColor(Color.GRAY)
                } else {
                    // Тест ещё не пройден — активируем кнопку
                    binding.buttonTest.isEnabled = true
                    binding.buttonTest.text = "Пройти тест"
                    binding.buttonTest.setBackgroundColor(resources.getColor(R.color.green, null))
                }
            }
        }
    }

    private fun scheduleTestReminders(userId: Int) {
        val db = AppDatabase.getDatabase(requireContext())
        val driverDao = db.driverDao()

        lifecycleScope.launch {
            val driver = driverDao.getDriverById(userId)
            val testingTimes = driver?.testingTime ?: return@launch

            val workManager = WorkManager.getInstance(requireContext())
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Calendar.getInstance()

            testingTimes.forEach { timeString ->
                if (timeString.isEmpty()) {
                    Log.e("TestReminderSetup", "Skipping empty testing time for userId: $userId")
                    return@forEach // Пропускаем пустое время
                }

                val tag = "test_reminder_${userId}_$timeString"

                // Отменяем предыдущие задачи с этим тегом
                workManager.cancelAllWorkByTag(tag)

                try {
                    // Планируем следующее уведомление только для тех случаев, когда оно действительно должно быть
                    val calendar = Calendar.getInstance().apply {
                        time = formatter.parse(timeString) ?: return@forEach
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.YEAR, now.get(Calendar.YEAR))
                        set(Calendar.MONTH, now.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))

                        if (before(now)) {
                            add(Calendar.DAY_OF_MONTH, 1)
                        }
                    }

                    val delay = calendar.timeInMillis - System.currentTimeMillis()

                    val inputData = Data.Builder()
                        .putInt("userId", userId)
                        .putString("time", timeString)
                        .build()

                    val workRequest = OneTimeWorkRequestBuilder<TestReminderWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(inputData)
                        .addTag(tag) // теперь тег точно используется и управляется
                        .build()

                    workManager.enqueue(workRequest)

                    Log.d("TestReminderSetup", "Reminder scheduled for $timeString (delay: $delay ms)")
                } catch (e: ParseException) {
                    Log.e("TestReminderSetup", "Error parsing time: $timeString", e)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}