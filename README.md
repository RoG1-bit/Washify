# Washify

## Integración de Firebase

Washify usa Firebase Authentication para iniciar sesión con correo y contraseña o Google.
La app consulta la base de Firestore con ID `default`. En `usuarios/{uid}`, las subcolecciones
`clientes`, `vehiculos` y `suscripciones` guardan los datos asociados a cada cuenta.

La pantalla de Reportes e Historial guarda los lavados en `lavados` con la placa, el nombre
del cliente, el plan, la fecha del servidor y el UID del administrador que registra el servicio.
Cuando el cliente tiene una cuenta Firebase vinculada, el registro incluye `clienteUid` y
aparece en su historial por UID. Los registros de fichas manuales sin cuenta vinculada quedan
visibles en el panel administrativo.

### Preparar el proyecto de pruebas

1. En Firebase Console, habilita los proveedores de correo y contraseña y Google. Usa la base
   de Firestore con ID `default` del proyecto del equipo.
2. En Authentication, crea la cuenta de prueba `admin.washify@example.com` con una contraseña
   única. El menú reconoce ese correo de ejemplo para mostrar el panel administrativo.
3. Copia el UID de esa cuenta desde Authentication. En Firestore, crea un documento en
   `administradores` con ese UID como ID y el campo booleano `activo` en `true`.
   El servidor usa ese documento para autorizar el registro de lavados.
4. Combina las reglas de [firestore.rules](firestore.rules) con las del proyecto y publícalas
   en la base `default`. Incluye el acceso por UID a `usuarios/{userId}`, las reglas de
   `lavados` y las consultas administrativas a clientes, vehículos y suscripciones. Revisa
   los permisos que se superponen, porque Firestore permite una operación si alguna regla
   aplicable la autoriza.
5. Para probar Google, registra el SHA-1 de la firma usada para instalar la app y descarga
   el `google-services.json` actualizado en `app/`.

### Probar la persistencia

1. Registra una cuenta de cliente con correo e inicia sesión. En una sesión aparte,
   comprueba también el acceso con Google.
2. Registra un vehículo y asígnale una suscripción activa. Comprueba ambos documentos
   debajo de `usuarios/{uid}` y que los datos vuelvan a aparecer al reabrir la app.
3. Cierra sesión e inicia con la cuenta administradora. En **Reportes e Historial**,
   elige la opción que muestre la placa y el cliente correctos y registra el lavado.
4. En Firestore, verifica el documento en `lavados` y sus campos `clienteUid`,
   `registradoPorUid` y `fechaHora`. Vuelve a iniciar sesión con el cliente, cierra la app,
   ábrela de nuevo y confirma que el lavado siga apareciendo.
5. Prueba con una segunda cuenta de cliente para comprobar que consulta únicamente sus
   propios lavados.

El reporte relaciona cada suscripción con su vehículo y su cliente usando el UID de la
cuenta y los IDs de sus documentos. Las cuentas pueden tener los mismos IDs numéricos.

Fuentes

- [Reglas de seguridad de Firestore](https://firebase.google.com/docs/firestore/security/rules-conditions)
- [Cambios en tiempo real](https://firebase.google.com/docs/firestore/query-data/listen)
- [Autenticación con Google en Android](https://firebase.google.com/docs/auth/android/google-signin)
