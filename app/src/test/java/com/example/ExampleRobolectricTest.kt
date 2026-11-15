package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.OmniSearchEngine
import com.example.model.SearchCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app_name and verify single pass omni search fuzzy engine`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CHRONO-OS 70", appName)

    val engine = OmniSearchEngine(context)
    val apps = engine.loadInstalledAndCoreApps()
    val files = engine.loadLocalAndSafFiles(emptySet())
    val contacts = engine.loadContacts(emptyList())

    val result = engine.executeSinglePassSearch(
      rawQuery = "pwr",
      category = SearchCategory.ALL,
      allApps = apps,
      allFiles = files,
      allContacts = contacts,
      hiddenAppIds = emptySet(),
    )

    assertTrue("Expected Poweramp match for fuzzy query 'pwr'", result.apps.isNotEmpty())
    assertNotNull("Expected exact target lock for 'pwr'", result.exactMatchApp)
    assertEquals("cmd.poweramp", result.exactMatchApp?.id)
  }
}
