package com.example.driveremote.sessionManagers

import android.content.Context
import com.example.driveremote.models.Driver
import com.google.gson.Gson

object DriverSession {
    fun saveDriver(context: Context, driver: Driver) {
        val prefs = context.getSharedPreferences("DriverSession", Context.MODE_PRIVATE)
        prefs.edit().putString("driver", Gson().toJson(driver)).apply()
    }

    fun loadDriver(context: Context): Driver? {
        val prefs = context.getSharedPreferences("DriverSession", Context.MODE_PRIVATE)
        val json = prefs.getString("driver", null)
        return json?.let { Gson().fromJson(it, Driver::class.java) }
    }

    fun clearDriver(context: Context) {
        val prefs = context.getSharedPreferences("DriverSession", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}