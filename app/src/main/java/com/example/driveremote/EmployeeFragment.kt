package com.example.driveremote

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentEmployeeBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Results
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmployeeFragment : Fragment() {
    private var _binding: FragmentEmployeeBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    private lateinit var resultsAdapter: TestResultAdapter
    private var resultsList: List<Results> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getInt("userId") ?: -1

        if (userId != -1) {
            showLoadingState()
            loadUserInfo(userId)

            resultsAdapter = TestResultAdapter(userId) { results ->
                resultsList = results.sortedByDescending {
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        .parse(it.testDate.split(" ")[0])
                }
                setupChart()
                showResultsState()
            }

            binding.recyclerViewResults.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewResults.adapter = resultsAdapter
        }

        binding.viewMenu.setOnClickListener {
            findNavController().navigate(R.id.action_employeeFragment_to_managerMenuFragment)
        }

        binding.viewSearch.setOnClickListener {
            findNavController().navigate(R.id.action_employeeFragment_to_searchFragment)
        }

        binding.viewRequests.setOnClickListener {
            findNavController().navigate(R.id.action_employeeFragment_to_requestsFragment)
        }
    }

    private fun showLoadingState() {
        // Прячем все основные элементы
        binding.driverName.visibility = View.GONE
        binding.driverAge.visibility = View.GONE
        binding.driverEmail.visibility = View.GONE
        binding.lineChart.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.userInfoProgressBar.visibility = View.VISIBLE
        binding.chartProgressBar.visibility = View.VISIBLE
    }

    private fun showResultsState() {
        // Показываем все данные и скрываем спиннеры
        binding.driverName.visibility = View.VISIBLE
        binding.driverAge.visibility = View.VISIBLE
        binding.driverEmail.visibility = View.VISIBLE
        binding.lineChart.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.userInfoProgressBar.visibility = View.GONE
        binding.chartProgressBar.visibility = View.GONE
    }

    private fun loadUserInfo(userId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val user = RetrofitClient.api.getUserById(userId)
                withContext(Dispatchers.Main) {
                    binding.driverName.text = "${user.surName} ${user.firstName} ${user.fatherName}"
                    binding.driverAge.text = "${user.age} лет"
                    binding.driverEmail.text = user.email
                }
            } catch (e: Exception) {
                Log.e("EmployeeFragment", "Ошибка загрузки пользователя: ${e.message}")
            }
        }
    }

    private fun setupChart() {
        val chart = binding.lineChart

        if (resultsList.isEmpty()) {
            chart.clear()
            return
        }

        val legend = chart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.setWordWrapEnabled(true)
        legend.textSize = 16f

        val entriesBurnout = ArrayList<Entry>()
        val entriesDepersonalization = ArrayList<Entry>()
        val entriesReduction = ArrayList<Entry>()
        val dateLabels = ArrayList<String>()

        val intValueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return entry?.y?.toInt().toString()
            }
        }

        val reversedResults = resultsList.reversed()

        reversedResults.forEachIndexed { index, result ->
            val date = result.testDate.split(" ")[0]
            val formattedDate = SimpleDateFormat("dd.MM", Locale.getDefault())
                .format(SimpleDateFormat("dd.MM.yyyy").parse(date) ?: Date())
            dateLabels.add(formattedDate)

            entriesBurnout.add(Entry(index.toFloat(), result.emotionalExhaustionScore.toFloat()))
            entriesDepersonalization.add(Entry(index.toFloat(), result.depersonalizationScore.toFloat()))
            entriesReduction.add(Entry(index.toFloat(), result.personalAchievementScore.toFloat()))
        }

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.setPinchZoom(true)

        val dataSetBurnout = LineDataSet(entriesBurnout, "Эмоциональное истощение").apply {
            color = Color.RED
            circleRadius = 5f
            setDrawCircles(true)
            setCircleColor(Color.RED)
            valueTextColor = Color.BLACK
            setDrawValues(true)
            valueFormatter = intValueFormatter
        }

        val dataSetDepersonalization = LineDataSet(entriesDepersonalization, "Деперсонализация").apply {
            color = Color.BLUE
            circleRadius = 5f
            setDrawCircles(true)
            setCircleColor(Color.BLUE)
            valueTextColor = Color.BLACK
            setDrawValues(true)
            valueFormatter = intValueFormatter
        }

        val dataSetReduction = LineDataSet(entriesReduction, "Редукция достижений").apply {
            color = Color.GREEN
            circleRadius = 5f
            setDrawCircles(true)
            setCircleColor(Color.GREEN)
            valueTextColor = Color.BLACK
            setDrawValues(true)
            valueFormatter = intValueFormatter
        }

        chart.data = LineData(dataSetBurnout, dataSetDepersonalization, dataSetReduction)
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}