// DebtCalculator.kt
package com.rich.familymoney.util // Можете выбрать другую папку, например, com.rich.familymoney.logic

import com.rich.familymoney.data.Payment
import com.rich.familymoney.repository.GroupMember
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

// 1. Модель данных, описывающая один долг
data class Debt(
    val from: String,   // Кто должен
    val to: String,     // Кому должен
    val amount: Double  // Сколько
)

// 2. Основная функция-калькулятор
fun calculateDebts(payments: List<Payment>, members: List<GroupMember>): List<Debt> {
    if (members.isEmpty() || payments.isEmpty()) {
        return emptyList()
    }

    // Используем BigDecimal для точных финансовых расчётов, чтобы избежать ошибок с Double
    val totalSpent = payments.sumOf { BigDecimal.valueOf(it.sum) }
    val sharePerPerson = totalSpent.divide(BigDecimal(members.size), 2, RoundingMode.HALF_UP)

    // Считаем, сколько каждый участник заплатил
    val amountPaidByMember = payments.groupBy { it.name }
        .mapValues { (_, payments) ->
            payments.sumOf { BigDecimal.valueOf(it.sum) }
        }

    // Считаем баланс каждого: (сколько заплатил) - (сколько должен был заплатить)
    val balances = members.associate { member ->
        val paid = amountPaidByMember[member.name] ?: BigDecimal.ZERO
        member.name to (paid - sharePerPerson)
    }.toMutableMap()

    // Разделяем на тех, кто переплатил (кредиторы), и тех, кто недоплатил (должники)
    val creditors = balances.filter { it.value > BigDecimal.ZERO }.toMutableMap()
    val debtors = balances.filter { it.value < BigDecimal.ZERO }.toMutableMap()

    val debts = mutableListOf<Debt>()

    // Алгоритм погашения долгов
    while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
        val debtorEntry = debtors.entries.first()
        val creditorEntry = creditors.entries.first()

        val debtorName = debtorEntry.key
        val creditorName = creditorEntry.key

        // Сумма перевода — минимальная из двух сумм (долга и переплаты)
        val amountToTransfer = minOf(abs(debtorEntry.value.toDouble()), creditorEntry.value.toDouble())
        val transferBigDecimal = BigDecimal.valueOf(amountToTransfer)

        if (amountToTransfer > 0.001) { // Избегаем нулевых переводов из-за ошибок округления
            debts.add(Debt(from = debtorName, to = creditorName, amount = amountToTransfer))

            // Обновляем балансы
            debtors[debtorName] = debtorEntry.value + transferBigDecimal
            creditors[creditorName] = creditorEntry.value - transferBigDecimal
        }

        // Удаляем из списка тех, чей баланс стал нулевым
        if (abs(debtors[debtorName]!!.toDouble()) < 0.01) {
            debtors.remove(debtorName)
        }
        if (abs(creditors[creditorName]!!.toDouble()) < 0.01) {
            creditors.remove(creditorName)
        }
    }

    return debts
}