package com.gtkim.pexelssearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gtkim.pexelssearch.ui.search.SearchScreen
import com.gtkim.pexelssearch.ui.theme.PexelsSearchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PexelsSearchTheme {
                SearchScreen(onNavigateToDetail = {})
            }
        }
    }
}
