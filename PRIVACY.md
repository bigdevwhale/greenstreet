# Green Street — Privacy Policy

*Last updated: 15 November 2026* · [Русская версия ниже](#зелёная-улица--политика-конфиденциальности)

Green Street is a cozy farming game for Android on the map of your own district, published as open source at
<https://github.com/bigdevwhale/greenstreet>.

## What we collect

**Nothing.** Green Street has no accounts, no servers, no analytics, no advertising and no crash reporting.
The developer never receives your location, your walks or anything else from the app.

## Your location

The game uses your phone's location (GPS) to know where you are: standing by a lawn lets you sow a bed,
walking past a bed waters it, and finds turn up along your way. The location is used only on the phone and
is never sent anywhere, with the two exceptions below, which get only an approximate area.

- While the app is open, it reads your location to draw you on the map.
- When you tap **Go for a walk**, a foreground service with a visible notification keeps reading your location
  while the screen is off, so passing your beds still waters them. It stops when you end the walk.
- The app does not request background location access and never reads your location otherwise.

## What goes over the network

- **The map of your district.** When you choose your home, the app downloads the map tiles around it, once,
  from OpenFreeMap (`tiles.openfreemap.org`, OpenStreetMap data). Like any website, that server sees your
  IP address and which tiles are requested, which reveals the area of a few square kilometres around your
  home, but not your exact position or your walks. The map is then kept on the phone.
- **The weather.** The forecast comes from MET Norway (`api.met.no`) and the rain that has already fallen
  from Open-Meteo (`api.open-meteo.com`). Both get only your coordinates rounded to two decimal places
  (about a kilometre).
- **Links you tap.** Settings has links to the StillCraft Telegram bot and chat and to the support page;
  they open in your browser or Telegram only when you tap them.

No other network requests are made.

## What stays on your device

The app keeps your home, beds, harvests, neighbours, letters, progress and settings only in its private
storage on your phone. You can delete everything at any time by clearing the app's storage or uninstalling it.

## Android backup

If you have enabled Android's own device backup, the system may include Green Street's data in your
personal backup with Google, under your Google account and Google's privacy policy. The developer
has no access to that backup. You can turn it off in your phone's backup settings.

## Children

Green Street is not directed at children under 13 and collects no personal information from anyone.

## Changes and contact

If this policy changes, the new version will be published here. Questions: through the StillCraft bot
on Telegram (Settings → Community in the app) or an issue on GitHub.

---

# Зелёная улица — политика конфиденциальности

*Последнее обновление: 15 ноября 2026*

«Зелёная улица» — уютная игра-ферма для Android на карте вашего района, с открытым кодом:
<https://github.com/bigdevwhale/greenstreet>.

## Что мы собираем

**Ничего.** В «Зелёной улице» нет аккаунтов, серверов, аналитики, рекламы и отчётов о сбоях.
Разработчик не получает ни вашего местоположения, ни прогулок, ни чего-либо ещё.

## Ваше местоположение

Игра узнаёт, где вы, по GPS телефона: у газона можно посадить грядку, проходя мимо грядки — полить её,
по дороге попадаются находки. Местоположение используется только на телефоне и никуда не отправляется,
кроме двух случаев ниже, где уходит лишь примерный район.

- Пока приложение открыто, оно берёт местоположение, чтобы показать вас на карте.
- Когда вы нажимаете **«Выйти на прогулку»**, служба с видимым уведомлением продолжает получать
  местоположение при выключенном экране, чтобы грядки поливались. Она останавливается, когда вы
  заканчиваете прогулку.
- Доступ к местоположению в фоне приложение не запрашивает и в другое время его не берёт.

## Что уходит в сеть

- **Карта района.** Когда вы выбираете дом, приложение один раз скачивает тайлы карты вокруг него
  с OpenFreeMap (`tiles.openfreemap.org`, данные OpenStreetMap). Как любой сайт, этот сервер видит ваш
  IP-адрес и запрошенные тайлы, то есть район в несколько квадратных километров вокруг дома, но не точное
  место и не прогулки. Дальше карта хранится на телефоне.
- **Погода.** Прогноз — от MET Norway (`api.met.no`), уже выпавший дождь — от Open-Meteo
  (`api.open-meteo.com`). Обоим уходят только координаты, округлённые до двух знаков (около километра).
- **Ссылки, на которые вы нажимаете.** В настройках есть ссылки на бота и чат StillCraft в Telegram
  и на страницу поддержки; они открываются в браузере или Telegram, только если на них нажать.

Других запросов в сеть нет.

## Что остаётся на телефоне

Дом, грядки, урожай, соседи, письма, прогресс и настройки хранятся только в памяти приложения
на телефоне. Удалить всё можно в любой момент, очистив данные приложения или удалив его.

## Резервное копирование Android

Если на телефоне включено системное резервное копирование, Android может сохранить данные игры в вашей
личной резервной копии Google — по правилам вашего аккаунта Google. У разработчика к ней доступа нет.
Отключить можно в настройках резервного копирования телефона.

## Дети

Игра не предназначена для детей младше 13 лет и ни у кого не собирает личных данных.

## Изменения и связь

Если политика изменится, новая версия появится здесь. Вопросы — через бота StillCraft в Telegram
(Настройки → Сообщество в приложении) или issue на GitHub.
