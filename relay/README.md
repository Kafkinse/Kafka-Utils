# Kafka-Utils — релей мессенджера

Крошечный сервер (один файл, чистая Java, без зависимостей), через который
общаются игроки с Kafka-Utils. Сообщения идут **напрямую между игроками через
этот релей** и вообще не проходят через Minecraft-сервер.

## Установка на VPS (Ubuntu)

Нужна Java 17+ (для сборки мода вы уже ставили 21 — подойдёт).

```bash
mkdir -p ~/kafka-relay && cd ~/kafka-relay
# скопируйте сюда KafkaRelay.java из папки relay/ репозитория, затем:
java KafkaRelay.java 8765 МОЙ_СЕКРЕТНЫЙ_КЛЮЧ
```

Ключ — любая строка; её же вводят игроки в настройках модуля Messenger.
Не забудьте открыть порт: `sudo ufw allow 8765/tcp`.

### Автозапуск (systemd)

```bash
sudo tee /etc/systemd/system/kafka-relay.service >/dev/null <<'EOF'
[Unit]
Description=Kafka-Utils messenger relay
After=network.target

[Service]
User=%i
WorkingDirectory=/home/ВАШ_ПОЛЬЗОВАТЕЛЬ/kafka-relay
ExecStart=/usr/bin/java KafkaRelay.java 8765 МОЙ_СЕКРЕТНЫЙ_КЛЮЧ
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
sudo systemctl daemon-reload
sudo systemctl enable --now kafka-relay
```

Группы сохраняются в `groups.txt` рядом с файлом сервера.

## Настройка мода (у каждого игрока)

В ClickGui → Friends → **Messenger**:

| Настройка | Значение |
|---|---|
| Server URL | `ws://IP_ВАШЕЙ_VPS:8765` |
| Key | тот самый секретный ключ |
| Fallback via /msg | вкл — при недоступном релее ЛС пойдут через /msg |
| Chat Notify | показывать входящие в обычном чате |

## Устойчивость к блокировкам (рекомендуется)

1. **Домен вместо IP**: заведите домен, A-запись на VPS, в моде укажите
   `ws://chat.вашдомен.xyz:8765`. Сменили IP — обновили DNS, у всех работает.
2. **Cloudflare (wss на 443)**: включите оранжевое облако на записи домена,
   поставьте на VPS обратный прокси (nginx/caddy) с апгрейдом WebSocket на
   `localhost:8765` и используйте в моде `wss://chat.вашдомен.xyz`. IP VPS
   больше нигде не светится, трафик выглядит как обычный HTTPS-сайт.
3. **Fallback**: даже если релей недоступен, личные сообщения автоматически
   уходят через серверный `/msg` (это видно в логах игрового сервера — как и
   обычная личка).

## Протокол (для любопытных)

Текстовые WebSocket-кадры, поля через `|`, каждое URL-encoded:
`auth|ник|ключ`, `msg|кому|текст`, `gmsg|группа|текст`, `gcreate|группа`,
`gadd|группа|ник` → сервер шлёт `ok`, `err|причина`, `msg|от|текст|ts`,
`gmsg|группа|от|текст|ts`, `users|...`, `join|ник`, `left|ник`, `groups|...`.

Идентификация по нику + общий ключ: это чат для своей компании, а не
банковский мессенджер. Ключ никому не показывайте.
