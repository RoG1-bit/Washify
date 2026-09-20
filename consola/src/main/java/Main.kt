import com.example.consola.GestorClientes
import com.example.consola.GestorVehiculos
import com.example.consola.PlanBasico
import com.example.consola.PlanPremium
import com.example.consola.menuReportes
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Scanner

object ErrorLogger {

    private val archivoLog = File("errores_washify_log.txt")

    fun registrarError(
        mensaje: String,
        excepcion: Exception
    ) {
        try {

            val fechaHora = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            )

            val textoError =
                "[$fechaHora] ERROR: $mensaje - Detalle: ${excepcion.message}\n"

            archivoLog.appendText(textoError)

        } catch (_: Exception) {

            println(
                "Error fatal al intentar escribir en el archivo de log."
            )
        }
    }
}

fun menuClientes(
    scanner: Scanner,
    gestor: GestorClientes
) {

    var salir = false

    println(
        "\n=== BIENVENIDO AL MÓDULO DE CLIENTES WASHIFY ==="
    )

    while (!salir) {

        println("\nSeleccione una opción:")
        println("1. Registrar nuevo cliente")
        println("2. Listar clientes")
        println("3. Actualizar cliente")
        println("4. Eliminar cliente")
        println("5. Volver al menú principal")
        print("Opción: ")

        try {

            when (scanner.nextLine().toInt()) {

                1 -> {

                    print("Ingrese el nombre: ")
                    val nombre = scanner.nextLine()

                    print("Ingrese el correo: ")
                    val correo = scanner.nextLine()

                    gestor.registrarCliente(
                        nombre,
                        correo
                    )
                }

                2 -> {
                    gestor.listarClientes()
                }

                3 -> {

                    print("Ingrese el ID a actualizar: ")
                    val id = scanner.nextLine().toInt()

                    print(
                        "Nuevo nombre (deje en blanco para no cambiar): "
                    )
                    val nombre = scanner.nextLine()

                    print(
                        "Nuevo correo (deje en blanco para no cambiar): "
                    )
                    val correo = scanner.nextLine()

                    gestor.actualizarCliente(
                        id,
                        nombre,
                        correo
                    )
                }

                4 -> {

                    print("Ingrese el ID a eliminar: ")
                    val id = scanner.nextLine().toInt()

                    gestor.eliminarCliente(id)
                }

                5 -> {
                    salir = true
                }

                else -> {
                    println("❌ Opción no válida.")
                }
            }

        } catch (_: NumberFormatException) {

            println(
                "❌ Error: Por favor, ingrese un número válido."
            )

        } catch (e: Exception) {

            println(
                "❌ Ocurrió un error inesperado: ${e.message}"
            )
        }
    }
}

