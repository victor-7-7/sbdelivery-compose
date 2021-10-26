package ru.skillbranch.sbdelivery.screens.dishes.logic

import android.util.Log
import ru.skillbranch.sbdelivery.screens.dishes.data.DishesUiState
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState


fun Set<DishesFeature.Eff>.toEffs(): Set<Eff> = mapTo(HashSet(), Eff::Dishes)

fun DishesState.reduceCategory(msg: DishesMsg, root: RootState): Pair<RootState, Set<Eff>> {
    val (dishesState, effs) = selfReduce(msg)
    // Блок copy(dishesState = dishesState) будет выполнен на экземпляре ScreenState.Dishes,
    // который имеет свойство dishesState типа DishesState
    return root.updateCurrentScreenState<ScreenState.Dishes> { copy(dishesState = dishesState) } to effs
}

fun DishesState.selfReduce(msg: DishesMsg): Pair<DishesState, Set<Eff>> {
    val pair = when (msg) {
        is DishesMsg.SearchInput -> copy(input = msg.newInput) to emptySet()
        is DishesMsg.SearchSubmit -> copy(uiState = DishesUiState.Loading) to setOf(
            DishesFeature.Eff.SearchDishes(category = category, query = msg.query)
        ).toEffs()
        is DishesMsg.ConnectionFailed -> copy(uiState = DishesUiState.Error) to emptySet()
        is DishesMsg.ShowLoading -> copy(uiState = DishesUiState.Loading) to emptySet()
        is DishesMsg.UpdateSuggestionResult -> this to setOf(
            DishesFeature.Eff.FindSuggestions(category = category, query = msg.query)
        ).toEffs()

        is DishesMsg.ShowSuggestion -> copy(suggestions = msg.sug) to emptySet()
        is DishesMsg.SuggestionSelect -> copy(
            suggestions = emptyMap(),
            input = msg.it
        ) to setOf(DishesFeature.Eff.SearchDishes(category = category, query = msg.it)).toEffs()

        is DishesMsg.ShowDishes -> {
            val dishes = if (msg.dishes.isEmpty()) DishesUiState.Empty
            else DishesUiState.Value(msg.dishes)
            copy(uiState = dishes, suggestions = emptyMap()) to emptySet()
        }

        is DishesMsg.SearchToggle -> when {
            input.isNotEmpty() && isSearch -> copy(
                input = "",
                suggestions = emptyMap()
            ) to setOf(DishesFeature.Eff.FindDishes(category)).toEffs()
            input.isEmpty() && !isSearch -> copy(isSearch = true) to emptySet()
            else -> copy(isSearch = false, suggestions = emptyMap()) to emptySet()
        }
    }
    return pair
}

