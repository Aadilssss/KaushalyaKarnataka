package com.kaushalyakarnataka.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseBootstrap {
    lateinit var auth: FirebaseAuth
        private set

    lateinit var firestore: FirebaseFirestore
        private set

    lateinit var storage: FirebaseStorage
        private set

    fun init(context: Context) {
        val app = FirebaseApp.getInstance()
        auth = FirebaseAuth.getInstance(app)
        
        // Use default instance if ID is (default)
        firestore = if (BuildConfig.FIRESTORE_DATABASE_ID == "(default)") {
            FirebaseFirestore.getInstance(app)
        } else {
            FirebaseFirestore.getInstance(app, BuildConfig.FIRESTORE_DATABASE_ID)
        }

        storage = FirebaseStorage.getInstance(app)
    }
}
