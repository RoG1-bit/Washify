package com.example.consola

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Scanner

val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

enum class EstadoSuscripcion { ACTIVA, VENCIDA }

class Suscripcion(
    val id: Int,
    val cliente: Cliente,
    val vehiculo: Vehiculo,
    val plan: Plan,
    var fechaInicio: LocalDate,
    var fechaFin: LocalDate,
    var estado: EstadoSuscripcion = EstadoSuscripcion.ACTIVA
) {
  fun actualizarEstado(hoy: LocalDate = LocalDate.now()): Boolean {
        val nuevoEstado = if (hoy.isBefore(fechaFin)) EstadoSuscripcion.ACTIVA else EstadoSuscripcion.VENCIDA
        val cambio = nuevoEstado != estado
        estado = nuevoEstado
if (cambio && nuevoEstado == EstadoSuscripcion.VENCIDA && vehiculo.plan === plan) {
            vehiculo.plan = null
        }
        return cambio
    }

    override fun toString(): String =
        "ID: $id | Cliente: ${cliente.nombre} | Vehículo: ${vehiculo.placa} | Plan: ${plan.nombre} | " +
            "Inicio: ${fechaInicio.format(FORMATO_FECHA)} | Vence: ${fechaFin.format(FORMATO_FECHA)} | Estado: $estado"
}
class GestorSuscripciones(
    private val buscarCliente: (Int) -> Cliente?,
    private val buscarVehiculo: (Int) -> Vehiculo?
) {
    private val listaSuscripciones = mutableListOf<Suscripcion>()
    private var contadorId = 1

    fun crearSuscripcion(
        idCliente: Int,
        idVehiculo: Int,
        plan: Plan,
        meses: Int = 1,
        fechaInicio: LocalDate = LocalDate.now()
    ): Boolean {
        if (meses <= 0) {
            println("❌ Error: La cantidad de meses debe ser mayor a 0.")
            return false
        }
        val cliente = buscarCliente(idCliente) ?: run {
            println("❌ Error: No existe un cliente con el ID $idCliente.")
            return false
        }
        val vehiculo = buscarVehiculo(idVehiculo) ?: run {
            println("❌ Error: No existe un vehículo con el ID $idVehiculo.")
            return false
        }
        if (vehiculo.clienteId != cliente.id) {
            println("❌ Error: El vehículo ${vehiculo.placa} no pertenece al cliente ${cliente.nombre}.")
            return false
        }
        if (tieneSuscripcionActiva(vehiculo.id)) {
            println("❌ Error: Este vehículo ya tiene una suscripción activa.")
            return false
        }

        val fechaFin = fechaInicio.plusMonths(meses.toLong())
        if (!fechaFin.isAfter(LocalDate.now())) {
            println("❌ Error: Con esa fecha de inicio la suscripción ya estaría vencida.")
            return false
        }

        val nueva = Suscripcion(contadorId++, cliente, vehiculo, plan, fechaInicio, fechaFin)
        nueva.actualizarEstado()
        listaSuscripciones.add(nueva)
        vehiculo.plan = plan // el vehículo queda asociado al plan de su suscripción

        println("✅ Suscripción creada exitosamente: $nueva")
        println("   Total del período: $${plan.precioMensual * meses}")
        return true
    }

    fun listarSuscripciones() {
        actualizarEstados()
        if (listaSuscripciones.isEmpty()) {
            println("⚠️ No hay suscripciones registradas en el sistema.")
        } else {
            println("--- LISTA DE SUSCRIPCIONES ---")
            listaSuscripciones.forEach { println(it) }
        }
    }
/** Revisa todas las suscripciones y marca como VENCIDA las que llegaron a su fecha fin. */
    fun actualizarEstados(hoy: LocalDate = LocalDate.now()): Int =
        listaSuscripciones.count { it.actualizarEstado(hoy) }

    fun renovarSuscripcion(id: Int, meses: Int = 1): Boolean {
        if (meses <= 0) {
            println("❌ Error: La cantidad de meses debe ser mayor a 0.")
            return false
        }
        val suscripcion = listaSuscripciones.find { it.id == id } ?: run {
            println("❌ Error: No se encontró una suscripción con el ID $id.")
            return false
        }
        val hoy = LocalDate.now()
        suscripcion.actualizarEstado(hoy)

        if (suscripcion.estado == EstadoSuscripcion.ACTIVA) {
            // Sigue vigente: se extiende desde la fecha fin actual
            suscripcion.fechaFin = suscripcion.fechaFin.plusMonths(meses.toLong())
        } else {
            // Vencida: se reinicia desde hoy, salvo que el vehículo ya tenga otra activa
            if (tieneSuscripcionActiva(suscripcion.vehiculo.id)) {
                println("❌ Error: Este vehículo ya tiene otra suscripción activa.")
                return false
            }
            suscripcion.fechaInicio = hoy
            suscripcion.fechaFin = hoy.plusMonths(meses.toLong())
        }
        suscripcion.actualizarEstado(hoy)
        suscripcion.vehiculo.plan = suscripcion.plan

        println("✅ Suscripción renovada: $suscripcion")
        return true
    }
fun obtenerActivaDeVehiculo(idVehiculo: Int): Suscripcion? {
        actualizarEstados()
        return listaSuscripciones.find { it.vehiculo.id == idVehiculo && it.estado == EstadoSuscripcion.ACTIVA }
    }

    fun tieneSuscripcionActiva(idVehiculo: Int): Boolean = obtenerActivaDeVehiculo(idVehiculo) != null

    /** Para Integrante 5 (reportes): copia de solo lectura de todas las suscripciones. */
    fun obtenerTodas(): List<Suscripcion> {
        actualizarEstados()
        return listaSuscripciones.toList()
    }
}

