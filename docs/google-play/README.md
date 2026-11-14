# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `GreenStreet-v<версия>.aab`
  (начиная с первой версии после 0.14.2). Секреты `SIGNING_*` в репозитории заданы; без них сборка
  подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках игры перестаёт работать. Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.greenstreet`.
  2. **Select key** → ключ с SHA-256 `9160a877ffe017424dd5c6d3e6767ff5e1dd773808e39b032173983ce32376ad`
     (сертификат CN=bigdevwhale, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет и запустить workflow **Package registration proof**
     (Actions → Run workflow → вставить сниппет). Он соберёт релизный APK с файлом
     `assets/adi-registration.properties`, подпишет тем же ключом и проверит отпечаток; APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Тогда APK из GitHub и версия из Play
  обновляют друг друга. Если оставить «Google создаст ключ подписи», они друг на друга не встанут.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне (на двух языках). Ссылка:
  `https://github.com/bigdevwhale/greenstreet/blob/main/PRIVACY.md`.
- **Видео для декларации местоположения** (раздел 4, Foreground service). Снимите заранее 20–40 секунд
  записи экрана: открыть «Улицу», нажать «Выйти на прогулку», показать уведомление в шторке, выключить
  экран, пройти мимо своей грядки, включить — грядка «сегодня полита»; нажать «Закончить прогулку» —
  уведомление исчезло. Загрузите на YouTube (доступ по ссылке).
