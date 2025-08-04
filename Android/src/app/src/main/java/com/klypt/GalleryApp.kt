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

package com.klypt

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.klypt.ui.navigation.GalleryNavHost
import com.klypt.ui.navigation.NavigationContextViewModel

/** Top level composable representing the main screen of the application. */
@Composable
fun GalleryApp(navController: NavHostController = rememberNavController()) {
  val navigationContextViewModel: NavigationContextViewModel = hiltViewModel()
  
  GalleryNavHost(
    navController = navController,
    userContextProvider = navigationContextViewModel.userContextProvider
  )
}
