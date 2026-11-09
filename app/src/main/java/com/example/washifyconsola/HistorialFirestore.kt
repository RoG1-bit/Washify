package com.example.washifyconsola

import com.example.consola.Suscripcion
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query

internal data class RegistroLavado(
    val id: String,
    val vehiculoPlaca: String,
    val clienteNombre: String,
    val planNombre: String,
    val fechaHora: Timestamp
)

internal class HistorialFirestore(db: FirebaseFirestore = FirebaseFirestore.getInstance("default")) {
    private val lavados = db.collection("lavados")

    fun observar(
        uid: String,
        esAdministrador: Boolean,
        recibir: (List<RegistroLavado>) -> Unit,
        fallo: (FirebaseFirestoreException) -> Unit
    ): ListenerRegistration {
        val consulta: Query = if (esAdministrador) {
            lavados
        } else {
            lavados.whereEqualTo("clienteUid", uid)
        }

        return consulta.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                fallo(error)
            } else if (snapshot != null && !snapshot.metadata.isFromCache) {
                // Esperar la respuesta del servidor comprueba el permiso de esta sesión.
                val registros = snapshot.documents.mapNotNull { documento ->
                    val fecha = documento.getTimestamp("fechaHora") ?: return@mapNotNull null
                    RegistroLavado(
                        id = documento.id,
                        vehiculoPlaca = documento.getString("vehiculoPlaca").orEmpty(),
                        clienteNombre = documento.getString("clienteNombre").orEmpty(),
                        planNombre = documento.getString("planNombre").orEmpty(),
                        fechaHora = fecha
                    )
                }.sortedByDescending { it.fechaHora }
                recibir(registros)
            }
        }
    }

    fun registrar(uidAdministrador: String, suscripcion: Suscripcion): Task<Void> =
        lavados.document().set(mapOf(
            "clienteUid" to suscripcion.cliente.firebaseUid,
            "registradoPorUid" to uidAdministrador,
            "vehiculoPlaca" to suscripcion.vehiculo.placa,
            "clienteNombre" to suscripcion.cliente.nombre,
            "planNombre" to suscripcion.plan.nombre,
            "fechaHora" to FieldValue.serverTimestamp()
        ))
}