- **Автозагрузка из CI** (`.github/workflows/play.yml`) — после каждого успешного Release берёт AAB из
  релиза и отправляет в Play, release notes — из `listing/*/release_notes.txt` на теге. До первой ручной
  загрузки она не нужна, поэтому:
  1. В репозитории Settings → Environments → создать **`google-play`** с Required reviewer (вы) —
     иначе job запустится без подтверждения сразу после релиза.
  2. Play Console → Users and permissions → пригласить `play-publisher@intense-context-514611-h3.iam.gserviceaccount.com`
     с правом выпускать релизы для Green Street.
  3. В Google Cloud у пула `github` (Workload Identity Federation) разрешить репозиторию
     `bigdevwhale/greenstreet` действовать от имени этого сервисного аккаунта — как для остальных приложений.
  Пока это не сделано, отклоняйте запуск «Google Play» после релиза (или он упадёт на авторизации — без вреда).
  По умолчанию workflow выкладывает в **production**; пока идёт закрытый тест, запускайте его вручную
  с треком `alpha` (closed testing).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Green Street: Walk & Garden` |
| Default language | English (United States) – en-US |
| App or game | **Game** |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать (игра только для телефона, портрет) | — |
| Video | пусто | — |

Скриншоты 1080×1920 собирает `tools/play.py` из экранов демо (`docs/raw/<язык>/*.png`, 1080×2400):
Play не берёт картинки длиннее двух ширин, поэтому экран уходит за нижний край под подписью;
строка состояния телефона срезана. Подписи — пиксельным шрифтом игры m6x11. На скриншотах: улица с картой,
задание и грядки, сосед у своей кофейни, двор, соседи, кухня, сумка с семенами, хозяйство.
Баннер — нарисованная обложка `docs/cover.png` с названием. Как пересобрать:

```bash
./gradlew :app:assembleDebug -Pdemo -Pshots   # демо с районом Петроградки, время всегда 12:30
# снять экраны 1080×2400 в docs/raw/en и docs/raw/ru (имена — как сейчас)
python tools/play.py                          # графика для Play (нужен pip install pillow)
```

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Game → **Simulation** |
| Tags (до 5) | **Farming**, **Life simulation**, **Social simulation**, **Casual**, **Simulation** (запасной — Restaurant & cooking) |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/greenstreet/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** Это игра-ферма: в Play её будут искать рядом с фермами и уютными симуляторами
(«ферма», «огород», «cozy farming»). Альтернатива — **Casual**. Как у Fogwalk, можно было бы уйти
в App → Health & Fitness ради «прогулок», но тут главное — ферма, соседи и истории, а ходьба лишь способ
поливать; к тому же шагомера нет. Категорию можно сменить позже.

**Теги.** Farming — главное: грядки, урожай, куры. Life simulation — двор, кухня, времена года, три года
жизни. Social simulation — соседи, дружба, просьбы и письма. Casual — спокойный темп без проигрыша.
Simulation — общий тег жанра. Educational не брать: с ним Play считает игру детской.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, как у остальных приложений.

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*
(Аккаунтов нет. Для проверки без прогулки ревьюеру хватит открыть игру: район скачивается вокруг
места, где стоит телефон, а дом, двор, соседи и кухня доступны сразу.)

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC, категория **Game**)
- На все вопросы — **No**: насилия, страха, сексуального контента, ненормативной лексики нет.
  Алкоголя, табака и наркотиков нет (проверено по всем текстам; в рецептах — квас, морс, сбитень и чай).
- **Gambling / simulated gambling** — No: гроши — внутриигровая валюта, их зарабатывают урожаем и
  помощью соседям, купить нельзя, случайных наград за деньги нет.
- **Users interact / share content** — **No**: общения между игроками нет.
- **Shares location with other users** — **No**: местоположение никому не передаётся.
- **Digital purchases** — No.
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over**. Младше 13 не отмечать: иначе действует политика Families
  с отдельными требованиями (а у нас местоположение).
- *Could your store listing unintentionally appeal to children?* → **No**: уютная ферма про прогулки
  по своему району и соседей; если Google не согласится из-за пиксельной графики, ответ меняется на Yes
  без последствий для 13+.

**News apps** — No. **COVID-19** — My app is not a publicly available COVID-19 app.
**Government apps** — No. **Financial features** — *My app doesn't provide any financial features.*

**Health apps** — не отмечать ничего: шагомера, Health Connect и данных о здоровье нет; прогулка —
только способ полить грядки.

**Data safety**

Разработчику не уходит ничего, но Play считает «сбором» любую передачу с телефона, в том числе в чужие
сервисы. Точное местоположение (GPS) используется только на телефоне. Наружу уходит лишь примерный район:
тайлы карты вокруг дома с OpenFreeMap (один раз) и координаты, округлённые до 0,01°, для погоды.

- *Does your app collect or share any of the required user data types?* → **Yes**.
- *Is all of the user data collected by your app encrypted in transit?* → **Yes** (всё по HTTPS).
- *Do you provide a way for users to request that their data is deleted?* → **No** (аккаунтов нет,
  на серверах ничего не хранится; данные удаляются вместе с приложением).
- Типы данных:
  - **Location → Approximate location** — Collected: **Yes**. Shared: **No** (передача сервису, который
    выполняет функцию приложения, — не «sharing»). **Processed ephemerally: Yes**.
    Required: **Yes** (без карты района и погоды игры нет). Purpose: **App functionality**.
  - **Precise location** — *не* отмечать: точные координаты с телефона не уходят.
- Больше ничего: аналитики, рекламы, идентификаторов, отчётов о сбоях нет. Системное резервное
  копирование Android делает система, а не приложение, его не декларируем.
- Итог в карточке: «Collects: Location», «No data shared with third parties», «Data is encrypted in transit».

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Foreground service permissions** (появится после загрузки AAB: targetSdk 36 и `FOREGROUND_SERVICE_LOCATION`)
- Тип: **Location**. Задача: подходящего пункта нет — выберите **Other** и опишите:
  > Green Street is a farming game on the real map of the player's district: walking past a garden bed the
  > player planted by the road waters it. When the player taps "Go for a walk", a location foreground
  > service with a visible, ongoing notification keeps receiving GPS positions while the screen is off or the
  > phone is in a pocket, so the beds the player passes are watered. It starts only on the player's tap,
  > stops when they end the walk in the app or the notification, and positions never leave the device.
- Ссылка на видео — см. раздел 0.

**Location permissions** — отдельной декларации не нужно: `ACCESS_BACKGROUND_LOCATION` не запрашивается,
местоположение берётся только при открытом приложении или в начатой игроком прогулке с уведомлением.

**Прочие разрешения** — особых деклараций не нужно: `POST_NOTIFICATIONS` (уведомление прогулки и тихие
уведомления о находках), `INTERNET`.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Closed testing** (для нового аккаунта; иначе Internal testing) → Create new release.
2. Загрузить `GreenStreet-v<версия>.aab` из GitHub Release.
3. Release name — номер версии. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели;
   с декларацией местоположения может дольше.
5. После установки из Play проверьте: онбординг → разрешение на местоположение → район скачался;
   «Выйти на прогулку» показывает уведомление; Настройки → Язык → Русский/English — экран переключается сразу.

## Следующие версии

- `release_notes.txt` переписать под новую версию (≤500 символов — `play.yml` это проверяет;
  для первой там «First release on Google Play»). Дальше релиз уходит в Play сам через `play.yml`
  после подтверждения в окружении `google-play`.
- Скриншоты пересобирать, только если поменялись показанные экраны.

## Риск: OpenFreeMap, MET Norway, Open-Meteo

Все бесплатные и без ключей; запросы идут с собственным User-Agent со ссылкой на репозиторий (`AGENT`):
MET Norway требует, чтобы по нему можно было связаться. Карта скачивается один раз на район; погода
запрашивается несколько раз в день. Open-Meteo бесплатен для некоммерческого использования.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.greenstreet`.
