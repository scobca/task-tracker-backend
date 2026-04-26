package org.esc.tasktracker

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class LocaleTestClass(
    /**
     * В классах, где делаем локализацию нужно будет импортировать класс, который отвечает за подтягивание данных
     * Строка ниже - импорт нужного класса в наш класс, просто добавляем ее в конструктор класса после ,
     */
    private val messageSource: MessageSource,
) {

    /**
     * Вот эти 2 строки просто копируем и вставляем в начало каждого класса, где делаем локализацию.
     * Это - получение базового языка локализации из конфига приложения
     */
    @Value($$"${spring.locales.default}")
    private lateinit var defaultLocale: String

    fun logMessage() {
        println(messageSource.getMessage("response.ok", null, Locale.of(defaultLocale)))
    }

    fun logError() {
        println(
            messageSource.getMessage("error.not_found", null, Locale.of(defaultLocale))
        )
    }
}