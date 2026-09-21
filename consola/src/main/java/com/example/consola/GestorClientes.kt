package com.example.consola

class GestorClientes {

    private val listaClientes = mutableListOf<Cliente>()
    private var contadorId = 1

    fun registrarCliente(
        nombre: String,
        correo: String
    ): Boolean {

        if (nombre.isBlank() || correo.isBlank()) {
            println(
                "❌ Error: El nombre y el correo no pueden estar vacíos."
            )
            return false
        }

        val nuevoCliente = Cliente(
            contadorId++,
            nombre,
            correo
        )

        listaClientes.add(nuevoCliente)

        println(
            "✅ Cliente registrado exitosamente: $nuevoCliente"
        )

        return true
    }

    fun listarClientes() {

        if (listaClientes.isEmpty()) {

            println(
                "⚠️ No hay clientes registrados en el sistema."
            )

        } else {

            println("--- LISTA DE CLIENTES ---")

            listaClientes.forEach { cliente ->

                println(
                    "ID: ${cliente.id} | " +
                            "Nombre: ${cliente.nombre} | " +
                            "Correo: ${cliente.correo}"
                )
            }
        }
    }

    fun actualizarCliente(
        id: Int,
        nuevoNombre: String,
        nuevoCorreo: String
    ) {

        val cliente = listaClientes.find {
            it.id == id
        }

        if (cliente != null) {

            if (nuevoNombre.isNotBlank()) {
                cliente.nombre = nuevoNombre
            }

            if (nuevoCorreo.isNotBlank()) {
                cliente.correo = nuevoCorreo
            }

            println(
                "✅ Cliente actualizado: $cliente"
            )

        } else {

            println(
                "❌ Error: No se encontró un cliente con el ID $id."
            )
        }
    }

    fun eliminarCliente(id: Int) {

        val eliminado = listaClientes.removeIf {
            it.id == id
        }

        if (eliminado) {

            println(
                "✅ Cliente con ID $id eliminado correctamente."
            )

        } else {

            println(
                "❌ Error: No se pudo eliminar. ID no encontrado."
            )
        }
    }

    /*
     * Permite que otros módulos, como Reportes,
     * puedan consultar los clientes sin modificar
     * directamente la lista original.
     */
    fun obtenerClientes(): List<Cliente> {
        return listaClientes.toList()
    }
}