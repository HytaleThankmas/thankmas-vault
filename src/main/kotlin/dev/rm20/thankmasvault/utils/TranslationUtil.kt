package dev.rm20.thankmasvault.utils

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.modules.i18n.I18nModule

object TranslationUtil {


    fun resolve(text: String?): String? {
        if (text.isNullOrBlank()) return text

        return try {
            val formatted = Message.translation(text).ansiMessage
            if (formatted.isNotEmpty() && formatted != text) formatted else text
        } catch (_: Exception) {
            text
        }
    }
}
