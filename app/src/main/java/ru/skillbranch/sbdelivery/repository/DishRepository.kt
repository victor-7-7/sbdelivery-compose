package ru.skillbranch.sbdelivery.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.skillbranch.sbdelivery.data.db.dao.CartDao
import ru.skillbranch.sbdelivery.data.db.dao.DishesDao
import ru.skillbranch.sbdelivery.data.db.entity.CartItemPersist
import ru.skillbranch.sbdelivery.data.db.entity.DishLikedPersist
import ru.skillbranch.sbdelivery.data.network.RestService
import ru.skillbranch.sbdelivery.data.network.req.ReviewReq
import ru.skillbranch.sbdelivery.data.network.res.ReviewRes
import ru.skillbranch.sbdelivery.data.toDish
import ru.skillbranch.sbdelivery.domain.Dish
import java.util.*
import javax.inject.Inject

interface IDishRepository {
    suspend fun findDish(id: String): Flow<Dish>
    suspend fun addToCart(id: String, count: Int)
    suspend fun cartCount(): Int
    suspend fun loadReviews(dishId: String): List<ReviewRes>
    suspend fun sendReview(id: String, rating: Int, review: String): ReviewRes
    suspend fun insertFavorite(id: String)
    suspend fun removeFavorite(id: String)
}


class DishRepository @Inject constructor(
    private val api: RestService,
    private val dishesDao: DishesDao,
    private val cartDao: CartDao,
) : IDishRepository {

    override suspend fun findDish(id: String): Flow<Dish> = dishesDao.findDish(id)
        .map { it.toDish() }

    // Здесь count - количество штук блюда, добавляемых в корзину за раз
    override suspend fun addToCart(id: String, count: Int) {
        val dishCount = cartDao.dishCount(id) ?: 0
        // Если в корзине уже есть такое блюдо, то просто увеличиваем
        // на count количество штук этого блюда в корзине
        if (dishCount > 0) cartDao.updateItemCount(id, dishCount + count)
        // Если в корзине такого блюда еще не было, то добавляем его
        // с указанием количества добавки
        else cartDao.addItem(CartItemPersist(dishId = id, count = count))
    }

    override suspend fun cartCount()= cartDao.cartCount() ?: 0

    override suspend fun loadReviews(dishId: String): List<ReviewRes> {
        val reviews = mutableListOf<ReviewRes>()
        var offset = 0
        while (true) {
            val res = api.getReviews(dishId, offset * 10, 10)
            if (res.isSuccessful) {
                offset++
                reviews.addAll(res.body()!!)
            } else break
        }
        return if(reviews.isEmpty()) reviewsStub() else reviews
    }

    override suspend fun sendReview(id: String, rating: Int, review: String): ReviewRes {
        return try {
            // Третий параметр - token - следует рефрешить, но это не реализовано
            val reviewResp = api.sendReview(id, ReviewReq(rating, text = review))
            Log.e("XXX", "DishRepository.sendReview(). ReviewRes: $reviewResp")
            reviewResp
        } catch (e: Exception) {
            Log.e("XXX", "DishRepository.sendReview(). Exception: $e")
            // todo: убрать временный костыль
            ReviewRes("stubName", Date().time, rating, review)
        }
    }

    override suspend fun insertFavorite(id: String) = dishesDao.addToFavorite(DishLikedPersist(id))

    override suspend fun removeFavorite(id: String) = dishesDao.removeFromFavorite(id)

    private fun reviewsStub(): List<ReviewRes> {
        val cal = Calendar.getInstance()
        cal.set(2021, 8, 10)
        return listOf(
            ReviewRes("Глеб", cal.timeInMillis, 4, "Понравилось"),
            ReviewRes("Алина", cal.timeInMillis - 5 * 60 * 60 * 1000, 1, "Не вкусно"),
            ReviewRes("Иван", cal.timeInMillis + 2 * 60 * 60 * 1000, 3, "Что-то среднее")
        )
//        return listOf()
    }
}