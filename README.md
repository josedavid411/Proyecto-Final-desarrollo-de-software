# Cards4Play API

Backend REST para Cards4Play, empresa de productos de cartas coleccionables (TCG) y torneos. Centraliza el inventario, las ventas, los boosters y los torneos. Proyecto Final de Desarrollo de Software, Institución Universitaria EAM, Armenia, Colombia.

## Integrantes

- Jose David Hernández
- Santiago Rojas G

## Tecnologías

- Java 25 y Maven
- Spark Java 2.9.4 (microframework REST)
- Gson (JSON y persistencia en archivos de texto)
- Docker (imagen de dos etapas y volumen para los datos)
- Despliegue en PaaS: [pegar plataforma y URL pública aquí]

## Estructura del proyecto

```
src/main/java/com/mycompany/proyectofinaldesarollosoft
├── Main.java          Arranque: lee PORT y crea la Tienda
├── Rutas.java         Endpoints REST, autenticación Basic y errores HTTP
├── controller/        Controladores (usuarios, productos, boosters, compras, torneos)
├── enums/             Rareza (con probabilidades) y Rol
├── excepcion/         Excepciones que se traducen a 401, 403, 404 y 409
├── model/             Usuario, Administrador, Cliente, Producto, Carta, Booster,
│                      ProductoSellado, Accesorio, Torneo, Compra, Inventario, Tienda
└── servicio/          ServicioTasaCambio y PersistenciaJson
data/tienda.json       Datos persistidos (se crea al iniciar)
Dockerfile             Imagen de la aplicación
postman/               Colección de Postman
```

## Ejecución con Docker

Construir la imagen:

```
docker build -t cards4play-api .
```

Ejecutar el contenedor con volumen para los datos y la tasa por defecto:

```
docker run -d -p 4567:4567 -v cards4play_data:/app/data -e TASA_DEFECTO=4200.00 --name cards4play-app cards4play-api
```

Ver los registros de la aplicación:

```
docker logs cards4play-app
```

Probar la persistencia: eliminar y volver a crear el contenedor con el mismo volumen.

```
docker rm -f cards4play-app
docker run -d -p 4567:4567 -v cards4play_data:/app/data -e TASA_DEFECTO=4200.00 --name cards4play-app cards4play-api
```

Los datos se conservan porque `/app/data` está montado en el volumen `cards4play_data`.

## Ejecución local sin Docker

```
mvn clean package -DskipTests
java -jar target/ProyectoFinalDesarolloSoft-1.0-SNAPSHOT.jar
```

La API queda en `http://localhost:4567` (o en el puerto de la variable `PORT`).

## Usuario por defecto

- Usuario: `admin`
- Contraseña: `admin123`
- Rol: ADMIN. Es el único que registra clientes, cartas, productos, boosters y torneos.

## Autenticación

HTTP Basic: cada petición lleva el encabezado `Authorization: Basic <usuario:contraseña en base64>`. Sin credenciales o con credenciales inválidas responde 401. Si el rol no corresponde, responde 403. Solo `/salud` es público.

## API externa y tasa por defecto

