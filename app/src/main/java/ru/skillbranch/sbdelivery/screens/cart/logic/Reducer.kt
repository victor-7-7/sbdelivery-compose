package ru.skillbranch.sbdelivery.screens.cart.logic

import android.util.Log
import ru.skillbranch.sbdelivery.screens.cart.data.CartUiState
import ru.skillbranch.sbdelivery.screens.cart.data.ConfirmDialogState
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.NavCmd
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState

fun Set<CartFeature.Eff>.toEffs(): Set<Eff> = mapTo(HashSet(), Eff::Cart)

fun CartFeature.State.reduce(msg: CartFeature.Msg, root: RootState): Pair<RootState, Set<Eff>> {
    val (cartState, effs) = selfReduce(msg)
    // Блок copy(cartState = cartState) будет выполнен на экземпляре ScreenState.Cart,
    // который имеет свойство cartState типа CartFeature.State

    return root.updateCurrentScreenState<ScreenState.Cart> { copy(cartState = cartState) } to effs
}

fun CartFeature.State.selfReduce(msg: CartFeature.Msg): Pair<CartFeature.State, Set<Eff>> {
    val pair = when (msg) {
        is CartFeature.Msg.DecrementCount -> this to setOf(CartFeature.Eff.DecrementItem(msg.dishId)).toEffs()
        is CartFeature.Msg.IncrementCount -> this to setOf(CartFeature.Eff.IncrementItem(msg.dishId)).toEffs()
        is CartFeature.Msg.RemoveFromCart -> copy(confirmDialog = ConfirmDialogState.Hide) to setOf(
            CartFeature.Eff.RemoveItem(msg.dishId)
        ).toEffs()
        is CartFeature.Msg.ShowConfirm -> copy(
            confirmDialog = ConfirmDialogState.Show(msg.id, msg.title)
        ) to emptySet()
        is CartFeature.Msg.HideConfirm -> copy(confirmDialog = ConfirmDialogState.Hide) to emptySet()
        is CartFeature.Msg.SendOrder -> copy() to setOf(CartFeature.Eff.SendOrder(msg.order)).toEffs()

        is CartFeature.Msg.ClickOnDish -> {
            this to setOf(Eff.Nav(NavCmd.ToDishItem(msg.dishId, msg.title)))
        }

        is CartFeature.Msg.ShowCart -> {
            val res: Pair<CartFeature.State, Set<Eff>> =
                if (msg.cart.isEmpty()) copy(uiState = CartUiState.Empty) to emptySet()
                else copy(uiState = CartUiState.Value(msg.cart)) to emptySet()
            res
        }
    }
    return pair
}

