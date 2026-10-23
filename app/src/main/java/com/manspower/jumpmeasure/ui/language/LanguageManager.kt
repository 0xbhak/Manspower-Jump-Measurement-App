package com.manspower.jumpmeasure.ui.language

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that manages the active language across the entire app.
 * Call [toggle] to switch between Indonesian and English.
 * Observe [language] to react to language changes.
 */
object LanguageManager {

    private val _language = MutableStateFlow(AppLanguage.INDONESIAN)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun toggle() {
        _language.value = if (_language.value == AppLanguage.INDONESIAN)
            AppLanguage.ENGLISH
        else
            AppLanguage.INDONESIAN
    }

    /** Returns the currently active AppStrings instance. Useful in non-Compose contexts (e.g. ViewModel). */
    fun currentStrings(): AppStrings =
        if (_language.value == AppLanguage.ENGLISH) EnglishStrings else IndonesianStrings
}
