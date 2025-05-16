package com.example.driveremote

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentMainMenuBinding
import com.example.driveremote.models.Results
import kotlinx.coroutines.launch
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
import java.util.concurrent.TimeUnit
import androidx.core.content.ContextCompat
import com.example.driveremote.api.Constants
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.models.Driver
import com.example.driveremote.sessionManagers.DriverSession
import com.example.driveremote.sessionManagers.ResultsSession
import com.github.mikephil.charting.components.Legend
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainMenuFragment : Fragment() {
    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    private var resultsList: List<Results> = emptyList()
    private val apiService = RetrofitClient.api

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
            loadUserInfo(userId)
            loadResults(userId)
            updateTestButtonState(userId)
            scheduleTestReminders(userId)
        }

        binding.detailsLink.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_profileFragment)
        }

        binding.settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_settingsFragment)
        }

        binding.testButton.setOnClickListener {
            if (binding.testButton.isEnabled) {
                findNavController().navigate(R.id.action_mainMenuFragment_to_testFragment)
            }
        }

        binding.logoutButton.setOnClickListener {
            val workManager = WorkManager.getInstance(requireContext())
            lifecycleScope.launch {
                val driver = apiService.getDriverById(userId)
                val testingTimes = driver?.testingTime ?: emptyList()

                testingTimes.forEach { time ->
                    val tag = "test_reminder_${userId}_$time"
                    workManager.cancelAllWorkByTag(tag)
                }

                sharedPreferences.edit().clear().apply()
                DriverSession.clearDriver(requireContext())
                ResultsSession.clearResults(requireContext())
                findNavController().navigate(R.id.action_mainMenuFragment_to_signInFragment)
            }
        }

        binding.viewSearch.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_searchFragment)
        }

        binding.viewRequests.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_requestsFragment)
        }

        binding.settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_settingsFragment)
        }
    }

    private fun loadUserInfo(userId: Int) {
        binding.userInfoProgressBar.visibility = VISIBLE
        binding.driverName.visibility = GONE
        binding.driverAge.visibility = GONE
        binding.driverEmail.visibility = GONE
        binding.driverStatus.visibility = GONE
        binding.testButton.visibility = GONE

        lifecycleScope.launch {
            try {
                val user = apiService.getUserById(userId)
                val driver = apiService.getDriverById(userId)

                if (user != null && driver != null) {
                    DriverSession.saveDriver(requireContext(), driver)

                    binding.driverName.text = "${user.surName} ${user.firstName} ${user.fatherName}"
                    binding.driverAge.text = "${user.age} лет"
                    binding.driverEmail.text = user.email

                    binding.driverStatus.text = driver.status
                    val statusColor = when (driver.status) {
                        "Норма" -> Constants.STATUS_NORMAL
                        "Внимание" -> Constants.STATUS_WARNING
                        "Критическое" -> Constants.STATUS_CRITICAL
                        else -> Constants.BLACK
                    }
                    binding.driverStatus.setTextColor(Color.parseColor(statusColor))
                } else {
                    throw Exception("Empty user or driver")
                }
            } catch (e: Exception) {
                Log.e("MainMenuFragment", "Error loading user info from server, fallback to session", e)

                DriverSession.loadDriver(requireContext())?.let { driver ->
                    val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                    val name = "${prefs.getString("surName", "")} ${prefs.getString("firstName", "")} ${prefs.getString("fatherName", "")}"
                    val age = "${prefs.getInt("age", 0)} лет"
                    val email = prefs.getString("email", "") ?: ""

                    binding.driverName.text = name
                    binding.driverAge.text = age
                    binding.driverEmail.text = email
                    binding.driverStatus.text = driver.status

                    val statusColor = when (driver.status) {
                        "Норма" -> Constants.STATUS_NORMAL
                        "Внимание" -> Constants.STATUS_WARNING
                        "Критическое" -> Constants.STATUS_CRITICAL
                        else -> Constants.BLACK
                    }
                    binding.driverStatus.setTextColor(Color.parseColor(statusColor))
                }
            } finally {
                binding.userInfoProgressBar.visibility = GONE
                binding.driverName.visibility = VISIBLE
                binding.driverAge.visibility = VISIBLE
                binding.driverEmail.visibility = VISIBLE
                binding.driverStatus.visibility = VISIBLE
                binding.testButton.visibility = VISIBLE
            }
        }
    }

    private fun loadResults(userId: Int) {
        binding.chartProgressBar.visibility = VISIBLE
        binding.lineChart.visibility = GONE

        lifecycleScope.launch {
            try {
                val allResults = apiService.getResultsByUser(userId)
                ResultsSession.saveResults(requireContext(), allResults)

                resultsList = filterRecentResults(allResults)
            } catch (e: Exception) {
                Log.e("MainMenuFragment", "Error loading results from server, fallback to session", e)
                resultsList = filterRecentResults(ResultsSession.loadResults(requireContext()))
            } finally {
                setupChart()
                binding.chartProgressBar.visibility = GONE
                binding.lineChart.visibility = VISIBLE
            }
        }
    }

    private fun setupChart() {
        val chart = binding.lineChart
        val orientation = resources.configuration.orientation

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.setPinchZoom(true)

        val legend = chart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.setWordWrapEnabled(true)
        legend.textSize = 16f

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.lineChart.minimumWidth = resources.getDimensionPixelSize(R.dimen.chart_landscape_min_width)
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            binding.topBar.visibility = GONE
            binding.bottomBar.visibility = GONE
        } else {
            binding.lineChart.minimumWidth = resources.getDimensionPixelSize(R.dimen.chart_portrait_min_width)
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
            binding.topBar.visibility = VISIBLE
            binding.bottomBar.visibility = VISIBLE
        }

        if (resultsList.isEmpty()) {
            chart.clear()
            return
        }

        val entriesBurnout = mutableListOf<Entry>()
        val entriesDepersonalization = mutableListOf<Entry>()
        val entriesReduction = mutableListOf<Entry>()
        val dateLabels = mutableListOf<String>()

        val sortedResults = resultsList.sortedByDescending {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                .parse(it.testDate.split(" ")[0])
        }

        val reversedResults = sortedResults.reversed()

        reversedResults.forEachIndexed { index, result ->
            val date = result.testDate.split(" ")[0]
            val formattedDate = SimpleDateFormat("dd.MM", Locale.getDefault())
                .format(SimpleDateFormat("dd.MM.yyyy").parse(date) ?: Date())
            dateLabels.add(formattedDate)

            entriesBurnout.add(Entry(index.toFloat(), result.emotionalExhaustionScore.toFloat()))
            entriesDepersonalization.add(Entry(index.toFloat(), result.depersonalizationScore.toFloat()))
            entriesReduction.add(Entry(index.toFloat(), result.personalAchievementScore.toFloat()))
        }

        xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)

        val dataSetBurnout = LineDataSet(entriesBurnout, "Эмоциональное истощение").apply {
            color = Color.RED
            circleRadius = 7f
            setDrawCircles(true)
            setCircleColor(Color.parseColor(Constants.RED_GRAPH))
            valueTextColor = Color.BLACK
            setDrawValues(false)
        }

        val dataSetDepersonalization = LineDataSet(entriesDepersonalization, "Деперсонализация").apply {
            color = Color.BLUE
            circleRadius = 6f
            setDrawCircles(true)
            setCircleColor(Color.parseColor(Constants.BLUE_GRAPH))
            valueTextColor = Color.BLACK
            setDrawValues(false)
        }

        val dataSetReduction = LineDataSet(entriesReduction, "Редукция достижений").apply {
            color = Color.GREEN
            circleRadius = 5f
            setDrawCircles(true)
            setCircleColor(Color.parseColor(Constants.GREEN_GRAPH))
            valueTextColor = Color.BLACK
            setDrawValues(false)
        }

        chart.data = LineData(dataSetBurnout, dataSetDepersonalization, dataSetReduction)
        chart.invalidate()
    }

    private fun updateTestButtonState(userId: Int) {
        lifecycleScope.launch {
            try {
                val driver = apiService.getDriverById(userId)
                DriverSession.saveDriver(requireContext(), driver)
                updateTestButtonUI(driver)
            } catch (e: Exception) {
                Log.e("MainMenuFragment", "Error updating test button state from server, using cache", e)
                val cachedDriver = DriverSession.loadDriver(requireContext())
                cachedDriver?.let {
                    updateTestButtonUI(it)
                } ?: run {
                    binding.testButton.isEnabled = false
                    binding.testButton.text = "Нет данных"
                    binding.testButton.setBackgroundColor(Color.GRAY)
                }
            }
        }
    }

    private fun updateTestButtonUI(driver: Driver) {
        if (driver.isCompleted) {
            binding.testButton.isEnabled = false

            val now = LocalTime.now()
            val times = driver.testingTime?.mapNotNull {
                try {
                    LocalTime.parse(it)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            val nextTime = times
                .filter { it.isAfter(now) }
                .minOrNull() ?: times.minOrNull()

            val displayTime = nextTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "неизвестно"
            binding.testButton.text = "Следующее тестирование в $displayTime"
            binding.testButton.setBackgroundColor(Color.GRAY)
        } else {
            binding.testButton.isEnabled = true
            binding.testButton.text = "Пройти тестирование"
            binding.testButton.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.green)
            )
        }
    }

    private fun scheduleTestReminders(userId: Int) {
        lifecycleScope.launch {
            val driver = try {
                val remoteDriver = apiService.getDriverById(userId)
                DriverSession.saveDriver(requireContext(), remoteDriver)
                remoteDriver
            } catch (e: Exception) {
                Log.e("MainMenuFragment", "Error scheduling test reminders from server, using cache", e)
                DriverSession.loadDriver(requireContext())
            }

            val testingTimes = driver?.testingTime ?: return@launch

            val workManager = WorkManager.getInstance(requireContext())
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Calendar.getInstance()

            testingTimes.forEach { timeString ->
                if (timeString.isEmpty()) {
                    Log.e("TestReminderSetup", "Skipping empty testing time for userId: $userId")
                    return@forEach
                }

                val tag = "test_reminder_${userId}_$timeString"
                workManager.cancelAllWorkByTag(tag)

                try {
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
                        .addTag(tag)
                        .build()

                    workManager.enqueue(workRequest)
                    Log.d("TestReminderSetup", "Reminder scheduled for $timeString (delay: $delay ms)")
                } catch (e: ParseException) {
                    Log.e("TestReminderSetup", "Error parsing time: $timeString", e)
                }
            }
        }
    }

    private fun filterRecentResults(allResults: List<Results>): List<Results> {
        val sevenDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.time

        return allResults.filter { result ->
            val datePart = result.testDate.split(" ")[0]
            val resultDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(datePart)
            resultDate != null && resultDate.after(sevenDaysAgo)
        }.sortedByDescending {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                .parse(it.testDate.split(" ")[0])
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}