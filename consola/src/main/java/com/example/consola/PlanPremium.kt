package com.example.consola

class PlanPremium : Plan{
    override val nombre = "Plan Premium"
    override val precioMensual = 25.00
    override val limiteLavadosMensuales: Int? = null

    override fun mostrarInformacion() {
        println("=== PLAN Premium ===")
        println("Precio mensual: $$precioMensual")
        println("Lavados incluidos al mes: Ilimitados")
    }
}