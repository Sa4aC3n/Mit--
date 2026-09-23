package com.example.data.security

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Manages Firebase App Check initialization across Debug and Production environments.
 * Follows Phase A (Monitoring) and Phase B (Attestation) without breaking development builds.
 *
 * Debug builds use DebugAppCheckProviderFactory (never committing debug tokens to source control).
 * Production release builds use PlayIntegrityAppCheckProviderFactory (Google Play Integrity API).
 */
object MetGhamrAppCheckManager {

    private const val TAG = "MetGhamrAppCheck"
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            FirebaseApp.initializeApp(context)
            val firebaseAppCheck = FirebaseAppCheck.getInstance()

            val providerFactory = if (BuildConfig.DEBUG) {
                Log.d(TAG, "Initializing Firebase App Check with Debug provider")
                DebugAppCheckProviderFactory.getInstance()
            } else {
                Log.d(TAG, "Initializing Firebase App Check with Google Play Integrity provider")
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }

            firebaseAppCheck.installAppCheckProviderFactory(providerFactory)
            isInitialized = true
            Log.i(TAG, "Firebase App Check initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase App Check initialization non-fatal warning: ${e.message}")
        }
    }
}
