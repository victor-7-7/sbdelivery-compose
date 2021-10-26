package ru.skillbranch.sbdelivery.screens.root.logic

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import ru.skillbranch.sbdelivery.repository.RootRepository
import ru.skillbranch.sbdelivery.screens.cart.logic.CartEffHandler
import ru.skillbranch.sbdelivery.screens.cart.logic.CartFeature
import ru.skillbranch.sbdelivery.screens.dish.logic.DishEffHandler
import ru.skillbranch.sbdelivery.screens.dish.logic.DishFeature
import ru.skillbranch.sbdelivery.screens.dishes.logic.DishesEffHandler
import ru.skillbranch.sbdelivery.screens.dishes.logic.DishesFeature
import ru.skillbranch.sbdelivery.screens.favorites.logic.FavoriteEffHandler
import ru.skillbranch.sbdelivery.screens.favorites.logic.FavoriteFeature
import ru.skillbranch.sbdelivery.screens.home.logic.HomeEffHandler
import ru.skillbranch.sbdelivery.screens.home.logic.HomeFeature
import ru.skillbranch.sbdelivery.screens.menu.logic.MenuEffHandler
import ru.skillbranch.sbdelivery.screens.menu.logic.MenuFeature
import javax.inject.Inject


@FlowPreview
class EffDispatcher @Inject constructor(
    private val dishesHandler: DishesEffHandler,
    private val dishHandler: DishEffHandler,
    private val cartHandler: CartEffHandler,
    private val homeHandler: HomeEffHandler,
    private val menuHandler: MenuEffHandler,
    private val favoriteHandler: FavoriteEffHandler,
    private val rootRepo: RootRepository,
    //Channels for ui (ui effects) and android command
    private val _cmdChanel: Channel<Command>,
    private val _notifyChanel: Channel<Eff.Notification>,

    ) : IEffHandler<Eff, Msg> {
    // fan-out => развёртываться как веер
    // receiveAsFlow -> Represents the given receive channel as a hot flow and
    // receives from the channel in fan-out fashion every time this flow is
    // collected. One element will be emitted to one collector only

    // К этому флоу, связанному с чаннелом эффектов-нотификаций,
    // приколлекчен компоузбл RootScreen
    //for notification / UI effects
    val notifications = _notifyChanel.receiveAsFlow()

    // К этому флоу, связанному с чаннелом андроид-команд, приколлекчена RootActivity
    //for android command
    val commands = _cmdChanel.receiveAsFlow()

    @ExperimentalCoroutinesApi
    override suspend fun handle(effect: Eff, commit: (Msg) -> Unit) {
        Log.w("XXX", "EffDispatcher.handle() [EFFECT: $effect] | [COMMIT: $commit]")
        when (effect) {
            //feature effects
            is Eff.Dishes -> dishesHandler.handle(effect.dishesEff, commit)
            is Eff.Dish -> dishHandler.handle(effect.dishEff, commit)
            is Eff.Cart -> cartHandler.handle(effect.cartEff, commit)
            is Eff.Home -> homeHandler.handle(effect.homeEff, commit)
            is Eff.Menu -> menuHandler.handle(effect.menuEff, commit)
            is Eff.Favorite -> favoriteHandler.handle(effect.favoriteEff, commit)

            //sync effects
            is Eff.SyncCounter -> {
                rootRepo.cartCount()
                    .map(Msg::UpdateCartCount)
                    .collect { commit(it) }
            }

            is Eff.SyncEntity -> {
                coroutineScope {
                    launch {
                        val isEmpty = rootRepo.isEmptyDishes()
                        // Если в БД девайса блюд нет, то загрузим их из сети
                        // и добавим в БД
                        if (isEmpty) rootRepo.syncDishes()
                    }
                    launch {
                        val isEmpty = rootRepo.isEmptyCategories()
                        // Если в БД девайса категорий нет, то загрузим их из сети
                        // и добавим в БД
                        if (isEmpty) rootRepo.syncCategories()
                    }
                }
            }

            //global effects
            is Eff.AddToCart -> {
                rootRepo.addDishToCart(effect.id)
                _notifyChanel.send(
                    Eff.Notification.Action(
                        "${effect.title} успешно добавлен в корзину",
                        label = "Отмена",
                        action = Msg.RemoveFromCart(effect.id, effect.title)
                    )
                )
            }

            is Eff.RemoveFromCart -> {
                rootRepo.removeDishFromCart(effect.id)
                _notifyChanel.send(Eff.Notification.Text("${effect.title} удален из корзины"))
            }

            is Eff.ToggleLike -> {
                if (effect.isFavorite) rootRepo.insertFavorite(effect.id)
                else rootRepo.removeFavorite(effect.id)
            }

            //core effects
            is Eff.Nav -> commit(Msg.Navigate(effect.navCmd))
            is Eff.Cmd -> _cmdChanel.send(effect.command)
            is Eff.Notification -> _notifyChanel.send(effect)
            is Eff.Terminate -> when (effect.route) {
                HomeFeature.route -> homeHandler.cancelJob()
                DishesFeature.route -> dishesHandler.cancelJob()
                DishFeature.route -> dishHandler.cancelJob()
                CartFeature.route -> cartHandler.cancelJob()
                MenuFeature.route -> menuHandler.cancelJob()
                FavoriteFeature.route -> favoriteHandler.cancelJob()
            }
        }
    }

    override var localJob: Job = SupervisorJob()
}