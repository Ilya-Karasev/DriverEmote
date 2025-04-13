package com.example.driveremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentProfileBinding
import com.example.driveremote.models.AppDatabase
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        val surname = sharedPreferences.getString("surName", "Неизвестно") ?: "Неизвестно"
        val firstName = sharedPreferences.getString("firstName", "Неизвестно") ?: "Неизвестно"
        val fatherName = sharedPreferences.getString("fatherName", "Неизвестно") ?: "Неизвестно"
        val age = sharedPreferences.getInt("age", 0)
        val email = sharedPreferences.getString("email", "Не указан") ?: "Не указан"
        val post = sharedPreferences.getString("post", "Неизвестно") ?: "Неизвестно"

        // Подключение нужного layout в зависимости от роли
        val inflater = layoutInflater
        val view1Container = binding.view1
        view1Container.removeAllViews()

        if (post == "ВОДИТЕЛЬ") {
            val driverView = inflater.inflate(R.layout.item_driver_view1, view1Container, false)
            view1Container.addView(driverView)
            binding.buttonTime.visibility = View.VISIBLE // Показываем кнопку для водителя
        } else if (post == "РУКОВОДИТЕЛЬ") {
            val managerView = inflater.inflate(R.layout.item_manager_view1, view1Container, false)
            view1Container.addView(managerView)
            binding.buttonTime.visibility = View.GONE // Скрываем кнопку для руководителя
        } else {
            binding.buttonTime.visibility = View.GONE // На всякий случай скрываем кнопку, если роль не определена
        }

        binding.profileFullName.text = "$surname $firstName $fatherName"

        val iconResId = if (post == "РУКОВОДИТЕЛЬ") R.drawable.manager else R.drawable.driver
        binding.profileIcon.setImageResource(iconResId)

        binding.profileInfo.text = """
        Возраст: $age год(а) / лет
        Эл. почта: $email
    """.trimIndent()

        binding.iconLeft.setOnClickListener {
            requireActivity().finish()
        }

        binding.iconRight.setOnClickListener {
            sharedPreferences.edit().clear().apply()
            findNavController().navigate(R.id.action_profileFragment_to_signInFragment)
        }

        binding.view1.setOnClickListener {
            if (post == "ВОДИТЕЛЬ") {
                findNavController().navigate(R.id.action_profileFragment_to_mainMenuFragment)
            } else if (post == "РУКОВОДИТЕЛЬ") {
                findNavController().navigate(R.id.action_profileFragment_to_managerMenuFragment)
            }
        }

        binding.view2.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_searchFragment)
        }

        binding.buttonTime.setOnClickListener {
            showTimeDialog()
        }

        binding.buttonRequests.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_requestsFragment)
        }
    }

    private fun showTimeDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_driver_time, null)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupQuantity)
        val radioOne = dialogView.findViewById<RadioButton>(R.id.radioOne)
        val radioTwo = dialogView.findViewById<RadioButton>(R.id.radioTwo)

        val layoutTime2 = dialogView.findViewById<View>(R.id.timePicker2)
        val labelTime2 = dialogView.findViewById<View>(R.id.textTime2Label)

        val timePicker1 = dialogView.findViewById<TimePicker>(R.id.timePicker1)
        val timePicker2 = dialogView.findViewById<TimePicker>(R.id.timePicker2)

        timePicker1.setIs24HourView(true)
        timePicker2.setIs24HourView(true)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val isTwo = checkedId == R.id.radioTwo
            timePicker2.visibility = if (isTwo) View.VISIBLE else View.GONE
            labelTime2.visibility = if (isTwo) View.VISIBLE else View.GONE
        }

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.buttonSave).setOnClickListener {
            val quantity = if (radioOne.isChecked) 1 else 2
            val hour1 = timePicker1.hour
            val minute1 = timePicker1.minute
            val time1 = String.format("%02d:%02d", hour1, minute1)

            val times = if (quantity == 1) {
                listOf(time1)
            } else {
                val hour2 = timePicker2.hour
                val minute2 = timePicker2.minute
                val time2 = String.format("%02d:%02d", hour2, minute2)
                listOf(time1, time2)
            }

            val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("userId", -1)

            if (userId != -1) {
                val db = AppDatabase.getDatabase(requireContext())
                val driverDao = db.driverDao()
                lifecycleScope.launch {
                    val driver = driverDao.getDriverById(userId)
                    if (driver != null) {
                        driverDao.insertDriver(driver.copy(quantity = quantity, testingTime = times))
                    }
                }
            }

            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}