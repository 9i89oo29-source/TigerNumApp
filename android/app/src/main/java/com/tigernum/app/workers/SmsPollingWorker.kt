package com.tigernum.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tigernum.app.data.repository.TigerRepository
import com.tigernum.app.data.remote.dto.SmsCodeResponseDto   // 👈 إضافة الاستيراد
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@HiltWorker
class SmsPollingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TigerRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_ORDER_ID = "order_id"
        const val WORK_NAME_PREFIX = "sms_poll_"

        fun enqueue(context: Context, orderId: String) {
            val data = workDataOf(KEY_ORDER_ID to orderId)
            val request = OneTimeWorkRequestBuilder<SmsPollingWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME_PREFIX + orderId, ExistingWorkPolicy.REPLACE, request)
        }
    }

    override suspend fun doWork(): Result {
        val orderId = inputData.getString(KEY_ORDER_ID) ?: return Result.failure()
        var attempts = 0
        while (attempts < 24) { // محاولة لمدة دقيقتين
            if (isStopped) return Result.failure()
            delay(5000)
            val result = repository.getSmsCode(orderId)
            result.onSuccess { response: SmsCodeResponseDto ->   // 👈 تحديد النوع صراحة
                if (response.status == "ok" && response.code != null) {
                    return Result.success()
                }
            }
            attempts++
        }
        return Result.failure()
    }
}
