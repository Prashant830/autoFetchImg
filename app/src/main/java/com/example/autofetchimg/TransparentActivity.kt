package com.example.autofetchimg

import android.app.Activity
import android.os.Bundle

class TransparentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Finish the activity immediately
        finish()
    }
}