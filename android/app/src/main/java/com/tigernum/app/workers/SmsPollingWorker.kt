package com.tigernum.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tigernum.app.data.repository.TigerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@HiltWorker
class SmsPollingWorker // // @AssistedInject constructor(
    // @Assisted context: Context,
    // @Assisted params: WorkerParameters,
    private val repository: TigerRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_ORDER_ID = "order_id"
        const val WORK_NAME_PREFIX = "sms_poll_"
    }

    override suspend fun doWork(): Result {
        val orderId = inputData.getString(KEY_ORDER_ID) ?: return Result.failure()
        var attempts = 0
        while (attempts < 24) { // محاولة لمدة دقيقتين
            if (isStopped) return Result.failure()
            delay(5000)
            val result = repository.getSmsCode(orderId)
            result.onSuccess { response ->
                if (response.status == "ok" && response.code != null) {
                    // يمكن إرسال إشعار أو تحديث via LiveData
                    return Result.success()
                }
            }
            attempts++
        }
        return Result.failure()
    }

    fun enqueue(orderId: String) {
        val data = workDataOf(KEY_ORDER_ID to orderId)
        val request = OneTimeWorkRequestBuilder<SmsPollingWorker>()
            .setInputData(data)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(WORK_NAME_PREFIX + orderId, ExistingWorkPolicy.REPLACE, request)
    }
}
