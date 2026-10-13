package com.bottlecast.app.common.ui

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview


val fakeClickHandler: () -> Unit = { println("I'm a fake function to satisfy jetpack compose preview requirements") }


val fakeChildComposable: @Composable () -> Unit = { println("I'm a fake function to satisfy jetpack compose preview requirements") }

/** Previews a composable in both themes, Night and Dawn. Wrap its content in [BottlingPreview]. */
@Preview(name = "Night", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Preview(name = "Dawn", uiMode = UI_MODE_NIGHT_NO or UI_MODE_TYPE_NORMAL)
annotation class NightDawnPreviews

/** The theme plus the window background, for previews. */
@Composable
fun BottlingPreview(content: @Composable () -> Unit) {
    BottlingTheme {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
