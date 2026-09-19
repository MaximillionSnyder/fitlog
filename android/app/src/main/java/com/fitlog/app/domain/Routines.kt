package com.fitlog.app.domain

object RoutineOrder {

    fun moveItem(items: List<String>, fromIndex: Int, toIndex: Int): List<String> {
        require(fromIndex in items.indices && toIndex in items.indices) {
            "Índices fuera de rango: no se puede mover $fromIndex a $toIndex en una lista de ${items.size}"
        }
        if (fromIndex == toIndex) return items.toList()

        val copy = items.toMutableList()
        val moved = copy.removeAt(fromIndex)
        copy.add(toIndex, moved)
        return copy
    }

    fun assignPositions(ids: List<String>): List<Pair<String, Int>> =
        ids.mapIndexed { index, id -> id to index + 1 }
}
