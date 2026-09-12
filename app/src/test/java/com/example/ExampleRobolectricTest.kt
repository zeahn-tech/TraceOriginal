package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.resolveMediaSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TraceNet", appName)
  }

  @Test
  fun `resolveMediaSource returns web url as is`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val url = "https://firebasestorage.googleapis.com/v0/b/tracenet/o/media.mp4?alt=media"
    val resolved = resolveMediaSource(context, url)
    assertEquals(url, resolved)
  }

  @Test
  fun `resolveMediaSource handles non-existent file gracefully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val url = "file:///data/user/0/com.example/files/non_existent.mp4"
    val resolved = resolveMediaSource(context, url)
    assertEquals("error://file_not_found", resolved)
  }

  @Test
  fun `resolveMediaSource handles empty string gracefully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resolved = resolveMediaSource(context, "")
    assertEquals("error://empty_url", resolved)
  }

  @Test
  fun `resolveMediaSource resolves existing local file`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempFile = File(context.filesDir, "test_audio.mp3")
    tempFile.writeText("dummy audio content")
    
    val url = "file://${tempFile.absolutePath}"
    val resolved = resolveMediaSource(context, url)
    assertEquals(url, resolved)
    
    tempFile.delete()
  }
}

