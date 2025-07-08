package org.bidon.demoapp.ui.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.bidon.sdk.BidonSdk

class SdkStateViewModel : ViewModel() {

    private val _isInitialized = MutableStateFlow(BidonSdk.isInitialized())
    val isInitialized: StateFlow<Boolean> = _isInitialized

    fun notifySdkInitialized() {
        _isInitialized.value = true
    }
}