fun menuSuscripciones(scanner: Scanner, gestor: GestorSuscripciones) {
    var salir = false

    println("\n=== BIENVENIDO AL MÓDULO DE SUSCRIPCIONES WASHIFY ===")

    while (!salir) {
        println("\nSeleccione una opción:")
        println("1. Crear suscripción")
        println("2. Listar suscripciones")
        println("3. Renovar suscripción")
        println("4. Actualizar estados (Activa/Vencida)")
        println("5. Volver al menú principal")
        print("Opción: ")

        try {
            when (scanner.nextLine().toInt()) {
                1 -> {
                    print("Ingrese el ID del cliente: ")
                    val idCliente = scanner.nextLine().toInt()
                    print("Ingrese el ID del vehículo: ")
                    val idVehiculo = scanner.nextLine().toInt()
                    print("Plan (1. Básico, 2. Premium): ")
                    val plan: Plan? = when (scanner.nextLine().toInt()) {
                        1 -> PlanBasico()
                        2 -> PlanPremium()
                        else -> null
                    }
                    if (plan == null) {
                        println("❌ Plan no válido. Elija 1 o 2.")
                    } else {
                        print("Cantidad de meses (deje en blanco para 1): ")
                        val textoMeses = scanner.nextLine().trim()
                        val meses = if (textoMeses.isBlank()) 1 else textoMeses.toInt()
                        print("Fecha de inicio dd-MM-yyyy (deje en blanco para usar hoy): ")
                        val textoFecha = scanner.nextLine().trim()
                        val fechaInicio = if (textoFecha.isBlank()) LocalDate.now()
                        else LocalDate.parse(textoFecha, FORMATO_FECHA)
                        gestor.crearSuscripcion(idCliente, idVehiculo, plan, meses, fechaInicio)
                    }
                }
                2 -> gestor.listarSuscripciones()
                3 -> {
                    print("Ingrese el ID de la suscripción a renovar: ")
                    val id = scanner.nextLine().toInt()
                    print("Cantidad de meses (deje en blanco para 1): ")
                    val textoMeses = scanner.nextLine().trim()
                    val meses = if (textoMeses.isBlank()) 1 else textoMeses.toInt()
                    gestor.renovarSuscripcion(id, meses)
                }
                4 -> {
                    val cambiadas = gestor.actualizarEstados()
                    println("✅ Estados actualizados. Suscripciones que cambiaron: $cambiadas")
                }
                5 -> salir = true
                else -> println("❌ Opción no válida.")
            }
        } catch (_: NumberFormatException) {
            println("❌ Error: Por favor, ingrese un número válido.")
        } catch (_: DateTimeParseException) {
            println("❌ Error: La fecha debe tener el formato dd-MM-yyyy (ej. 25-12-2026).")
        } catch (e: Exception) {
            println("❌ Ocurrió un error inesperado: ${e.message}")
        }
    }
}