package com.example.consola

object NotificacionService {

    private val notificaciones = mutableListOf<String>()

    fun agregarNotificacion(mensaje: String) {

        if (mensaje.isNotBlank()) {
            notificaciones.add(mensaje)
        }
    }

    fun mostrarNotificaciones() {

        println("\n======================================")
        println("       NOTIFICACIONES WASHIFY")
        println("======================================")

        if (notificaciones.isEmpty()) {

            println("No hay notificaciones pendientes.")

        } else {

            notificaciones.forEachIndexed { indice, mensaje ->

                println("${indice + 1}. $mensaje")
            }
        }

        println("======================================")
    }

    fun obtenerNotificaciones(): List<String> {
        return notificaciones.toList()
    }

    fun limpiarNotificaciones() {
        notificaciones.clear()
    }
}