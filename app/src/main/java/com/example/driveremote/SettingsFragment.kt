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
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentSettingsBinding
import com.example.driveremote.sessionManagers.DriverSession
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

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        if (userId != -1) {
            val prefs = requireContext().getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
            val notifyEnabled = prefs.getBoolean("notificationsEnabled_$userId", true)
            binding.switchNotify.isChecked = notifyEnabled

            lifecycleScope.launch {
                try {
                    val driver = RetrofitClient.api.getDriverById(userId)

                    if (driver.quantity == 2) {
                        binding.radioTwo.isChecked = true
                        binding.layoutTime2.visibility = View.VISIBLE
                        binding.labelTime2.visibility = View.VISIBLE
                    } else {
                        binding.radioOne.isChecked = true
                        binding.layoutTime2.visibility = View.GONE
                        binding.labelTime2.visibility = View.GONE
                    }

                    driver.testingTime?.let {
                        if (it.isNotEmpty()) {
                            binding.editTime1.setText(it.getOrNull(0) ?: "")
                            binding.editTime2.setText(it.getOrNull(1) ?: "")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Загрузка из кэша настроек", Toast.LENGTH_SHORT).show()

                    val cachedDriver = DriverSession.loadDriver(requireContext())
                    if (cachedDriver != null) {
                        if (cachedDriver.quantity == 2) {
                            binding.radioTwo.isChecked = true
                            binding.layoutTime2.visibility = View.VISIBLE
                            binding.labelTime2.visibility = View.VISIBLE
                        } else {
                            binding.radioOne.isChecked = true
                            binding.layoutTime2.visibility = View.GONE
                            binding.labelTime2.visibility = View.GONE
                        }

                        cachedDriver.testingTime?.let {
                            if (it.isNotEmpty()) {
                                binding.editTime1.setText(it.getOrNull(0) ?: "")
                                binding.editTime2.setText(it.getOrNull(1) ?: "")
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Нет доступа к настройкам", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

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

        if (userId == -1) {
            Toast.makeText(requireContext(), "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val driver = RetrofitClient.api.getDriverById(userId)
                val updatedTimes = mutableListOf<String>()

                if (quantity == 1) {
                    if (time1.isNotEmpty()) {
                        updatedTimes.add(time1)
                    } else {
                        driver.testingTime?.let { updatedTimes.addAll(it.take(1)) }
                    }
                } else {
                    if (time1.isNotEmpty()) {
                        updatedTimes.add(time1)
                    } else {
                        driver.testingTime?.getOrNull(0)?.let { updatedTimes.add(it) }
                    }

                    if (time2.isNotEmpty()) {
                        updatedTimes.add(time2)
                    } else {
                        driver.testingTime?.getOrNull(1)?.let { updatedTimes.add(it) }
                    }
                }

                val updatedDriver = driver.copy(quantity = quantity, testingTime = updatedTimes)
                RetrofitClient.api.updateDriver(driver.id, updatedDriver)
                DriverSession.saveDriver(requireContext(), updatedDriver)

                val notifyEnabled = binding.switchNotify.isChecked
                val prefs = requireContext().getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("notificationsEnabled_$userId", notifyEnabled).apply()

                Toast.makeText(requireContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()

                val cachedDriver = DriverSession.loadDriver(requireContext())
                if (cachedDriver != null) {
                    val updatedDriver = cachedDriver.copy(
                        quantity = quantity,
                        testingTime = buildList {
                            if (quantity == 1) add(time1)
                            else {
                                add(time1)
                                add(time2)
                            }
                        }
                    )
                    DriverSession.saveDriver(requireContext(), updatedDriver)

                    val notifyEnabled = binding.switchNotify.isChecked
                    val prefs = requireContext().getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("notificationsEnabled_$userId", notifyEnabled).apply()

                    Toast.makeText(requireContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Ошибка при сохранении настроек", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}