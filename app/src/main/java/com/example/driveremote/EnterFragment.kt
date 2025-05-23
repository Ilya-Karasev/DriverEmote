package com.example.driveremote
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.databinding.FragmentEnterBinding
import com.example.driveremote.models.Post
import com.example.driveremote.sessionManagers.DriverSession
import com.example.driveremote.sessionManagers.ResultsSession
import kotlinx.coroutines.launch
class EnterFragment : Fragment() {
    private var _binding: FragmentEnterBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be accessed after destroying view")
    private val apiService = RetrofitClient.api
    private val sharedPreferences by lazy {
        requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnterBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageEnterIcon.setOnClickListener {
            val userId = sharedPreferences.getInt("userId", -1)
            if (userId == -1) {
                findNavController().navigate(R.id.action_enterFragment_to_signInFragment)
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    val user = RetrofitClient.api.getUserById(userId)
                    when (user.post) {
                        Post.РУКОВОДИТЕЛЬ -> findNavController().navigate(R.id.action_enterFragment_to_managerMenuFragment)
                        Post.ВОДИТЕЛЬ -> syncOfflineData(userId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    val postName = sharedPreferences.getString("post", null)
                    val post = postName?.let { Post.valueOf(it) }
                    if (post != null) {
                        when (post) {
                            Post.РУКОВОДИТЕЛЬ -> findNavController().navigate(R.id.action_enterFragment_to_managerMenuFragment)
                            Post.ВОДИТЕЛЬ -> syncOfflineData(userId)
                        }
                    } else {
                        sharedPreferences.edit().clear().apply()
                        findNavController().navigate(R.id.action_enterFragment_to_signInFragment)
                    }
                }
            }
        }
    }
    private fun syncOfflineData(userId: Int) {
        lifecycleScope.launch {
            try {
                val localDriver = DriverSession.loadDriver(requireContext())
                if (localDriver != null) {
                    apiService.updateDriver(userId, localDriver)
                    Log.d("EnterFragment", "Driver synced with server")
                }
                val offlineResults = ResultsSession.loadResults(requireContext())
                    .filter { it.id == 0 }
                for (result in offlineResults) {
                    apiService.addResult(result)
                    Log.d("EnterFragment", "Offline result sent: ${result.testDate}")
                }
                if (offlineResults.isNotEmpty()) {
                    val syncedResults = apiService.getResultsByUser(userId)
                    ResultsSession.saveResults(requireContext(), syncedResults)
                }
            } catch (e: Exception) {
                Log.e("EnterFragment", "Error syncing offline data", e)
            }
            findNavController().navigate(R.id.action_enterFragment_to_mainMenuFragment)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}