# Proyecto Final de Desarrollo de Software

## Ejecutar la API

La aplicación expone la API REST en el puerto `4567` por defecto. El puerto y la
tasa COP de respaldo se pueden configurar con `PORT` y `TASA_DEFECTO`.

```text
mvn package
java -jar target/ProyectoFinalDesarolloSoft-1.0-SNAPSHOT.jar
```

Inicia sesión con `POST /login` enviando `{"username":"admin","password":"admin123"}`.
Envía el token recibido en las demás rutas protegidas mediante
`Authorization: Bearer <token>`.

Las rutas para gestión de productos, clientes, boosters y torneos requieren rol
administrador. Las compras, aperturas de boosters e inscripciones a torneos
requieren rol cliente.
