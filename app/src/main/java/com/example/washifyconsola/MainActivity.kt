package com.example.washifyconsola

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.consola.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

private enum class Rol {
    CLIENTE,
    ADMINISTRADOR
}

private enum class Pantalla {
    LOGIN,
    REGISTRO,
    INICIO_CLIENTE,
    INICIO_ADMIN,
    CLIENTES,
    VEHICULOS,
    PLANES,
    SUSCRIPCIONES,
    HISTORIAL,
    PERFIL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WashifyApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WashifyApp() {
    var pantalla by remember { mutableStateOf(Pantalla.LOGIN) }
    val context = LocalContext.current
    
    // Firebase Auth
    val auth = remember { FirebaseAuth.getInstance() }
    
    // Form States
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var mostrarPassword by remember { mutableStateOf(false) }
    var autenticando by remember { mutableStateOf(false) }

    // Session States
    var nombreActual by remember { mutableStateOf("") }
    var correoActual by remember { mutableStateOf("") }
    var rolActual by remember { mutableStateOf<Rol?>(null) }
    var clienteIdActual by remember { mutableStateOf<Int?>(null) }
    var uidActual by remember { mutableStateOf<String?>(null) }
    var clientesIniciales by remember { mutableStateOf(emptyList<Cliente>()) }

    val repositorio = remember { FirestoreRepository() }
    val gestorClientes = remember(uidActual) {
        GestorClientes().apply { sincronizarClientes(clientesIniciales) }
    }
    val gestorVehiculos = remember(uidActual) { GestorVehiculos() }
    val clientes = remember(uidActual) { mutableStateListOf<Cliente>() }
    val gestorSuscripciones = remember(gestorClientes, gestorVehiculos) {
        GestorSuscripciones(
            buscarCliente = { id -> gestorClientes.obtenerClientes().find { it.id == id } },
            buscarVehiculo = { id -> gestorVehiculos.obtenerTodos().find { it.id == id } }
        )
    }

    fun fallarAcceso(error: String) {
        auth.signOut()
        autenticando = false
        mensaje = error
    }

    fun procesarLoginExitoso(usuario: FirebaseUser) {
        val correo = usuario.email
        if (correo.isNullOrBlank()) {
            fallarAcceso("La cuenta debe tener un correo electrónico.")
            return
        }

        val uid = usuario.uid
        val nombreFirebase = usuario.displayName?.takeIf { it.isNotBlank() } ?: correo.substringBefore('@')
        val esAdminUser = correo.equals("admin.washify@example.com", ignoreCase = true)

        fun entrar(lista: List<Cliente>, cliente: Cliente?) {
            if (auth.currentUser?.uid != uid) return
            clientesIniciales = lista
            clienteIdActual = cliente?.id
            nombreActual = cliente?.nombre ?: nombreFirebase
            correoActual = correo
            rolActual = if (esAdminUser) Rol.ADMINISTRADOR else Rol.CLIENTE
            uidActual = uid
            mensaje = ""
            autenticando = false
            pantalla = if (esAdminUser) Pantalla.INICIO_ADMIN else Pantalla.INICIO_CLIENTE
        }

        repositorio.obtenerClientes(
            uid,
            onSuccess = { lista ->
                if (esAdminUser) {
                    entrar(lista, null)
                } else {
                    val anterior = lista.find { it.firebaseUid == uid }
                        ?: lista.find { it.correo.equals(correo, ignoreCase = true) }
                    if (anterior?.firebaseUid == uid) {
                        entrar(lista, anterior)
                    } else {
                        val cliente = anterior?.copy(firebaseUid = uid)
                            ?: Cliente((lista.maxOfOrNull { it.id } ?: 0) + 1, nombreFirebase, correo, uid)
                        repositorio.guardarCliente(
                            uid,
                            cliente,
                            onSuccess = { entrar(lista.filterNot { it.id == cliente.id } + cliente, cliente) },
                            onError = { fallarAcceso("No se pudo guardar el perfil: $it") }
                        )
                    }
                }
            },
            onError = { fallarAcceso("No se pudo consultar el perfil: $it") }
        )
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = try {
            GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
        } catch (e: ApiException) {
            fallarAcceso("No se completó el acceso con Google (código ${e.statusCode}).")
            return@rememberLauncherForActivityResult
        }

        val idToken = account?.idToken
        if (idToken.isNullOrBlank()) {
            fallarAcceso("Google no devolvió un token de acceso para Firebase.")
            return@rememberLauncherForActivityResult
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            val usuario = if (task.isSuccessful) task.result?.user else null
            if (usuario != null) {
                procesarLoginExitoso(usuario)
            } else {
                fallarAcceso("Firebase no pudo iniciar sesión con Google. Inténtalo nuevamente.")
            }
        }
    }

    fun iniciarGoogleSignIn() {
        if (autenticando) return
        try {
            val idRecurso = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (idRecurso == 0) {
                mensaje = "Configura Google en Firebase y actualiza google-services.json."
                return
            }

            val opciones = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(context.getString(idRecurso))
                .build()
            val googleSignInClient = GoogleSignIn.getClient(context, opciones)
            autenticando = true
            mensaje = ""
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        } catch (e: Exception) {
            fallarAcceso("Error al abrir ventana de Google: ${e.localizedMessage}")
        }
    }

    val esAdmin = rolActual == Rol.ADMINISTRADOR

    fun cerrarSesion() {
        try { auth.signOut() } catch (_: Exception) {}
        nombreActual = ""
        correoActual = ""
        rolActual = null
        clienteIdActual = null
        clientesIniciales = emptyList()
        uidActual = null
        email = ""
        password = ""
        confirmPassword = ""
        autenticando = false
        mensaje = ""
        pantalla = Pantalla.LOGIN
    }

    // Header Bar helper
    @Composable
    fun HeaderBar(titulo: String, mostrarVolver: Boolean = true) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (nombreActual.isNotBlank()) {
                        Text(
                            text = "Usuario: $nombreActual (${rolActual?.name ?: ""})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (nombreActual.isNotBlank() && pantalla != Pantalla.PERFIL) {
                        TextButton(onClick = { pantalla = Pantalla.PERFIL }) {
                            Text("Perfil")
                        }
                    }
                    if (mostrarVolver && pantalla != Pantalla.INICIO_CLIENTE && pantalla != Pantalla.INICIO_ADMIN && pantalla != Pantalla.LOGIN) {
                        Button(
                            onClick = {
                                mensaje = ""
                                pantalla = if (esAdmin) Pantalla.INICIO_ADMIN else Pantalla.INICIO_CLIENTE
                            }
                        ) {
                            Text("Menú")
                        }
                    }
                }
            }
        }
    }

    when (pantalla) {
        Pantalla.LOGIN -> {
            ColumnaBase {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Washify 🚗✨",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Sistema Premium de Gestión de Lavado",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo electrónico") },
                            placeholder = { Text("ejemplo@washify.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { mostrarPassword = !mostrarPassword }) {
                                    Text(if (mostrarPassword) "Ocultar" else "Ver")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (mensaje.isNotBlank()) {
                            Text(
                                text = mensaje,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                val correoIngresado = email.trim()
                                val regexEmail = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$")

                                if (correoIngresado.isBlank() || password.isBlank()) {
                                    mensaje = "Por favor ingresa tu correo y contraseña."
                                    return@Button
                                }

                                if (!regexEmail.matches(correoIngresado)) {
                                    mensaje = "El formato de correo electrónico es inválido."
                                    return@Button
                                }

                                autenticando = true
                                mensaje = ""
                                auth.signInWithEmailAndPassword(correoIngresado, password)
                                    .addOnCompleteListener { task ->
                                        val usuario = if (task.isSuccessful) task.result?.user else null
                                        if (usuario != null) {
                                            procesarLoginExitoso(usuario)
                                        } else {
                                            fallarAcceso("No se pudo iniciar sesión. Verifica tus datos e inténtalo nuevamente.")
                                        }
                                    }
                            },
                            enabled = !autenticando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (autenticando) "Conectando…" else "Iniciar sesión", fontSize = 16.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                iniciarGoogleSignIn()
                            },
                            enabled = !autenticando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🌐 Continuar con Google")
                        }

                        TextButton(
                            onClick = {
                                nombre = ""
                                email = ""
                                password = ""
                                confirmPassword = ""
                                mensaje = ""
                                pantalla = Pantalla.REGISTRO
                            },
                            enabled = !autenticando
                        ) {
                            Text("¿No tienes cuenta? Regístrate aquí")
                        }
                    }
                }
            }
        }

        Pantalla.REGISTRO -> {
            ColumnaBase {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Crear Cuenta Washify 📝",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre completo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo electrónico") },
                            placeholder = { Text("ejemplo@washify.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña (mínimo 6 caracteres)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirmar Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (mensaje.isNotBlank()) {
                            Text(
                                text = mensaje,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(
                            onClick = {
                                val nombreIngresado = nombre.trim()
                                val correo = email.trim()
                                val regexEmail = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$")

                                when {
                                    nombreIngresado.length < 3 -> {
                                        mensaje = "Ingresa un nombre de al menos 3 caracteres."
                                    }
                                    !regexEmail.matches(correo) -> {
                                        mensaje = "El correo electrónico no tiene un formato válido."
                                    }
                                    password.length < 6 -> {
                                        mensaje = "La contraseña debe tener al menos 6 caracteres."
                                    }
                                    password != confirmPassword -> {
                                        mensaje = "Las contraseñas no coinciden."
                                    }
                                    else -> {
                                        autenticando = true
                                        mensaje = ""
                                        auth.createUserWithEmailAndPassword(correo, password)
                                            .addOnCompleteListener { task ->
                                                val usuario = if (task.isSuccessful) task.result?.user else null
                                                if (usuario != null) {
                                                    fun volverAlAcceso(resultado: String) {
                                                        auth.signOut()
                                                        autenticando = false
                                                        nombre = ""
                                                        email = correo
                                                        password = ""
                                                        confirmPassword = ""
                                                        mensaje = resultado
                                                        pantalla = Pantalla.LOGIN
                                                    }
                                                    repositorio.guardarCliente(
                                                        usuario.uid,
                                                        Cliente(1, nombreIngresado, correo, usuario.uid),
                                                        onSuccess = { volverAlAcceso("¡Cuenta creada con éxito! Ya puedes iniciar sesión.") },
                                                        onError = { volverAlAcceso("Cuenta creada. Inicia sesión para completar el perfil.") }
                                                    )
                                                } else {
                                                    fallarAcceso("Error al crear cuenta: ${task.exception?.localizedMessage}")
                                                }
                                            }
                                    }
                                }
                            },
                            enabled = !autenticando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (autenticando) "Conectando…" else "Registrarme")
                        }

                        OutlinedButton(
                            onClick = {
                                iniciarGoogleSignIn()
                            },
                            enabled = !autenticando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🌐 Registrarme con Google")
                        }

                        TextButton(
                            onClick = {
                                mensaje = ""
                                pantalla = Pantalla.LOGIN
                            },
                            enabled = !autenticando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver al Inicio de Sesión")
                        }
                    }
                }
            }
        }

        Pantalla.INICIO_CLIENTE -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderBar(titulo = "Inicio Cliente", mostrarVolver = false)

                Text(
                    text = "¡Hola, $nombreActual! 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Gestiona tus vehículos, suscripciones y servicios de lavado.")

                TarjetaMenu("🚘 Mis Vehículos", "Registra y consulta tus carros") {
                    mensaje = ""
                    pantalla = Pantalla.VEHICULOS
                }

                TarjetaMenu("💎 Planes de Lavado", "Conoce nuestros planes ilimitados y básicos") {
                    mensaje = ""
                    pantalla = Pantalla.PLANES
                }

                TarjetaMenu("📋 Mis Suscripciones", "Asigna planes a tus carros y renueva vigencias") {
                    mensaje = ""
                    pantalla = Pantalla.SUSCRIPCIONES
                }

                TarjetaMenu("📜 Mi Historial de Lavados", "Consulta el historial de servicios realizados") {
                    mensaje = ""
                    pantalla = Pantalla.HISTORIAL
                }

                TarjetaMenu("👤 Mi Perfil", "Ver y editar tu cuenta") {
                    mensaje = ""
                    pantalla = Pantalla.PERFIL
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { cerrarSesion() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Pantalla.INICIO_ADMIN -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderBar(titulo = "Panel de Administración", mostrarVolver = false)

                Text(
                    text = "Bienvenido, $nombreActual 🛠️",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Control total del sistema Washify")

                TarjetaMenu("👥 Gestión de Clientes", "Consultar clientes registrados en el sistema") {
                    clientes.clear()
                    clientes.addAll(gestorClientes.obtenerClientes())
                    mensaje = ""
                    pantalla = Pantalla.CLIENTES
                }

                TarjetaMenu("🚘 Gestión de Vehículos", "Ver y administrar todos los vehículos") {
                    mensaje = ""
                    pantalla = Pantalla.VEHICULOS
                }

                TarjetaMenu("💎 Planes de Lavado", "Consultar características de los planes") {
                    mensaje = ""
                    pantalla = Pantalla.PLANES
                }

                TarjetaMenu("📋 Suscripciones Generales", "Ver, asignar, renovar o cancelar suscripciones") {
                    mensaje = ""
                    pantalla = Pantalla.SUSCRIPCIONES
                }

                TarjetaMenu("📊 Reportes e Historial", "Resumen del sistema y registro de lavados") {
                    mensaje = ""
                    pantalla = Pantalla.HISTORIAL
                }

                TarjetaMenu("👤 Mi Perfil Admin", "Ver información de tu cuenta administrativa") {
                    mensaje = ""
                    pantalla = Pantalla.PERFIL
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { cerrarSesion() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Pantalla.CLIENTES -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderBar(titulo = "Gestión de Clientes")

                PantallaClientes(
                    gestor = gestorClientes,
                    clientes = clientes,
                    uid = uidActual,
                    onVolver = {
                        pantalla = Pantalla.INICIO_ADMIN
                    }
                )
            }
        }

        Pantalla.VEHICULOS -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderBar(titulo = if (esAdmin) "Administrar Vehículos" else "Mis Vehículos")

                PantallaVehiculos(
                    gestor = gestorVehiculos,
                    clienteId = clienteIdActual,
                    esAdministrador = esAdmin,
                    clientes = gestorClientes.obtenerClientes(),
                    uid = uidActual,
                    onVolver = {
                        pantalla = if (esAdmin) Pantalla.INICIO_ADMIN else Pantalla.INICIO_CLIENTE
                    }
                )
            }
        }

        Pantalla.PLANES -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderBar(titulo = "Planes de Lavado")

                PantallaPlanes(
                    esAdministrador = esAdmin,
                    onVolver = {
                        pantalla = if (esAdmin) Pantalla.INICIO_ADMIN else Pantalla.INICIO_CLIENTE
                    }
                )
            }
        }

        Pantalla.SUSCRIPCIONES -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderBar(titulo = "Suscripciones")

                PantallaSuscripciones(
                    gestorSuscripciones = gestorSuscripciones,
                    gestorVehiculos = gestorVehiculos,
                    gestorClientes = gestorClientes,
                    clienteId = clienteIdActual,
                    esAdministrador = esAdmin,
                    uid = uidActual,
                    onVolver = {
                        pantalla = if (esAdmin) Pantalla.INICIO_ADMIN else Pantalla.INICIO_CLIENTE
                    }
                )
            }
        }

        Pantalla.HISTORIAL -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderBar(titulo = "Historial & Servicios")

                PantallaHistorial(
                    uid = uidActual,
                    esAdministrador = esAdmin,
                    onVolver = {
                        pantalla = if (esAdmin) Pantalla.INICIO_ADMIN else Pantalla.INICIO_CLIENTE
                    }
                )
            }
        }

        Pantalla.PERFIL -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderBar(titulo = "Mi Perfil")

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Detalles de la Cuenta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Divider()
                        Text("Nombre: $nombreActual", style = MaterialTheme.typography.bodyLarge)
                        Text("Correo: $correoActual", style = MaterialTheme.typography.bodyLarge)
                        Text("Rol en el Sistema: ${rolActual?.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (clienteIdActual != null) {
                            Text("ID de Cliente: #$clienteIdActual", style = MaterialTheme.typography.bodyLarge)
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                pantalla = if (esAdmin) Pantalla.INICIO_ADMIN else Pantalla.INICIO_CLIENTE
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("← Volver al Menú Principal")
                        }

                        OutlinedButton(
                            onClick = { cerrarSesion() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PantallaVehiculos(
    gestor: GestorVehiculos,
    clienteId: Int?,
    esAdministrador: Boolean,
    clientes: List<Cliente>,
    uid: String?,
    onVolver: () -> Unit
) {
    var placa by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var mostrarConfirmacionEliminar by remember { mutableStateOf(false) }
    var vehiculoPendienteEliminar by remember { mutableStateOf<Vehiculo?>(null) }
    var clienteIdTexto by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var listaCargada by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    val vehiculos = remember { mutableStateListOf<Vehiculo>() }
    val firestoreRepository = remember { FirestoreRepository() }

    fun actualizarLista() {
        listaCargada = false
        if (uid != null) {
            firestoreRepository.obtenerVehiculos(
                uid = uid,
                onSuccess = { lista ->
                    gestor.sincronizarVehiculos(lista)

                    vehiculos.clear()

                    val listaFiltrada = if (esAdministrador) {
                        lista
                    } else {
                        lista.filter { it.clienteId == clienteId }
                    }

                    vehiculos.addAll(listaFiltrada)
                    listaCargada = true
                },
                onError = { error ->
                    mensaje = "Error al consultar vehículos: $error"
                }
            )
        } else {
            // Mantiene el funcionamiento de las cuentas locales de prueba.
            vehiculos.clear()

            val lista = if (esAdministrador) {
                gestor.obtenerTodos()
            } else {
                clienteId?.let {
                    gestor.obtenerVehiculosPorCliente(it)
                } ?: emptyList()
            }

            vehiculos.addAll(lista)
            listaCargada = true
        }
    }

    LaunchedEffect(uid, clienteId, esAdministrador) {
        actualizarLista()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Registrar Nuevo Vehículo 🚗",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (esAdministrador) {
                OutlinedTextField(
                    value = clienteIdTexto,
                    onValueChange = { clienteIdTexto = it },
                    label = { Text("ID del Cliente Propietario") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = placa,
                onValueChange = { placa = it.uppercase() },
                label = { Text("Placa del vehículo (Ej: P123-456)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = marca,
                onValueChange = { marca = it },
                label = { Text("Marca (Ej: Toyota, Nissan)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = modelo,
                onValueChange = { modelo = it },
                label = { Text("Modelo (Ej: Corolla, Sentra)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (mensaje.isNotBlank()) {
                Text(
                    text = mensaje,
                    color = if (mensaje.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                enabled = listaCargada && !guardando,
                onClick = {
                    val propietarioId = if (esAdministrador) clienteIdTexto.toIntOrNull() else clienteId
                    val placaLimpia = placa.trim()
                    val marcaLimpia = marca.trim()
                    val modeloLimpio = modelo.trim()

                    val regexPlaca = Regex("^[A-Za-z0-9-]{3,10}$")

                    when {
                        placaLimpia.isBlank() || marcaLimpia.isBlank() || modeloLimpio.isBlank() -> {
                            mensaje = "Completa todos los campos (Placa, Marca y Modelo)."
                        }
                        !regexPlaca.matches(placaLimpia) -> {
                            mensaje = "Placa inválida. Ingresa entre 3 y 10 caracteres alfanuméricos."
                        }
                        propietarioId == null || clientes.none { it.id == propietarioId } -> {
                            mensaje = "Ingresa un ID de cliente válido y existente."
                        }
                        else -> {
                            val registrado = gestor.registrarVehiculo(
                                placa = placaLimpia,
                                marca = marcaLimpia,
                                modelo = modeloLimpio,
                                clienteId = propietarioId
                            )

                            if (registrado) {
                                val vehiculoRegistrado = gestor.obtenerTodos()
                                    .find { it.placa.equals(placaLimpia, ignoreCase = true) }

                                if (uid != null && vehiculoRegistrado != null) {
                                    guardando = true
                                    firestoreRepository.guardarVehiculo(
                                        uid = uid,
                                        vehiculo = vehiculoRegistrado,
                                        onSuccess = {
                                            guardando = false
                                            placa = ""
                                            marca = ""
                                            modelo = ""
                                            clienteIdTexto = ""
                                            actualizarLista()
                                            mensaje = "✅ Vehículo guardado en Firestore."
                                        },
                                        onError = { error ->
                                            gestor.eliminarVehiculo(vehiculoRegistrado.id)
                                            guardando = false
                                            mensaje = "ERROR FIRESTORE: $error"
                                            actualizarLista()
                                        }
                                    )
                                } else {
                                    actualizarLista()
                                    mensaje = if (uid == null) {
                                        "✅ Vehículo registrado localmente. Inicia sesión con Firebase para sincronizarlo."
                                    } else {
                                        "⚠️ No se pudo recuperar el vehículo registrado."
                                    }
                                    placa = ""
                                    marca = ""
                                    modelo = ""
                                    clienteIdTexto = ""
                                }
                            } else {
                                mensaje = "Ya existe un vehículo registrado con esa placa."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (guardando) "Guardando…" else "Registrar Vehículo")
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // List and Filter
    OutlinedTextField(
        value = busqueda,
        onValueChange = { busqueda = it },
        label = { Text("🔍 Buscar por placa, marca o modelo") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    val vehiculosFiltrados = vehiculos.filter {
        it.placa.contains(busqueda, ignoreCase = true) ||
                it.marca.contains(busqueda, ignoreCase = true) ||
                it.modelo.contains(busqueda, ignoreCase = true)
    }

    Text(
        text = "Vehículos Registrados (${vehiculosFiltrados.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    if (vehiculosFiltrados.isEmpty()) {
        Text("No se encontraron vehículos.")
    } else {
        vehiculosFiltrados.forEach { vehiculo ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${vehiculo.marca} ${vehiculo.modelo}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Placa: ${vehiculo.placa}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text("ID Vehículo: #${vehiculo.id}")
                    Text("ID Propietario: #${vehiculo.clienteId}")
                    
                    val planNom = vehiculo.plan?.nombre ?: "Sin plan activo"
                    Text(
                        text = "Plan Actual: $planNom",
                        fontWeight = if (vehiculo.plan != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (vehiculo.plan != null) MaterialTheme.colorScheme.primary else Color.Gray
                    )

                    OutlinedButton(
                        onClick = {
                            vehiculoPendienteEliminar = vehiculo
                            mostrarConfirmacionEliminar = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar Vehículo")
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onVolver,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Text("← Volver al Menú Principal")
    }
    if (mostrarConfirmacionEliminar) {
        AlertDialog(
            onDismissRequest = {
                mostrarConfirmacionEliminar = false
                vehiculoPendienteEliminar = null
            },
            title = {
                Text("Confirmar eliminación")
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar el vehículo " +
                            "${vehiculoPendienteEliminar?.placa}? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val vehiculo = vehiculoPendienteEliminar

                        if (vehiculo != null && uid != null) {
                            firestoreRepository.eliminarVehiculo(
                                uid = uid,
                                vehiculoId = vehiculo.id,
                                onSuccess = {
                                    gestor.eliminarVehiculo(vehiculo.id)
                                    actualizarLista()
                                    mensaje = "✅ Vehículo eliminado correctamente."
                                },
                                onError = { error ->
                                    mensaje = "Error al eliminar: $error"
                                }
                            )
                        } else {
                            mensaje = "No hay una sesión Firebase activa."
                        }

                        mostrarConfirmacionEliminar = false
                        vehiculoPendienteEliminar = null
                    }
                ) {
                    Text("Sí, eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmacionEliminar = false
                        vehiculoPendienteEliminar = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PantallaClientes(
    gestor: GestorClientes,
    clientes: MutableList<Cliente>,
    uid: String?,
    onVolver: () -> Unit
) {
    var nombreCliente by remember { mutableStateOf("") }
    var correoCliente by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    val firestoreRepository = remember { FirestoreRepository() }

    LaunchedEffect(uid) {
        if (uid != null) {
            firestoreRepository.obtenerClientes(
                uid = uid,
                onSuccess = { lista ->
                    gestor.sincronizarClientes(lista)

                    clientes.clear()
                    clientes.addAll(lista)
                },
                onError = { error ->
                    mensaje = "Error al cargar clientes: $error"
                }
            )
        }
    }


    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Registrar Nuevo Cliente 👤", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = nombreCliente,
                onValueChange = { nombreCliente = it },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = correoCliente,
                onValueChange = { correoCliente = it },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            if (mensaje.isNotBlank()) {
                Text(
                    text = mensaje,
                    color = if (mensaje.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    val nom = nombreCliente.trim()
                    val corr = correoCliente.trim()
                    val regexEmail = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$")

                    if (nom.length < 3) {
                        mensaje = "Ingresa un nombre de al menos 3 caracteres."
                    } else if (!regexEmail.matches(corr)) {
                        mensaje = "Ingresa un correo electrónico válido."
                    } else {
                        val exito = gestor.registrarCliente(nom, corr)

                        if (exito) {

                            val nuevoCliente = gestor.obtenerClientes().last()

                            if (uid != null) {

                                firestoreRepository.guardarCliente(
                                    uid = uid,
                                    cliente = nuevoCliente,
                                    onSuccess = {
                                        clientes.clear()
                                        clientes.addAll(gestor.obtenerClientes())

                                        nombreCliente = ""
                                        correoCliente = ""

                                        mensaje = "✅ Cliente guardado correctamente en Firebase."
                                    },
                                    onError = { error ->
                                        mensaje = "Error al guardar cliente: $error"
                                    }
                                )

                            } else {
                                mensaje = "No hay una sesión autenticada para guardar el cliente."
                            }

                        } else {
                            mensaje = "Ese correo ya pertenece a otro cliente."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrar Cliente")
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = busqueda,
        onValueChange = { busqueda = it },
        label = { Text("🔍 Buscar por nombre o correo") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    val clientesFiltrados = clientes.filter {
        it.nombre.contains(busqueda, ignoreCase = true) ||
                it.correo.contains(busqueda, ignoreCase = true)
    }

    Text("Lista de Clientes (${clientesFiltrados.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    if (clientesFiltrados.isEmpty()) {
        Text("No hay clientes que coincidan.")
    } else {
        clientesFiltrados.forEach { cliente ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(cliente.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("ID del Cliente: #${cliente.id}")
                    Text("Correo: ${cliente.correo}")
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onVolver,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Text("← Volver al Menú Principal")
    }
}

@Composable
private fun PantallaPlanes(
    esAdministrador: Boolean,
    onVolver: () -> Unit
) {
    val planes = listOf(PlanBasico(), PlanPremium())

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Conoce nuestros Planes de Lavado 💎",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        planes.forEach { plan ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = plan.nombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "\$${plan.precioMensual} / mes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Divider()

                    Text(
                        text = "• Lavados mensuales incluidos: ${plan.limiteLavadosMensuales ?: "ILIMITADOS"}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = if (plan is PlanPremium) "• Incluye encerado, aspirado profundo y secado a mano gratis."
                        else "• Incluye lavado exterior rápido y secado con microfibra.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("← Volver al Menú Principal")
        }
    }
}

@Composable
private fun PantallaSuscripciones(
    gestorSuscripciones: GestorSuscripciones,
    gestorVehiculos: GestorVehiculos,
    gestorClientes: GestorClientes,
    clienteId: Int?,
    esAdministrador: Boolean,
    uid: String?,
    onVolver: () -> Unit
) {
    val firestoreRepository = remember { FirestoreRepository() }
    val suscripciones = remember { mutableStateListOf<Suscripcion>() }
    var idClienteAsignar by remember { mutableStateOf("") }
    var idVehiculoAsignar by remember { mutableStateOf("") }
    var planElegido by remember { mutableStateOf(1) } // 1: Básico, 2: Premium
    var mesesDuracion by remember { mutableStateOf(1) }
    var metodoPago by remember { mutableStateOf("Tarjeta de Crédito / Débito") }
    var busqueda by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var listaCargada by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    fun actualizarLista() {
        listaCargada = false
        if (uid == null) {
            // Mantener el funcionamiento de las cuentas locales de prueba.
            suscripciones.clear()
            gestorSuscripciones.actualizarEstados()

            val lista = if (esAdministrador) {
                gestorSuscripciones.obtenerTodas()
            } else {
                clienteId?.let { id ->
                    gestorSuscripciones.obtenerTodas().filter {
                        it.cliente.id == id
                    }
                } ?: emptyList()
            }

            suscripciones.addAll(lista)
            listaCargada = true
            return
        }

        // 1. Recuperar clientes de Firestore.
        firestoreRepository.obtenerClientes(
            uid = uid,
            onSuccess = { listaClientes ->
                gestorClientes.sincronizarClientes(listaClientes)

                // 2. Recuperar vehículos de Firestore.
                firestoreRepository.obtenerVehiculos(
                    uid = uid,
                    onSuccess = { listaVehiculos ->

                        gestorVehiculos.sincronizarVehiculos(listaVehiculos)

                        // 3. Recuperar suscripciones de Firestore.
                        firestoreRepository.obtenerSuscripciones(
                            uid = uid,
                            buscarCliente = { id ->
                                gestorClientes.obtenerClientes().find {
                                    it.id == id
                                }
                            },
                            buscarVehiculo = { id ->
                                gestorVehiculos.obtenerTodos().find {
                                    it.id == id
                                }
                            },
                            onSuccess = { listaSuscripciones ->

                                gestorSuscripciones.sincronizarSuscripciones(
                                    listaSuscripciones
                                )

                                suscripciones.clear()

                                val listaFiltrada = if (esAdministrador) {
                                    listaSuscripciones
                                } else {
                                    listaSuscripciones.filter {
                                        it.cliente.id == clienteId
                                    }
                                }

                                suscripciones.addAll(listaFiltrada)
                                listaCargada = true
                            },
                            onError = { error ->
                                mensaje = "Error al consultar suscripciones: $error"
                            }
                        )
                    },
                    onError = { error ->
                        mensaje = "Error al consultar vehículos: $error"
                    }
                )
            },
            onError = { error ->
                mensaje = "Error al consultar clientes: $error"
            }
        )
    }

    LaunchedEffect(uid, clienteId, esAdministrador) {
        actualizarLista()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Asignar / Contratar Suscripción 📋",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (esAdministrador) {
                OutlinedTextField(
                    value = idClienteAsignar,
                    onValueChange = { idClienteAsignar = it },
                    label = { Text("ID del Cliente") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = idVehiculoAsignar,
                onValueChange = { idVehiculoAsignar = it },
                label = { Text("ID del Vehículo a Asignar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Text("Selecciona el Plan:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = planElegido == 1,
                    onClick = { planElegido = 1 },
                    label = { Text("Plan Básico (\$15/mes)") }
                )
                FilterChip(
                    selected = planElegido == 2,
                    onClick = { planElegido = 2 },
                    label = { Text("Plan Premium (\$25/mes)") }
                )
            }

            Text("Duración de la Suscripción:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mesesDuracion == 1, onClick = { mesesDuracion = 1 }, label = { Text("1 Mes") })
                FilterChip(selected = mesesDuracion == 3, onClick = { mesesDuracion = 3 }, label = { Text("3 Meses") })
                FilterChip(selected = mesesDuracion == 6, onClick = { mesesDuracion = 6 }, label = { Text("6 Meses") })
            }

            Text("Método de Pago con el que se realizó el pago:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = metodoPago == "Efectivo",
                    onClick = { metodoPago = "Efectivo" },
                    label = { Text("Efectivo") }
                )
                FilterChip(
                    selected = metodoPago == "Tarjeta (POS/Terminal)",
                    onClick = { metodoPago = "Tarjeta (POS/Terminal)" },
                    label = { Text("Tarjeta") }
                )
                FilterChip(
                    selected = metodoPago == "Transferencia Bancaria",
                    onClick = { metodoPago = "Transferencia Bancaria" },
                    label = { Text("Transferencia") }
                )
            }

            if (mensaje.isNotBlank()) {
                Text(
                    text = mensaje,
                    color = if (mensaje.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                enabled = listaCargada && !guardando,
                onClick = {
                    val vehId = idVehiculoAsignar.toIntOrNull()
                    val cliId = if (esAdministrador) idClienteAsignar.toIntOrNull() else clienteId

                    when {
                        vehId == null -> mensaje = "Por favor ingresa un ID de vehículo válido."
                        cliId == null -> mensaje = "Por favor ingresa un ID de cliente válido."
                        else -> {
                            val plan = if (planElegido == 1) PlanBasico() else PlanPremium()
                            val vehiculo = gestorVehiculos.obtenerTodos().find { it.id == vehId }
                            val cliente = gestorClientes.obtenerClientes().find { it.id == cliId }

                            when {
                                vehiculo == null -> mensaje = "No se encontró ningún vehículo con el ID #$vehId."
                                cliente == null -> mensaje = "No se encontró ningún cliente con el ID #$cliId."
                                vehiculo.clienteId != cliente.id -> mensaje = "El vehículo #${vehiculo.placa} no pertenece al cliente #${cliente.nombre}."
                                gestorSuscripciones.tieneSuscripcionActiva(vehiculo.id) -> mensaje = "Este vehículo ya cuenta con una suscripción activa."
                                else -> {
                                    val exito = gestorSuscripciones.crearSuscripcion(cliId, vehId, plan, mesesDuracion)
                                    if (exito) {

                                        val suscripcion = gestorSuscripciones.obtenerActivaDeVehiculo(vehId)

                                        if (uid != null && suscripcion != null) {
                                            guardando = true
                                            firestoreRepository.guardarSuscripcion(
                                                uid = uid,
                                                suscripcion = suscripcion,
                                                metodoPago = metodoPago,
                                                onSuccess = {
                                                    guardando = false
                                                    mensaje = "✅ ¡Suscripción guardada correctamente!"
                                                    actualizarLista()
                                                    idVehiculoAsignar = ""
                                                    idClienteAsignar = ""
                                                },
                                                onError = { error: Exception ->
                                                    gestorSuscripciones.eliminarSuscripcion(suscripcion.id)
                                                    guardando = false
                                                    mensaje = "Error al guardar: ${error.localizedMessage}"
                                                    actualizarLista()
                                                }
                                            )

                                        } else {
                                            mensaje = "Suscripción creada localmente, pero no se guardó en Firebase."
                                            actualizarLista()
                                        }

                                    } else {
                                        mensaje = "No se pudo activar la suscripción. Verifica los datos."
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (guardando) "Guardando…" else "Confirmar y Contratar Suscripción")
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = busqueda,
        onValueChange = { busqueda = it },
        label = { Text("🔍 Buscar suscripción por placa o cliente") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    val suscripcionesFiltradas = suscripciones.filter {
        it.vehiculo.placa.contains(busqueda, ignoreCase = true) ||
                it.cliente.nombre.contains(busqueda, ignoreCase = true)
    }

    Text(
        text = "Suscripciones en Sistema (${suscripcionesFiltradas.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    if (suscripcionesFiltradas.isEmpty()) {
        Text("No hay suscripciones registradas.")
    } else {
        suscripcionesFiltradas.forEach { sub ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Suscripción #${sub.id}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (sub.estado == EstadoSuscripcion.ACTIVA) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = sub.estado.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (sub.estado == EstadoSuscripcion.ACTIVA) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Text("Vehículo: ${sub.vehiculo.marca} ${sub.vehiculo.modelo} (${sub.vehiculo.placa})")
                    Text("Propietario: ${sub.cliente.nombre}")
                    Text("Plan: ${sub.plan.nombre} (\$$${sub.plan.precioMensual}/mes)")

                    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    Text("Vigencia: ${sub.fechaInicio.format(formatter)} ➜ ${sub.fechaFin.format(formatter)}")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Button(
                            enabled = listaCargada && !guardando,
                            onClick = {
                                if (!gestorSuscripciones.renovarSuscripcion(sub.id, 1)) {
                                    mensaje = "No se pudo renovar la suscripción."
                                    return@Button
                                }
                                val renovada = gestorSuscripciones.obtenerActivaDeVehiculo(sub.vehiculo.id)
                                if (uid == null || renovada == null) {
                                    actualizarLista()
                                    return@Button
                                }
                                guardando = true
                                firestoreRepository.actualizarVigencia(
                                    uid,
                                    renovada,
                                    onSuccess = {
                                        guardando = false
                                        mensaje = "✅ Suscripción #${sub.id} renovada +1 mes."
                                        actualizarLista()
                                    },
                                    onError = { error ->
                                        guardando = false
                                        mensaje = "Error al renovar: $error"
                                        actualizarLista()
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Renovar (+1 Mes)")
                        }

                        if (sub.estado == EstadoSuscripcion.ACTIVA) {
                            OutlinedButton(
                                enabled = listaCargada && !guardando,
                                onClick = {
                                    val uidUsuario = uid

                                    if (uidUsuario.isNullOrBlank()) {
                                        mensaje = "No existe un usuario autenticado."
                                    } else {
                                        firestoreRepository.cancelarSuscripcion(
                                            uid = uidUsuario,
                                            id = sub.id,
                                            onSuccess = {
                                                gestorSuscripciones.cancelarSuscripcion(sub.id)

                                                val indice = suscripciones.indexOfFirst {
                                                    it.id == sub.id
                                                }

                                                if (indice != -1) {
                                                    val suscripcionActualizada = Suscripcion(
                                                        id = sub.id,
                                                        cliente = sub.cliente,
                                                        vehiculo = sub.vehiculo,
                                                        plan = sub.plan,
                                                        fechaInicio = sub.fechaInicio,
                                                        fechaFin = sub.fechaFin,
                                                        estado = EstadoSuscripcion.VENCIDA
                                                    )

                                                    suscripciones[indice] = suscripcionActualizada
                                                }

                                                mensaje = "Suscripción cancelada correctamente."
                                            },
                                            onError = { error ->
                                                mensaje = "Error al cancelar: $error"
                                            }
                                        )
                                    }
                                }
                            ) {
                                Text("Cancelar")
                            }
                        }

                        OutlinedButton(
                            enabled = listaCargada && !guardando,
                            onClick = {
                                val uidUsuario = uid

                                if (uidUsuario.isNullOrBlank()) {
                                    mensaje = "No existe un usuario autenticado."
                                } else {
                                    firestoreRepository.eliminarSuscripcion(
                                        uid = uidUsuario,
                                        id = sub.id,
                                        onSuccess = {
                                            gestorSuscripciones.eliminarSuscripcion(sub.id)
                                            mensaje = "Suscripción eliminada correctamente."
                                            actualizarLista()
                                        },
                                        onError = { error ->
                                            mensaje = "Error al eliminar: $error"
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Eliminar")
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onVolver,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Text("← Volver al Menú Principal")
    }
}

@Composable
private fun PantallaHistorial(
    uid: String?,
    esAdministrador: Boolean,
    onVolver: () -> Unit
) {
    if (uid == null) {
        Text("Inicia sesión con Firebase para consultar el historial.")
        TextButton(onClick = onVolver) { Text("Volver al menú") }
        return
    }

    val repositorio = remember { HistorialFirestore() }
    val datosLavados = remember { FirestoreRepository() }
    val formatoFechaHora = remember {
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.systemDefault())
    }
    var intentoConsulta by remember(uid) { mutableIntStateOf(0) }
    var registros by remember(uid, esAdministrador, intentoConsulta) {
        mutableStateOf<List<RegistroLavado>?>(null)
    }
    var errorConsulta by remember(uid, esAdministrador, intentoConsulta) {
        mutableStateOf<String?>(null)
    }
    DisposableEffect(uid, esAdministrador, intentoConsulta) {
        val escucha = repositorio.observar(
            uid,
            esAdministrador,
            recibir = { registros = it },
            fallo = { error ->
                registros = null
                errorConsulta = if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    "Firestore rechazó la consulta. Verifica los permisos de la cuenta."
                } else {
                    "No se pudo cargar el historial. Verifica tu conexión."
                }
            }
        )
        onDispose { escucha.remove() }
    }

    var intentoReporte by remember(uid) { mutableIntStateOf(0) }
    var reporte by remember(uid, intentoReporte) { mutableStateOf<ReporteLavados?>(null) }
    var errorReporte by remember(uid, intentoReporte) { mutableStateOf<String?>(null) }
    LaunchedEffect(uid, esAdministrador, intentoReporte) {
        if (esAdministrador) {
            datosLavados.obtenerReporteLavados(uid)
                .addOnSuccessListener { reporte = it }
                .addOnFailureListener {
                    errorReporte = "No se pudo cargar el reporte. Verifica la conexión y las reglas de Firestore."
                }
        }
    }

    val historialLavados = registros.orEmpty()
    val suscripciones = reporte?.suscripciones.orEmpty()
    val activas = suscripciones.filter { it.estado == EstadoSuscripcion.ACTIVA }
    val vencidas = suscripciones.filter { it.estado == EstadoSuscripcion.VENCIDA }

    var numeroSuscripcion by remember(uid, intentoReporte) { mutableStateOf("") }
    var mensajeLavado by remember(uid) { mutableStateOf("") }
    var guardando by remember(uid) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (esAdministrador) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📊 Reporte General Washify",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Divider()
                    Text("Total Clientes Registrados: ${reporte?.totalClientes ?: "…"}")
                    Text("Total Vehículos Registrados: ${reporte?.totalVehiculos ?: "…"}")
                    Text("Suscripciones Activas: ${reporte?.let { activas.size } ?: "…"}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Suscripciones Vencidas: ${reporte?.let { vencidas.size } ?: "…"}", color = MaterialTheme.colorScheme.error)
                    Text("Servicios de Lavado Realizados: ${registros?.size ?: "…"}", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Service Wash Trigger Section (Solo para Administradores/Personal del Autolavado)
        if (esAdministrador) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Registrar Lavado Realizado (Exclusivo Admin) 🧼✨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Selecciona un vehículo de un cliente con suscripción activa para registrar que se efectuó el servicio de lavado.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    val problema = errorReporte
                    if (problema != null) {
                        Text(problema, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { intentoReporte++ }) { Text("Reintentar reporte") }
                    } else if (reporte == null) {
                        Text("Consultando suscripciones en Firestore…")
                    } else if (activas.isEmpty()) {
                        Text("No hay suscripciones activas en el sistema para registrar lavados.", color = Color.Gray)
                    } else {
                        activas.forEachIndexed { indice, suscripcion ->
                            Text("${indice + 1} · ${suscripcion.vehiculo.placa} · ${suscripcion.cliente.nombre}")
                        }
                        OutlinedTextField(
                            value = numeroSuscripcion,
                            onValueChange = { numeroSuscripcion = it },
                            label = { Text("Número de la suscripción en la lista") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !guardando,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        if (mensajeLavado.isNotBlank()) {
                            Text(
                                text = mensajeLavado,
                                color = if (mensajeLavado.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (guardando) return@Button
                                val sub = numeroSuscripcion.toIntOrNull()
                                    ?.takeIf { it > 0 }
                                    ?.let { activas.getOrNull(it - 1) }

                                if (sub == null || sub.actualizarEstado()) {
                                    mensajeLavado = "Selecciona una suscripción activa de la lista."
                                } else {
                                    guardando = true
                                    mensajeLavado = ""
                                    repositorio.registrar(uid, sub).addOnCompleteListener { tarea ->
                                        guardando = false
                                        if (tarea.isSuccessful) {
                                            mensajeLavado = "✅ Lavado guardado en Firestore para ${sub.vehiculo.placa}."
                                            numeroSuscripcion = ""
                                        } else {
                                            mensajeLavado = "No se pudo guardar el lavado. Revisa la conexión y el rol administrador."
                                        }
                                    }
                                }
                            },
                            enabled = !guardando && registros != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (guardando) "Guardando…" else "Marcar Servicio de Lavado Realizado")
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ℹ️ Historial de Servicios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aquí puedes consultar cada uno de los lavados que el personal de Washify ha registrado en tus vehículos suscritos.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Historial de Servicios de Lavado (${registros?.size ?: "…"})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val error = errorConsulta
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { intentoConsulta++ }) { Text("Reintentar") }
        } else if (registros == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Consultando Firestore…")
        } else if (historialLavados.isEmpty()) {
            Text("Aún no hay lavados registrados en el historial.")
        } else {
            historialLavados.forEach { registro ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lavado #${registro.id.take(8)} - Placa: ${registro.vehiculoPlaca}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatoFechaHora.format(registro.fechaHora.toDate().toInstant()),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Text("Cliente: ${registro.clienteNombre}")
                        Text("Plan Aplicado: ${registro.planNombre}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onVolver,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Text("← Volver al Menú Principal")
    }
}

@Composable
private fun ColumnaBase(contenido: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            contenido()
        }
    )
}

@Composable
private fun TarjetaMenu(
    titulo: String,
    descripcion: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(descripcion, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
