package com.apexlions.cepiptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.apexlions.cepiptv.ui.CepIptvApp
import com.apexlions.cepiptv.ui.theme.CepIptvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CepIptvTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CepIptvApp()
                }
            }
        }
    }
}
