package com.kaushalyakarnataka.app.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiSession(
    val isAuthLoading: Boolean = true,
    val isProfileLoading: Boolean = false,
    val uid: String? = null,
    val profile: UserProfile? = null,
    val isNewUser: Boolean = false,
    val error: String? = null
)

class MainViewModel(
    private val repo: KaushalyaRepository,
    application: Application,
) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"
    private val prefs = application.getSharedPreferences("kk_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        when (prefs.getString("lang", "en")) {
            "kn" -> AppLanguage.KN
            else -> AppLanguage.EN
        },
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        prefs.edit { putString("lang", if (lang == AppLanguage.KN) "kn" else "en") }
        _language.value = lang
    }

    private val _session = MutableStateFlow(UiSession())
    val session: StateFlow<UiSession> = _session.asStateFlow()

    private var profileJob: Job? = null

    init {
        viewModelScope.launch {
            repo.observeAuthUid().collect { uid ->
                profileJob?.cancel()
                if (uid == null) {
                    _session.update { UiSession(isAuthLoading = false) }
                } else {
                    // Auth confirmed. Enter sync state.
                    _session.update { it.copy(
                        isAuthLoading = false, 
                        isProfileLoading = true, 
                        uid = uid, 
                        profile = null,
                        isNewUser = false,
                        error = null
                    )}
                    
                    profileJob = launch {
                        // Crucial: Wait for Auth token to be ready before querying DB
                        delay(800)

                        repo.observeProfileResilient(uid)
                            .catch { e ->
                                Log.e(TAG, "Sync connection issue", e)
                                // We don't set error here; Firestore will auto-retry.
                            }
                            .collect { result ->
                                when {
                                    result.profile != null -> {
                                        // Case 1: Data found!
                                        _session.update { it.copy(
                                            profile = result.profile,
                                            isProfileLoading = false,
                                            isNewUser = false,
                                            error = null
                                        )}
                                    }
                                    result.isConfirmedNewUser -> {
                                        // Case 2: Server confirmed no account exists.
                                        _session.update { it.copy(
                                            isProfileLoading = false,
                                            isNewUser = true,
                                            profile = null,
                                            error = null
                                        )}
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    fun retrySync() {
        val uid = _session.value.uid ?: return
        _session.update { it.copy(error = null, isProfileLoading = true) }
    }

    fun signInWithGoogle(token: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                repo.signInWithGoogleIdToken(token)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun completeOnboarding(uid: String, fields: Map<String, Any>, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                repo.mergeUserProfile(uid, fields)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun updateProfile(uid: String, fields: Map<String, Any>, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                repo.mergeUserProfile(uid, fields)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun hireWorker(customerId: String, workerId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val res = repo.hireWorker(customerId, workerId)
                onResult(res)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun acceptRequest(requestId: String, workerId: String, customerId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                repo.acceptRequest(requestId, workerId, customerId)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun rejectRequest(requestId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                repo.rejectRequest(requestId)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun completeJob(requestId: String, workerId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                repo.completeJob(requestId, workerId)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun addService(uid: String, title: String, price: Double, desc: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                repo.addService(uid, title, price, desc)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun deleteService(serviceId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                repo.deleteService(serviceId)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun submitReview(workerId: String, customerId: String, rating: Int, comment: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                repo.submitReview(workerId, customerId, rating, comment)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun signOut() {
        repo.signOut()
    }

    companion object {
        fun factory(application: Application, repo: KaushalyaRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repo, application) as T
                }
            }
    }
}
