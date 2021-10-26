package ru.skillbranch.sbdelivery.screens.menu.logic

import ru.skillbranch.sbdelivery.domain.CategoryItem
import java.io.Serializable


object MenuFeature {
    const val route: String = "menu"

    fun initialState(): State = State()
    fun initialEffects(): Set<Eff> = setOf(Eff.FindCategories)

    data class State(
        // Video-2 t.c. 01:43:50.
        /** Список всех возможных категорий - и верхнеуровневых, и дочерних */
        val categories: List<CategoryItem> = emptyList(),
        val parentId: String? = null
    ) : Serializable {
        val parent: CategoryItem?
            // it.id всегда not null. Если parentId -> null, то find вернет null
            get() = categories.find { it.id == parentId }
        /** Список категорий, являющихся дочерними для категории parent. Если
         * parent -> null, то это список всех верхнеуровневых категорий
         * (у которых parentId -> null) */
        val current: List<CategoryItem>
            get() {
                //if parentId null return top level categories
                val cats = categories.filter { it.parentId == parentId }

                return parent?.icon
                    // А если c.icon не null, все равно ведь будет замена на parent.icon ???
                    // Video-2 t.c. 01:45:00. Все дочерние категории должны иметь иконку своего родителя
                    //if child icon == null set parent icon
                    ?.let { parIcon -> cats.map { c -> c.copy(icon = parIcon) } }
                    ?: cats //if parent null -> return filtered top level categories
            }
    }

    sealed class Eff {
        object FindCategories : Eff()
    }

    sealed class Msg {
        data class ShowMenu(val categories: List<CategoryItem>) : Msg()
        data class ClickCategory(val id: String, val title: String) : Msg()
        object PopCategory : Msg()
    }
}

