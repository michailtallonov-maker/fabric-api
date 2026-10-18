# Goodfooding

Клиентский мод goodfooding для Fabric (Minecraft 26.2 и 26.3). Отделяет камеру от игрока: можно свободно летать и проходить сквозь блоки.
Мод полностью клиентский: на сервер ничего не отправляется, поэтому он работает в одиночной игре и на любом сервере.
Ставить его на сервер не нужно.

## Управление

| Клавиша | Действие |
| --- | --- |
| **F4** (меняется в «Настройки → Управление → Goodfooding») | включить или выключить goodfooding |
| W / A / S / D | лететь вперёд, влево, назад, вправо (относительно взгляда) |
| Пробел / Shift | вверх / вниз |
| Ctrl (бег) | лететь в 3 раза быстрее |
| Мышь | смотреть по сторонам |

Пока goodfooding включён:
- персонаж стоит на месте и не двигается, его видно со стороны;
- ломать, ставить блоки и бить нельзя, чтобы случайно не сделать что-то «из камеры» (сервер бы это всё равно отклонил);
- при смерти, смене измерения или выходе с сервера goodfooding выключается сам.

## Скачать

Готовые jar лежат в релизе: https://github.com/michailtallonov-maker/fabric-api/releases/tag/goodfooding-latest

- `goodfooding-1.0.0+26.2.jar` — для Minecraft 26.2
- `goodfooding-1.0.0+26.3.jar` — для Minecraft 26.3

Положи нужный файл в `.minecraft/mods` вместе с [Fabric API](https://modrinth.com/mod/fabric-api) для своей версии.

## Сборка

Нужен JDK 25.

```sh
cd goodfooding
./gradlew build                                                              # 26.3
./gradlew build -Pminecraft_version=26.2 -Pfabric_api_version=0.161.0+26.2   # 26.2
```

Готовый `.jar` появится в `goodfooding/build/libs/`. Положи его в папку `mods` вместе с [Fabric API](https://modrinth.com/mod/fabric-api).

Мод также собирается автоматически в GitHub Actions (workflow **Build Goodfooding**), а готовый jar можно скачать оттуда из артефактов.

Запустить игру с модом из исходников: `./gradlew runClient`.
