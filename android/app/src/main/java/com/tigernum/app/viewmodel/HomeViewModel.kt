package com.tigernum.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigernum.app.data.local.entity.CountryEntity
import com.tigernum.app.data.local.entity.ServiceEntity
import com.tigernum.app.data.repository.TigerRepository
import com.tigernum.app.data.remote.dto.BuyResponseDto   // 👈 إضافة الاستيراد
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val factories: List<String> = listOf("المصنع الرئيسي", "المصنع الاحتياطي"),
    val selectedFactory: String = "المصنع الرئيسي",
    val countries: List<CountryEntity> = emptyList(),
    val selectedCountry: CountryEntity? = null,
    val services: List<ServiceEntity> = emptyList(),
    val selectedService: String = "",
    val balance: Double = 0.0,
    val currency: String = "USD",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val buySuccess: Pair<String, String>? = null // orderId, number
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TigerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(balance = repository.getBalance(), currency = repository.getCurrency()) }
        }
        // تحميل الكاش أو جلب جديد
        loadCountries()
        loadServices()
    }

    private fun loadCountries() {
        viewModelScope.launch {
            repository.fetchAndCacheCountries().onFailure { /* استخدم الكاش */ }
            repository.getCachedCountries().collect { countries ->
                _uiState.update { it.copy(countries = countries) }
            }
        }
    }

    private fun loadServices() {
        viewModelScope.launch {
            repository.fetchAndCacheServices().onFailure { /* استخدم الكاش */ }
            repository.getCachedServices().collect { services ->
                _uiState.update { it.copy(services = services) }
            }
        }
    }

    fun onFactorySelected(factory: String) {
        _uiState.update { it.copy(selectedFactory = factory) }
    }

    fun onCountrySelected(country: CountryEntity) {
        _uiState.update { it.copy(selectedCountry = country) }
    }

    fun onServiceSelected(service: String) {
        _uiState.update { it.copy(selectedService = service) }
    }

    fun buyNumber() {
        val state = _uiState.value
        val factoryCode = if (state.selectedFactory == "المصنع الرئيسي") "main" else "backup"
        val country = state.selectedCountry ?: return
        val service = state.selectedService
        if (service.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.buyNumber(factoryCode, country.code, service)
            result.onSuccess { response: BuyResponseDto ->   // 👈 تحديد النوع صراحة
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        buySuccess = Pair(response.orderId, response.number),
                        balance = repository.getBalance()
                    )
                }
            }.onFailure { e: Throwable ->   // 👈 تحديد النوع صراحة
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "حدث خطأ")
                }
            }
        }
    }

    fun resetBuySuccess() {
        _uiState.update { it.copy(buySuccess = null) }
    }
}
