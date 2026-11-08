# ServerKeepAwake

Небольшой серверный мод, который предотвращает переход Windows в сон, пока работает Minecraft-сервер.

## Структура исходников

Каждая реализация хранится отдельно по платформе, версии Minecraft и версии загрузчика:

- `neoforge/1.21.1/21.1.265/` — NeoForge 21.1.265 для Minecraft 1.21.1.

В будущем сюда можно добавлять `forge/`, `fabric/`, `paper/` и другие платформы, не смешивая зависимости и исходники.

## Имена файлов релиза

Формат: `ServerKeepAwake-<platform>-MC<minecraft-version>-<loader><loader-version>-v<mod-version>.jar`.

Пример: `ServerKeepAwake-neoforge-MC1.21.1-NF21.1.265-v1.0.0.jar`.

Сборка и публикация выполняются GitHub Actions. Игрокам на клиенте мод не требуется.
