# Washify

## Historial de lavados en Firestore

La pantalla de Reportes e Historial guarda los lavados en `lavados` con la placa, el nombre
del cliente, el plan, la fecha del servidor y el UID del administrador que registra el servicio.
Cuando el cliente tiene una cuenta Firebase vinculada, el registro incluye `clienteUid` y
aparece en su historial por UID. Los registros de fichas manuales sin cuenta vinculada quedan
visibles en el panel administrativo.

### Preparar el proyecto de pruebas

1. En Firebase Console, habilita el proveedor de correo y contraseña y crea Cloud Firestore
   en el proyecto del equipo.
2. En Authentication, crea la cuenta de prueba `admin.washify@example.com` con una contraseña
   única. El menú reconoce ese correo de ejemplo para mostrar el panel administrativo.
3. Copia el UID de esa cuenta desde Authentication. En Firestore, crea un documento en
   `administradores` con ese UID como ID y el campo booleano `activo` en `true`.
   El servidor usa ese documento para autorizar el registro de lavados.
4. Publica las reglas de [firestore.rules](firestore.rules), que incluyen el acceso del equipo
   a `usuarios/{userId}` y las reglas del historial. Si el equipo tiene otras reglas, combina
   la función `administraHistorial` y el bloque `match /lavados/{lavadoId}` con las existentes.
   Revisa cualquier regla general que también coincida con `lavados`, porque Firestore
   concede acceso si cualquiera de las reglas aplicables lo permite.
5. Para probar Google, activa el proveedor, registra el SHA-1 de la firma de depuración
   y descarga el `google-services.json` actualizado en `app/`. El acceso requiere que Firebase
   confirme el inicio de sesión.

### Probar la persistencia

1. Registra una cuenta de cliente con correo e inicia sesión.
2. Registra un vehículo y asígnale una suscripción activa. La ficha del cliente queda
   vinculada al UID que devolvió Firebase Authentication.
3. Cierra sesión desde la app e inicia con la cuenta administradora. En **Reportes e Historial**,
   elige el ID de la suscripción y registra el lavado.
4. En Firestore verifica el documento en `lavados` y sus campos `clienteUid`,
   `registradoPorUid` y `fechaHora`. Vuelve a iniciar sesión con el cliente, cierra la app,
   ábrela de nuevo y confirma que el lavado siga apareciendo.
5. Prueba con una segunda cuenta de cliente para comprobar que consulta únicamente sus
   propios lavados.

El selector de suscripciones de esta versión usa las fichas del proceso de la app. Completa
los pasos 1 a 3 en la misma ejecución. Al integrar vehículos y suscripciones con Firestore,
carga el UID del dueño en `Cliente.firebaseUid` cuando reconstruyas la suscripción.

Fuentes

- [Reglas de seguridad de Firestore](https://firebase.google.com/docs/firestore/security/rules-conditions)
- [Cambios en tiempo real](https://firebase.google.com/docs/firestore/query-data/listen)
- [Autenticación con Google en Android](https://firebase.google.com/docs/auth/android/google-signin)
