/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klypt.ui.common

import android.graphics.Typeface
import android.text.Spanned
import android.widget.TextView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * Simplified beautiful Markdown renderer using Markwon core features.
 * This provides enhanced markdown rendering without complex syntax highlighting dependencies.
 */
@Composable
fun SimpleMarkdownText(
  markdown: String,
  modifier: Modifier = Modifier,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  fontSize: Float = 16f,
) {
  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  
  val markwon = remember(textColor, isDark, fontSize) {
    Markwon.builder(context)
      .usePlugin(object : AbstractMarkwonPlugin() {
        override fun configureTheme(builder: MarkwonTheme.Builder) {
          builder
            .headingTextSizeMultipliers(floatArrayOf(2f, 1.75f, 1.5f, 1.25f, 1.1f, 1f))
            .headingTypeface(Typeface.DEFAULT_BOLD)
            .codeTextColor(textColor.copy(alpha = 0.9f).toArgb())
            .codeBackgroundColor(
              if (isDark) 
                Color(0xFF2D2D2D).toArgb() 
              else 
                Color(0xFFF5F5F5).toArgb()
            )
            .codeBlockTextColor(textColor.toArgb())
            .codeBlockBackgroundColor(
              if (isDark) 
                Color(0xFF1E1E1E).toArgb() 
              else 
                Color(0xFFF8F8F8).toArgb()
            )
            .linkColor(
              if (isDark)
                Color(0xFF64B5F6).toArgb()
              else
                Color(0xFF1976D2).toArgb()
            )
            .blockQuoteColor(textColor.copy(alpha = 0.7f).toArgb())
            .listItemColor(textColor.toArgb())
            .bulletListItemStrokeWidth(2)
        }
      })
      .usePlugin(HtmlPlugin.create())
      .usePlugin(StrikethroughPlugin.create())
      .usePlugin(LinkifyPlugin.create())
      .build()
  }

  AndroidView(
    modifier = modifier.fillMaxWidth(),
    factory = { ctx ->
      TextView(ctx).apply {
        textSize = fontSize
        setTextColor(textColor.toArgb())
        setPadding(16, 8, 16, 8)
        // Enable text selection
        setTextIsSelectable(true)
        // Improve line spacing
        setLineSpacing(6f, 1.2f)
        // Better link handling
        linksClickable = true
        isClickable = true
      }
    },
    update = { textView ->
      val spanned: Spanned = markwon.toMarkdown(markdown)
      textView.text = spanned
      textView.setTextColor(textColor.toArgb())
      textView.textSize = fontSize
    }
  )
}
