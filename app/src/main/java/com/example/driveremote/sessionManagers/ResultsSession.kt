package com.example.driveremote.sessionManagers
import android.content.Context
import com.example.driveremote.models.Results
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
object ResultsSession {
    fun saveResults(context: Context, results: List<Results>) {
        val prefs = context.getSharedPreferences("ResultsSession", Context.MODE_PRIVATE)
        prefs.edit().putString("results", Gson().toJson(results)).apply()
    }
    fun loadResults(context: Context): List<Results> {
        val prefs = context.getSharedPreferences("ResultsSession", Context.MODE_PRIVATE)
        val json = prefs.getString("results", null)
        return json?.let {
            val type = object : TypeToken<List<Results>>() {}.type
            Gson().fromJson(it, type)
        } ?: emptyList()
    }
    fun clearResults(context: Context) {
        val prefs = context.getSharedPreferences("ResultsSession", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}