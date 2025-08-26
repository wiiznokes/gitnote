[English](README.md) | [Русский](README_RU.md)

<p align="left">
  <img align="left" src="assets/app_icon.svg" alt="иконка приложения" width="90px" />
  <h1 style="display: inline-block; margin-left: 12px; vertical-align: middle;">GitNote</h1>
</p>

<a href="https://f-droid.org/en/packages/io.github.wiiznokes.gitnote/"><img src="https://f-droid.org/badge/get-it-on.svg" alt="Значок F-Droid" height="50"></a>
<a href="https://apt.izzysoft.de/packages/io.github.wiiznokes.gitnote"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Значок IzzyOnDroid" height="50"></a>

<br/>

_Поддерживаемые версии Android: 11 — 15_

Android-приложение для заметок с интеграцией Git. Вы можете использовать это приложение вместе с другими редакторами на компьютере.

## Зачем

Потому что все приложения с интеграцией Git на Android либо отделяют заголовок заметки от имени файла, либо используют устаревшие UI/UX-фреймворки.

# Возможности

- [x] создание/открытие/клонирование репозиториев  
- [x] нечеткий поиск (глобальный и по конкретной папке)  
- [x] отображение в виде сетки  
- [x] отображение в виде дерева  
- [x] режим редактирования  
- [x] приватные репозитории с https  
- [x] удалённая синхронизация  
- [x] сортировка по времени  

<p style="text-align: center;">
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/grid.png" width="32%" alt="экран сетки"/>
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/drawer.png" width="32%" alt="экран меню"/>
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/edit.png" width="32%" alt="экран редактирования"/>
</p>

# Сборка

[См.](./BUILD_RU.md).

# Текущие ограничения

- Android не различает регистр в именах файлов, поэтому если у вас есть папка с именем `A` и другая с именем `a`, папка `a` не будет отображаться.  
- Изменение ориентации экрана нарушает состояние SSL. Обходной путь: перезапустить приложение.  
- Конфликты могут привести к сбою приложения.  
