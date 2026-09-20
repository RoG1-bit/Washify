package com.example.consola

import java.util.Scanner

fun menuReportes(
    scanner: Scanner,
    gestorClientes: GestorClientes
) {
    var salir = false

    while (!salir) {

        println("\n======================================")
        println("       REPORTES WASHIFY")
        println("======================================")
        println("1. Ver historial de lavados")
        println("2. Ver resumen general del sistema")
        println("3. Ver suscripciones activas")
        println("4. Ver suscripciones vencidas")
        println("5. Ver total de servicios realizados")
        println("6. Ver notificaciones")
        println("7. Volver al menú principal")
        print("Seleccione una opción: ")

        try {

            when (scanner.nextLine().toInt()) {

                1 -> {
                    println(
                        ">> Historial de lavados aún no disponible."
                    )
                }

                2 -> {
                    mostrarResumenGeneral(gestorClientes)
                }

                3 -> {
                    println(
                        ">> Suscripciones activas aún no disponibles."
                    )
                }

                4 -> {
                    println(
                        ">> Suscripciones vencidas aún no disponibles."
                    )
                }

                5 -> {
                    println(
                        ">> Servicios realizados aún no disponibles."
                    )
                }

                6 -> {
                    NotificacionService.mostrarNotificaciones()
                }

                7 -> {
                    salir = true
                }

                else -> {
                    println("❌ Opción no válida.")
                }
            }

        } catch (_: NumberFormatException) {

            println(
                "❌ Debe ingresar un número válido."
            )
        }
    }
}

fun mostrarResumenGeneral(
    gestorClientes: GestorClientes
) {

    val clientes = gestorClientes.obtenerClientes()

    println("\n======================================")
    println("       RESUMEN GENERAL WASHIFY")
    println("======================================")

    println(
        "Clientes registrados: ${clientes.size}"
    )

    if (clientes.isEmpty()) {

        println(
            "No hay clientes registrados actualmente."
        )

    } else {

        println("\nClientes en el sistema:")

        clientes.forEach { cliente ->

            println(
                "• ${cliente.nombre} | ${cliente.correo}"
            )
        }
    }

    println("======================================")
}