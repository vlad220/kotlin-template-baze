# kotlin-template-baze — универсальный Android-скелет (Kotlin + Compose)

Готовый собираемый шаблон для быстрого создания **новых** Android-приложений на основе
актуального кода `ai-taxi/` (самый свежий проект платформы). Общие страницы уже собраны
и работают: вход по SMS, обратная связь, чат, карта-пикер, голосовой ввод, профиль,
помощь, донат, тёмная/светлая тема, баннер `vsem72.ru`, автообновление.

На его основе можно собрать новый Kotlin-проект с **другим функционалом**, но с теми же
общими экранами — без переписывания из нуля.

## Что уже есть в шаблоне (общие фичи)

| Фича | Где | Как работает |
|---|---|---|
| Согласие + вход | `screens/auth/AuthScreen.kt`, `screens/agreement/` | Email/Телефон (SMS-пинкод с автоподстановкой `SmsAutofillHelper`), кнопка «Пропустить» |
| Обратная связь | `screens/feedback/FeedbackScreen.kt` | `POST /api/v1/feedback/submit` с `app=...` |
| Чат | `screens/chat/` | WebSocket + Room (офлайн-кэш), список диалогов |
| Карта-пикер | `screens/map/MapPickerScreen.kt` | Яндекс-карта в WebView, выбор точки, возврат координат |
| Голосовой ввод | `presentation/components/PhraseInput.kt` + `util/PhraseParser.kt` | Системный распознаватель, разбор «еду на Широтную, 63» → поля |
| Геолокация | `util/LocationTracker.kt`, `util/GeocoderUtils.kt` | Координаты, адрес ↔ координаты |
| Профиль | `screens/profile/ProfileScreen.kt` | Имя, телефон, роль, шаринг профиля |
| Помощь | `screens/help/HelpScreen.kt` | Частые вопросы |
| Донат/СБП | `screens/donation/DonationScreen.kt` | QR СБП |
| Тема | `presentation/theme/` | Тёмная/светлая (жёлто-янтарный акцент), переключатель |
| Баннер | `presentation/screens/AdBanner.kt` | Золотая плашка `vsem72.ru` сверху главного экрана |
| Автообновление | `util/AppUpdater.kt` | Сравнение versionCode, диалог обновления |
| Офлайн-баннер | `presentation/components/OfflineBanner.kt` | Плашка «нет интернета» |

## Банк такси-экранов (для примера, НЕ подключены)

В `screens/client/` и `screens/driver/` лежат готовые экраны такси (`ClientScreen`,
`DriverScreen`, `MyOrdersScreen`) и `screens/map/TripMapCard.kt` — они **не вызываются**
из навигации, но скомпилированы и служат образцом того, как строить переходы по статусам,
карточки заказов и т.п. Можете копировать из них нужные части в своё приложение.

## Как собрать новый проект из шаблона

1. **Скопируйте** папку `kotlin-template-baze/` в место вашего проекта и переименуйте
   корневую папку.
2. **Переименуйте пакет** `com.template` → `com.<ваш_пакет>` (Search & Replace по всему
   проекту, включая `applicationId` и `namespace` в `app/build.gradle.kts`).
3. **Поменяйте название** в `app/src/main/res/values/strings.xml` (`app_name`), заголовок
   в `MainScreen.kt` и `APP_NAME` в `build.gradle.kts` (для автообновления/фидбека).
4. **SDK:** создайте `local.properties` со своим `sdk.dir` (путь к Android SDK с
   удвоенным `\\`) — файл не хранится в шаблоне.
5. **Соберите:** на Windows `JAVA_HOME=D:\jdk-21\jdk-21; .\gradlew.bat assembleDebug`.
   В WSL: `sh gradlew assembleDebug` с `ANDROID_HOME` на ваш SDK.
6. Поменяйте экраны и мой главный «герой»-экран на свой функционал.

## Страница-памятка для быстрого старта

- Пакет и все импорты уже завязаны на `com.template` — одна правка `build.gradle.kts`
  и один replace в коде переводят приложение на ваше имя.
- `API_BASE_URL` уже указывает на `https://vsem72.ru/api/v1/` — для чужого сервера
  поменяйте в `build.gradle.kts`.
- Яндекс-ключ — клиентский (`509f2bc0-...`), только для карты в WebView; серверные
  запросы к Яндексу требуют отдельного серверного ключа.
- В шаблоне **нет секретов**: `keystore.*` удалены, локальные ключи занесены в `.gitignore`.

## Сборка

Проверено: `sh gradlew assembleDebug` в WSL (SDK `/mnt/d/wsl/android-sdk`) → BUILD
SUCCESSFUL, APK ~18 МБ.