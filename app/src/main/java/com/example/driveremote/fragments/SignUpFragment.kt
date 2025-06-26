package com.example.driveremote.fragments
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentSignUpBinding
import kotlinx.coroutines.launch
import android.util.Patterns
import androidx.core.content.ContextCompat
import com.example.driveremote.R
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.models.Driver
import com.example.driveremote.models.Manager
import com.example.driveremote.models.Post
import com.example.driveremote.models.User
class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")
    private var selectedPost: Post? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val textFields = listOf(
            binding.editTextSurName,
            binding.editTextFirstName,
            binding.editTextFatherName,
            binding.editTextAge,
            binding.editTextEmail,
            binding.editTextPassword
        )
        textFields.forEach { editText ->
            editText.addTextChangedListener { updateRegisterButtonState() }
        }
        binding.buttonDriver.setOnClickListener {
            selectPost(Post.ВОДИТЕЛЬ)
        }
        binding.buttonManager.setOnClickListener {
            selectPost(Post.РУКОВОДИТЕЛЬ)
        }
        binding.textRegisterButton.setOnClickListener {
            val surName = binding.editTextSurName.text.toString().trim()
            val firstName = binding.editTextFirstName.text.toString().trim()
            val fatherName = binding.editTextFatherName.text.toString().trim()
            val age = binding.editTextAge.text.toString().toIntOrNull() ?: 0
            val post = selectedPost
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            if (post == null) {
                Toast.makeText(requireContext(), "Выберите роль, нажав на белую кнопку", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isValidEmail(email)) {
                Toast.makeText(requireContext(), "Email не введён или имеет некорректный формат", Toast.LENGTH_SHORT).show()
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
                try {
                    val createdUser = RetrofitClient.api.createUser(user)
                    if (post == Post.ВОДИТЕЛЬ) {
                        val driver = Driver(
                            id = createdUser.id,
                            isCompleted = false,
                            testingTime = listOf("08:00"),
                            quantity = 1
                        )
                        RetrofitClient.api.saveDriver(driver)
                    }
                    if (post == Post.РУКОВОДИТЕЛЬ) {
                        val manager = Manager(
                            user = createdUser,
                            employeesList = emptyList()
                        )
                        RetrofitClient.api.saveManager(manager)
                    }
                    Toast.makeText(requireContext(), "Пользователь зарегистрирован", Toast.LENGTH_SHORT).show()
                    Log.d("SignInFragment", "Регистрация успешна: ${createdUser.id}, ${createdUser.surName} ${createdUser.firstName} ${createdUser.fatherName}")
                    findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка регистрации: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
        binding.viewBack.setOnClickListener {
            findNavController().navigateUp()
        }
        updateRegisterButtonState()
    }
    private fun selectPost(post: Post) {
        selectedPost = post
        updatePostButtons()
        updateRegisterButtonState()
    }
    private fun updatePostButtons() {
        val green = ContextCompat.getColor(requireContext(), R.color.dark_green)
        val white = ContextCompat.getColor(requireContext(), R.color.white)
        if (selectedPost == Post.ВОДИТЕЛЬ) {
            binding.buttonDriver.apply {
                backgroundTintList = ColorStateList.valueOf(green)
                setTextColor(white)
                isEnabled = false
            }
            binding.buttonManager.apply {
                backgroundTintList = ColorStateList.valueOf(white)
                setTextColor(green)
                isEnabled = true
            }
        } else if (selectedPost == Post.РУКОВОДИТЕЛЬ) {
            binding.buttonManager.apply {
                backgroundTintList = ColorStateList.valueOf(green)
                setTextColor(white)
                isEnabled = false
            }
            binding.buttonDriver.apply {
                backgroundTintList = ColorStateList.valueOf(white)
                setTextColor(green)
                isEnabled = true
            }
        }
    }
    private fun updateRegisterButtonState() {
        val surName = binding.editTextSurName.text?.isNotEmpty() == true
        val firstName = binding.editTextFirstName.text?.isNotEmpty() == true
        val fatherName = binding.editTextFatherName.text?.isNotEmpty() == true
        val ageValid = binding.editTextAge.text?.toString()?.toIntOrNull() != null
        val emailValid = isValidEmail(binding.editTextEmail.text?.toString() ?: "")
        val passwordValid = (binding.editTextPassword.text?.length ?: 0) >= 6
        val postSelected = selectedPost != null
        val allFieldsFilled = surName && firstName && fatherName && ageValid && emailValid && passwordValid && postSelected
        binding.viewRegister.isEnabled = allFieldsFilled
        binding.textRegisterButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                if (allFieldsFilled) R.color.dark_green else R.color.gray
            )
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