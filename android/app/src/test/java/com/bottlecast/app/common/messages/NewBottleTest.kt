package com.bottlecast.app.common.messages

import com.bottlecast.app.common.messages.data.newBottle
import com.google.firebase.firestore.FieldValue
import org.junit.Assert
import org.junit.Test

/**
 * The cast's shape must match firestore.rules exactly, which checks the document's set of keys.
 */
class NewBottleTest {

    @Test
    fun `a new bottle has exactly the keys the rules accept for a cast`() {
        // given
        val expectedKeys = setOf(
            "id", "messageContent", "userId", "createdAt", "lang", "status",
            "rand", "penName", "sealable", "reportCount", "findCount", "foundAt",
        )

        // when
        val bottle = newBottle("bottle00000000000001", "Hello from the sea", "user-1", rand = 0.25, "en", "Driftwood", sealable = true)

        // then
        Assert.assertEquals(expectedKeys, bottle.keys)
    }

    @Test
    fun `a new bottle starts afloat and unfound, with the writer's choices`() {
        // when
        val bottle = newBottle("bottle00000000000001", "Hello from the sea", "user-1", rand = 0.25, "es", "Driftwood", sealable = true)

        // then
        Assert.assertEquals("bottle00000000000001", bottle["id"])
        Assert.assertEquals("afloat", bottle["status"])
        Assert.assertEquals("es", bottle["lang"])
        Assert.assertEquals(true, bottle["sealable"])
        Assert.assertEquals("Driftwood", bottle["penName"])
        Assert.assertNull(bottle["foundAt"])
        Assert.assertEquals(0, bottle["reportCount"])
        Assert.assertEquals(0, bottle["findCount"])
        Assert.assertEquals(0.25, bottle["rand"])
        Assert.assertEquals(FieldValue.serverTimestamp(), bottle["createdAt"])
    }
}
