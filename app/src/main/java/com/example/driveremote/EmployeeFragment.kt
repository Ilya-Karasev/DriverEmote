package com.example.driveremote

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driveremote.databinding.FragmentEmployeeBinding
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Results
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
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

        val args = arguments
        val fullName = args?.getString("fullName") ?: "Имя не указано"
        val age = args?.getInt("age") ?: 0
        val email = args?.getString("email") ?: "email не указан"
        val post = args?.getString("post") ?: "ДОЛЖНОСТЬ"
        val userId = args?.getInt("userId") ?: -1

        binding.profileFullName.text = fullName
        binding.profileInfo.text = "Возраст: $age год(а) / лет\nПочта: $email"

        binding.profileIcon.setImageResource(
            if (post == "ВОДИТЕЛЬ") R.drawable.driver else R.drawable.manager
        )

        if (userId != -1) {
            loadResults(userId)
        }

        binding.iconLeft.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.iconRight.setOnClickListener {
            val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()
            findNavController().navigate(R.id.action_employeeFragment_to_signInFragment)
        }

        binding.textExit.setOnClickListener {
            findNavController().navigate(R.id.action_employeeFragment_to_managerMenuFragment)
        }
    }

    private fun loadResults(userId: Int) {
        val db = AppDatabase.getDatabase(requireContext())
        val resultsDao = db.resultsDao()

        lifecycleScope.launch {
            resultsList = resultsDao.getResultsByUser(userId)
            resultsAdapter = TestResultAdapter(resultsList)
            binding.recyclerViewResults.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewResults.adapter = resultsAdapter
            resultsAdapter.notifyDataSetChanged()
            setupChart()
        }
    }

    private fun setupChart() {
        val chart = binding.lineChart

        val adapterResults = resultsAdapter.results
        if (adapterResults.isEmpty()) {
            chart.clear()
            return
        }

        val entriesBurnout = ArrayList<Entry>()
        val entriesDepersonalization = ArrayList<Entry>()
        val entriesReduction = ArrayList<Entry>()
        val dateLabels = ArrayList<String>()

        val sortedResultsList = adapterResults.sortedBy { it.testDate }

        sortedResultsList.forEachIndexed { index, result ->
            val date = result.testDate.split(" ")[0]
            val formattedDate = SimpleDateFormat("dd.MM", Locale.getDefault())
                .format(SimpleDateFormat("yyyy-MM-dd").parse(date) ?: Date())
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}