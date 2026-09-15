package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.DimensionCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PixelBoost AI", appName)
  }

  @Test
  fun `dimension calculator preserves aspect ratio for 16K`() {
    // 16:9 input (1920x1080)
    val dims16k = DimensionCalculator.calculateTargetDimensions(1920, 1080, "16K")
    assertEquals(15360, dims16k.width)
    assertEquals(8640, dims16k.height)

    // 4K input
    val dims4k = DimensionCalculator.calculateTargetDimensions(1920, 1080, "4K")
    assertEquals(3840, dims4k.width)
    assertEquals(2160, dims4k.height)

    // 4:3 input (1600x1200) - aspect ratio preserved!
    val dims4_3 = DimensionCalculator.calculateTargetDimensions(1600, 1200, "4K")
    assertEquals(3840, dims4_3.width)
    assertEquals(2880, dims4_3.height)
  }
}
