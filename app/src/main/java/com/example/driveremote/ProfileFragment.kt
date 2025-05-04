package com.example.driveremote

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.databinding.FragmentProfileBinding
import com.example.driveremote.models.Results
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    private lateinit var resultsAdapter: TestResultAdapter
    private var resultsList = mutableListOf<Results>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLoadingState()

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        if (userId != -1) {
            resultsAdapter = TestResultAdapter(userId) { loadedResults ->
                resultsList = loadedResults.sortedByDescending {
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        .parse(it.testDate.split(" ")[0])
                }.toMutableList()

                binding.recyclerViewResults.layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerViewResults.adapter = resultsAdapter

                setupChart()
                showContentState()
            }
        }

        binding.viewMenu.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_mainMenuFragment)
        }

        binding.viewSearch.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_searchFragment)
        }

        binding.viewRequests.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_requestsFragment)
        }

        binding.settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_settingsFragment)
        }
    }

    private fun showLoadingState() {
        binding.chartProgressBar.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewResults.visibility = View.GONE
        binding.lineChart.visibility = View.GONE
    }

    private fun showContentState() {
        binding.chartProgressBar.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewResults.visibility = View.VISIBLE
        binding.lineChart.visibility = View.VISIBLE
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

        xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)

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

        val lineData = LineData(dataSetBurnout, dataSetDepersonalization, dataSetReduction)
        chart.data = lineData
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
