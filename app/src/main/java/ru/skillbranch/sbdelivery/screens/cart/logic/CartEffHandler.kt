package ru.skillbranch.sbdelivery.screens.cart.logic

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import ru.skillbranch.sbdelivery.repository.CartRepository
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.IEffectHandler
import ru.skillbranch.sbdelivery.screens.root.logic.Msg
import javax.inject.Inject
import kotlin.coroutines.coroutineContext


class CartEffHandler @Inject constructor(
    private val cartRepo: CartRepository,
    private val notifyChanel: Channel<Eff.Notification>,
    override var localJob: Job
) : IEffectHandler<CartFeature.Eff, Msg> {

    private val errHandler = CoroutineExceptionHandler{_, t ->
        t.printStackTrace()
        t.message?.let { notifyChanel.trySend(Eff.Notification.Error(it)) }
    }

    @ExperimentalCoroutinesApi
    override suspend fun handle(effect: CartFeature.Eff, commit: (Msg) -> Unit) {
        CoroutineScope(coroutineContext + localJob + errHandler).launch {

            when (effect) {
                is CartFeature.Eff.LoadCart -> {
                    // Как было, когда loadItems() возвращал List<CartItem>
//                    val cart = repository.loadItems()         //suspend load items
//                    val msg = CartFeature.Msg.ShowCart(cart)  //items transform to CartFeature.Msg
//                    val rootMsg  = Msg.Cart(msg)              //transform local msg to Msg.Cart - root msg
//                    commit(rootMsg)                           //commit state changes

                    // Как стало, когда loadItems() возвращает Flow<List<CartItem>>
                    cartRepo.loadItems()                      //load flow
                        // Синтаксический сахар -> :: вызывает конструктор класса ShowCart
                        // https://kotlinlang.org/docs/keyword-reference.html#operators-and-special-symbols
                        // :: creates a member reference or a class reference
                        .map(CartFeature.Msg::ShowCart)         //items transform to CartFeature.Msg
                        .map (Msg::Cart)                        //transform local msg to Msg.Cart - root msg
                        .collect { commit(it) }                 //commit state changes
                }

                is CartFeature.Eff.DecrementItem -> cartRepo.decrementItem(effect.dishId)
                is CartFeature.Eff.IncrementItem -> cartRepo.incrementItem(effect.dishId)
                is CartFeature.Eff.RemoveItem -> cartRepo.removeItem(effect.dishId)
                is CartFeature.Eff.SendOrder -> {
                    cartRepo.clearCart()
                    notifyChanel.send(Eff.Notification.Text("Заказ оформлен"))
                }
            }
        }
    }
}