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
import com.example.driveremote.databinding.FragmentProfileBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Results
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

//    private lateinit var resultsAdapter: TestResultAdapter
//    private var resultsList: List<Results> = emptyList()
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentProfileBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
//        val userId = sharedPreferences.getInt("userId", -1)
//
//        if (userId != -1) {
//            loadResults(userId)
//        }
//
//        binding.viewMenu.setOnClickListener {
//            findNavController().navigate(R.id.action_profileFragment_to_mainMenuFragment)
//        }
//
//        binding.viewSearch.setOnClickListener {
//            findNavController().navigate(R.id.action_profileFragment_to_searchFragment)
//        }
//
//        binding.viewRequests.setOnClickListener {
//            findNavController().navigate(R.id.action_profileFragment_to_requestsFragment)
//        }
//
//        binding.settingsIcon.setOnClickListener {
//            findNavController().navigate(R.id.action_searchFragment_to_settingsFragment)
//        }
//    }
//
//    // Вызов setupChart() после загрузки данных
//    private fun loadResults(userId: Int) {
//        val db = AppDatabase.getDatabase(requireContext())
//        val resultsDao = db.resultsDao()
//
//        lifecycleScope.launch {
//            val rawResults = resultsDao.getResultsByUser(userId)
//            resultsList = rawResults.sortedByDescending {
//                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
//                    .parse(it.testDate.split(" ")[0])
//            }
//            resultsAdapter = TestResultAdapter(resultsList)
//
//            binding.recyclerViewResults.layoutManager = LinearLayoutManager(requireContext())
//            binding.recyclerViewResults.adapter = resultsAdapter
//            resultsAdapter.notifyDataSetChanged()
//
//            // Отображаем график
//            setupChart()
//        }
//    }
//
//    private fun setupChart() {
//        val chart = binding.lineChart
//
//        val xAxis = chart.xAxis
//        xAxis.position = XAxis.XAxisPosition.BOTTOM
//        xAxis.granularity = 1f
//        xAxis.setDrawGridLines(false)
//
//        chart.axisRight.isEnabled = false
//        chart.description.isEnabled = false
//        chart.setPinchZoom(true)
//
//        val legend = chart.legend
//        legend.isEnabled = true
//        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
//        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
//        legend.orientation = Legend.LegendOrientation.HORIZONTAL
//        legend.setDrawInside(false)
//        legend.setWordWrapEnabled(true)
//        legend.textSize = 16f
//
//        if (resultsList.isEmpty()) {
//            chart.clear()
//            return
//        }
//
//        val entriesBurnout = ArrayList<Entry>()
//        val entriesDepersonalization = ArrayList<Entry>()
//        val entriesReduction = ArrayList<Entry>()
//        val dateLabels = ArrayList<String>()
//
//        val reversedResults = resultsList.reversed()
//
//        reversedResults.forEachIndexed { index, result ->
//            val date = result.testDate.split(" ")[0]
//            val formattedDate = SimpleDateFormat("dd.MM", Locale.getDefault())
//                .format(SimpleDateFormat("dd.MM.yyyy").parse(date) ?: Date())
//            dateLabels.add(formattedDate)
//
//            entriesBurnout.add(Entry(index.toFloat(), result.emotionalExhaustionScore.toFloat()))
//            entriesDepersonalization.add(Entry(index.toFloat(), result.depersonalizationScore.toFloat()))
//            entriesReduction.add(Entry(index.toFloat(), result.personalAchievementScore.toFloat()))
//        }
//
//        xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)
//
//        val dataSetBurnout = LineDataSet(entriesBurnout, "Эмоциональное истощение").apply {
//            color = Color.RED
//            circleRadius = 5f
//            setDrawCircles(true)
//            setCircleColor(Color.RED)  // Цвет точек
//            valueTextColor = Color.BLACK
//            setDrawValues(true)
//        }
//
//        val dataSetDepersonalization = LineDataSet(entriesDepersonalization, "Деперсонализация").apply {
//            color = Color.BLUE
//            circleRadius = 5f
//            setDrawCircles(true)
//            setCircleColor(Color.BLUE)
//            valueTextColor = Color.BLACK
//            setDrawValues(true)
//        }
//
//        val dataSetReduction = LineDataSet(entriesReduction, "Редукция достижений").apply {
//            color = Color.GREEN
//            circleRadius = 5f
//            setDrawCircles(true)
//            setCircleColor(Color.GREEN)
//            valueTextColor = Color.BLACK
//            setDrawValues(true)
//        }
//
//        chart.data = LineData(dataSetBurnout, dataSetDepersonalization, dataSetReduction)
//        chart.invalidate()
//    }
//
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}