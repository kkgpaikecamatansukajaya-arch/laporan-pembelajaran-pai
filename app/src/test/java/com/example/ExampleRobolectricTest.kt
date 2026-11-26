package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ADM GURU PAI", appName)
  }

  @Test
  fun `verify Sukajaya schools data initialization`() {
    val schools = com.example.data.models.SukajayaDataProvider.getInitialSchools()
    org.junit.Assert.assertTrue(schools.isNotEmpty())
    val kiarapandak = schools.find { it.schoolName.contains("Kiarapandak 01", ignoreCase = true) }
    org.junit.Assert.assertNotNull(kiarapandak)
    assertEquals(100, kiarapandak?.progressPercent)
  }

  @Test
  fun `verify GeminiAssistant PAI document and HOTS question generation`() = kotlinx.coroutines.runBlocking {
    val modul = com.example.util.GeminiAssistant.generatePAIContent(
      prompt = "Menyambut Usia Baligh",
      docType = "Modul Ajar",
      topic = "Menyambut Usia Baligh",
      classLevel = "IV"
    )
    org.junit.Assert.assertTrue(modul.contains("MODUL AJAR"))
    org.junit.Assert.assertTrue(modul.contains("020 TAHUN 2026"))

    val soalText = com.example.util.GeminiAssistant.generatePAIContent(
      prompt = "Q.S. Al-Hujurat:13",
      docType = "Asesmen",
      topic = "Q.S. Al-Hujurat:13",
      classLevel = "IV"
    )
    val parsedQuestions = com.example.util.GeminiAssistant.parseQuestionsFromText(soalText, "Q.S. Al-Hujurat:13")
    org.junit.Assert.assertTrue(parsedQuestions.size >= 5)

    val chatReply = com.example.util.GeminiAssistant.chatWithPAI(
      history = emptyList(),
      userMessage = "Metode mengajar tajwid agar siswa antusias",
      classLevel = "IV"
    )
    org.junit.Assert.assertTrue(chatReply.contains("Tajwid", ignoreCase = true))
  }
}
