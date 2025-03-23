package com.example.driveremote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.driveremote.databinding.FragmentSignInBinding
import androidx.room.*
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surName: String,
    val firstName: String,
    val fatherName: String,
    val age: Int,
    val post: Post,
    val email: String,
    val password: String
)

enum class Post {
    ВОДИТЕЛЬ, РУКОВОДИТЕЛЬ
}

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM User")
    suspend fun getAllUsers(): List<User>  // Correct return type

    @Query("SELECT * FROM User WHERE email = :email AND password = :password LIMIT 1")
    suspend fun getUserByEmailAndPassword(email: String, password: String): User?  // Correct return type
}

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

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

        val db = AppDatabase.getDatabase(requireContext())
        val userDao = db.userDao()

        // Проверка, авторизован ли уже пользователь
        if (isUserLoggedIn()) {
            findNavController().navigate(R.id.action_signInFragment_to_mainMenuFragment)
            return
        }

        lifecycleScope.launch {
            val users = userDao.getAllUsers()
            if (users.isEmpty()) {
                userDao.insertUser(User(0, "Иванов", "Иван", "Иванович", 30, Post.ВОДИТЕЛЬ, "driver@example.com", "driver123"))
                userDao.insertUser(User(0, "Петров", "Петр", "Петрович", 40, Post.РУКОВОДИТЕЛЬ, "manager@example.com", "manager123"))
            }
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
                val user = userDao.getUserByEmailAndPassword(email, password)

                if (user != null) {
                    saveUserSession(user)  // Сохраняем пользователя в SharedPreferences
                    findNavController().navigate(R.id.action_signInFragment_to_mainMenuFragment)
                } else {
                    Toast.makeText(requireContext(), "Неверные логин или пароль", Toast.LENGTH_SHORT).show()
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