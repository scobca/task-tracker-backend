# Управление локализациями в проекте

> Файлы локализации лежат в папке `src/main/resourses/i18n`

## 1. Примеры

> Файл с примером примененеия локализаций —> [`LocaleTestClass.kt`](src/main/kotlin/org/esc/tasktracker/LocaleTestClass.kt)

В этом классе есть 2 метода: 
- `logMessage()` — вызывает вывод в консоль какого-то сообщения из бандла `responses_*.properties`
- logError()` — вызывает вывод в консоль какого-то сообщения из бандла `errors_*.properties`

## 2. Настройка

Настроить какая локаль будет применена к билду приложения можно через `.env` файл —> заполняем параметр `LOCALE_DEFAULT` значениями `ru` или `en`

> ### Пример:
> 
> **.env**: `LOCALE_DEFAULT=en`
> 
> **Вывод в консоль**: `The operation was successful`, `Object not found`
> 
> ---
> 
> **.env**: `LOCALE_DEFAULT=ru`
>
> **Вывод в консоль**: `Операция прошла успешно`, `Объект не найден`

## 3. Что изменять?

1. Класс **[`DefaultExceptionMessages.kt`](src/main/kotlin/org/esc/tasktracker/enums/DefaultExceptionMessages.kt)** — в нем прописаны шаблонные сообщения ошибок. Необходимо вынести их в бандл `errors_*.properties`
2. Класс **[`GlobalExceptionsHandler.kt`](src/main/kotlin/org/esc/tasktracker/io/GlobalExceptionsHandler.kt)** — в некоторых местах прописаны кастомные сообщения об ошибках, также необходимо вынести их в бандл `errors_*.properties`
3. Классы в пакете **`src/main/kotlin/services/*`** — помещаем все `return` сообщения в бандл `response_*.properties`, если внутри метода вызывается какая-то ошибка с кастомным сообщением (не из [`LocaleTestClass.kt`](src/main/kotlin/org/esc/tasktracker/LocaleTestClass.kt)) — помещаем ее в бандл `errors_*.properties` 