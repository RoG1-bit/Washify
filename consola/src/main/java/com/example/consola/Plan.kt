package com.example.consola

interface Plan {
    val nombre: String
    val precioMensual: Double
    val limiteLavadosMensuales: Int?

    fun mostrarInformacion()
}