fun menuVehiculos(
    scanner: Scanner,
    gestorVehiculos: GestorVehiculos
) {

    var salir = false

    println(
        "\n=== MÓDULO DE VEHÍCULOS Y PLANES WASHIFY ==="
    )

    while (!salir) {

        println("\nSeleccione una opción:")
        println("1. Registrar vehículo")
        println("2. Listar vehículos")
        println("3. Actualizar vehículo")
        println("4. Eliminar vehículo")
        println("5. Asignar Plan Básico")
        println("6. Asignar Plan Premium")
        println("7. Ver información de planes")
        println("8. Volver al menú principal")
        print("Opción: ")

        try {

            when (scanner.nextLine().toInt()) {

                1 -> {

                    print("Ingrese la placa: ")
                    val placa = scanner.nextLine()

                    print("Ingrese la marca: ")
                    val marca = scanner.nextLine()

                    print("Ingrese el modelo: ")
                    val modelo = scanner.nextLine()

                    print("Ingrese el ID del cliente: ")
                    val clienteId = scanner.nextLine().toInt()

                    gestorVehiculos.registrarVehiculo(
                        placa,
                        marca,
                        modelo,
                        clienteId
                    )
                }

                2 -> {
                    gestorVehiculos.listarVehiculos()
                }

                3 -> {

                    print("Ingrese el ID del vehículo: ")
                    val id = scanner.nextLine().toInt()

                    print(
                        "Nueva placa (deje vacío para no cambiar): "
                    )
                    val placa = scanner.nextLine()

                    print(
                        "Nueva marca (deje vacío para no cambiar): "
                    )
                    val marca = scanner.nextLine()

                    print(
                        "Nuevo modelo (deje vacío para no cambiar): "
                    )
                    val modelo = scanner.nextLine()

                    gestorVehiculos.actualizarVehiculo(
                        id,
                        placa,
                        marca,
                        modelo
                    )
                }

                4 -> {

                    print("Ingrese el ID del vehículo: ")
                    val id = scanner.nextLine().toInt()

                    gestorVehiculos.eliminarVehiculo(id)
                }

                5 -> {

                    print("Ingrese el ID del vehículo: ")
                    val id = scanner.nextLine().toInt()

                    gestorVehiculos.asignarPlan(
                        id,
                        PlanBasico()
                    )
                }

                6 -> {

                    print("Ingrese el ID del vehículo: ")
                    val id = scanner.nextLine().toInt()

                    gestorVehiculos.asignarPlan(
                        id,
                        PlanPremium()
                    )
                }

                7 -> {

                    println()

                    PlanBasico().mostrarInformacion()

                    println()

                    PlanPremium().mostrarInformacion()
                }

                8 -> {
                    salir = true
                }

                else -> {
                    println("❌ Opción no válida.")
                }
            }

        } catch (_: NumberFormatException) {

            println(
                "❌ Error: Debe ingresar un número válido."
            )

        } catch (e: Exception) {

            println(
                "❌ Ocurrió un error inesperado: ${e.message}"
            )
        }
    }
}

fun main() {

    val scanner = Scanner(System.`in`)

    val gestorClientes = GestorClientes()
    val gestorVehiculos = GestorVehiculos()

    var salir = false

    println("==========================================")
    println("        BIENVENIDO A WASHIFY")
    println("==========================================")

    while (!salir) {

        println("\n--- MENÚ PRINCIPAL ---")
        println(
            "1. Módulo de Clientes y Base (Roger Ramirez)"
        )
        println(
            "2. Gestión de Vehículos y Planes (Rodolfo Herrera)"
        )
        println(
            "3. Gestión de Suscripciones (Integrante 3)"
        )
        println(
            "4. Registrar Lavado y Validaciones (Integrante 4)"
        )
        println(
            "5. Reportes y Resumen del Sistema (Integrante 5)"
        )
        println("6. Salir")

        print("Seleccione un módulo para ingresar: ")

        try {

            when (scanner.nextLine().toInt()) {

                1 -> {
                    menuClientes(
                        scanner,
                        gestorClientes
                    )
                }

                2 -> {
                    menuVehiculos(
                        scanner,
                        gestorVehiculos
                    )
                }

                3 -> {
                    println(
                        ">> Entrando al Módulo de Suscripciones..."
                    )
                }

                4 -> {
                    println(
                        ">> Entrando al Registro de Lavados..."
                    )
                }

                5 -> {
                    menuReportes(
                        scanner,
                        gestorClientes
                    )
                }

                6 -> {

                    salir = true

                    println(
                        "Saliendo del sistema Washify... ¡Hasta pronto!"
                    )
                }

                else -> {

                    println(
                        "❌ Opción inválida. Ingrese un número del 1 al 6."
                    )
                }
            }

        } catch (e: NumberFormatException) {

            println(
                "❌ Error: Debe ingresar obligatoriamente un NÚMERO."
            )

            ErrorLogger.registrarError(
                "El usuario ingresó un texto en lugar de número",
                e
            )

        } catch (e: Exception) {

            println(
                "❌ Ocurrió un error inesperado. Revisa el archivo de logs."
            )

            ErrorLogger.registrarError(
                "Error general en main",
                e
            )
        }
    }
}