package com.example.washifyconsola

import com.example.consola.Cliente
import com.example.consola.PlanBasico
import com.example.consola.PlanPremium
import com.example.consola.Vehiculo
import com.example.consola.Suscripcion
import com.example.consola.GestorVehiculos
import java.time.LocalDate
import com.example.consola.EstadoSuscripcion
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

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
            "correo" to cliente.correo
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
            .get()
            .addOnSuccessListener { resultado ->

                val listaClientes = resultado.documents.mapNotNull { documento ->

                    val id = documento.getLong("id")?.toInt()
                    val nombre = documento.getString("nombre")
                    val correo = documento.getString("correo")

                    if (id != null && nombre != null && correo != null) {
                        Cliente(id, nombre, correo)
                    } else {
                        null
                    }
                }

                onSuccess(listaClientes)
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
        Log.d(
            "WashifyFirestore",
            "Proyecto: ${db.app.options.projectId} | UID: $uid | Vehículo: ${vehiculo.id}"
        )

        db.collection("usuarios")
            .document(uid)
            .collection("vehiculos")
            .document(vehiculo.id.toString())
            .set(datos)
            .addOnSuccessListener {
                Log.d("WashifyFirestore", "GUARDADO CONFIRMADO POR FIREBASE")
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e("WashifyFirestore", "ERROR AL GUARDAR", error)
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
            .get()
            .addOnSuccessListener { resultado ->

                val vehiculos = resultado.documents.mapNotNull { documento ->

                    val id = documento.getLong("id")?.toInt()
                    val placa = documento.getString("placa")
                    val marca = documento.getString("marca")
                    val modelo = documento.getString("modelo")
                    val clienteId = documento.getLong("clienteId")?.toInt()
                    val planNombre = documento.getString("planNombre")

                    if (
                        id != null &&
                        placa != null &&
                        marca != null &&
                        modelo != null &&
                        clienteId != null
                    ) {
                        val plan = when (planNombre) {
                            "Plan Basico" -> PlanBasico()
                            "Plan Premium" -> PlanPremium()
                            else -> null
                        }

                        Vehiculo(
                            id = id,
                            placa = placa,
                            marca = marca,
                            modelo = modelo,
                            clienteId = clienteId,
                            plan = plan
                        )
                    } else {
                        null
                    }
                }

                onSuccess(vehiculos)
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

        db.collection("usuarios")
            .document(uid)
            .collection("suscripciones")
            .document(suscripcion.id.toString())
            .set(datos)
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
            .get()
            .addOnSuccessListener { resultado ->

                val suscripciones = resultado.documents.mapNotNull { documento ->

                    try {
                        val id = documento.getLong("id")?.toInt()
                        val clienteId = documento.getLong("clienteId")?.toInt()
                        val vehiculoId = documento.getLong("vehiculoId")?.toInt()
                        val planNombre = documento.getString("planNombre")
                        val fechaInicio = documento.getString("fechaInicio")
                            ?.let { LocalDate.parse(it) }
                        val fechaFin = documento.getString("fechaFin")
                            ?.let { LocalDate.parse(it) }
                        val estadoNombre = documento.getString("estado")

                        val cliente = clienteId?.let { buscarCliente(it) }
                        val vehiculo = vehiculoId?.let { buscarVehiculo(it) }
                        println("=== DIAGNÓSTICO SUSCRIPCIÓN ===")
                        println("Cliente ID buscado: $clienteId")
                        println("Cliente encontrado: $cliente")
                        println("Vehículo ID buscado: $vehiculoId")
                        println("Vehículo encontrado: $vehiculo")
                        println("Plan: $planNombre")
                        println("Estado: $estadoNombre")

                        val plan = when (planNombre) {
                            "Plan Basico" -> PlanBasico()
                            "Plan Premium" -> PlanPremium()
                            else -> null
                        }

                        val estado = estadoNombre?.let {
                            runCatching { EstadoSuscripcion.valueOf(it) }.getOrNull()
                        }

                        if (
                            id != null &&
                            cliente != null &&
                            vehiculo != null &&
                            plan != null &&
                            fechaInicio != null &&
                            fechaFin != null &&
                            estado != null
                        ) {
                            Suscripcion(
                                id = id,
                                cliente = cliente,
                                vehiculo = vehiculo,
                                plan = plan,
                                fechaInicio = fechaInicio,
                                fechaFin = fechaFin,
                                estado = estado
                            )
                        } else {
                            null
                        }

                    } catch (e: Exception) {
                        null
                    }
                }

                onSuccess(suscripciones)
            }
            .addOnFailureListener { error ->
                onError(
                    error.message ?: "No se pudieron consultar las suscripciones."
                )
            }
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


