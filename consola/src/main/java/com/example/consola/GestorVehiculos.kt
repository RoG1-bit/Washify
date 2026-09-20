package com.example.consola

class GestorVehiculos {

    private val listaVehiculos = mutableListOf<Vehiculo>()
    private var contadorId = 1

    fun registrarVehiculo(
        placa: String,
        marca: String,
        modelo: String,
        clienteId: Int
    ): Boolean {

        if (placa.isBlank() || marca.isBlank() || modelo.isBlank()) {
            println("❌ Error: La placa, marca y modelo no pueden estar vacíos.")
            return false
        }

        if (listaVehiculos.any { it.placa.equals(placa, ignoreCase = true) }) {
            println("❌ Error: Ya existe un vehículo con esa placa.")
            return false
        }

        val nuevoVehiculo = Vehiculo(
            id = contadorId++,
            placa = placa,
            marca = marca,
            modelo = modelo,
            clienteId = clienteId
        )

        listaVehiculos.add(nuevoVehiculo)

        println("✅ Vehículo registrado correctamente.")
        println(nuevoVehiculo)

        return true
    }

    fun listarVehiculos() {

        if (listaVehiculos.isEmpty()) {
            println("⚠️ No hay vehículos registrados.")
            return
        }

        println("\n--- LISTA DE VEHÍCULOS ---")

        listaVehiculos.forEach { vehiculo ->
            println(
                "ID: ${vehiculo.id} | " +
                        "Placa: ${vehiculo.placa} | " +
                        "Marca: ${vehiculo.marca} | " +
                        "Modelo: ${vehiculo.modelo} | " +
                        "Cliente ID: ${vehiculo.clienteId} | " +
                        "Plan: ${vehiculo.plan?.nombre ?: "Sin plan"}"
            )
        }
    }

    fun buscarVehiculo(id: Int): Vehiculo? {
        return listaVehiculos.find { it.id == id }
    }

    fun actualizarVehiculo(
        id: Int,
        nuevaPlaca: String,
        nuevaMarca: String,
        nuevoModelo: String
    ) {

        val vehiculo = buscarVehiculo(id)

        if (vehiculo == null) {
            println("❌ Error: No se encontró un vehículo con ID $id.")
            return
        }

        if (nuevaPlaca.isNotBlank()) {
            vehiculo.placa = nuevaPlaca
        }

        if (nuevaMarca.isNotBlank()) {
            vehiculo.marca = nuevaMarca
        }

        if (nuevoModelo.isNotBlank()) {
            vehiculo.modelo = nuevoModelo
        }

        println("✅ Vehículo actualizado correctamente.")
    }

    fun eliminarVehiculo(id: Int) {

        val eliminado = listaVehiculos.removeIf { it.id == id }

        if (eliminado) {
            println("✅ Vehículo eliminado correctamente.")
        } else {
            println("❌ Error: No se encontró un vehículo con ID $id.")
        }
    }

    fun asignarPlan(idVehiculo: Int, plan: Plan) {

        val vehiculo = buscarVehiculo(idVehiculo)

        if (vehiculo == null) {
            println("❌ Error: No se encontró el vehículo.")
            return
        }

        vehiculo.plan = plan

        println("✅ ${plan.nombre} asignado al vehículo ${vehiculo.placa}.")
    }

    fun obtenerVehiculosPorCliente(clienteId: Int): List<Vehiculo> {
        return listaVehiculos.filter { it.clienteId == clienteId }
    }
}