package ru.skillbranch.sbdelivery.screens.root.logic

import android.util.Log
import ru.skillbranch.sbdelivery.screens.cart.logic.CartFeature
import ru.skillbranch.sbdelivery.screens.dish.logic.DishFeature
import ru.skillbranch.sbdelivery.screens.dishes.logic.DishesFeature
import ru.skillbranch.sbdelivery.screens.favorites.logic.FavoriteFeature
import ru.skillbranch.sbdelivery.screens.home.logic.HomeFeature
import ru.skillbranch.sbdelivery.screens.menu.logic.MenuFeature
import java.lang.IllegalStateException

fun RootState.reduceNavigate(msg: NavCmd): Pair<RootState, Set<Eff>> {
    // todo: убрать временный костыль
    if(msg is NavCmd.To && (msg.route == "profile" || msg.route == "order"
                || msg.route == "notifications")) {
        return this to setOf(Eff.Notification.Text("Not yet implemented (${msg.route})"))
    }
    // поскольку мы уходим с текущего экрана на другой (у нас навигационный мессидж),
    // то к сету эффектов надо будет добавить эффект терминации корутин,
    // выполняющихся на экране в момент, когда мы уходим с него. Они будут прерваны
    val navEff = Eff.Terminate(currentRoute)

    val pair = when (msg) {
        // Была нажата либо кнопка "назад" в тулбаре (рут-экран не имеет этой кнопки),
        // либо системная кнопка back key девайса
        is NavCmd.Back -> {
            // Выбрасываем из бэкстека стейт текущего экрана,
            // ИЗ которого мы переходим на новый
            val newBackstack = backstack.dropLast(1)
            // Берем из бэкстека верхний стейт экрана (на который нам надо перейти)
            val newScreenState: ScreenState? = backstack.lastOrNull()
            // Если бэкстек оказался пустым, значит мы были в рут-экране
            // и значит выходим из проги. Посылаем эффект на закрытие RootActivity
            if (newScreenState == null) this to setOf(Eff.Cmd(Command.Finish))
            else {
                // Создаем мутабельную мапу из свойства screens (Map<String, ScreenState>)
                // класса RootState. Элементы мапы представлют из себя название экрана [ключ]
                // и стейт экрана [значение].
                // sealed-классу ScreenState(val route: String, val title: String)
                // наследуют несколько классов ScreenState.Dishes, ScreenState.Cart и пр.
                // Каждый из них имеет дополнительное state-свойство своего типа. Например:
                // Dish(val dishState: DishesFeature.State...) : ScreenState(...)
                // При этом DishFeature имеет константное поле route = "dish" и
                // экземпляр ScreenState.Dish имеет свойство route = "dish". Аналогично
                // для прочих экранов
                val newScreens = screens.toMutableMap()
                    // operator fun <K, V> MutableMap<K, V>.set(key: K, value: V)
                    // Allows to use the index operator for storing values in a mutable map
                    // Более короткая запись для оператора set -> mutableMap[key] = value
                    // Обновляем значение элемента мутабельной мапы по ключу этого элемента
                    .also { screens -> screens[newScreenState.route] = newScreenState }
                // Создаем новый экземпляр RootState с новыми свойствами (неуказанные
                // свойства останутся с прежними значениями) и формируем итоговую
                // пару для возврата из функции reduceNavigate
                copy(
                    screens = newScreens,
                    backstack = newBackstack,
                    currentRoute = newScreenState.route
                ) to newScreenState.initialEffects()
            }
            // После того как пара <новый стейт/эффекты> будет возвращена из reduceNavigate
            // она прокинется из reduceDispatcher в функцию scan на потоке mutations
        }

        NavCmd.ToCart -> {
            val newState = screenStateFactory<ScreenState.Cart>(CartFeature.route) {
                copy(cartState = CartFeature.initialState())
            }
            newState to newState.currentScrSt.initialEffects()
        }

        is NavCmd.ToCategory -> {
            val newState = screenStateFactory<ScreenState.Dishes>(DishesFeature.route) {
                copy(dishesState = DishesFeature.initialState(title = msg.title, category = msg.id))
            }
            val newEffs = DishesFeature.initialEffects(msg.id).mapTo(HashSet(), Eff::Dishes)
            newState to newEffs
        }

        is NavCmd.ToDishItem -> {
            val newState = screenStateFactory<ScreenState.Dish>(DishFeature.route) {
                copy(dishState = DishFeature.initialState(id = msg.id, title = msg.title))
            }
            newState to newState.currentScrSt.initialEffects()
        }

        is NavCmd.To -> {
            // Текущий экран (с которого уходим) добавляем в бэкстэк
            val newBackstack = backstack.plus(currentScrSt)
            // Получаем промежуточный стейт в неспецифичной манере (однообразной для всех стейтов)
            val transitionalState = copy(currentRoute = msg.route, backstack = newBackstack)
            // Обновляем промежуточный стейт в специфичном для каждого конкретного стейта блоке кода
            val newState = when (msg.route) {

                HomeFeature.route -> {
                    transitionalState.updateCurrentScreenState<ScreenState.Home> {
                        copy(homeState = HomeFeature.initialState())
                    }
                }

                MenuFeature.route -> {
                    transitionalState.updateCurrentScreenState<ScreenState.Menu> {
                        copy(menuState = MenuFeature.initialState())
                    }
                }

                FavoriteFeature.route -> {
                    transitionalState.updateCurrentScreenState<ScreenState.Favorites> {
                        copy(dishesState = FavoriteFeature.initialState())
                    }
                }

                CartFeature.route -> {
                    transitionalState.updateCurrentScreenState<ScreenState.Cart> {
                        copy(cartState = CartFeature.initialState())
                    }
                }

                else -> throw IllegalStateException("Not found navigation for route ${msg.route}")
            }
            newState to newState.currentScrSt.initialEffects()
        }
    // Прежде чем вернуть пару из reduceNavigate() докидываем
    // в сет эффектов (во второй элемент пары) эффект-терминатор navEff
    }.run { first to second.plus(navEff)}
    return pair
}

fun <T : ScreenState> RootState.screenStateFactory(route: String, block: T.() -> T): RootState {
    // Текущий экран (с которого уходим) добавляем в бэкстэк
    val newBackstack = backstack.plus(currentScrSt)
    // Получаем промежуточный стейт в неспецифичной манере (однообразной для всех стейтов)
    val transitionalState = copy(currentRoute = route, backstack = newBackstack)
    // Обновляем промежуточный стейт в специфичном для каждого конкретного стейта блоке кода
    return transitionalState.updateCurrentScreenState(block)
}