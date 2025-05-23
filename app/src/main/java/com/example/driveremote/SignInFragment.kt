package com.example.driveremote
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentSignInBinding
import com.example.driveremote.models.Post
import com.example.driveremote.models.User
import kotlinx.coroutines.launch
import retrofit2.HttpException
class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isUserLoggedIn()) {
            findNavController().navigate(R.id.action_signInFragment_to_mainMenuFragment)
            return
        }
        binding.buttonSignIn.isEnabled = false
        binding.buttonSignIn.setBackgroundColor(Color.GRAY)
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val email = binding.editTextEmail.text.toString().trim()
                val password = binding.editTextPassword.text.toString().trim()
                val isValid = email.isNotEmpty() && password.length >= 6
                binding.buttonSignIn.isEnabled = isValid
                binding.buttonSignIn.setBackgroundColor(
                    if (isValid) ContextCompat.getColor(requireContext(), R.color.green)
                    else Color.GRAY
                )
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.editTextEmail.addTextChangedListener(textWatcher)
        binding.editTextPassword.addTextChangedListener(textWatcher)
        binding.buttonSignIn.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            if (email.isEmpty() || password.length < 6) {
                Toast.makeText(requireContext(), "Введите корректные данные", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    val user = RetrofitClient.api.loginUser(email, password)
                    if (user != null) {
                        saveUserSession(user)
                        if (user.post == Post.ВОДИТЕЛЬ) {
                            val reminderPrefs = requireContext().getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
                            reminderPrefs.edit().putBoolean("notificationsEnabled_${user.id}", true).apply()
                            findNavController().navigate(R.id.action_signInFragment_to_mainMenuFragment)
                        } else if (user.post == Post.РУКОВОДИТЕЛЬ) {
                            findNavController().navigate(R.id.action_signInFragment_to_managerMenuFragment)
                        }
                        Log.d("SignInFragment", "Авторизация успешна: ${user.id}, ${user.surName} ${user.firstName} ${user.fatherName}")
                    } else {
                        Toast.makeText(requireContext(), "Неверные логин или пароль", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: HttpException) {
                    if (e.code() == 401) {
                        Toast.makeText(requireContext(), "Неверные логин или пароль", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Требуется подключение к интернету и серверу", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("SignInFragment", "Login error", e)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Требуется подключение к интернету и серверу", Toast.LENGTH_SHORT).show()
                    Log.e("SignInFragment", "Login error", e)
                }
            }
        }
        binding.textExit.setOnClickListener {
            requireActivity().finish()
        }
        binding.textRegister.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }
    }
    private fun saveUserSession(user: User) {
        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt("userId", user.id)
            putString("surName", user.surName)
            putString("firstName", user.firstName)
            putString("fatherName", user.fatherName)
            putInt("age", user.age)
            putString("post", user.post.name)
            putString("email", user.email)
            apply()
        }
    }
    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        return sharedPreferences.contains("userId")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}