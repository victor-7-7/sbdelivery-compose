package ru.skillbranch.sbdelivery.screens.dish.logic

import android.util.Log
import ru.skillbranch.sbdelivery.screens.dish.data.DishUiState
import ru.skillbranch.sbdelivery.screens.dish.data.ReviewUiState
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState

fun Set<DishFeature.Eff>.toEffs(): Set<Eff> = mapTo(HashSet(), Eff::Dish)

fun DishFeature.State.reduce(msg: DishFeature.Msg, root: RootState): Pair<RootState, Set<Eff>> {
    val (dishState, effs) = selfReduce(msg)
    // Блок copy(dishState = dishState) будет выполнен на экземпляре ScreenState.Dish,
    // который имеет свойство dishState типа DishFeature.State
    return root.updateCurrentScreenState<ScreenState.Dish> { copy(dishState = dishState) } to effs
}

fun DishFeature.State.selfReduce(msg: DishFeature.Msg): Pair<DishFeature.State, Set<Eff>> {
    val pair = when (msg) {
        is DishFeature.Msg.ShowDish -> copy(content = DishUiState.Value(msg.dish)) to emptySet()

        is DishFeature.Msg.ShowReviews -> {
            if (msg.reviews.isNotEmpty())
                copy(reviews = ReviewUiState.Value(msg.reviews)) to emptySet()
            else copy(reviews = ReviewUiState.Empty) to emptySet()
        }

        is DishFeature.Msg.IncrementCount -> copy(count = count.inc()) to emptySet()
        is DishFeature.Msg.DecrementCount -> {
            if (count <= 1) this to emptySet() else copy(count = count.dec()) to emptySet()
        }

        // Здесь msg.count - количество штук блюда, добавляемых в корзину за раз. При этом мы
        // в стейте сбрасываем свойство count (цифру в степпере) к 1
        is DishFeature.Msg.AddToCart -> copy(count = 1) to setOf(
            DishFeature.Eff.AddToCart(msg.id, msg.count)
        ).toEffs()

        is DishFeature.Msg.ShowReviewDialog -> copy(isReviewDialog = true) to emptySet()
        is DishFeature.Msg.HideReviewDialog -> copy(isReviewDialog = false) to emptySet()

        is DishFeature.Msg.SendReview -> {
            val currReviews = if (reviews is ReviewUiState.Value)
                reviews.reviewList else emptyList()

            copy(
                isReviewDialog = false,
                reviews = ReviewUiState.ValueWithLoading(currReviews)
            ) to setOf(DishFeature.Eff.SendReview(
                dishId = msg.dishId, stars = msg.stars, review = msg.review,
                currReviews = currReviews.toMutableList())
            ).toEffs()
        }
    }
    return pair
}