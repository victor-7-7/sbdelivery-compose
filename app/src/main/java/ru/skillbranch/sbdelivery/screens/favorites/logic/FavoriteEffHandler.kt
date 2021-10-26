package ru.skillbranch.sbdelivery.screens.favorites.logic

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.skillbranch.sbdelivery.repository.DishesRepository
import ru.skillbranch.sbdelivery.screens.dishes.logic.DishesMsg
import ru.skillbranch.sbdelivery.screens.dishes.logic.toMsg
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.IEffHandler
import ru.skillbranch.sbdelivery.screens.root.logic.Msg
import javax.inject.Inject
import kotlin.coroutines.coroutineContext


class FavoriteEffHandler @Inject constructor(
    private val dishesRepo: DishesRepository,
    private val notifyChanel: Channel<Eff.Notification>,
    override var localJob: Job
) : IEffHandler<FavoriteFeature.Eff, Msg> {

    private val errHandler = CoroutineExceptionHandler{_, t ->
        t.printStackTrace()
        t.message?.let { notifyChanel.trySend(Eff.Notification.Error(it)) }
    }

    @ExperimentalCoroutinesApi
    override suspend fun handle(effect: FavoriteFeature.Eff, commit: (Msg) -> Unit) {

        CoroutineScope(coroutineContext + localJob + errHandler).launch {
                when (effect) {
                    is FavoriteFeature.Eff.FindDishes -> {
                        commit(DishesMsg.ShowLoading.toMsg())

                        dishesRepo.findFavoriteDishes()
                            .onEach { Log.e("XXX", "FavoriteEffHandler. $it") }
                            .map(DishesMsg::ShowDishes)
                            .map(Msg::Dishes)
                            .collect { commit(it) }
                    }

                    is FavoriteFeature.Eff.SearchDishes -> {
                        commit(DishesMsg.ShowLoading.toMsg())

                        dishesRepo.searchFavoriteDishes(effect.query)
                            .map(DishesMsg::ShowDishes)
                            .map(Msg::Dishes)
                            .collect { commit(it) }
                    }

                    is FavoriteFeature.Eff.FindSuggestions -> {
                        dishesRepo.findFavoriteSuggestions(effect.query)
                            .map(DishesMsg::ShowSuggestion)
                            .map(Msg::Dishes)
                            .collect { commit(it) }
                    }
                }
        }
    }
}