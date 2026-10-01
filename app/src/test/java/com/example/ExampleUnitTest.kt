package com.example

import com.example.universavideodownloader.resolver.Platform
import com.example.universavideodownloader.resolver.PlatformDetector
import com.example.universavideodownloader.utils.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun urlValidator_validatesHttpAndHttps() {
    assertTrue(UrlValidator.isValidUrl("https://example.com/video.mp4"))
    assertTrue(UrlValidator.isValidUrl("http://example.com/video.mp4"))
    assertFalse(UrlValidator.isValidUrl("javascript:alert(1)"))
    assertFalse(UrlValidator.isValidUrl("file:///android_asset/video.mp4"))
    assertFalse(UrlValidator.isValidUrl("intent://host#Intent;scheme=app;end"))
    assertFalse(UrlValidator.isValidUrl("content://media/external/video/media/1"))
  }

  @Test
  fun platformDetector_recognizesPlatforms() {
    assertEquals(
      Platform.YOUTUBE,
      PlatformDetector.detectPlatform("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    )
    assertEquals(
      Platform.FACEBOOK,
      PlatformDetector.detectPlatform("https://www.facebook.com/watch/?v=12345")
    )
    assertEquals(
      Platform.INSTAGRAM,
      PlatformDetector.detectPlatform("https://www.instagram.com/reel/C3zY5kLp/")
    )
    assertEquals(
      Platform.TWITTER,
      PlatformDetector.detectPlatform("https://x.com/user/status/123456789")
    )
    assertEquals(
      Platform.TIKTOK,
      PlatformDetector.detectPlatform("https://www.tiktok.com/@user/video/123456789")
    )
    assertEquals(
      Platform.VIMEO,
      PlatformDetector.detectPlatform("https://vimeo.com/123456789")
    )
    assertEquals(
      Platform.GENERIC,
      PlatformDetector.detectPlatform("https://example.com/files/sample.mp4")
    )
  }

  @Test
  fun urlValidator_detectsVideoExtensions() {
    assertTrue(UrlValidator.hasDirectVideoExtension("https://example.com/video.mp4?token=abc"))
    assertTrue(UrlValidator.hasDirectVideoExtension("https://example.com/video.webm"))
    assertTrue(UrlValidator.hasDirectVideoExtension("https://example.com/video.mov"))
    assertFalse(UrlValidator.hasDirectVideoExtension("https://example.com/page.html"))
  }
}
