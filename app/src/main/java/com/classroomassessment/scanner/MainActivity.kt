package com.classroomassessment.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.classroomassessment.scanner.feature.app.ClassroomScannerApp
import com.classroomassessment.scanner.ui.ScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ScannerApplication).container
        setContent {
            ScannerTheme {
                ClassroomScannerApp(container)
            }
        }
    }
}
