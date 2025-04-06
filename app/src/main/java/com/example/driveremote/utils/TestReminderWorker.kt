package com.example.driveremote.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.driveremote.models.AppDatabase
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
            val db = AppDatabase.getDatabase(context)
            val driverDao = db.driverDao()
            val driver = driverDao.getDriverById(userId)

            driver?.let {
                // Сбрасываем isCompleted
                driverDao.updateCompletionStatus(userId, false)

                // Отправляем уведомление
                NotificationUtils.sendNotification(
                    context,
                    "Пришло время тестирования!",
                    "Пожалуйста, пройдите тестирование, чтобы отследить ваше эмоциональное состояние!"
                )

                // Переустанавливаем задачу на следующий день
                scheduleNextReminder(context, userId, timeString)
            }

            return Result.success()
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

        WorkManager.getInstance(context).enqueue(workRequest)

        Log.d("TestReminderWorker", "Next reminder scheduled for $timeString")
    }
}