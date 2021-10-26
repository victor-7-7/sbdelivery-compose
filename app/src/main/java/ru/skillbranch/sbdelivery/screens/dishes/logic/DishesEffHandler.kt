package ru.skillbranch.sbdelivery.screens.dishes.logic

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import ru.skillbranch.sbdelivery.repository.DishesRepository
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.IEffHandler
import ru.skillbranch.sbdelivery.screens.root.logic.Msg
import javax.inject.Inject
import kotlin.coroutines.coroutineContext


class DishesEffHandler @Inject constructor(
    private val dishesRepo: DishesRepository,
    private val notifyChanel: Channel<Eff.Notification>,
    override var localJob: Job
) : IEffHandler<DishesFeature.Eff, Msg> {

    private val errHandler = CoroutineExceptionHandler { _, t ->
        t.printStackTrace()
        t.message?.let { notifyChanel.trySend(Eff.Notification.Error(it)) }
    }

    @ExperimentalCoroutinesApi
    override suspend fun handle(effect: DishesFeature.Eff,  commit: (Msg) -> Unit    ) {

        CoroutineScope(coroutineContext + localJob + errHandler).launch {
            when (effect) {
                is DishesFeature.Eff.FindDishes -> {
                    commit(DishesMsg.ShowLoading.toMsg())

                    dishesRepo.findDishesByCategory(effect.category)
                        .map(DishesMsg::ShowDishes)
                        .map(Msg::Dishes)
                        .collect { commit(it) }
                }

                is DishesFeature.Eff.SearchDishes -> {
                    commit(DishesMsg.ShowLoading.toMsg())

                    dishesRepo.searchDishes(effect.category, effect.query)
                        .map(DishesMsg::ShowDishes)
                        .map(Msg::Dishes)
                        .collect { commit(it) }
                }

                is DishesFeature.Eff.FindSuggestions -> {
                    dishesRepo.findSuggestions(effect.category, effect.query)
                        .map(DishesMsg::ShowSuggestion)
                        .map(Msg::Dishes)
                        .collect { commit(it) }
                }
            }
        }
    }
}

fun DishesMsg.toMsg(): Msg = Msg.Dishes(this)