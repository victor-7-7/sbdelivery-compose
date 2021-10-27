package ru.skillbranch.sbdelivery.screens.dish.logic

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import ru.skillbranch.sbdelivery.screens.root.logic.Msg
import ru.skillbranch.sbdelivery.repository.DishRepository
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.IEffectHandler
import javax.inject.Inject
import kotlin.coroutines.coroutineContext


class DishEffHandler @Inject constructor(
    private val dishRepo: DishRepository,
    private val notifyChanel: Channel<Eff.Notification>,
    override var localJob: Job
) : IEffectHandler<DishFeature.Eff, Msg> {

    private val errHandler = CoroutineExceptionHandler{_, t ->
        t.printStackTrace()
        t.message?.let { notifyChanel.trySend(Eff.Notification.Error(it)) }
    }

    @ExperimentalCoroutinesApi
    override suspend fun handle(effect: DishFeature.Eff, commit: (Msg) -> Unit) {

        CoroutineScope(coroutineContext + localJob + errHandler).launch {
            when (effect) {
                is DishFeature.Eff.LoadDish -> {
                    dishRepo.findDish(effect.dishId)
                        .map(DishFeature.Msg::ShowDish)
                        .map(Msg::Dish)
                        .collect { commit(it) }
                }

                is DishFeature.Eff.AddToCart -> {
                    dishRepo.addToCart(effect.dishId, effect.count)
                    dishRepo.cartCount()
                        .let(Msg::UpdateCartCount)
                        .also(commit)
                    notifyChanel.send(Eff.Notification.Text("В корзину добавлено ${effect.count} товаров"))
                }

                is DishFeature.Eff.LoadReviews -> {
                    dishRepo.loadReviews(effect.dishId)
                        .let(DishFeature.Msg::ShowReviews)
                        .let(Msg::Dish)
                        .also(commit)
                }

                is DishFeature.Eff.SendReview -> {
                    val reviewResp = dishRepo.sendReview(effect.dishId, effect.stars, effect.review)
                    //------------------------
                    // Вариант 1. Один запрос к серверу. Сервер возвращает данные
                    // по отзыву, записанному на сервере. Мы сами добавляем этот
                    // отзыв к списку уже имеющихся на экране (список был скачан с
                    // сервера при открытии экрана блюда)
                    val reviews = effect.currReviews
                    reviews.add(reviewResp)
                    reviews
                        .let(DishFeature.Msg::ShowReviews)
                        .let(Msg::Dish)
                        .also(commit)
                    //------------------------
                    // Вариант 2. Два запроса к серверу. Сначала отправляем отзыв на сервер.
                    // Затем скачиваем с сервера обновленный там список отзывов.
                    /*dishRepo.loadReviews(effect.dishId)
                        .let(DishFeature.Msg::ShowReviews)
                        .let(Msg::Dish)
                        .also(commit)*/
                    //------------------------
                    // Вариант 3. Один запрос к серверу. Для этого сервер должен
                    // вернуть не reviewResp, а уже обновленный список всех отзывов
                    // на данное блюдо. Для этого должно быть изменено серверное REST API
                    //------------------------
                    notifyChanel.send(Eff.Notification.Text("Отзыв успешно отправлен"))
                }
            }
        }
    }
}



