package ru.skillbranch.sbdelivery.screens.dishes.logic

import ru.skillbranch.sbdelivery.domain.DishItem
import ru.skillbranch.sbdelivery.screens.dishes.data.DishesUiState
import java.io.Serializable


object DishesFeature {
    const val route: String = "dishes"

    fun initialState(title: String, category: String): DishesState =
        DishesState(title = title, category = category)

    fun initialEffects(category: String): Set<Eff> = setOf<Eff>(Eff.FindDishes(category))

    sealed class Eff {
        /** Ищем блюда, относящиеся к выбранной в меню категории блюд */
        data class FindDishes(val category: String) : Eff()
        /** Ищем блюда либо по категории, если query - пустая строка,
         * либо по поисковой строке ввода с учетом категории */
        data class SearchDishes(val category: String, val query: String) : Eff()
        /** Формируем подсказки при вводе юзером символов в поисковой строке с учетом категории */
        data class FindSuggestions(val category: String, val query: String) : Eff()
    }
}

// Video-2 t.c. 01:56:40.
// Стейт глобальный, потому что используется на нескольких экранах

data class DishesState(
    val category: String = "",
    val title: String = "",
    val input: String = "",
    val isSearch: Boolean = false,
    val suggestions: Map<String, Int> = emptyMap(),
    val uiState: DishesUiState = DishesUiState.Loading
) : Serializable

// Мессиджи глобальные

sealed class DishesMsg {
    data class SearchInput(val newInput: String) : DishesMsg()
    data class ShowDishes(val dishes: List<DishItem>) : DishesMsg()
    data class SearchSubmit(val query: String) : DishesMsg()
    data class UpdateSuggestionResult(val query: String) : DishesMsg()
    data class ShowSuggestion(val sug: Map<String, Int>) : DishesMsg()
    data class SuggestionSelect(val it: String) : DishesMsg()

    object SearchToggle : DishesMsg()
    object ConnectionFailed : DishesMsg()
    object ShowLoading : DishesMsg()
}