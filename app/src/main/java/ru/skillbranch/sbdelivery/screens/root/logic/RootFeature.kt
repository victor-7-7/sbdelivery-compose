package ru.skillbranch.sbdelivery.screens.root.logic

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.skillbranch.sbdelivery.domain.User
import ru.skillbranch.sbdelivery.screens.cart.logic.CartFeature
import ru.skillbranch.sbdelivery.screens.cart.logic.reduce
import ru.skillbranch.sbdelivery.screens.dish.logic.DishFeature
import ru.skillbranch.sbdelivery.screens.dish.logic.reduce
import ru.skillbranch.sbdelivery.screens.dishes.logic.DishesFeature
import ru.skillbranch.sbdelivery.screens.dishes.logic.DishesMsg
import ru.skillbranch.sbdelivery.screens.dishes.logic.DishesState
import ru.skillbranch.sbdelivery.screens.dishes.logic.reduceCategory
import ru.skillbranch.sbdelivery.screens.favorites.logic.FavoriteFeature
import ru.skillbranch.sbdelivery.screens.favorites.logic.reduceFavorite
import ru.skillbranch.sbdelivery.screens.home.logic.HomeFeature
import ru.skillbranch.sbdelivery.screens.home.logic.reduce
import ru.skillbranch.sbdelivery.screens.menu.logic.MenuFeature
import ru.skillbranch.sbdelivery.screens.menu.logic.reduce
import java.io.Serializable


@FlowPreview
class RootFeature(private val initState: RootState? = null) {

    // Функция вызывается только при запуске приложения в двух местах.
    // 1. При инициализации _state. 2. При эмите первого мессиджа в поток мутаций
    private fun initialState(): RootState = initState ?:
    RootState(
        screens = mapOf(
            HomeFeature.route to ScreenState.Home(),
            DishesFeature.route to ScreenState.Dishes(),
            FavoriteFeature.route to ScreenState.Favorites(),
            DishFeature.route to ScreenState.Dish(),
            CartFeature.route to ScreenState.Cart(),
            MenuFeature.route to ScreenState.Menu(),
        ),
        currentRoute = HomeFeature.route
    )

    private fun initialEffects(): Set<Eff> =
        setOf(Eff.SyncEntity, Eff.SyncCounter) + HomeFeature.initialEffects()
            .mapTo(HashSet(), Eff::Home)

    // Это будет скоуп вьюмодели RootViewModel
    private lateinit var _scope: CoroutineScope

    // StateFlow is a SharedFlow that represents a read-only state with a single
    // updatable data value that emits updates to the value to its collectors.
    // A state flow is a hot flow because its active instance exists independently
    // of the presence of collectors. Its current value can be retrieved via
    // the value property
    /** Горячий разделяемый поток стейтов RootState */
    private val _state: MutableStateFlow<RootState> = MutableStateFlow(initialState())
    // Свойство, доступное снаружи, должно быть read-only. К этому стейт флоу
    // в RootScreen приколлекчены два(?) композбла - ContentHost и AppbarHost
    /** read-only горячий разделяемый поток стейтов RootState */
    val state
        get() = _state.asStateFlow()

    // SharedFlow is a hot Flow that shares emitted values among all its collectors
    // in a broadcast fashion, so that all collectors get all emitted values.
    // A shared flow is called hot because its active instance exists
    // independently of the presence of collectors. This is opposed to a
    // regular Flow which is cold and is started separately for each collector
    /** Горячий разделяемый поток сообщений Msg */
    private val mutations: MutableSharedFlow<Msg> = MutableSharedFlow()

    /** Сердцевинная функция, на которую завязана вся интерактивность приложения.
     * Она во вьюмодельном корутинном скоупе эмитит очередной мессидж в горячий
     * разделяемый поток мутаций. Эта функция вызывается в хэндлерах эффектов
     * под именем commit, а в композблах ContentHost и AppbarHost она вызывается
     * под именем accept (композблы дотягиваются до нее через вьюмодель, а ее
     * в RootScreen передает RootActivity при своей инициализации) */
    fun mutate(mutation: Msg) {
        Log.w("XXX", "RootFeature.mutate() [MUTATION: $mutation]")
        _scope.launch {
            mutations.emit(mutation)
        }
    }

