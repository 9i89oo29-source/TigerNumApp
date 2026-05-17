package com.tigernum.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigernum.app.data.repository.TigerRepository
import com.tigernum.app.data.remote.dto.SmsCodeResponseDto   // 👈 إضافة الاستيراد
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuyNumberUiState(
    val orderId: String = "",
    val number: String = "",
    val smsCode: String? = null,
    val isWaiting: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class BuyNumberViewModel @Inject constructor(
    private val repository: TigerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuyNumberUiState())
    val uiState: StateFlow<BuyNumberUiState> = _uiState

    private var pollingJob: Job? = null

    fun initialize(orderId: String, number: String) {
        _uiState.update { it.copy(orderId = orderId, number = number, isWaiting = true) }
        startPolling(orderId)
    }

    private fun startPolling(orderId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                val result = repository.getSmsCode(orderId)
                result.onSuccess { response: SmsCodeResponseDto ->   // 👈 تحديد النوع صراحة
                    if (response.status == "ok" && response.code != null) {
                        _uiState.update { it.copy(smsCode = response.code, isWaiting = false) }
                        cancelPolling()
                    }
                }.onFailure { error: Throwable ->   // 👈 أيضاً تحديد نوع الخطأ (اختياري ولكن مفيد)
                    _uiState.update { it.copy(errorMessage = "فشل في جلب الكود", isWaiting = false) }
                    cancelPolling()
                }
            }
        }
    }

    private fun cancelPolling() {
        pollingJob?.cancel()
    }

    override fun onCleared() {
        cancelPolling()
        super.onCleared()
    }
}