- API externa: [open.er-api.com](https://open.er-api.com/v6/latest/USD), que devuelve la tasa USD a COP (`rates.COP`).
- Tiempo máximo de espera: 3 segundos.
- Si la API falla, se usa la variable de entorno `TASA_DEFECTO` y se imprime en consola: `La API no respondió, por lo que se toma un valor por defecto de X`.
- **Valor por defecto: `4000.00`** (definido con `ENV` en el Dockerfile). Se puede cambiar con `-e TASA_DEFECTO=4200.00` al ejecutar el contenedor.
- Las respuestas con precios en COP incluyen `"fuenteTasa": "API"` o `"fuenteTasa": "DEFECTO"`.

## Reglas de los boosters

- Solo el administrador los crea. Se asignan exactamente 10 cartas.
- Cada carta: primero se sortea la rareza y luego se elige al azar una carta de esa rareza.

| Rareza | Código | Probabilidad |
|---|---|---|
| Normal | N | 50 % |
| Poco común | UC | 25 % |
| Raro | R | 15 % |
| Súper raro | SR | 7 % |
| Ultra raro | UR | 3 % |

- Regla de respaldo: si no hay cartas de la rareza sorteada, se usa la inferior más cercana con cartas; si no hay, la superior más cercana. Si no hay ninguna carta registrada, responde 409.
- Bonificación: cada booster incluye al menos una carta R o superior (si existen cartas de esas rarezas), sin alterar la tabla para las demás.
- El contenido es oculto hasta que su dueño lo abre, una única vez.

## Endpoints

| Método | Ruta | Rol | Descripción | Códigos |
|---|---|---|---|---|
| GET | `/salud` | Público | Estado de la API | 200 |
| POST | `/clientes` | ADMIN | Registra un cliente (nombre, username, password) | 201, 400, 401, 403, 409 |
| GET | `/clientes` | ADMIN | Lista los clientes | 200, 401, 403 |
| POST | `/cartas` | ADMIN | Registra una carta (nombre, rareza, precioUSD) | 201, 400, 401, 403 |
| POST | `/sellados` | ADMIN | Registra un producto sellado | 201, 400, 401, 403 |
| POST | `/accesorios` | ADMIN | Registra un accesorio | 201, 400, 401, 403 |
| GET | `/productos` | Autenticado | Inventario con precioUSD, precioCOP y fuenteTasa | 200, 401 |
| POST | `/boosters` | ADMIN | Crea un booster (nombre, precioUSD), sin revelar cartas | 201, 400, 401, 403, 409 |
| GET | `/boosters` | Autenticado | Boosters disponibles, sin revelar cartas | 200, 401 |
| POST | `/compras` | CLIENTE | Compra un producto (productoId) | 201, 400, 401, 403, 404, 409 |
| GET | `/compras/historial` | CLIENTE | Historial propio (boosters abiertos con cartas) | 200, 401, 403 |
| GET | `/clientes/{id}/compras` | ADMIN | Historial de un cliente | 200, 401, 403, 404 |
| POST | `/boosters/{id}/abrir` | CLIENTE | Abre un booster propio, una sola vez | 200, 401, 403, 404, 409 |
| POST | `/torneos` | ADMIN | Crea un torneo (nombre, fecha AAAA-MM-DD, maxParticipantes) | 201, 400, 401, 403 |
| GET | `/torneos` | Autenticado | Lista los torneos | 200, 401 |
| POST | `/torneos/{id}/inscripciones` | CLIENTE | Inscribe al cliente en el torneo | 201, 401, 403, 404, 409 |

Todos los errores responden con el mismo cuerpo JSON:

```json
{"error": "descripción del problema"}
```

Códigos usados: 200, 201, 400, 401, 403, 404 y 409.

## Persistencia

Los datos se guardan en `data/tienda.json` y se conservan entre ejecuciones. Con Docker, esa carpeta es un volumen, así que se mantienen aunque el contenedor se elimine y se cree de nuevo. En el PaaS los datos pueden reiniciarse; lo único obligatorio es que la aplicación inicie con el administrador por defecto.

## Pruebas con Postman

La colección está en `postman/Cards4Play.postman_collection.json` e incluye casos exitosos y de error. Se importa en Postman y se ejecuta con el Collection Runner, en orden y con datos vacíos. La variable `baseUrl` es `http://localhost:4567` para pruebas locales; para el despliegue se cambia por la URL pública.

## Despliegue

URL pública: https://proyecto-final-desarrollo-de-software-production.up.railway.app/inventario

## Flujo de trabajo con Git

- `main` contiene siempre una versión estable.
- Cada funcionalidad se desarrolla en su propia rama (por ejemplo `feature/boosters`, `feature/api-rest`, `feature/docker`) y se integra a `main` con merge.
- Todos los integrantes tienen commits con mensajes descriptivos. 
