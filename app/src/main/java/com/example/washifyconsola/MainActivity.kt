package com.example.washifyconsola

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.example.consola.Cliente
import com.example.consola.GestorClientes
import androidx.compose.runtime.mutableStateListOf
import com.example.consola.GestorVehiculos
import com.example.consola.Vehiculo
import com.example.consola.PlanBasico
import com.example.consola.PlanPremium
import com.example.consola.Plan
import com.example.consola.GestorSuscripciones
import com.example.consola.Suscripcion
import com.example.consola.EstadoSuscripcion
import java.time.format.DateTimeFormatter

private enum class Rol {
    CLIENTE,
    ADMINISTRADOR
}

private data class CuentaTemporal(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val rol: Rol
)

private enum class Pantalla {
    LOGIN,
    REGISTRO,
    INICIO_CLIENTE,
    INICIO_ADMIN,
    CLIENTES,
    VEHICULOS,
    PLANES,
    SUSCRIPCIONES,
    HISTORIAL
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


@Composable
fun WashifyApp() {
    var pantalla by remember { mutableStateOf(Pantalla.LOGIN) }
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    var nombreActual by remember { mutableStateOf("") }
    var rolActual by remember { mutableStateOf<Rol?>(null) }
    var clienteIdActual by remember { mutableStateOf<Int?>(null) }

    val gestorClientes = remember { GestorClientes() }
    val gestorVehiculos = remember { GestorVehiculos() }
    val clientes = remember { mutableStateListOf<Cliente>() }
    
    val gestorSuscripciones = remember { 
        GestorSuscripciones(
            buscarCliente = { id -> gestorClientes.obtenerClientes().find { it.id == id } },
            buscarVehiculo = { id -> gestorVehiculos.obtenerTodos().find { it.id == id } }
        )
    }

    // Cuenta administrativa temporal para pruebas.
    val cuentas = remember {
        mutableStateListOf(
            CuentaTemporal(
                nombre = "Administrador",
                correo = "admin@washify.com",
                contrasena = "Admin123",
                rol = Rol.ADMINISTRADOR
            )
        )
    }

    when (pantalla) {
        Pantalla.LOGIN -> {
            ColumnaBase {
                Text(
                    text = "Washify",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text("Inicia sesión en tu cuenta")

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (mensaje.isNotBlank()) {
                    Text(
                        text = mensaje,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        val correoIngresado = email.trim()

                        val cuenta = cuentas.find {
                            it.correo.equals(correoIngresado, ignoreCase = true) &&
                                    it.contrasena == password
                        }

                        if (cuenta == null) {
                            mensaje = "Correo o contraseña incorrectos."
                        } else {
                            nombreActual = cuenta.nombre
                            rolActual = cuenta.rol
                            mensaje = ""

                            if (cuenta.rol == Rol.CLIENTE) {
                                val cliente = gestorClientes.obtenerClientes()
                                    .firstOrNull {
                                        it.correo.equals(
                                            cuenta.correo,
                                            ignoreCase = true
                                        )
                                    }

                                if (cliente == null) {
                                    mensaje =
                                        "No se encontró el cliente asociado a esta cuenta."
                                    return@Button
                                }

                                clienteIdActual = cliente.id
                            } else {
                                clienteIdActual = null
                            }

                            pantalla = when (cuenta.rol) {
                                Rol.CLIENTE -> Pantalla.INICIO_CLIENTE
                                Rol.ADMINISTRADOR -> Pantalla.INICIO_ADMIN
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Iniciar sesión")
                }

                TextButton(
                    onClick = {
                        nombre = ""
                        email = ""
                        password = ""
                        mensaje = ""
                        pantalla = Pantalla.REGISTRO
                    }
                ) {
                    Text("¿No tienes cuenta? Regístrate")
                }
            }
        }

        Pantalla.REGISTRO -> {
            ColumnaBase {
                Text(
                    text = "Crear cuenta",
                    style = MaterialTheme.typography.headlineMedium
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (mensaje.isNotBlank()) {
                    Text(
                        text = mensaje,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        val nombreIngresado = nombre.trim()
                        val correo = email.trim()
                        val correoValido =
                            correo.contains("@") &&
                                    correo.substringAfter("@").contains(".")

                        when {
                            nombreIngresado.isBlank() ||
                                    !correoValido ||
                                    password.length < 6 -> {
                                mensaje =
                                    "Completa los campos y utiliza una contraseña de al menos 6 caracteres."
                            }

                            cuentas.any {
                                it.correo.equals(correo, ignoreCase = true)
                            } -> {
                                mensaje = "Ese correo ya está registrado."
                            }

                            else -> {
                                val registrado = gestorClientes.registrarCliente(
                                    nombreIngresado,
                                    correo
                                )

                                if (registrado) {
                                    cuentas.add(
                                        CuentaTemporal(
                                            nombre = nombreIngresado,
                                            correo = correo,
                                            contrasena = password,
                                            rol = Rol.CLIENTE
                                        )
                                    )

                                    nombre = ""
                                    email = correo
                                    password = ""
                                    mensaje =
                                        "Cuenta creada. Ya puedes iniciar sesión."
                                    pantalla = Pantalla.LOGIN
                                } else {
                                    mensaje = "No se pudo crear la cuenta."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Registrarme")
                }

                TextButton(
                    onClick = {
                        mensaje = ""
                        pantalla = Pantalla.LOGIN
                    }
                ) {
                    Text("Volver al inicio de sesión")
                }
            }
        }

        Pantalla.INICIO_CLIENTE -> {
            ColumnaBase {
                Text(
                    text = "¡Hola, $nombreActual!",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text("Tu espacio personal de Washify")

                TarjetaMenu(
                    "Mis vehículos",
                    "Gestionar tus carros"
                ) {
                    mensaje = ""
                    pantalla = Pantalla.VEHICULOS
                }

                TarjetaMenu(
                    "Planes",
                    "Consultar planes de lavado"
                ) {
                    mensaje = ""
                    pantalla = Pantalla.PLANES
                }

                TarjetaMenu(
                    "Mis suscripciones",
                    "Consultar tu plan y vigencia"
                ) {
                    mensaje = ""
                    pantalla = Pantalla.SUSCRIPCIONES
                }

                TarjetaMenu(
                    "Mi historial",
                    "Consultar tus lavados"
                ) {
                    mensaje = ""
                    pantalla = Pantalla.HISTORIAL
                }

                if (mensaje.isNotBlank()) {
                    Text(mensaje)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        nombreActual = ""
                        rolActual = null
                        clienteIdActual = null
                        email = ""
                        password = ""
                        mensaje = ""
                        pantalla = Pantalla.LOGIN
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar sesión")
                }
            }
        }

        Pantalla.INICIO_ADMIN -> {
            ColumnaBase {
                Text(
                    text = "Panel administrativo",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text("Bienvenido, $nombreActual")

                TarjetaMenu(
                    "Clientes",
                    "Consultar y registrar clientes"
                ) {
                    clientes.clear()
                    clientes.addAll(gestorClientes.obtenerClientes())
                    mensaje = ""
                    pantalla = Pantalla.CLIENTES
                }

                TarjetaMenu(
                    "Vehículos",
                    "Gestionar vehículos"
                ) {
                    mensaje = ""
                    pantalla = Pantalla.VEHICULOS
                }

                TarjetaMenu(
                    "Planes",
                    "Gestionar planes de lavado"
                ) {
                    mensaje = ""
                    pantalla = Pantalla.PLANES
                }

                TarjetaMenu(
                    "Suscripciones",
                    "Gestionar suscripciones"
                ) {
                    mensaje = ""
                    pantalla = Pantalla.SUSCRIPCIONES
                }

                TarjetaMenu(
                    "Historial",
                    "Consultar lavados"
                ) {
                    mensaje = ""
                    pantalla = Pantalla.HISTORIAL
                }

                if (mensaje.isNotBlank()) {
                    Text(mensaje)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        nombreActual = ""
                        rolActual = null
                        clienteIdActual = null
                        email = ""
                        password = ""
                        mensaje = ""
                        pantalla = Pantalla.LOGIN
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar sesión")
                }
            }
        }

        Pantalla.CLIENTES -> {
            PantallaClientes(
                gestor = gestorClientes,
                clientes = clientes,
                onVolver = {
                    mensaje = ""
                    pantalla = Pantalla.INICIO_ADMIN
                }
            )
        }

        Pantalla.VEHICULOS -> {
            PantallaVehiculos(
                gestor = gestorVehiculos,
                clienteId = clienteIdActual,
                esAdministrador = rolActual == Rol.ADMINISTRADOR,
                clientes = gestorClientes.obtenerClientes(),
                onVolver = {
                    mensaje = ""
                    pantalla = if (rolActual == Rol.ADMINISTRADOR) {
                        Pantalla.INICIO_ADMIN
                    } else {
                        Pantalla.INICIO_CLIENTE
                    }
                }
            )
        }
        
        Pantalla.PLANES -> {
            PantallaPlanes(
                esAdministrador = rolActual == Rol.ADMINISTRADOR,
                onVolver = {
                    mensaje = ""
                    pantalla = if (rolActual == Rol.ADMINISTRADOR) {
                        Pantalla.INICIO_ADMIN
                    } else {
                        Pantalla.INICIO_CLIENTE
                    }
                }
            )
        }
        
        Pantalla.SUSCRIPCIONES -> {
            PantallaSuscripciones(
                gestorSuscripciones = gestorSuscripciones,
                gestorVehiculos = gestorVehiculos,
                gestorClientes = gestorClientes,
                clienteId = clienteIdActual,
                esAdministrador = rolActual == Rol.ADMINISTRADOR,
                onVolver = {
                    mensaje = ""
                    pantalla = if (rolActual == Rol.ADMINISTRADOR) {
                        Pantalla.INICIO_ADMIN
                    } else {
                        Pantalla.INICIO_CLIENTE
                    }
                }
            )
        }
        
        Pantalla.HISTORIAL -> {
            PantallaHistorial(
                gestorSuscripciones = gestorSuscripciones,
                gestorClientes = gestorClientes,
                clienteId = clienteIdActual,
                esAdministrador = rolActual == Rol.ADMINISTRADOR,
                onVolver = {
                    mensaje = ""
                    pantalla = if (rolActual == Rol.ADMINISTRADOR) {
                        Pantalla.INICIO_ADMIN
                    } else {
                        Pantalla.INICIO_CLIENTE
                    }
                }
            )
        }
    }
}


@Composable
private fun PantallaVehiculos(
    gestor: GestorVehiculos,
    clienteId: Int?,
    esAdministrador: Boolean,
    clientes: List<Cliente>,
    onVolver: () -> Unit
) {
    var placa by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var clienteIdTexto by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    val vehiculos = remember {
        mutableStateListOf<Vehiculo>()
    }

    fun actualizarLista() {
        vehiculos.clear()

        val lista = if (esAdministrador) {
            gestor.obtenerTodos()
        } else {
            clienteId?.let {
                gestor.obtenerVehiculosPorCliente(it)
            } ?: emptyList()
        }

        vehiculos.addAll(lista)
    }

    // Carga la lista al entrar a la pantalla.
    LaunchedEffect(clienteId, esAdministrador) {
        actualizarLista()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (esAdministrador) {
                "Gestión de vehículos"
            } else {
                "Mis vehículos"
            },
            style = MaterialTheme.typography.headlineMedium
        )

        if (esAdministrador) {
            Text("Ingresa el ID del cliente propietario.")

            OutlinedTextField(
                value = clienteIdTexto,
                onValueChange = { clienteIdTexto = it },
                label = { Text("ID del cliente") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = placa,
            onValueChange = { placa = it },
            label = { Text("Placa del vehículo") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = marca,
            onValueChange = { marca = it },
            label = { Text("Marca") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = modelo,
            onValueChange = { modelo = it },
            label = { Text("Modelo") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val propietarioId = if (esAdministrador) {
                    clienteIdTexto.toIntOrNull()
                } else {
                    clienteId
                }

                when {
                    placa.isBlank() ||
                            marca.isBlank() ||
                            modelo.isBlank() -> {
                        mensaje = "Completa la placa, marca y modelo."
                    }

                    propietarioId == null ||
                            clientes.none { it.id == propietarioId } -> {
                        mensaje = "Ingresa un ID de cliente válido."
                    }

                    else -> {
                        val registrado = gestor.registrarVehiculo(
                            placa = placa.trim(),
                            marca = marca.trim(),
                            modelo = modelo.trim(),
                            clienteId = propietarioId
                        )

                        if (registrado) {
                            actualizarLista()
                            placa = ""
                            marca = ""
                            modelo = ""
                            clienteIdTexto = ""
                            mensaje = "Vehículo registrado correctamente."
                        } else {
                            mensaje =
                                "No se pudo registrar. Verifica los datos y la placa."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar vehículo")
        }

        if (mensaje.isNotBlank()) {
            Text(
                text = mensaje,
                color = if (mensaje.startsWith("No se pudo") ||
                    mensaje.startsWith("Completa") ||
                    mensaje.startsWith("Ingresa")
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }

        Text(
            text = "Vehículos registrados: ${vehiculos.size}",
            style = MaterialTheme.typography.titleMedium
        )

        if (vehiculos.isEmpty()) {
            Text("Todavía no hay vehículos registrados.")
        } else {
            vehiculos.forEach { vehiculo ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${vehiculo.marca} ${vehiculo.modelo}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Placa: ${vehiculo.placa}")
                        Text("ID del vehículo: ${vehiculo.id}")
                        Text("ID del propietario: ${vehiculo.clienteId}")
                        Text("Plan: ${vehiculo.plan?.nombre ?: "Sin plan"}")

                        Button(
                            onClick = {
                                gestor.eliminarVehiculo(vehiculo.id)
                                actualizarLista()
                                mensaje = "Lista de vehículos actualizada."
                            }
                        ) {
                            Text("Eliminar vehículo")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver al menú")
        }
    }
}


@Composable
private fun PantallaClientes(
    gestor: GestorClientes,
    clientes: MutableList<Cliente>,
    onVolver: () -> Unit
) {
    var nombreCliente by remember { mutableStateOf("") }
    var correoCliente by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Gestión de clientes",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Registra clientes para asociarles sus vehículos.")

        OutlinedTextField(
            value = nombreCliente,
            onValueChange = { nombreCliente = it },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = correoCliente,
            onValueChange = { correoCliente = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (
                    nombreCliente.isBlank() ||
                    !correoCliente.contains("@") ||
                    !correoCliente.substringAfter("@").contains(".")
                ) {
                    mensaje = "Ingresa un nombre y un correo válido."
                } else {
                    val registrado = gestor.registrarCliente(
                        nombreCliente.trim(),
                        correoCliente.trim()
                    )

                    if (registrado) {
                        clientes.clear()
                        clientes.addAll(gestor.obtenerClientes())

                        nombreCliente = ""
                        correoCliente = ""
                        mensaje = "Cliente registrado correctamente."
                    } else {
                        mensaje = "No se pudo registrar el cliente."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar cliente")
        }

        if (mensaje.isNotBlank()) {
            Text(mensaje)
        }

        Text(
            "Clientes registrados: ${clientes.size}",
            style = MaterialTheme.typography.titleMedium
        )

        if (clientes.isEmpty()) {
            Text("Todavía no hay clientes registrados.")
        } else {
            clientes.forEach { cliente ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            cliente.nombre,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("ID: ${cliente.id}")
                        Text("Correo: ${cliente.correo}")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver al menú")
        }
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
            .padding(vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium
            )
            Text(descripcion)
        }
    }
}

@Composable
fun PantallaPlanes(
    esAdministrador: Boolean,
    onVolver: () -> Unit
) {
    val planes = listOf(PlanBasico(), PlanPremium())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Planes de Lavado",
            style = MaterialTheme.typography.headlineMedium
        )
        
        if (esAdministrador) {
            Text(
                text = "Modo Administrador: Puedes ver los planes ofrecidos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            Text(
                text = "Descubre nuestros planes diseñados para mantener tu vehículo impecable.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        planes.forEach { plan ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = plan.nombre,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Precio mensual: \$${plan.precioMensual}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Lavados incluidos: ${plan.limiteLavadosMensuales ?: "Ilimitados"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver al menú")
        }
    }
}

@Composable
fun PantallaSuscripciones(
    gestorSuscripciones: GestorSuscripciones,
    gestorVehiculos: GestorVehiculos,
    gestorClientes: GestorClientes,
    clienteId: Int?,
    esAdministrador: Boolean,
    onVolver: () -> Unit
) {
    val suscripciones = remember { mutableStateListOf<Suscripcion>() }
    var idClienteAsignar by remember { mutableStateOf("") }
    var idVehiculoAsignar by remember { mutableStateOf("") }
    var planElegido by remember { mutableStateOf(1) } // 1: Básico, 2: Premium
    var mensaje by remember { mutableStateOf("") }

    fun actualizarLista() {
        suscripciones.clear()
        gestorSuscripciones.actualizarEstados()
        val lista = if (esAdministrador) {
            gestorSuscripciones.obtenerTodas()
        } else {
            clienteId?.let { id ->
                gestorSuscripciones.obtenerTodas().filter { it.cliente.id == id }
            } ?: emptyList()
        }
        suscripciones.addAll(lista)
    }

    LaunchedEffect(clienteId, esAdministrador) {
        actualizarLista()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Suscripciones",
            style = MaterialTheme.typography.headlineMedium
        )

        // Asignar Plan 
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Asignar Suscripción",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (esAdministrador) {
                    OutlinedTextField(
                        value = idClienteAsignar,
                        onValueChange = { idClienteAsignar = it },
                        label = { Text("ID del Cliente") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                OutlinedTextField(
                    value = idVehiculoAsignar,
                    onValueChange = { idVehiculoAsignar = it },
                    label = { Text("ID del Vehículo") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Plan: ")
                    TextButton(onClick = { planElegido = 1 }, enabled = planElegido != 1) { Text("Básico") }
                    TextButton(onClick = { planElegido = 2 }, enabled = planElegido != 2) { Text("Premium") }
                }

                if (mensaje.isNotBlank()) {
                    Text(mensaje, color = MaterialTheme.colorScheme.primary)
                }

                Button(
                    onClick = {
                        val vehId = idVehiculoAsignar.toIntOrNull()
                        val cliId = if (esAdministrador) idClienteAsignar.toIntOrNull() else clienteId
                        
                        if (vehId == null || cliId == null) {
                            mensaje = "Por favor, ingresa IDs válidos."
                            return@Button
                        }
                        
                        val plan = if (planElegido == 1) PlanBasico() else PlanPremium()
                        val vehiculo = gestorVehiculos.obtenerTodos().find { it.id == vehId }
                        val cliente = gestorClientes.obtenerClientes().find { it.id == cliId }
                        
                        if (vehiculo == null) {
                            mensaje = "Vehículo no encontrado."
                        } else if (cliente == null) {
                            mensaje = "Cliente no encontrado."
                        } else if (vehiculo.clienteId != cliente.id) {
                            mensaje = "Ese vehículo no te pertenece."
                        } else if (gestorSuscripciones.tieneSuscripcionActiva(vehiculo.id)) {
                            mensaje = "Este vehículo ya tiene una suscripción."
                        } else {
                            val exito = gestorSuscripciones.crearSuscripcion(cliId, vehId, plan)
                            if (exito) {
                                mensaje = "Suscripción asignada exitosamente."
                                actualizarLista()
                                idVehiculoAsignar = ""
                                idClienteAsignar = ""
                            } else {
                                mensaje = "Error al asignar suscripción."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Asignar a Vehículo")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Lista de Suscripciones",
            style = MaterialTheme.typography.titleLarge
        )

        if (suscripciones.isEmpty()) {
            Text("No hay suscripciones registradas.")
        } else {
            suscripciones.forEach { sub ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Suscripción ID: ${sub.id}", style = MaterialTheme.typography.titleMedium)
                        Text("Vehículo: ${sub.vehiculo.placa}")
                        if (esAdministrador) Text("Cliente: ${sub.cliente.nombre}")
                        Text("Plan: ${sub.plan.nombre} ($${sub.plan.precioMensual})")
                        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                        Text("Vigencia: ${sub.fechaInicio.format(formatter)} al ${sub.fechaFin.format(formatter)}")
                        Text(
                            text = "Estado: ${sub.estado}", 
                            color = if (sub.estado == EstadoSuscripcion.ACTIVA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val renovado = gestorSuscripciones.renovarSuscripcion(sub.id, 1)
                                    if (renovado) {
                                        mensaje = "Suscripción #${sub.id} renovada por 1 mes."
                                        actualizarLista()
                                    } else {
                                        mensaje = "No se pudo renovar la suscripción."
                                    }
                                }
                            ) {
                                Text("Renovar (+1 mes)")
                            }

                            Button(
                                onClick = {
                                    val eliminado = gestorSuscripciones.eliminarSuscripcion(sub.id)
                                    if (eliminado) {
                                        mensaje = "Suscripción #${sub.id} eliminada del sistema."
                                        actualizarLista()
                                    } else {
                                        mensaje = "No se pudo eliminar."
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
        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al menú")
        }
    }
}

@Composable
fun PantallaHistorial(
    gestorSuscripciones: GestorSuscripciones,
    gestorClientes: GestorClientes,
    clienteId: Int?,
    esAdministrador: Boolean,
    onVolver: () -> Unit
) {
    val clientes = gestorClientes.obtenerClientes()
    val suscripciones = gestorSuscripciones.obtenerTodas()
    
    val suscripcionesActivas = suscripciones.count { it.estado == EstadoSuscripcion.ACTIVA }
    val suscripcionesVencidas = suscripciones.count { it.estado == EstadoSuscripcion.VENCIDA }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Historial / Reportes",
            style = MaterialTheme.typography.headlineMedium
        )
        
        if (esAdministrador) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Resumen General Washify", style = MaterialTheme.typography.titleLarge)
                    Text("Clientes registrados: ${clientes.size}")
                    Text("Suscripciones totales: ${suscripciones.size}")
                    Text("Suscripciones activas: $suscripcionesActivas", color = MaterialTheme.colorScheme.primary)
                    Text("Suscripciones vencidas: $suscripcionesVencidas", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text("Todas las Suscripciones Históricas", style = MaterialTheme.typography.titleMedium)
            if (suscripciones.isEmpty()) {
                Text("No hay historial registrado.")
            } else {
                suscripciones.forEach { sub ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Cliente: ${sub.cliente.nombre} | Vehículo: ${sub.vehiculo.placa}")
                            Text("Plan: ${sub.plan.nombre} - Estado: ${sub.estado}")
                        }
                    }
                }
            }
        } else {
            val misSuscripciones = clienteId?.let { id ->
                suscripciones.filter { it.cliente.id == id }
            } ?: emptyList()
            
            Text("Tu Historial de Suscripciones", style = MaterialTheme.typography.titleLarge)
            if (misSuscripciones.isEmpty()) {
                Text("Todavía no tienes historial registrado.")
            } else {
                misSuscripciones.forEach { sub ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Vehículo: ${sub.vehiculo.placa}", style = MaterialTheme.typography.titleMedium)
                            Text("Plan: ${sub.plan.nombre} - Estado: ${sub.estado}")
                            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                            Text("Adquirido el: ${sub.fechaInicio.format(formatter)}")
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al menú")
        }
    }
}


