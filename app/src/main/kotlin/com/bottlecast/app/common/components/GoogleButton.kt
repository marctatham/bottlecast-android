package com.bottlecast.app.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme

/**
 * "Continue with Google" (spec §3.2): a 56dp full-width pill in Google's brand colours, with the 20dp
 * G. While it works, the label gives way to a spinner.
 */
@Composable
fun GoogleButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = BottlingTheme.colors
    val label = stringResource(R.string.sign_in_google)
    Button(
        // loading looks busy, not disabled, and a second tap does nothing
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BottlingTheme.sizes.pillMinHeight)
            .semantics { if (isLoading) contentDescription = label },
        enabled = enabled,
        shape = BottlingTheme.shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.googleButtonContainer,
            contentColor = colors.googleButtonContent,
            disabledContainerColor = colors.googleButtonContainer.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = colors.googleButtonContent.copy(alpha = DISABLED_ALPHA),
        ),
        border = BorderStroke(
            BottlingTheme.sizes.hairline,
            if (enabled) colors.googleButtonBorder else colors.googleButtonBorder.copy(alpha = DISABLED_ALPHA),
        ),
        contentPadding = PaddingValues(horizontal = BottlingTheme.spacing.l, vertical = BottlingTheme.spacing.xs),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(BottlingTheme.sizes.icon),
                color = colors.googleButtonContent,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(BottlingTheme.sizes.googleLogoGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_glyph_google),
                    contentDescription = null,
                    modifier = Modifier.size(BottlingTheme.sizes.googleLogo),
                    alpha = if (enabled) 1f else DISABLED_ALPHA,
                )
                Text(text = label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            }
        }
    }
}

// Google's disabled button: its colours at 38%
private const val DISABLED_ALPHA = 0.38f
