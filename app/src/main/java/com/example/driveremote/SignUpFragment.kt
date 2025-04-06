package com.example.driveremote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentSignUpBinding
import kotlinx.coroutines.launch
import android.util.Patterns
import android.widget.AdapterView
import com.example.driveremote.models.AppDatabase
import com.example.driveremote.models.Driver
import com.example.driveremote.models.Post
import com.example.driveremote.models.User

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val userDao = db.userDao()
        val driverDao = db.driverDao()

        val postAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            Post.values().map { it.name }
        )
        binding.spinnerPost.adapter = postAdapter

        val textFields = listOf(
            binding.editTextSurName,
            binding.editTextFirstName,
            binding.editTextFatherName,
            binding.editTextAge,
            binding.editTextEmail,
            binding.editTextPassword
        )

        textFields.forEach { editText ->
            editText.addTextChangedListener {
                updateRegisterButtonState()
            }
        }

        binding.spinnerPost.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateRegisterButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                updateRegisterButtonState()
            }
        }

        binding.viewRegister.setOnClickListener {
            val surName = binding.editTextSurName.text.toString().trim()
            val firstName = binding.editTextFirstName.text.toString().trim()
            val fatherName = binding.editTextFatherName.text.toString().trim()
            val age = binding.editTextAge.text.toString().toIntOrNull() ?: 0
            val post = Post.valueOf(binding.spinnerPost.selectedItem.toString())
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (!isValidEmail(email)) {
                Toast.makeText(requireContext(), "Некорректный email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = User(
                surName = surName,
                firstName = firstName,
                fatherName = fatherName,
                age = age,
                post = post,
                email = email,
                password = password
            )

            lifecycleScope.launch {
                userDao.insertUser(user)
                // Получаем созданного пользователя с помощью email и пароля
                val createdUser = userDao.getUserByEmailAndPassword(email, password)

                // Если пользователь — водитель, создаем соответствующую запись в Driver
                if (createdUser != null && createdUser.post == Post.ВОДИТЕЛЬ) {
                    val driver = Driver(
                        id = createdUser.id,
                        isCompleted = false,
                        testingTime = null,
                        quantity = 1
                    )
                    driverDao.insertDriver(driver)
                }

                Toast.makeText(requireContext(), "Пользователь зарегистрирован", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
            }

        }

        binding.viewBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun updateRegisterButtonState() {
        val surName = binding.editTextSurName.text?.isNotEmpty() == true
        val firstName = binding.editTextFirstName.text?.isNotEmpty() == true
        val fatherName = binding.editTextFatherName.text?.isNotEmpty() == true
        val ageValid = binding.editTextAge.text?.toString()?.toIntOrNull() != null
        val emailValid = isValidEmail(binding.editTextEmail.text?.toString() ?: "")
        val passwordValid = (binding.editTextPassword.text?.length ?: 0) >= 6
        val postSelected = binding.spinnerPost.selectedItem != null

        val allFieldsFilled = surName && firstName && fatherName && ageValid && emailValid && passwordValid && postSelected

        binding.viewRegister.isEnabled = allFieldsFilled
        binding.viewRegister.setBackgroundResource(
            if (allFieldsFilled) R.color.green else R.color.gray
        )
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}