    /** Вьюмодель при своей инициализации вызывает функцию listen() и последняя собирает
     * реактивную систему. Сначала каждый мессидж потока мутаций трансформируется
     * (через reduceDispatcher) в пару из рутстейта и набора эффектов, затем пара попадает
     * в подсоединенный к потоку коллектор. В нем рутстейт из пары эмитится в горячий
     * разделяемый поток стейтов (на поток стейтов реагируют в RootScreen два композбла -
     * ContentHost и AppbarHost). Затем каждый эффект из набора передается в диспетчер
     * эффектов для дальнейшей обработки. Вторым параметром в диспетчер передается (под
     * именем commit) ссылка на функцию mutate(msg) */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun listen(scope: CoroutineScope, effDispatcher: EffDispatcher, initState: RootState?) {
        Log.e("XXX", "RootFeature. Start listen. Init state: $initState")
        _scope = scope
        _scope.launch {
            mutations.onEach { Log.e("XXX", "MUTATION $it") }
                // fun <T, R> Flow<T>.scan(initial: R, operation: suspend (R, T) -> R): Flow<R>
                // Folds the given flow with operation, emitting every intermediate result,
                // including initial value.
                // flowOf(1, 2, 3).scan(emptyList<Int>()) { acc, value -> acc + value }
                // will produce [], [1], [1, 2], [1, 2, 3]
                // При запуске приложения initState = null. Начальная пара для свертки:
                // RootState (мапа из нескольких экранов, текущий рут "home"...) и сет из
                // нескольких эффектов - SyncEntity, SyncCounter, SyncRecommended, FindBest, FindPopular.
                // Эта начальная пара будет проэмичена в коллектор. Затем в коллектор
                // будут эмитится пары, являющиеся результатом свертки (в функции reduceDispatcher)
                // текущей (проэмиченой до того) пары (стейт/эффект) и очередного мессиджа
                // с возвратом из свертки новой пары - стейт/эффект
                .scan(
                    (initState ?: initialState()) to initialEffects()
                ) { (s, _), m ->
                    Log.e("XXX", "Before reduceDispatcher: s => $s\nm => $m")
                    reduceDispatcher(s, m)
                }
                .collect { (s, effs) ->
                    // Эмитим очередной стейт, чтобы подписчики на поток стейтов среагировали
                    _state.emit(s)
                    // Разбираемся с очередным набором эффектов (в отдельных корутинах)
                    effs.forEach { eff ->
                        launch {
                            Log.e("XXX", "Before effDispatcher.handle: eff => $eff")
                            // Каждый эффект (из очередного набора) передаем в рутовый
                            // диспетчер в его метод handle вместе с блоком mutate
                            // (этот блок в отдельной корутине эмитит соответствующий
                            // мессидж в поток мутаций)
                            effDispatcher.handle(eff, ::mutate)
                        }
                    }
                }
        }
    }

    private fun reduceDispatcher(root: RootState, msg: Msg): Pair<RootState, Set<Eff>> {
        return when {
            msg is Msg.Navigate -> root.reduceNavigate(msg.navCmd)

            msg is Msg.UpdateCartCount -> root.copy(cartCount = msg.count) to emptySet()
            msg is Msg.ToggleLike -> root to setOf(Eff.ToggleLike(msg.id, msg.isFavorite))
            msg is Msg.AddToCart -> root to setOf(Eff.AddToCart(msg.id, msg.title))
            msg is Msg.RemoveFromCart -> root to setOf(Eff.RemoveFromCart(msg.id, msg.title))
            msg is Msg.ClickDish -> root to setOf(Eff.Nav(NavCmd.ToDishItem(msg.id, msg.title)))
            msg is Msg.ShowAbout -> root.copy(isAbout = true) to emptySet()
            msg is Msg.HideAbout -> root.copy(isAbout = false) to emptySet()

            // Если мессидж ДЛЯ экрана блюд И текущий экран - это экран блюд, то
            // тогда делаем свертку, иначе свертку делать нет смысла. И кроме того,
            // если это уже другой экран, то видимо (за редким исключением) надо
            // прервать выполняющиеся для экрана блюд корутины
            msg is Msg.Dishes && root.currentScrSt is ScreenState.Dishes ->
                root.currentScrSt.dishesState.reduceCategory(msg.dishesMsg, root)

            msg is Msg.Dishes && root.currentScrSt is ScreenState.Favorites ->
                root.currentScrSt.dishesState.reduceFavorite(msg.dishesMsg, root)

            msg is Msg.Dish && root.currentScrSt is ScreenState.Dish ->
                root.currentScrSt.dishState.reduce(msg.dishMsg, root)

            msg is Msg.Cart && root.currentScrSt is ScreenState.Cart ->
                root.currentScrSt.cartState.reduce(msg.cartMsg, root)

            msg is Msg.Home && root.currentScrSt is ScreenState.Home ->
                root.currentScrSt.homeState.reduce(msg.homeMsg, root)

            msg is Msg.Menu && root.currentScrSt is ScreenState.Menu ->
                root.currentScrSt.menuState.reduce(msg.msg, root)

            else -> root to emptySet()
        }
    }
}


