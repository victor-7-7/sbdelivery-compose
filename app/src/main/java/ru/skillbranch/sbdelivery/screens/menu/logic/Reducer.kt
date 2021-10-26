package ru.skillbranch.sbdelivery.screens.menu.logic

import android.util.Log
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.NavCmd
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState

fun Set<MenuFeature.Eff>.toEffs(): Set<Eff> = mapTo(HashSet(), Eff::Menu)

fun MenuFeature.State.reduce(msg: MenuFeature.Msg, root: RootState): Pair<RootState, Set<Eff>> {
    val (menuState, effs) = selfReduce(msg)
    // Блок copy(menuState = menuState) будет выполнен на экземпляре ScreenState.Menu,
    // который имеет свойство menuState типа MenuFeature.State
    return root.updateCurrentScreenState<ScreenState.Menu> { copy(menuState = menuState) } to effs
}

fun MenuFeature.State.selfReduce(msg: MenuFeature.Msg): Pair<MenuFeature.State, Set<Eff>> {
    val pair = when (msg) {
        is MenuFeature.Msg.ClickCategory -> {
            // Video-2 t.c. 01:42:50.
            // Среди всех категорий ищем хотя бы одну, которая будет дочерней для
            // кликнутой категории (msg.id)
            val nextParent = categories.find { it.parentId == msg.id } //if has child category level
            // Если nextParent -> null, значит кликнули по категории (msg.id),
            // которая не имеет дочерних категорий, это категория верхнего уровня.
            //if has not child level then open category
            if (nextParent == null) copy() to setOf(Eff.Nav(NavCmd.ToCategory(msg.id, msg.title)))
            // Если nextParent != null, значит кликнули по категории (msg.id),
            // которая имеет дочерние категории (одну или несколько). Обновляем стейт
            //else has child subcategory then update current
            else copy(parentId = msg.id) to emptySet()
        }
        MenuFeature.Msg.PopCategory -> {
            copy(parentId = parent?.parentId) to emptySet()
        }
        is MenuFeature.Msg.ShowMenu -> copy(categories = msg.categories) to emptySet()
    }
    return pair
}

