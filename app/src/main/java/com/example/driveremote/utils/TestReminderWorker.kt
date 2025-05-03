package com.example.driveremote.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.driveremote.api.RetrofitClient
import com.example.driveremote.models.AppDatabase
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

        if (userId != -1) {
            val prefs = context.getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
            val lastNotificationTime = prefs.getLong("lastNotificationTime_${userId}_$timeString", 0L)
            val now = System.currentTimeMillis()

            try {
                // Получаем данные пользователя через Retrofit
                val user = RetrofitClient.api.getUserById(userId)
                if (user.post != Post.ВОДИТЕЛЬ) {
                    Log.d("TestReminderWorker", "User is not a driver or doesn't exist")
                    return Result.success()
                }

                // Получаем данные водителя через Retrofit
                val driver = RetrofitClient.api.getDriverById(userId)
                if (driver == null) {
                    Log.d("TestReminderWorker", "Driver not found")
                    return Result.success()
                }

                // Проверка включенности уведомлений
                val notificationsEnabled = prefs.getBoolean("notificationsEnabled_$userId", true)
                if (!notificationsEnabled) {
                    Log.d("TestReminderWorker", "Notifications are disabled for user $userId")
                    return Result.success()
                }

                // Проверка — если меньше 1 часа с последнего уведомления, не показываем снова
                if (now - lastNotificationTime < 60 * 60 * 1000) {
                    Log.d("TestReminderWorker", "Notification suppressed (already sent recently)")
                    return Result.success()
                }

                // Сбрасываем флаг прохождения теста и отправляем уведомление
                val updatedDriver = driver.copy()
                RetrofitClient.api.updateDriver(userId, updatedDriver)

                NotificationUtils.sendNotification(
                    context,
                    "Пришло время тестирования!",
                    "Пожалуйста, пройдите тестирование, чтобы отследить ваше эмоциональное состояние!"
                )

                // Сохраняем метку времени отправки
                prefs.edit().putLong("lastNotificationTime_${userId}_$timeString", now).apply()

                // Устанавливаем следующее напоминание
                scheduleNextReminder(context, userId, timeString)

                return Result.success()
            } catch (e: Exception) {
                Log.e("TestReminderWorker", "Error in TestReminderWorker: ${e.localizedMessage}")
                return Result.failure()
            }
        }

        return Result.failure()
    }

    private fun scheduleNextReminder(context: Context, userId: Int, timeString: String) {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            time = formatter.parse(timeString) ?: return
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1) // на следующий день
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

        // Отменяем все работы с этим тегом перед постановкой новой задачи
        WorkManager.getInstance(context).cancelAllWorkByTag("test_reminder_${userId}_$timeString")

        // Планируем задачу
        WorkManager.getInstance(context).enqueue(workRequest)

        Log.d("TestReminderWorker", "Next reminder scheduled for $timeString")
    }
}