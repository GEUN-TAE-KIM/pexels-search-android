package com.gtkim.pexelssearch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gtkim.pexelssearch.ui.search.SearchScreen
import kotlinx.serialization.Serializable

/**
 * type-safe route。詳細へは写真オブジェクトではなく [PhotoDetailRoute.photoId] だけを渡し、
 * 表示するデータは遷移先が自分で取得する。
 */
@Serializable
data object SearchRoute

@Serializable
data class PhotoDetailRoute(val photoId: Long)

@Composable
fun PexelsSearchNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SearchRoute,
        modifier = modifier,
    ) {
        composable<SearchRoute> {
            SearchScreen(
                onNavigateToDetail = { photoId ->
                    navController.navigate(PhotoDetailRoute(photoId))
                },
            )
        }

        composable<PhotoDetailRoute> { }
    }
}
