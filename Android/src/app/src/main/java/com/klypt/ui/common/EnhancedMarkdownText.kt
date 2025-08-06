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

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klypt.ui.theme.customColors
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle

/**
 * Enhanced Composable function to display beautiful Markdown-formatted text using RichText.
 * This provides better rendering with enhanced styling, theming, and visual improvements.
 * Handles \n characters by converting them to proper line breaks.
 */
@Composable
fun EnhancedMarkdownText(
  markdown: String,
  modifier: Modifier = Modifier,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  fontSize: Float = 16f,
) {
  val isDark = isSystemInDarkTheme()
  
  // Preprocess markdown to handle newlines properly
  // Convert \n to markdown line breaks (double space + newline or double newline)
  val processedMarkdown = markdown
    .replace("\\n", "\n") // Convert literal \n to actual newlines
    .replace("\n", "  \n") // Add double space before newlines for markdown line breaks
  
  // Enhanced styling based on theme
  val codeBlockBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF8F8F8)
  val codeInlineBgColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF0F0F0)
  val linkColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2)
  
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(8.dp)
  ) {
    CompositionLocalProvider {
      ProvideTextStyle(
        value = TextStyle(
          fontSize = fontSize.sp,
          lineHeight = (fontSize * 1.4).sp,
          color = textColor,
          fontWeight = FontWeight.Normal
        )
      ) {
        RichText(
          style = RichTextStyle(
            // Enhanced code block styling
            codeBlockStyle = CodeBlockStyle(
              textStyle = TextStyle(
                fontSize = (fontSize * 0.9).sp,
                fontFamily = FontFamily.Monospace,
                color = textColor,
                lineHeight = (fontSize * 1.3).sp
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(codeBlockBgColor)
                .padding(12.dp)
            ),
            
            // Enhanced string styling for links and inline code
            stringStyle = RichTextStringStyle(
              linkStyle = TextLinkStyles(
                style = SpanStyle(
                  color = linkColor,
                  textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                hoveredStyle = SpanStyle(
                  color = linkColor.copy(alpha = 0.8f)
                )
              ),
              codeStyle = SpanStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = (fontSize * 0.9).sp,
                color = textColor,
                background = codeInlineBgColor
              )
            )
          )
        ) {
          Markdown(content = processedMarkdown)
        }
      }
    }
  }
}
