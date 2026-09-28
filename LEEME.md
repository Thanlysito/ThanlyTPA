# ThanlyTPA

Plugin de TPA para Paper 1.21.x con menús, botones clicables en el chat y espera antes de teletransportar.

## Comandos
| Comando | Qué hace |
|---|---|
| `/tpa <jugador>` | Pedir ir hacia un jugador. Sin nombre abre un menú con los jugadores conectados. |
| `/tpahere <jugador>` | Pedirle a un jugador que venga a ti. Sin nombre abre el menú. |
| `/tpaccept [jugador]` | Aceptar. Sin nombre: si hay una solicitud la acepta; si hay varias abre el menú. |
| `/tpadeny [jugador]` | Rechazar (igual que arriba). |
| `/tpamenu` (o `/tpagui`) | Menú con tus solicitudes: clic izquierdo acepta, clic derecho rechaza. |
| `/tpareload` | Recarga la configuración (solo admins). |

Permisos: `thanlytpa.use` (todos), `thanlytpa.bypass.delay`, `thanlytpa.bypass.cooldown`, `thanlytpa.admin` (OP).

Configuración en `plugins/ThanlyTPA/config.yml` (espera, cancelar al moverse o recibir daño, tiempo para vencer, espera entre solicitudes, sonidos) e idiomas en `plugins/ThanlyTPA/lang/` (`en` y `es`).

## 1. Compilar (crear el .jar)
1. Abre una terminal en esta carpeta (en el Explorador: clic en la barra de dirección, escribe `cmd` y Enter).
2. Escribe:
   ```
   gradlew build
   ```
   La primera vez tarda un poco porque descarga Gradle y la API de Paper.
3. El plugin queda en `build\libs\ThanlyTPA-1.0.0.jar`.

## 2. Probarlo en un servidor en tu PC
1. Descarga Paper **1.21.1** desde https://papermc.io/downloads/paper (elige la versión 1.21.1).
2. Pon el .jar de Paper en una carpeta nueva, por ejemplo `mc_plugins\servidor`, y ahí ejecuta:
   ```
   java -Xmx2G -jar paper-1.21.1-XXX.jar --nogui
   ```
   (cambia `XXX` por el número del archivo que descargaste).
3. La primera vez se detiene: abre `eula.txt`, cambia `eula=false` por `eula=true` y vuelve a ejecutar.
4. Cierra el servidor (escribe `stop`), copia `ThanlyTPA-1.0.0.jar` a `servidor\plugins\` y enciéndelo otra vez.
5. Entra con Minecraft 1.21.1 → Multijugador → Conexión directa → `localhost`.

Para probar el TPA hacen falta dos jugadores: pide a un amigo que se conecte a tu servidor o prueba los menús y mensajes con otra cuenta.
