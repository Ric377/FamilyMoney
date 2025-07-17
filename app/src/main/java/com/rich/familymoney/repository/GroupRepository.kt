// GroupRepository.kt
package com.rich.familymoney.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rich.familymoney.data.Payment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

data class GroupMember(val name: String, val photoUrl: String, val email: String)

class GroupRepository {

    private val db = Firebase.firestore

    // --- МЕТОД ДЛЯ МАССОВОГО УДАЛЕНИЯ ---
    suspend fun deletePayments(groupId: String, paymentIds: List<String>) {
        if (paymentIds.isEmpty()) return

        val collection = db.collection("groups").document(groupId).collection("payments")
        // Используем WriteBatch для эффективного выполнения нескольких операций
        val batch = db.batch()
        paymentIds.forEach { id ->
            batch.delete(collection.document(id))
        }
        batch.commit().await() // .await() из kotlinx-coroutines-play-services
    }
    // --- КОНЕЦ НОВОГО МЕТОДА ---

    // Получает ВСЕ траты группы один раз для расчёта
    suspend fun getAllPayments(groupId: String): List<Payment> {
        val snapshot = db.collection("groups").document(groupId)
            .collection("payments").get().await()

        return snapshot.documents.mapNotNull { doc ->
            Payment(
                id = doc.id,
                sum = doc.getDouble("sum") ?: 0.0,
                comment = doc.getString("comment") ?: "",
                date = doc.getTimestamp("date")?.toDate()?.time ?: 0L,
                name = doc.getString("name") ?: "?",
                photoUrl = doc.getString("photoUrl") ?: ""
            )
        }
    }

    fun getPayments(groupId: String): Flow<List<Payment>> = callbackFlow {
        val collection = db.collection("groups").document(groupId).collection("payments")

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val payments = snapshot?.documents?.mapNotNull { doc ->
                Payment(
                    id = doc.id,
                    sum = doc.getDouble("sum") ?: 0.0,
                    comment = doc.getString("comment") ?: "",
                    date = doc.getTimestamp("date")?.toDate()?.time ?: 0L,
                    name = doc.getString("name") ?: "?",
                    photoUrl = doc.getString("photoUrl") ?: ""
                )
            } ?: emptyList()

            trySend(payments)
        }

        awaitClose { listener.remove() }
    }

    fun getMembers(groupId: String): Flow<List<GroupMember>> = callbackFlow {
        val docRef = db.collection("groups").document(groupId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            // --- ИСПРАВЛЕНИЕ ЗДЕСЬ ---
            // Делаем более безопасную проверку и преобразование типов
            val rawMembers = snapshot?.get("members") as? List<*> ?: emptyList<Any>()
            val members = rawMembers.mapNotNull { item ->
                if (item is Map<*, *>) {
                    val email = item["email"] as? String ?: ""
                    val name = item["name"] as? String ?: email.substringBefore('@')
                    val photoUrl = item["photoUrl"] as? String ?: ""
                    GroupMember(name, photoUrl, email)
                } else {
                    null // Игнорируем элементы, которые не являются картой (Map)
                }
            }
            // --- КОНЕЦ ИСПРАВЛЕНИЯ ---

            trySend(members)
        }
        awaitClose { listener.remove() }
    }

    suspend fun deletePayment(groupId: String, paymentId: String) {
        db.collection("groups").document(groupId)
            .collection("payments").document(paymentId)
            .delete()
    }
}