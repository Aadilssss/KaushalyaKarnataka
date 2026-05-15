package com.kaushalyakarnataka.app

import android.app.Application
import com.google.firebase.FirebaseApp

class KaushalyaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Standard automatic initialization from google-services.json
        FirebaseBootstrap.init(this)
    }
}
