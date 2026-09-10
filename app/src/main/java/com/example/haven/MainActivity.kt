package com.example.haven

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.haven.presentation.navigation.HavenNavHost
import com.example.haven.ui.theme.HAVENTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.haven.core.notification.HavenNotificationManager.initChannels(applicationContext)
        com.example.haven.data.repository.FamilyRepository.instance.init(applicationContext)
        val familyId = com.example.haven.data.repository.FamilyRepository.instance.currentFamily.value?.id ?: ""
        if (familyId.isNotBlank()) {
            com.example.haven.data.sync.HavenSyncManager.getInstance(applicationContext).syncAll(familyId, applicationContext)
        }
        enableEdgeToEdge()
        setContent {
            HAVENTheme {
                HavenNavHost()
            }
        }
    }
}