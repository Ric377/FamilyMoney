# Family Money - Менеджер общих расходов

`Family Money` — это мобильное Android-приложение для удобного совместного учёта расходов в группе. Идеально подходит для семьи, друзей или соседей по квартире, чтобы отслеживать, кто, сколько и на что потратил, а также автоматически рассчитывать долги. Логика Рачета долгов еще находится в разработке.

Приложение полностью написано на **Kotlin** с использованием декларативного UI-фреймворка **Jetpack Compose** и архитектуры **MVVM**. В качестве бэкенда используется **Firebase**.

## 📸 Скриншоты

| Главный экран | Добавление траты |
| :---: | :---: |
| ![Главный экран](screenshots/MainScreen.jpg) | ![Добавление траты](screenshots/AddPaymentScreen.jpg) |
| **Навигационная панель** | **Настройка профиля** |
| ![Навигационная выдвижная панель](screenshots/MainDrawer.jpg) | ![Настройка профиля](screenshots/Profile.jpg) |

## ✨ Основные возможности

* **Аутентификация**: Вход и регистрация через Email/Пароль, а также с помощью Google Sign-In.
* **Управление группами**:
    * Создание новой группы для учёта расходов.
    * Присоединение к существующей группе по уникальному коду.
    * Просмотр списка участников группы.
* **Учёт расходов**:
    * Добавление новых трат с указанием суммы, комментария и даты.
    * Редактирование и удаление существующих записей.
    * Пакетное удаление нескольких трат одновременно.
* **Аналитика и отчёты**:
    * Просмотр всех трат за выбранный месяц и год.
    * Отображение общей суммы расходов за период.
    * Суммарные траты по каждому участнику группы.
* **Расчёт долгов**: Автоматический расчёт, кто кому и сколько должен, как за всё время, так и по месяцам.
* **Push-уведомления**: Благодаря **Firebase Cloud Functions**, все участники группы (кроме автора траты) получают push-уведомление, как только кто-то добавляет новый расход.
* **Персонализация профиля**: Возможность изменить имя и выбрать один из предустановленных аватаров.
* **Офлайн-режим**: Благодаря кэшированию Firestore, приложением можно пользоваться даже при отсутствии стабильного интернет-соединения.

## 🛠️ Технологии и архитектура

* **Язык**: [Kotlin](https://kotlinlang.org/)
* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Архитектура**: MVVM (Model-View-ViewModel)
* **Асинхронность**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/)
* **Навигация**: [Navigation for Compose](https://developer.android.com/jetpack/compose/navigation)
* **Бэкенд и база данных**:
    * [Firebase Authentication](https://firebase.google.com/docs/auth)
    * [Cloud Firestore](https://firebase.google.com/docs/firestore)
    * [Firebase Cloud Messaging (FCM)](https://firebase.google.com/docs/cloud-messaging)
    * [Firebase Cloud Functions](https://firebase.google.com/docs/functions) (для серверной логики уведомлений)
* **Загрузка изображений**: [Coil](https://coil-kt.github.io/coil/)

## 🚀 Установка и запуск

1.  **Клонируйте репозиторий**:
    ```bash
    git clone [https://github.com/your-username/family-money.git](https://github.com/your-username/family-money.git)
    ```
2.  **Настройка Firebase**:
    * Создайте новый проект в [Firebase Console](https://console.firebase.google.com/).
    * Добавьте Android-приложение в ваш проект Firebase с `package name`: `com.rich.familymoney`.
    * Скачайте конфигурационный файл `google-services.json` и поместите его в директорию `app/` вашего проекта.
    * Добавьте **SHA-1 отпечаток** вашего debug-ключа в настройки Android-приложения в Firebase для корректной работы Google Sign-In.
    * В Firebase Console включите следующие сервисы:
        * **Authentication**: активируйте методы "Электронная почта/пароль" и "Google".
        * **Firestore Database**: создайте базу данных.
3.  **Откройте проект** в Android Studio.
4.  **Соберите и запустите** приложение на эмуляторе или физическом устройстве.
