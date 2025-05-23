package com.example.driveremote.utils
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.driveremote.api.Constants
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.models.Post
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
class TestReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val userId = inputData.getInt("userId", -1)
        val timeString = inputData.getString("time") ?: return Result.failure()
        if (userId == -1) return Result.failure()
        val prefs = context.getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
        val lastNotificationTime = prefs.getLong("lastNotificationTime_${userId}_$timeString", 0L)
        val now = System.currentTimeMillis()
        return try {
            val user = RetrofitClient.api.getUserById(userId)
            if (user.post != Post.ВОДИТЕЛЬ) {
                Log.d("TestReminderWorker", "User is not a driver")
                return Result.success()
            }
            val driver = RetrofitClient.api.getDriverByUserId(userId)
            val notificationsEnabled = prefs.getBoolean("notificationsEnabled_$userId", true)
            if (!notificationsEnabled) {
                Log.d("TestReminderWorker", "Notifications are disabled for user $userId")
                val updatedDriver = driver.copy(isCompleted = false)
                RetrofitClient.api.updateDriver(driver.id, updatedDriver)
                return Result.success()
            }
            if (now - lastNotificationTime < Constants.TIME_LIMIT) {
                Log.d("TestReminderWorker", "Notification suppressed (already sent recently)")
                return Result.success()
            }
            val updatedDriver = driver.copy(isCompleted = false)
            RetrofitClient.api.updateDriver(driver.id, updatedDriver)
            NotificationUtils.sendNotification(
                context,
                "Пришло время тестирования!",
                "Пожалуйста, пройдите тестирование, чтобы отследить ваше эмоциональное состояние!"
            )
            prefs.edit().putLong("lastNotificationTime_${userId}_$timeString", now).apply()
            scheduleNextReminder(context, userId, timeString)
            Result.success()
        } catch (e: Exception) {
            Log.e("TestReminderWorker", "Error during reminder check", e)
            Result.retry()
        }
    }
    private fun scheduleNextReminder(context: Context, userId: Int, timeString: String) {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            time = formatter.parse(timeString) ?: return
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = calendar.timeInMillis - System.currentTimeMillis()
        val inputData = Data.Builder()
            .putInt("userId", userId)
            .putString("time", timeString)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<TestReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("test_reminder_${userId}_$timeString")
            .build()
        WorkManager.getInstance(context).cancelAllWorkByTag("test_reminder_${userId}_$timeString")
        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d("TestReminderWorker", "Next reminder scheduled for $timeString")
    }
}