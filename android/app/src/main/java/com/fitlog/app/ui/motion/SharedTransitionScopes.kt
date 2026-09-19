@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.fitlog.app.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedContentScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun NavEntryScopes(
    animatedVisibilityScope: AnimatedVisibilityScope,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalNavAnimatedContentScope provides animatedVisibilityScope) {
        content()
    }
}

@Composable
fun Modifier.sharedNavBounds(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current ?: return this
    val base = this
    return with(sharedScope) {
        base.sharedBounds(rememberSharedContentState(key), animatedScope)
    }
}

@Composable
fun Modifier.sharedNavElement(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current ?: return this
    val base = this
    return with(sharedScope) {
        base.sharedElement(rememberSharedContentState(key), animatedScope)
    }
}
