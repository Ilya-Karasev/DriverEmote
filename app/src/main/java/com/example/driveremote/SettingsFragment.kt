package com.example.driveremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentSettingsBinding
import com.example.driveremote.models.AppDatabase
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.radioGroupQuantity.setOnCheckedChangeListener { _, checkedId ->
            val isTwoTimes = checkedId == R.id.radioTwo
            binding.layoutTime2.visibility = if (isTwoTimes) View.VISIBLE else View.GONE
            binding.labelTime2.visibility = if (isTwoTimes) View.VISIBLE else View.GONE
        }

        binding.buttonSave.setOnClickListener {
            saveSettings()
        }

        binding.viewMenu.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_mainMenuFragment)
        }

        binding.viewSearch.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_searchFragment)
        }

        binding.viewRequests.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_requestsFragment)
        }
    }

    private fun saveSettings() {
        val quantity = if (binding.radioOne.isChecked) 1 else 2
        val time1 = binding.editTime1.text.toString().trim()
        val time2 = binding.editTime2.text.toString().trim()

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        if (userId != -1) {
            val db = AppDatabase.getDatabase(requireContext())
            val driverDao = db.driverDao()
            lifecycleScope.launch {
                val driver = driverDao.getDriverById(userId)
                if (driver != null) {
                    val updatedTimes = mutableListOf<String>()

                    if (quantity == 1) {
                        if (time1.isNotEmpty()) {
                            updatedTimes.add(time1)
                        } else {
                            driver.testingTime?.let { updatedTimes.addAll(it) }
                        }
                    } else {
                        if (time1.isNotEmpty()) {
                            updatedTimes.add(time1)
                        } else if (driver.testingTime?.isNotEmpty() == true) {
                            updatedTimes.add(driver.testingTime[0])
                        }

                        if (time2.isNotEmpty()) {
                            updatedTimes.add(time2)
                        } else if (driver.testingTime?.size ?: 0 > 1) {
                            updatedTimes.add(driver.testingTime?.get(1) ?: "08:00")
                        }
                    }
                    driverDao.insertDriver(driver.copy(quantity = quantity, testingTime = updatedTimes))
                }
            }
        }

        // Сохраняем состояние переключателя уведомлений
        val notifyEnabled = binding.switchNotify.isChecked
        val prefs = requireContext().getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("notificationsEnabled_$userId", notifyEnabled).apply()

        Toast.makeText(requireContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}