package com.example.driveremote

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
import com.github.mikephil.charting.components.Legend

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
                // Cancel reminders and clear session
                val driver = apiService.getDriverById(userId)
                val testingTimes = driver?.testingTime ?: emptyList()

                // Cancel all WorkManager tasks related to notifications
                testingTimes.forEach { time ->
                    val tag = "test_reminder_${userId}_$time"
                    workManager.cancelAllWorkByTag(tag)
                }

                // Clear session preferences
                sharedPreferences.edit().clear().apply()

                // Navigate to sign-in screen
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
        binding.userInfoProgressBar.visibility = View.VISIBLE
        binding.driverName.visibility = View.GONE
        binding.driverAge.visibility = View.GONE
        binding.driverEmail.visibility = View.GONE
        binding.driverStatus.visibility = View.GONE
        binding.testButton.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val user = apiService.getUserById(userId)
                val driver = apiService.getDriverById(userId)

                user?.let {
                    binding.driverName.text = "${it.surName} ${it.firstName} ${it.fatherName}"
                    binding.driverAge.text = "${it.age} лет"
                    binding.driverEmail.text = it.email
                    driver?.let {
                        binding.driverStatus.text = driver.status

                        val statusColor = when (driver.status) {
                            "Норма" -> Constants.STATUS_NORMAL
                            "Внимание" -> Constants.STATUS_WARNING
                            "Критическое" -> Constants.STATUS_CRITICAL
                            else -> "#000000"
                        }
                        binding.driverStatus.setTextColor(Color.parseColor(statusColor))
                    }
                }
            } catch (e: Exception) {
                Log.e("MainMenuFragment", "Error loading user info", e)
            } finally {
                binding.userInfoProgressBar.visibility = View.GONE
                binding.driverName.visibility = View.VISIBLE
                binding.driverAge.visibility = View.VISIBLE
                binding.driverEmail.visibility = View.VISIBLE
                binding.driverStatus.visibility = View.VISIBLE
                binding.testButton.visibility = View.VISIBLE
            }
        }
    }

    private fun loadResults(userId: Int) {
        binding.chartProgressBar.visibility = View.VISIBLE
        binding.lineChart.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val allResults = apiService.getResultsByUser(userId)

                val sevenDaysAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                }.time

                resultsList = allResults.filter { result ->
                    val datePart = result.testDate.split(" ")[0]
                    val resultDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(datePart)
                    resultDate != null && resultDate.after(sevenDaysAgo)
                }.sortedByDescending {
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        .parse(it.testDate.split(" ")[0])
                }

                setupChart()
            } catch (e: Exception) {
                Log.e("MainMenuFragment", "Error loading results", e)
            } finally {
                binding.chartProgressBar.visibility = View.GONE
                binding.lineChart.visibility = View.VISIBLE
            }
        }
    }

    private fun setupChart() {
        val chart = binding.lineChart

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
            circleRadius = 5f
            setDrawCircles(true)
            setCircleColor(Color.RED)  // Цвет точек
            valueTextColor = Color.BLACK
            setDrawValues(false)
        }

        val dataSetDepersonalization = LineDataSet(entriesDepersonalization, "Деперсонализация").apply {
            color = Color.BLUE
            circleRadius = 5f
            setDrawCircles(true)
            setCircleColor(Color.BLUE)
            valueTextColor = Color.BLACK
            setDrawValues(false)
        }

        val dataSetReduction = LineDataSet(entriesReduction, "Редукция достижений").apply {
            color = Color.GREEN
            circleRadius = 5f
            setDrawCircles(true)
            setCircleColor(Color.GREEN)
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
                driver?.let {
                    if (driver.isCompleted) {
                        binding.testButton.isEnabled = false
                        binding.testButton.text = "Следующее тестирование в ${driver.testingTime?.firstOrNull() ?: "неизвестно"}"
                        binding.testButton.setBackgroundColor(Color.GRAY)
                    } else {
                        binding.testButton.isEnabled = true
                        binding.testButton.text = "Пройти тестирование"
                        binding.testButton.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.green)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MainMenuFragment", "Error updating test button state", e)
            }
        }
    }

    private fun scheduleTestReminders(userId: Int) {
        lifecycleScope.launch {
            try {
                val driver = apiService.getDriverById(userId)
                val testingTimes = driver?.testingTime ?: return@launch

                val workManager = WorkManager.getInstance(requireContext())
                val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                val now = Calendar.getInstance()

                testingTimes.forEach { timeString ->
                    if (timeString.isEmpty()) {
                        Log.e("TestReminderSetup", "Skipping empty testing time for userId: $userId")
                        return@forEach // Skip empty time
                    }

                    val tag = "test_reminder_${userId}_$timeString"

                    // Cancel previous tasks with this tag
                    workManager.cancelAllWorkByTag(tag)

                    try {
                        // Schedule new reminder task
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
                            .addTag(tag) // Tag used for task management
                            .build()

                        workManager.enqueue(workRequest)

                        Log.d("TestReminderSetup", "Reminder scheduled for $timeString (delay: $delay ms)")
                    } catch (e: ParseException) {
                        Log.e("TestReminderSetup", "Error parsing time: $timeString", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainMenuFragment", "Error scheduling test reminders", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}