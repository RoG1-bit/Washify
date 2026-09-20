package com.example.consola

data class Vehiculo(
    val id: Int,
    var placa: String,
    var marca: String,
    var modelo: String,
    var clienteId: Int,
    var plan: Plan? = null
)