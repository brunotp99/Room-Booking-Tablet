package com.example.apptablet

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.tapadoo.alerter.Alerter
import org.json.JSONException

public fun alertaDanger(activity: Activity, title: String, message: String){
    Alerter.create(activity)
        .setTitle(title)
        .setText(message)
        .setIcon(R.drawable.ic_error)
        .setIconColorFilter(0)
        .setBackgroundColorRes(R.color.danger_bg)
        .setDuration(5000)
        .show()
}

public fun alertaSuccess(activity: Activity, title: String, message: String){
    Alerter.create(activity)
        .setTitle(title)
        .setText(message)
        .setIcon(R.drawable.ic_check)
        .setIconColorFilter(0)
        .setBackgroundColorRes(R.color.default_bg)
        .setDuration(5000)
        .show()
}