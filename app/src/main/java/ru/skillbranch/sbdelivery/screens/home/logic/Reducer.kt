package ru.skillbranch.sbdelivery.screens.home.logic

import android.util.Log
import ru.skillbranch.sbdelivery.screens.dishes.data.DishesUiState
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState


fun Set<HomeFeature.Eff>.toEffs(): Set<Eff> = mapTo(HashSet(), Eff::Home)

fun HomeFeature.State.reduce(msg: HomeFeature.Msg, root: RootState): Pair<RootState, Set<Eff>> {
    val (homeState, effs) = selfReduce(msg)
    // Блок copy(homeState = homeState) будет выполнен на экземпляре ScreenState.Home,
    // который имеет свойство homeState типа HomeFeature.State
    return root.updateCurrentScreenState<ScreenState.Home> { copy(homeState = homeState) } to effs
}

fun HomeFeature.State.selfReduce(msg: HomeFeature.Msg): Pair<HomeFeature.State, Set<Eff>> {
    val pair = when (msg) {
        is HomeFeature.Msg.ShowRecommended -> copy(
            recommended = if (msg.dishes.isEmpty()) DishesUiState.Empty
            else DishesUiState.Value(msg.dishes)
        ) to emptySet<Eff>()

        is HomeFeature.Msg.ShowBest -> copy(
            best = if (msg.dishes.isEmpty()) DishesUiState.Empty
            else DishesUiState.Value(msg.dishes)
        ) to emptySet()

        is HomeFeature.Msg.ShowPopular -> copy(
            popular = if (msg.dishes.isEmpty()) DishesUiState.Empty
            else DishesUiState.Value(msg.dishes)
        ) to emptySet()
    }
    return pair
}

