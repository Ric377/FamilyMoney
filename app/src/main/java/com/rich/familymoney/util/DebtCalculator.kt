// DebtCalculator.kt
package com.rich.familymoney.util

import com.rich.familymoney.data.Payment
import com.rich.familymoney.repository.GroupMember
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

data class Debt(
    val from: String,
    val to: String,
    val amount: Double
)

// НОВАЯ, ОПТИМИЗИРОВАННАЯ ВЕРСИЯ
fun calculateDebts(payments: List<Payment>, members: List<GroupMember>): List<Debt> {
    if (members.isEmpty()) {
        return emptyList()
    }

    // 1. Считаем, сколько каждый участник заплатил
    val amountPaidByMember = payments.groupBy { it.name }
        .mapValues { (_, userPayments) ->
            userPayments.sumOf { BigDecimal.valueOf(it.sum) }
        }

    // 2. Считаем, сколько всего потрачено и долю каждого
    val totalSpent = payments.sumOf { BigDecimal.valueOf(it.sum) }
    // Делим с большим количеством знаков после запятой для точности
    val sharePerPerson = totalSpent.divide(BigDecimal(members.size), 10, RoundingMode.HALF_UP)

    // 3. Считаем баланс для каждого участника
    // Баланс = (сколько заплатил) - (сколько должен был заплатить)
    val balances = members.associate { member ->
        val paid = amountPaidByMember[member.name] ?: BigDecimal.ZERO
        member.name to (paid - sharePerPerson)
    }.toMutableMap()

    // 4. Разделяем на тех, кто переплатил (кредиторы), и тех, кто недоплатил (должники)
    val creditors = balances.filter { it.value > BigDecimal.ZERO }.toMutableMap()
    val debtors = balances.filter { it.value < BigDecimal.ZERO }.toMutableMap()

    val debts = mutableListOf<Debt>()

    // 5. ОПТИМАЛЬНЫЙ АЛГОРИТМ УРЕГУЛИРОВАНИЯ ДОЛГОВ
    // Пока есть и должники, и кредиторы, мы будем сводить их балансы
    while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
        // Берем первого должника и первого кредитора
        val debtorEntry = debtors.entries.first()
        val creditorEntry = creditors.entries.first()

        val debtorName = debtorEntry.key
        val debtorAmount = debtorEntry.value.abs() // Сколько он должен (положительное число)

        val creditorName = creditorEntry.key
        val creditorAmount = creditorEntry.value   // Сколько ему должны

        // Сумма перевода — это наименьшее из двух значений: долга и кредита
        val transferAmount = debtorAmount.min(creditorAmount)

        // Создаем запись о долге, если сумма значима
        if (transferAmount > BigDecimal("0.01")) {
            debts.add(Debt(
                from = debtorName,
                to = creditorName,
                // Округляем до 2 знаков только в самом конце, для отображения
                amount = transferAmount.setScale(2, RoundingMode.HALF_UP).toDouble()
            ))
        }

        // 6. Обновляем балансы
        // Уменьшаем долг должника и кредит кредитора на сумму перевода
        balances[debtorName] = (balances[debtorName] ?: BigDecimal.ZERO) + transferAmount
        balances[creditorName] = (balances[creditorName] ?: BigDecimal.ZERO) - transferAmount

        // 7. Удаляем из списков тех, кто полностью рассчитался
        // Используем compareTo для точного сравнения с нулем
        if (balances[debtorName]!!.abs().compareTo(BigDecimal("0.01")) < 0) {
            debtors.remove(debtorName)
        }
        if (balances[creditorName]!!.abs().compareTo(BigDecimal("0.01")) < 0) {
            creditors.remove(creditorName)
        }
    }

    return debts
}
