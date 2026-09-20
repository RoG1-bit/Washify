package com.example.consola

class PlanBasico : Plan{
    override val nombre = "Plan Basico"
    override val precioMensual = 15.00
    override val limiteLavadosMensuales = 4

    override fun mostrarInformacion() {
        println("=== PLAN BASICO ===")
        println("Precio mensual: $$precioMensual")
        println("Lavados incluidos al mes: $limiteLavadosMensuales")
    }
}