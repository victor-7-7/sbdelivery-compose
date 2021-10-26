package ru.skillbranch.sbdelivery.screens.dish.logic

import ru.skillbranch.sbdelivery.data.network.res.ReviewRes
import ru.skillbranch.sbdelivery.domain.Dish
import ru.skillbranch.sbdelivery.screens.dish.data.DishUiState
import ru.skillbranch.sbdelivery.screens.dish.data.ReviewUiState
import java.io.Serializable


object DishFeature {
    const val route = "dish"

    fun initialState(id: String, title: String): State = State(id, title)
    fun initialEffects(id: String): Set<Eff> = setOf(Eff.LoadDish(id), Eff.LoadReviews(id))

    data class State(
        val id: String = "",
        val title: String = "",
        val isReviewDialog: Boolean = false,
        val reviews: ReviewUiState = ReviewUiState.Loading,
        val content: DishUiState = DishUiState.Loading,
        val count: Int = 1, // Количество этого блюда для добавления в корзину
    ) : Serializable

    sealed class Eff {
        data class LoadDish(val dishId: String) : Eff()
        data class LoadReviews(val dishId: String) : Eff()
        data class AddToCart(val dishId: String, val count: Int) : Eff()
        data class SendReview(val dishId: String, val stars: Int, val review: String,
                              val currReviews: MutableList<ReviewRes> = mutableListOf()) : Eff()
    }

    sealed class Msg {
        object IncrementCount : Msg()
        object DecrementCount : Msg()
        object ShowReviewDialog : Msg()
        object HideReviewDialog : Msg()
        data class SendReview(val dishId: String, val stars: Int, val review: String) : Msg()
        data class ShowDish(val dish: Dish) : Msg()
        data class AddToCart(val id: String, val count: Int) : Msg()
        data class ShowReviews(val reviews: List<ReviewRes>) : Msg()
    }
}



