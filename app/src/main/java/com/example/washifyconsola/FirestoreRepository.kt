package com.example.washifyconsola

import com.example.consola.Cliente
import com.example.consola.Plan
import com.example.consola.PlanBasico
import com.example.consola.PlanPremium
import com.example.consola.Vehiculo
import com.example.consola.Suscripcion
import com.example.consola.EstadoSuscripcion
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import java.time.LocalDate

internal data class ReporteLavados(
    val totalClientes: Int,
    val totalVehiculos: Int,
    val suscripciones: List<Suscripcion>
)

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance("default")


    fun guardarCliente(
        uid: String,
        cliente: Cliente,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("No existe un usuario autenticado.")
            return
        }

        val datos = hashMapOf(
            "id" to cliente.id,
            "nombre" to cliente.nombre,
            "correo" to cliente.correo,
            "firebaseUid" to cliente.firebaseUid
        )

        db.collection("usuarios")
            .document(uid)
            .collection("clientes")
            .document(cliente.id.toString())
            .set(datos)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "No se pudo guardar el cliente.")
            }
    }



    fun obtenerClientes(
        uid: String,
        onSuccess: (List<Cliente>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("No existe un usuario autenticado.")
            return
        }

        db.collection("usuarios")
            .document(uid)
            .collection("clientes")
            .get(Source.SERVER)
            .addOnSuccessListener { resultado ->
                onSuccess(resultado.documents.mapNotNull { leerCliente(it, uid) })
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "No se pudieron consultar los clientes.")
            }
    }




    // Guardar un vehículo asociado al usuario autenticado
    fun guardarVehiculo(
        uid: String,
        vehiculo: Vehiculo,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("No existe un usuario autenticado.")
            return
        }

        val datos = hashMapOf(
            "id" to vehiculo.id,
            "placa" to vehiculo.placa,
            "marca" to vehiculo.marca,
            "modelo" to vehiculo.modelo,
            "clienteId" to vehiculo.clienteId,
            "planNombre" to vehiculo.plan?.nombre
        )
        val documento = db.collection("usuarios")
            .document(uid)
            .collection("vehiculos")
            .document(vehiculo.id.toString())
        crearDocumento(documento, datos)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "No se pudo guardar el vehículo.")
            }
    }

    // Consultar los vehículos guardados del usuario
    fun obtenerVehiculos(
        uid: String,
        onSuccess: (List<Vehiculo>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("No existe un usuario autenticado.")
            return
        }

        db.collection("usuarios")
            .document(uid)
            .collection("vehiculos")
            .get(Source.SERVER)
            .addOnSuccessListener { resultado ->
                onSuccess(resultado.documents.mapNotNull(::leerVehiculo))
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "No se pudieron consultar los vehículos.")
            }
    }

    fun eliminarVehiculo(
        uid: String,
        vehiculoId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("No existe un usuario autenticado.")
            return
        }

        db.collection("usuarios")
            .document(uid)
            .collection("vehiculos")
            .document(vehiculoId.toString())
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "No se pudo eliminar el vehículo.")
            }
    }


    fun guardarSuscripcion(
        uid: String,
        suscripcion: Suscripcion,
        metodoPago: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (uid.isBlank()) {
            onError(IllegalStateException("No existe un usuario autenticado."))
            return
        }

        val datos = hashMapOf(
            "id" to suscripcion.id,
            "clienteId" to suscripcion.cliente.id,
            "vehiculoId" to suscripcion.vehiculo.id,
            "planNombre" to suscripcion.plan.nombre,
            "precioMensual" to suscripcion.plan.precioMensual,
            "limiteLavadosMensuales" to suscripcion.plan.limiteLavadosMensuales,
            "fechaInicio" to suscripcion.fechaInicio.toString(),
            "fechaFin" to suscripcion.fechaFin.toString(),
            "estado" to suscripcion.estado.name,
            "metodoPago" to metodoPago
        )

        val documento = db.collection("usuarios")
            .document(uid)
            .collection("suscripciones")
            .document(suscripcion.id.toString())
        crearDocumento(documento, datos)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun obtenerSuscripciones(
        uid: String,
        buscarCliente: (Int) -> Cliente?,
        buscarVehiculo: (Int) -> Vehiculo?,
        onSuccess: (List<Suscripcion>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("No existe un usuario autenticado.")
            return
        }

        db.collection("usuarios")
            .document(uid)
            .collection("suscripciones")
            .get(Source.SERVER)
            .addOnSuccessListener { resultado ->
                onSuccess(resultado.documents.mapNotNull {
                    leerSuscripcion(it, buscarCliente, buscarVehiculo)
                })
            }
            .addOnFailureListener { error ->
                onError(
                    error.message ?: "No se pudieron consultar las suscripciones."
                )
            }
    }




    private fun crearDocumento(documento: DocumentReference, datos: Map<String, Any?>): Task<Unit> {
        return db.runTransaction { transaccion ->
            if (transaccion.get(documento).exists()) {
                throw FirebaseFirestoreException(
                    "El ID ya existe. Vuelve a intentar con la lista actualizada.",
                    FirebaseFirestoreException.Code.ALREADY_EXISTS
                )
            }
            transaccion.set(documento, datos)
            Unit
        }
    }

    internal fun obtenerReporteLavados(uidAdministrador: String): Task<ReporteLavados> {
        val fichasClientes = db.collectionGroup("clientes").get(Source.SERVER)
        val fichasVehiculos = db.collectionGroup("vehiculos").get(Source.SERVER)
        val fichasSuscripciones = db.collectionGroup("suscripciones").get(Source.SERVER)
        return Tasks.whenAll(fichasClientes, fichasVehiculos, fichasSuscripciones).continueWith { tarea ->
            if (!tarea.isSuccessful) {
                throw (tarea.exception ?: IllegalStateException("No se pudo cargar el reporte."))
            }

            // Los IDs numéricos se repiten entre cuentas. Vincularlos con el UID dueño.
            val clientes = fichasClientes.result.documents.mapNotNull { documento ->
                val uid = uidPropietario(documento) ?: return@mapNotNull null
                val cliente = leerCliente(documento, uid) ?: return@mapNotNull null
                val ficha = if (!documento.contains("firebaseUid") && uid != uidAdministrador) {
                    cliente.copy(firebaseUid = uid)
                } else {
                    cliente
                }
                (uid to ficha.id) to ficha
            }.toMap()
            val vehiculos = fichasVehiculos.result.documents.mapNotNull { documento ->
                val uid = uidPropietario(documento) ?: return@mapNotNull null
                leerVehiculo(documento)?.let { (uid to it.id) to it }
            }.toMap()
            val suscripciones = fichasSuscripciones.result.documents.mapNotNull { documento ->
                val uid = uidPropietario(documento) ?: return@mapNotNull null
                leerSuscripcion(documento, { clientes[uid to it] }, { vehiculos[uid to it] })
            }.sortedWith(compareBy<Suscripcion>({ it.vehiculo.placa }, { it.cliente.nombre }, { it.id }))

            ReporteLavados(clientes.size, vehiculos.size, suscripciones)
        }
    }

    private fun uidPropietario(documento: DocumentSnapshot): String? {
        val propietario = documento.reference.parent.parent ?: return null
        return propietario.id.takeIf { propietario.parent.path == "usuarios" }
    }

    private fun leerCliente(documento: DocumentSnapshot, uid: String): Cliente? {
        val id = (documento.get("id") as? Number)?.toInt() ?: return null
        val nombre = documento.get("nombre") as? String ?: return null
        val correo = documento.get("correo") as? String ?: return null
        val firebaseUid = (documento.get("firebaseUid") as? String)?.takeIf { it == uid }
        return Cliente(id, nombre, correo, firebaseUid)
    }

    private fun leerVehiculo(documento: DocumentSnapshot): Vehiculo? {
        val id = (documento.get("id") as? Number)?.toInt() ?: return null
        val placa = documento.get("placa") as? String ?: return null
        val marca = documento.get("marca") as? String ?: return null
        val modelo = documento.get("modelo") as? String ?: return null
        val clienteId = (documento.get("clienteId") as? Number)?.toInt() ?: return null
        return Vehiculo(id, placa, marca, modelo, clienteId, leerPlan(documento.get("planNombre") as? String))
    }

    private fun leerSuscripcion(
        documento: DocumentSnapshot,
        buscarCliente: (Int) -> Cliente?,
        buscarVehiculo: (Int) -> Vehiculo?
    ): Suscripcion? {
        return try {
            val id = (documento.get("id") as? Number)?.toInt() ?: return null
            val clienteId = (documento.get("clienteId") as? Number)?.toInt() ?: return null
            val vehiculoId = (documento.get("vehiculoId") as? Number)?.toInt() ?: return null
            val cliente = buscarCliente(clienteId) ?: return null
            val vehiculo = buscarVehiculo(vehiculoId) ?: return null
            if (vehiculo.clienteId != cliente.id) return null
            val plan = leerPlan(documento.get("planNombre") as? String) ?: return null
            val fechaInicio = (documento.get("fechaInicio") as? String)?.let(LocalDate::parse) ?: return null
            val fechaFin = (documento.get("fechaFin") as? String)?.let(LocalDate::parse) ?: return null
            val estado = (documento.get("estado") as? String)?.let(EstadoSuscripcion::valueOf) ?: return null
            Suscripcion(id, cliente, vehiculo, plan, fechaInicio, fechaFin, estado).apply { actualizarEstado() }
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: java.time.format.DateTimeParseException) {
            null
        }
    }

    private fun leerPlan(nombre: String?): Plan? = when (nombre) {
        "Plan Basico" -> PlanBasico()
        "Plan Premium" -> PlanPremium()
        else -> null
    }

    fun actualizarVigencia(
        uid: String,
        suscripcion: Suscripcion,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val vigencia = mapOf(
            "fechaInicio" to suscripcion.fechaInicio.toString(),
            "fechaFin" to suscripcion.fechaFin.toString(),
            "estado" to suscripcion.estado.name
        )
        db.collection("usuarios")
            .document(uid)
            .collection("suscripciones")
            .document(suscripcion.id.toString())
            .update(vigencia)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "No se pudo renovar la suscripción.") }
    }

    fun cancelarSuscripcion(
        uid: String,
        id: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("No existe un usuario autenticado.")
            return
        }

        db.collection("usuarios")
            .document(uid)
            .collection("suscripciones")
            .document(id.toString())
            .update("estado", EstadoSuscripcion.VENCIDA.name)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "No se pudo cancelar la suscripción.")
            }
    }

    fun eliminarSuscripcion(
        uid: String,
        id: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("No existe un usuario autenticado.")
            return
        }

        db.collection("usuarios")
            .document(uid)
            .collection("suscripciones")
            .document(id.toString())
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "No se pudo eliminar la suscripción.")
            }
    }


}
