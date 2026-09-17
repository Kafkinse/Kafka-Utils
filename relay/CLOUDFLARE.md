# Релей через домен + Cloudflare (wss на 443)

Цель: друзья подключаются к мессенджеру по адресу вида
`wss://chat.твойдомен.xyz` (порт 443). Трафик идёт через Cloudflare и выглядит
как обычный HTTPS-сайт — реальный IP VPS не светится, а блокировки по
IP/порту провайдером обходятся. Схема:

```
Игрок  ──wss:443──►  Cloudflare (прячет IP)  ──443──►  проброс NAT  ──►  Caddy (VPS)  ──►  релей :8765
```

Нужен свой домен, добавленный в Cloudflare (бесплатного плана достаточно —
WebSocket на нём включён всегда).

---

## Шаг 1. DNS-запись в Cloudflare

Cloudflare → твой домен → **DNS** → Add record:

| Type | Name | Content | Proxy status |
|---|---|---|---|
| A | `chat` | `163.123.180.81` (публичный IP VPS) | **Proxied (оранжевое облако)** |

Оранжевое облако обязательно — именно оно прячет IP и даёт 443.

## Шаг 2. Проброс порта 443 на VPS

Cloudflare (оранжевое облако) стучится к origin **только на разрешённые порты**,
из HTTPS это `443, 2053, 2083, 2087, 2096, 8443`. Порт `10041` для этого не
подходит. Добавь на своём NAT/роутере ещё один проброс:

| Public IP | Public Port | Inner IP | Inner Port |
|---|---|---|---|
| 163.123.180.81 | **443** | 192.168.122.28 | **443** |

(`Inner IP` — актуальный внутренний адрес VPS, проверь `hostname -I`.)
Проброс `10041 → 8765` можно оставить как запасной прямой вход.

## Шаг 3. Origin-сертификат Cloudflare (шифрование до VPS)

Cloudflare → **SSL/TLS → Origin Server → Create Certificate** →
оставь настройки по умолчанию (RSA, 15 лет) → Create.

Тебе покажут два блока. Сохрани их на VPS:

```bash
sudo mkdir -p /etc/caddy
sudo nano /etc/caddy/cf-origin.pem   # вставь "Origin Certificate", сохрани
sudo nano /etc/caddy/cf-origin.key   # вставь "Private Key", сохрани
sudo chmod 600 /etc/caddy/cf-origin.key
```

Затем Cloudflare → **SSL/TLS → Overview** → режим **Full (strict)**.

## Шаг 4. Caddy как обратный прокси (апгрейд WebSocket)

Caddy — самый простой прокси: WebSocket он проксирует автоматически.

```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudflare.com/...' 2>/dev/null || true   # см. официальную инструкцию Caddy при желании
# Проще всего:
sudo apt install -y caddy   # если пакет доступен; иначе поставь Caddy по инструкции с caddyserver.com/docs/install
```

Конфиг `/etc/caddy/Caddyfile` (замени домен на свой):

```
chat.твойдомен.xyz {
    tls /etc/caddy/cf-origin.pem /etc/caddy/cf-origin.key
    reverse_proxy 127.0.0.1:8765
}
```

Применить:

```bash
sudo systemctl restart caddy
sudo systemctl status caddy      # active (running)
```

Открой порт в фаерволе (и внутри, и в панели хостера, если есть):

```bash
sudo ufw allow 443/tcp
```

## Шаг 5. Настройка мода (у всех игроков)

ClickGui → Friends → **Messenger**:

| Настройка | Значение |
|---|---|
| Server URL | `wss://chat.твойдомен.xyz`  (без порта — это 443) |
| Key | тот же секретный ключ |

Мод (java.net.http) поддерживает `wss://` из коробки; сертификат домена —
доверенный сертификат Cloudflare, так что проверка проходит автоматически.

## Шаг 6. Проверка

- На VPS: `sudo journalctl -u caddy -f` и `sudo journalctl -u kafka-relay -f`.
- Зайди в игре в `/kafka pm` — в шапке должно стать `● релей` (зелёным), в чате
  `[Мессенджер] подключено к релею`.
- Снаружи можно проверить рукопожатие: `curl -I https://chat.твойдомен.xyz`
  должен ответить (через Cloudflare), а не висеть.

---

## Быстрый вариант без сертификата (менее безопасно)

Если не хочешь возиться с origin-сертификатом:

1. Проброс **80** (не 443) `→ 192.168.122.28:80`.
2. Cloudflare → SSL/TLS → **Flexible**.
3. Caddyfile:
   ```
   http://chat.твойдомен.xyz {
       reverse_proxy 127.0.0.1:8765
   }
   ```
4. В моде — всё равно `wss://chat.твойдомен.xyz` (Cloudflare сам даёт TLS
   наружу).

Минус: участок Cloudflare → твой VPS идёт без шифрования (по нему проходит и
секретный ключ). Для игрового чата терпимо, но `Full (strict)` из основной
инструкции надёжнее.

## Если сменился IP VPS

Просто поменяй `Content` у A-записи в Cloudflare — у всех игроков заработает
сразу, адрес `wss://chat.твойдомен.xyz` в моде трогать не надо. Это ещё один
плюс домена перед голым IP.
