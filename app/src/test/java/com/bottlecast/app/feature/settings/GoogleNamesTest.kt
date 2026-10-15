package com.bottlecast.app.feature.settings

import com.bottlecast.app.common.profile.domain.initialOf
import com.bottlecast.app.feature.settings.domain.googleInitials
import com.bottlecast.app.feature.settings.domain.shortGoogleName
import org.junit.Assert
import org.junit.Test

/** Unit tests for how names show in Settings › Account and in the avatars. */
class GoogleNamesTest {

    @Test
    fun `the Google name shows as the first name and the last name's initial`() {
        Assert.assertEquals("Marc T.", shortGoogleName("Marc Tatham"))
        Assert.assertEquals("Ana S.", shortGoogleName("  Ana  María   Silva "))
        Assert.assertEquals("Cher", shortGoogleName("Cher"))
    }

    @Test
    fun `the account's avatar takes the first and last initials`() {
        Assert.assertEquals("MT", googleInitials("Marc Tatham"))
        Assert.assertEquals("AS", googleInitials("ana maría silva"))
        Assert.assertEquals("C", googleInitials("Cher"))
        Assert.assertEquals("", googleInitials("   "))
    }

    @Test
    fun `a pen name's initial is its first letter, an emoji counting as one`() {
        Assert.assertEquals("D", initialOf("driftwood"))
        Assert.assertEquals("🌊", initialOf("🌊 Tide"))
        Assert.assertNull(initialOf("  "))
        Assert.assertNull(initialOf(null))
    }
}
