package com.manspower.jumpmeasure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.manspower.jumpmeasure.theme.JumpMeasureTheme
import com.manspower.jumpmeasure.ui.language.AppLanguage
import com.manspower.jumpmeasure.ui.language.EnglishStrings
import com.manspower.jumpmeasure.ui.language.IndonesianStrings
import com.manspower.jumpmeasure.ui.language.LanguageManager
import com.manspower.jumpmeasure.ui.language.LocalAppStrings

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      // Observe language changes and provide the correct strings to the whole tree
      val language by LanguageManager.language.collectAsState()
      val strings = if (language == AppLanguage.INDONESIAN) IndonesianStrings else EnglishStrings

      CompositionLocalProvider(LocalAppStrings provides strings) {
        JumpMeasureTheme {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            MainNavigation()
          }
        }
      }
    }
  }
}
