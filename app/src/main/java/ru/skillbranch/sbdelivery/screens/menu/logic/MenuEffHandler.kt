package ru.skillbranch.sbdelivery.screens.menu.logic

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import ru.skillbranch.sbdelivery.repository.CategoriesRepository
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.IEffectHandler
import ru.skillbranch.sbdelivery.screens.root.logic.Msg
import javax.inject.Inject
import kotlin.coroutines.coroutineContext


class MenuEffHandler @Inject constructor(
    private val categoriesRepo: CategoriesRepository,
    private val notifyChanel: Channel<Eff.Notification>,
    override var localJob: Job
) : IEffectHandler<MenuFeature.Eff, Msg> {

    @ExperimentalCoroutinesApi
    override suspend fun handle(effect: MenuFeature.Eff, commit: (Msg) -> Unit) {

        CoroutineScope(coroutineContext + localJob).launch {
            when (effect) {
                MenuFeature.Eff.FindCategories -> {
                    categoriesRepo.findCategories()
                        .map(MenuFeature.Msg::ShowMenu)
                        .map(Msg::Menu)
                        .collect { commit(it) }
                }
            }
        }
    }
}