package com.tigernum.app.data.repository

import com.tigernum.app.data.remote.dto.BuyResponseDto
import com.tigernum.app.data.remote.dto.SmsCodeResponseDto
import com.tigernum.app.data.local.SessionManager
import com.tigernum.app.data.local.dao.CountryDao
import com.tigernum.app.data.local.dao.OrderDao
import com.tigernum.app.data.local.dao.ServiceDao
import com.tigernum.app.data.local.entity.CountryEntity
import com.tigernum.app.data.local.entity.OrderEntity
import com.tigernum.app.data.local.entity.ServiceEntity
import com.tigernum.app.data.remote.ApiService
import com.tigernum.app.data.remote.dto.BuyRequest
import com.tigernum.app.data.remote.dto.RegisterRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TigerRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val countryDao: CountryDao,
    private val serviceDao: ServiceDao,
    private val orderDao: OrderDao
) {
    // === المستخدم ===
    suspend fun register(name: String, email: String, phone: String): Result<Unit> {
        return try {
            val response = apiService.register(RegisterRequest(name, email, phone))
            sessionManager.saveUser(response.uuid, name, email, phone)
            sessionManager.setBalance(response.balance, response.currency)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshBalance(): Result<Double> {
        val uuid = sessionManager.getUuid() ?: return Result.failure(Exception("Not registered"))
        return try {
            val bal = apiService.getBalance(uuid)
            sessionManager.setBalance(bal.balance, bal.currency)
            Result.success(bal.balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserName(): String = sessionManager.getUserName()
    fun getUserEmail(): String = sessionManager.getUserEmail()
    fun getUserPhone(): String = sessionManager.getUserPhone()
    fun getBalance(): Double = sessionManager.getBalance()
    fun getCurrency(): String = sessionManager.getCurrency()

    fun isLoggedIn(): Boolean = sessionManager.getUuid() != null

    // === الدول ===
    suspend fun fetchAndCacheCountries(): Result<Unit> {
        return try {
            val remote = apiService.getCountries()
            val entities = remote.map { CountryEntity(it.code, it.name, it.flag, it.dialCode) }
            countryDao.deleteAll()
            countryDao.insertAll(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedCountries(): Flow<List<CountryEntity>> = countryDao.getAllCountries()

    // === الخدمات ===
    suspend fun fetchAndCacheServices(): Result<Unit> {
        return try {
            val remote = apiService.getServices()
            val entities = remote.map { ServiceEntity(it.id, it.name) }
            serviceDao.deleteAll()
            serviceDao.insertAll(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedServices(): Flow<List<ServiceEntity>> = serviceDao.getAllServices()

    // === شراء رقم ===
    suspend fun buyNumber(factory: String, country: String, service: String): Result<BuyResponseDto> {
        val uuid = sessionManager.getUuid() ?: return Result.failure(Exception("Not registered"))
        return try {
            val response = apiService.buyNumber(uuid, BuyRequest(factory, country, service))
            // حفظ الطلب محلياً
            val orderEntity = OrderEntity(
                orderId = response.orderId,
                number = response.number,
                service = service,
                country = country,
                factory = factory,
                smsCode = null,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )
            orderDao.insert(orderEntity)
            // تحديث الرصيد
            refreshBalance()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSmsCode(orderId: String): Result<SmsCodeResponseDto> {
        val uuid = sessionManager.getUuid() ?: return Result.failure(Exception("Not registered"))
        return try {
            val resp = apiService.getSmsCode(uuid, orderId)
            if (resp.status == "ok" && resp.code != null) {
                orderDao.updateSmsCode(orderId, resp.code, "completed")
            }
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOrders(): Flow<List<OrderEntity>> = orderDao.getAllOrders()

    fun logout() {
        sessionManager.clear()
        // يمكن حذف قاعدة البيانات المحلية
        // countryDao.deleteAll() ... الخ
    }
}
