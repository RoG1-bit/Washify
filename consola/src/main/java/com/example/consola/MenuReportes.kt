package com.example.consola

import java.util.Scanner

fun menuReportes(
    scanner: Scanner,
    gestorClientes: GestorClientes,
    gestorSuscripciones: GestorSuscripciones
) {

    var salir = false

    while (!salir) {

        println("\n======================================")
        println("           REPORTES WASHIFY")
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

                    mostrarResumenGeneral(
                        gestorClientes,
                        gestorSuscripciones
                    )
                }

                3 -> {

                    mostrarSuscripcionesActivas(
                        gestorSuscripciones
                    )
                }

                4 -> {

                    mostrarSuscripcionesVencidas(
                        gestorSuscripciones
                    )
                }

                5 -> {

                    println(
                        ">> Servicios realizados aún no disponibles."
                    )
                }

                6 -> {

                    NotificacionService
                        .mostrarNotificaciones()
                }

                7 -> {

                    salir = true
                }

                else -> {

                    println(
                        "❌ Opción no válida."
                    )
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
    gestorClientes: GestorClientes,
    gestorSuscripciones: GestorSuscripciones
) {

    val clientes =
        gestorClientes.obtenerClientes()

    val suscripciones =
        gestorSuscripciones.obtenerTodas()

    val suscripcionesActivas =
        suscripciones.count {
            it.estado == EstadoSuscripcion.ACTIVA
        }

    val suscripcionesVencidas =
        suscripciones.count {
            it.estado == EstadoSuscripcion.VENCIDA
        }

    println("\n======================================")
    println("       RESUMEN GENERAL WASHIFY")
    println("======================================")

    println(
        "Clientes registrados: ${clientes.size}"
    )

    println(
        "Suscripciones registradas: ${suscripciones.size}"
    )

    println(
        "Suscripciones activas: $suscripcionesActivas"
    )

    println(
        "Suscripciones vencidas: $suscripcionesVencidas"
    )

    if (clientes.isEmpty()) {

        println(
            "\nNo hay clientes registrados actualmente."
        )

    } else {

        println(
            "\nClientes en el sistema:"
        )

        clientes.forEach { cliente ->

            println(
                "• ${cliente.nombre} | ${cliente.correo}"
            )
        }
    }

    println("======================================")
}

fun mostrarSuscripcionesActivas(
    gestorSuscripciones: GestorSuscripciones
) {

    val activas =
        gestorSuscripciones
            .obtenerTodas()
            .filter {
                it.estado == EstadoSuscripcion.ACTIVA
            }

    println("\n======================================")
    println("       SUSCRIPCIONES ACTIVAS")
    println("======================================")

    if (activas.isEmpty()) {

        println(
            "No hay suscripciones activas."
        )

    } else {

        println(
            "Total de suscripciones activas: ${activas.size}"
        )

        println()

        activas.forEach { suscripcion ->

            println(suscripcion)
        }
    }

    println("======================================")
}

fun mostrarSuscripcionesVencidas(
    gestorSuscripciones: GestorSuscripciones
) {

    val vencidas =
        gestorSuscripciones
            .obtenerTodas()
            .filter {
                it.estado == EstadoSuscripcion.VENCIDA
            }

    println("\n======================================")
    println("       SUSCRIPCIONES VENCIDAS")
    println("======================================")

    if (vencidas.isEmpty()) {

        println(
            "No hay suscripciones vencidas."
        )

    } else {

        println(
            "Total de suscripciones vencidas: ${vencidas.size}"
        )

        println()

        vencidas.forEach { suscripcion ->

            println(suscripcion)
        }
    }

    println("======================================")
}