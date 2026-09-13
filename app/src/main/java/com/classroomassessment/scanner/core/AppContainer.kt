package com.classroomassessment.scanner.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.classroomassessment.scanner.BuildConfig
import com.classroomassessment.scanner.core.db.ScannerDatabase
import com.classroomassessment.scanner.core.network.BackendDataSource
import com.classroomassessment.scanner.core.network.BackendRepository
import com.classroomassessment.scanner.core.network.EncryptedTokenStore
import com.classroomassessment.scanner.core.network.MockBackendDataSource
import com.classroomassessment.scanner.core.network.RetrofitBackendDataSource
import com.classroomassessment.scanner.core.network.TokenStore
import com.classroomassessment.scanner.core.scan.CaptureFileStore
import com.classroomassessment.scanner.core.scan.ScanQueueRepository
import com.classroomassessment.scanner.core.scan.UploadScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: ScannerDatabase = ScannerDatabase.create(appContext)
    val tokenStore: TokenStore = EncryptedTokenStore(appContext)
    val dataSource: BackendDataSource = if (BuildConfig.MOCK_BACKEND_ENABLED) {
        MockBackendDataSource { hasValidatedInternet() }
    } else {
        RetrofitBackendDataSource(BuildConfig.API_BASE_URL, tokenStore)
    }
    val backend = BackendRepository(dataSource, tokenStore)
    val uploadScheduler = UploadScheduler(appContext)
    val fileStore = CaptureFileStore(appContext)
    val scanQueue = ScanQueueRepository(database.scanDao(), uploadScheduler, fileStore)

    init {
        uploadScheduler.recoverPendingWork(database.scanDao())
    }

    private fun hasValidatedInternet(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
