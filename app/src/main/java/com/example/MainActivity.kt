package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import com.example.ui.AuthUiState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: AppViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val lifecycleOwner = LocalLifecycleOwner.current
      val brightTheme by viewModel.currentBrightTheme.collectAsStateWithLifecycle()
      val authState by viewModel.authState.collectAsStateWithLifecycle()

      DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.onAppResumed()
          }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
          lifecycleOwner.lifecycle.removeObserver(observer)
        }
      }

      LaunchedEffect(Unit) {
        viewModel.actionFeedback.collect { message ->
          Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
      }

      MyApplicationTheme(brightBgColor = Color(brightTheme.bgHex)) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          when (authState) {
            is AuthUiState.Loading -> {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                CircularProgressIndicator(color = GreenPrimary)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                  text = "Menghubungkan ke Supabase Cloud...",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.sp,
                  color = GreenPrimary
                )
              }
            }

            is AuthUiState.Unauthenticated -> {
              AuthScreen(viewModel = viewModel)
            }

            is AuthUiState.Authenticated -> {
              MainScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}
