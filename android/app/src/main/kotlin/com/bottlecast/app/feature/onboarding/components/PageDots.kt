package com.bottlecast.app.feature.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.onboarding.OnboardingDimens

/**
 * Which page this is (spec §3.1): 8dp dots in `outline`, which reaches 3:1 where outlineVariant
 * doesn't, and the current page a 24×8 primary pill. TalkBack reads "Page 2 of 3".
 */
@Composable
internal fun PageDots(page: Int, pageCount: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.onboarding_page_description, page + 1, pageCount)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val current = index == page
            Box(
                modifier = Modifier
                    .size(width = if (current) OnboardingDimens.activeDot else OnboardingDimens.dot, height = OnboardingDimens.dot)
                    .background(
                        color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    ),
            )
        }
    }
}
