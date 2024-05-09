package com.example.docx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainViewModel: ViewModel() {
    var splashCondition by mutableStateOf(true)
    init {
        viewModelScope.launch {
            delay(300)
            splashCondition = false
        }
    }
}