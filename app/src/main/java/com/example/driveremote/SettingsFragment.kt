package com.example.driveremote

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.api.Constants
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentSettingsBinding
import com.example.driveremote.sessionManagers.DriverSession
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    private var countDownTimer: CountDownTimer? = null

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

        val savePrefs = requireContext().getSharedPreferences("SavePrefs", Context.MODE_PRIVATE)
        val lastSavedTime = savePrefs.getLong("lastSavedTime_$userId", 0L)
        val currentTime = System.currentTimeMillis()
        val timePassed = currentTime - lastSavedTime

        if (timePassed < Constants.TIME_LIMIT.toLong()) {
            disableSaveButton(Constants.TIME_LIMIT.toLong() - timePassed)
        }

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
                val savePrefs = requireContext().getSharedPreferences("SavePrefs", Context.MODE_PRIVATE)
                savePrefs.edit().putLong("lastSavedTime_$userId", System.currentTimeMillis()).apply()
                disableSaveButton(Constants.TIME_LIMIT.toLong())
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
                    val savePrefs = requireContext().getSharedPreferences("SavePrefs", Context.MODE_PRIVATE)
                    savePrefs.edit().putLong("lastSavedTime_$userId", System.currentTimeMillis()).apply()
                    disableSaveButton(Constants.TIME_LIMIT.toLong())
                } else {
                    Toast.makeText(requireContext(), "Ошибка при сохранении настроек", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun disableSaveButton(durationMillis: Long) {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.setBackgroundColor(Color.GRAY)

        countDownTimer?.cancel() // отмена предыдущего таймера, если он был

        countDownTimer = object : CountDownTimer(durationMillis, 60_000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                val formatted = String.format("Следующее изменение через %02d:%02d:%02d", hours, minutes, seconds)
                binding.buttonSave.text = formatted
                binding.buttonSave.setTextSize(14.0F)
            }

            override fun onFinish() {
                _binding?.let {
                    it.buttonSave.isEnabled = true
                    it.buttonSave.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                    binding.buttonSave.setTextSize(16.0F)
                    it.buttonSave.text = "Сохранить настройки"
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}