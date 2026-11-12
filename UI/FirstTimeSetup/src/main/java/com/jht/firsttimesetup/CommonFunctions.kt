package com.jht.firsttimesetup

import android.content.Context
import android.widget.Toast

object CommonFunctions {
    val TWENTY_FOUR_HOUR_INDEX = 1 //This is used thorughtout the app

    fun getDefalutTimeZone(): Int = 9// Central Time // the best time

    /**
     * Shows a toast message
     */
    fun showToast(context: Context, message: String, length: Int = Toast.LENGTH_SHORT ) {
        Toast.makeText(context,message, length).show()
    }
}