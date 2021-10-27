package ru.skillbranch.sbdelivery.screens.home.logic

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import ru.skillbranch.sbdelivery.repository.DishesRepository
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.IEffectHandler
import ru.skillbranch.sbdelivery.screens.root.logic.Msg
import javax.inject.Inject
import kotlin.coroutines.coroutineContext


@FlowPreview
class HomeEffHandler @Inject constructor(
    private val dishesRepo: DishesRepository,
    private val notifyChanel: Channel<Eff.Notification>,
    override var localJob: Job
) : IEffectHandler<HomeFeature.Eff, Msg> {

    private val errHandler = CoroutineExceptionHandler{_, t ->
        t.printStackTrace()
        t.message?.let { notifyChanel.trySend(Eff.Notification.Error(it)) }
    }

    @ExperimentalCoroutinesApi
    override suspend fun handle(effect: HomeFeature.Eff, commit: (Msg) -> Unit) {

        CoroutineScope(coroutineContext + localJob + errHandler).launch {
            when (effect) {
                HomeFeature.Eff.FindBest -> {
                    dishesRepo.findBest()
                        .map(HomeFeature.Msg::ShowBest)
                        .map(Msg::Home)
                        .collect { commit(it) }

                }

                HomeFeature.Eff.FindPopular -> {
                    dishesRepo.findPopular()
                        .map(HomeFeature.Msg::ShowPopular)
                        .map(Msg::Home)
                        .collect { commit(it) }
                }

                HomeFeature.Eff.SyncRecommended -> {
                    Log.e("XXX", "HomeEffHandler.handle: eff => $effect")
                    // Video-2 t.c. 01:26:00. Сначала получаем список айдишников
                    // рекомендуемых блюд из сети
                    val ids = dishesRepo.getRecommended()
                    // Если рекомендуемых на сервере нет, то показываем - пусто
                    if (ids.isEmpty()) {
                        HomeFeature.Msg.ShowRecommended(emptyList())
                            .let(Msg::Home)
                            .also(commit)
                    } else {
                        // На сервере есть такой список. Ищем в БД девайса рекомендуемые
                        // блюда по списку их айдишников. Какая-то часть ids может
                        // отсутствовать в локальной БД.
                        val dishes = dishesRepo.findRecommended(ids)

                        //async sync recommended if need
                        launch {
                            // Video-2 t.c. 01:26:20.
                            dishes
                                // Берем первый заэмиченный во флоу список блюд (как раз тот,
                                // который пришел из локальной БД). После этого флоу отменяется.
                                .take(1)
                                .map { items -> items.map { it.id } }
                                .onEach { Log.e("XXX", "HomeEffHandler. Exist: $it") }
                                // Фильтруем уже имеющиеся в локальной БД айдишники, оставив
                                // в списке ids только те, которых нет в локальной БД
                                .map { existIds -> ids.filter { !existIds.contains(it) } }
                                .onEach { Log.e("XXX", "HomeEffHandler. NeedReload: $it") }
                                // в коллекторе синхронизируем рекомендуемые блюда с локальной БД
                                // (загружаем отсутствующие с сервера и вставляем их в БД)
                                // Непонятно - зачем здесь asFlow()??? Video-2 t.c. 01:27:48
                                .collect{ dishesRepo.syncRecommended(it).asFlow() }
                        }

                        //show recommended
                        launch {
                            dishes
                                .distinctUntilChanged()
                                .map(HomeFeature.Msg::ShowRecommended)
                                .map(Msg::Home)
                                .onEach { Log.e("XXX", "HomeEffHandler. ShowRecommended: $it") }
                                .collect { commit(it) }
                        }
                    }
                }
            }
        }
    }
}