data class RootState(
    val screens: Map<String, ScreenState>,
    val currentRoute: String,
    val backstack: List<ScreenState> = emptyList(),
    val cartCount: Int = 0,
    val notificationCount: Int = 0,
    val user: User? = User("Сидоров Иван", "sidorov.ivan@mail.ru") /*null*/,
    val isAbout: Boolean = false
) : Serializable {
    val currentScrSt: ScreenState =
        // Throws an IllegalStateException with the result of calling lazyMessage
        // if the value is null. Otherwise returns the not null value
        checkNotNull(screens[currentRoute]) { "check route $currentRoute or screens $screens" }

    /** Создает и возвращает новый рутстейт, в котором изменены:
     * 1. В map-свойстве screens -> скринстейт-значение для ключа текущего экрана
     * 2. Свойство currentScrSt рутстейта -> новое значение скринстейта,
     * полученное из блока (block: T.() -> T). Свойства currentRoute, backstack,
     * cartCount, notificationCount, user нового рутстейта будут с
     * такими же значениями, как в исходном рутстейте */
    fun <T : ScreenState> updateCurrentScreenState(block: T.() -> T): RootState {
        // На текущем СКРИНстейт-свойстве РУТстейта вызываем код блока,
        // в котором получаем экземпляр нового СКРИНстейта
        @Suppress("UNCHECKED_CAST")
        val newScreenState = (currentScrSt as? T)?.block()
        // Содаем новую мапу, в которой
        val newScreens = if (newScreenState != null)
            screens.toMutableMap().also { mutableScreens ->
                // у пары с ключом текущего рута будет новое значение скринстейта
                mutableScreens[currentRoute] = newScreenState
            } else screens
        // Возвращаем НОВЫЙ экземпляр РУТстейта, у которого будет изменена
        // мапа screens (конкретнее - только пара с ключом текущего рута),
        // а параметры currentRoute, backstack, cartCount, notificationCount, user
        // останутся с прежними значениями. Но в конструкторе этого рутстейта
        // движок будет инициализировать свойство currentScrSt взяв его
        // из мапы screens. Причем он возьмет как раз значение newScreenState,
        // которое мы положили в мапу перед созданием нового рутстейта
        return copy(screens = newScreens)
    }
}

/** Базовый скринстейт имеет только два String-свойства - route (название экрана)
 * и title (для заголовка аппбара). Производные скринстейты имеют у себя дополнительное
 * свойство - стейт соответствующего типа (DishesFeature.State и т.д) */

sealed class ScreenState(
    val route: String,
    val title: String
) : Serializable {
    abstract fun initialEffects(): Set<Eff>

    data class Dish(
        val dishState: DishFeature.State = DishFeature.State()
    ) : ScreenState(DishFeature.route, dishState.title) {
        override fun initialEffects(): Set<Eff> = DishFeature
            .initialEffects(dishState.id)
            .mapTo(HashSet(), Eff::Dish)

    }

    data class Dishes(
        val dishesState: DishesState = DishesState()
    ) : ScreenState(DishesFeature.route, dishesState.title) {
        override fun initialEffects(): Set<Eff> = DishesFeature
            .initialEffects(dishesState.category)
            .mapTo(HashSet(), Eff::Dishes)
    }

    data class Cart(
        val cartState: CartFeature.State = CartFeature.State()
    ) : ScreenState(CartFeature.route, "Корзина") {
        override fun initialEffects(): Set<Eff> = CartFeature
            .initialEffects()
            .mapTo(HashSet(), Eff::Cart)
    }

    data class Home(
        val homeState: HomeFeature.State = HomeFeature.State()
    ) : ScreenState(HomeFeature.route, "Главная") {
        override fun initialEffects(): Set<Eff> = HomeFeature
            .initialEffects()
            .mapTo(HashSet(), Eff::Home)
    }

    data class Menu(
        val menuState: MenuFeature.State = MenuFeature.State()
    ) : ScreenState(MenuFeature.route, "Меню") {
        override fun initialEffects(): Set<Eff> = MenuFeature
            .initialEffects()
            .mapTo(HashSet(), Eff::Menu)
    }

    data class Favorites(
        val dishesState: DishesState = DishesState()
    ) : ScreenState(FavoriteFeature.route, "Избраное") {
        override fun initialEffects(): Set<Eff> = FavoriteFeature
            .initialEffects()
            .mapTo(HashSet(), Eff::Favorite)
    }
}


sealed class Eff {
    data class Cmd(val command: Command) : Eff()
    sealed class Notification(open val message: String) : Eff() {

        data class Text(override val message: String) : Notification(message)
        data class Error(
            override val message: String,
            val label: String? = null,
            val action: Msg? = null
        ) : Notification(message)

        data class Action(
            override val message: String,
            val label: String,
            val action: Msg
        ) : Notification(message)
    }

    data class Dish(val dishEff: DishFeature.Eff) : Eff()
    data class Dishes(val dishesEff: DishesFeature.Eff) : Eff()
    data class Cart(val cartEff: CartFeature.Eff) : Eff()
    data class Home(val homeEff: HomeFeature.Eff) : Eff()
    data class Menu(val menuEff: MenuFeature.Eff) : Eff()
    data class Favorite(val favoriteEff: FavoriteFeature.Eff) : Eff()

    //Navigate
    data class Nav(val navCmd: NavCmd) : Eff()

    //Sync
    object SyncCounter : Eff() // Счетчик числа блюд в корзине юзера
    // Под Entity подразумеваются список блюд и список категорий блюд
    object SyncEntity : Eff()

    //Global effs
    data class ToggleLike(val id: String, val isFavorite: Boolean) : Eff()
    data class AddToCart(val id: String, val title: String) : Eff()
    data class RemoveFromCart(val id: String, val title: String) : Eff()

    //Terminate running coroutines
    // Надо прервать запущенные корутины, потому что их результат
    // уже не понадобится (юзер ушел с текущего экрана)
    data class Terminate(val route: String) : Eff()
}


sealed class Msg {
    object ShowAbout : Msg()
    object HideAbout : Msg()
    data class Dish(val dishMsg: DishFeature.Msg) : Msg()
    data class Dishes(val dishesMsg: DishesMsg) : Msg()
    data class Cart(val cartMsg: CartFeature.Msg) : Msg()
    data class Home(val homeMsg: HomeFeature.Msg) : Msg()
    data class Menu(val msg: MenuFeature.Msg) : Msg()

    /** Рутовый мессидж (востребован на нескольких экранах) */
    data class Navigate(val navCmd: NavCmd) : Msg()
    /** Рутовый мессидж (востребован на нескольких экранах) */
    data class UpdateCartCount(val count: Int) : Msg()
    /** Рутовый мессидж (востребован на нескольких экранах) */
    data class ToggleLike(val id: String, val isFavorite: Boolean) : Msg()
    /** Рутовый мессидж (востребован на нескольких экранах) */
    data class AddToCart(val id: String, val title: String) : Msg()
    /** Рутовый мессидж (востребован на нескольких экранах) */
    data class RemoveFromCart(val id: String, val title: String) : Msg()
    /** Рутовый мессидж (востребован на нескольких экранах) */
    data class ClickDish(val id: String, val title: String) : Msg()
}


sealed class NavCmd {
    data class To(val route: String) : NavCmd()
    object ToCart : NavCmd()
    data class ToDishItem(val id: String, val title: String) : NavCmd()
    data class ToCategory(val id: String, val title: String) : NavCmd()

    object Back : NavCmd()
}


sealed class Command {
    object Finish : Command()
    //Android specific commands Activity::finish(), startForResult, etc
}





