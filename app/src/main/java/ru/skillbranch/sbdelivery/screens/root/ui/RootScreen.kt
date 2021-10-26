package ru.skillbranch.sbdelivery.screens.root.ui

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.skillbranch.sbdelivery.screens.cart.ui.CartScreen
import ru.skillbranch.sbdelivery.screens.components.AboutDialog
import ru.skillbranch.sbdelivery.screens.components.DefaultToolbar
import ru.skillbranch.sbdelivery.screens.components.DishesToolbar
import ru.skillbranch.sbdelivery.screens.components.NavigationDrawer
import ru.skillbranch.sbdelivery.screens.dish.ui.DishScreen
import ru.skillbranch.sbdelivery.screens.dishes.ui.DishesScreen
import ru.skillbranch.sbdelivery.screens.favorites.ui.FavoriteScreen
import ru.skillbranch.sbdelivery.screens.home.ui.HomeScreen
import ru.skillbranch.sbdelivery.screens.menu.ui.MenuScreen
import ru.skillbranch.sbdelivery.screens.root.RootViewModel
import ru.skillbranch.sbdelivery.screens.root.logic.*
import java.util.*

@ExperimentalCoroutinesApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@FlowPreview
@Composable
fun RootScreen(vm: RootViewModel) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    // Video-2 t.c. 00:43:10
    LaunchedEffect(key1 = Unit) {
        launch {
            vm.notifications
                .collect { notification -> renderNotification(notification, scaffoldState, vm) }
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            AppbarHost(vm, onToggleDrawer = {
                val drawerState = scaffoldState.drawerState
                if (drawerState.isOpen) scope.launch { drawerState.close() }
                else scope.launch { drawerState.open() }
            })
        },
        content = { ContentHost(vm) },
        drawerContent = {
            val state = vm.stateFlow.collectAsState().value
            NavigationDrawer(
                currentRoute = state.currentRoute,
                cartCount = state.cartCount,
                notificationCount = state.notificationCount,
                user = state.user,
                onLogout = {}
            ) { route ->
                if(state.currentRoute == route) return@NavigationDrawer
                if (route == "about") { vm.accept(Msg.ShowAbout) }
                else { vm.navigate(NavCmd.To(route)) }
                scope.launch { scaffoldState.drawerState.close() }
            }
        },
        drawerScrimColor = MaterialTheme.colors.primaryVariant.copy(alpha = DrawerDefaults.ScrimOpacity),
        snackbarHost = { host ->
            SnackbarHost(
                hostState = host,
                snackbar = {
                    Snackbar(
                        backgroundColor = MaterialTheme.colors.onPrimary,
                        action = {
                            TextButton(
                                onClick = { host.currentSnackbarData?.performAction() }
                            ) {
                                Text(
                                    text = host.currentSnackbarData?.actionLabel?.uppercase(Locale.getDefault())
                                        ?: "",
                                    color = MaterialTheme.colors.secondary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        },
                        content = {
                            Text(
                                text = host.currentSnackbarData?.message ?: "",
                                color = MaterialTheme.colors.onBackground
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                })
        },
    )
}

@ExperimentalCoroutinesApi
@FlowPreview
private suspend fun renderNotification(
    notification: Eff.Notification,
    scaffoldState: ScaffoldState,
    vm: RootViewModel
) {
    // В компоузе метод showSnackbar показывает бар и затем возвращает результат
    // SnackbarResult.Dismissed (если истек таймаут или юзер смахнул снэкбар)
    // или SnackbarResult.ActionPerformed (если юзер тапнул кнопку экшн)
    val result = when (notification) {
        is Eff.Notification.Text -> {
            scaffoldState.snackbarHostState.showSnackbar(notification.message)
        }
        is Eff.Notification.Action -> {
            val (message, label) = notification
            scaffoldState.snackbarHostState.showSnackbar(message, label)
        }
        is Eff.Notification.Error -> {
            val (message, label) = notification
            scaffoldState.snackbarHostState.showSnackbar(message, label)
        }
    }

    when (result) {
        SnackbarResult.ActionPerformed -> {
            when (notification) {
                is Eff.Notification.Action -> {
                    vm.accept(notification.action)
                }
                is Eff.Notification.Error -> notification.action?.let(vm::accept)
                else -> { /*  no action needed */ }
            }
        }
        SnackbarResult.Dismissed -> { /* dismissed, no action needed */ }
    }
}

@Composable
fun Navigation(
    currScrSt: ScreenState,
    modifier: Modifier = Modifier,
    content: @Composable (ScreenState) -> Unit
) {
    // Как бы "локальная" переменная функции. На самом деле ее значение
    // будет восстановлено при вызове функции в то значение, которое она
    // имела при предыдущем вызове функции
    val restorableStateHolder = rememberSaveableStateHolder()

    Box(modifier) {
        // SaveableStateProvider ->
        // Put your content associated with a key inside the content.
        // This will automatically save all the states defined with
        // rememberSaveable before disposing the content and will restore
        // the states when you compose with this key again
        restorableStateHolder.SaveableStateProvider(key = currScrSt.route + currScrSt.title) {
            // Построенное в лямбде content-представление Composable, будет
            // закешировано в restorableStateHolder с ключом key при уходе с экрана
            content(currScrSt)
        }
    }
}

@ExperimentalCoroutinesApi
@ExperimentalCoilApi
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@FlowPreview
@Composable
fun ContentHost(vm: RootViewModel) {
    // collectAsState => Collects values from this StateFlow and represents its
    // latest value via State. The StateFlow.value is used as an initial value.
    // Every time there would be new value posted into the StateFlow the returned
    // State will be updated causing recomposition of every State.value usage
    val state: RootState by vm.stateFlow.collectAsState()

    if (state.isAbout) {
        AboutDialog(onDismiss = { vm.accept(Msg.HideAbout) })
        return
    }

    val screenState: ScreenState = state.currentScrSt
    // При возврате на предыдущий скрин, если его стейт не изменился, то фреймворк
    // подставит уже построенный ранее Composable. А если изменилось свойство Х
    // стейта, то фреймворк перерисует только тот компонент лейаута в Composable,
    // на который свойство Х влияет
    Navigation(currScrSt = screenState, modifier = Modifier.fillMaxSize()) { currScrSt ->
        // Здесь формируется @Composable представление контент-хоста. На этот блок кода
        // ссылается параметр content в функции Navigation()
        when (currScrSt) {
            // В зависимости от типа скринстейта выбираем соответствующий композбл и
            // передаем в него соответствующий ему стейт (напр - DishesFeature.State) и
            // лямбду (под именем accept), принимающую мессиджи из соответствующей группы
            // (напр - из DishesFeature.Msg) или рутовые мессиджи. Эта лямбда будет вызвана
            // внутри соотв композбла. Например, в DishesScreen она будет вызвана по клику
            // на блюде так: onClick = { accept(Msg.ClickDish(dish.id, dish.title)) }.
            // Это приведет к тому, что будет вызван метод accept() вьюмодели с
            // передачей в него мессиджа Msg.ClickDish и заработает реактивщина
            is ScreenState.Dishes -> DishesScreen(
                currScrSt.dishesState,
                vm::accept
            )
            is ScreenState.Dish -> DishScreen(
                currScrSt.dishState,
                vm::accept
            )
            is ScreenState.Favorites -> FavoriteScreen(
                currScrSt.dishesState,
                vm::accept
            )
            is ScreenState.Cart -> CartScreen(
                currScrSt.cartState,
                vm::accept
            )
            is ScreenState.Home -> HomeScreen(
                currScrSt.homeState,
                vm::accept
            )

            is ScreenState.Menu -> MenuScreen(
                currScrSt.menuState,
                vm::accept
            )
        }
    }
}


@ExperimentalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@FlowPreview
@Composable
fun AppbarHost(vm: RootViewModel, onToggleDrawer: () -> Unit) {
    // collectAsState => Collects values from this StateFlow and represents its
    // latest value via State. The StateFlow.value is used as an initial value.
    // Every time there would be new value posted into the StateFlow the returned
    // State will be updated causing recomposition of every State.value usage
    val state: RootState by vm.stateFlow.collectAsState()
    when (val currScrSt: ScreenState = state.currentScrSt) {
        is ScreenState.Dishes -> DishesToolbar(
            title = currScrSt.title,
            state = currScrSt.dishesState,
            cartCount = state.cartCount,
            canBack = true,
            accept = { vm.accept(Msg.Dishes(it)) },
            onCart = { vm.navigate(NavCmd.ToCart) },
            onDrawer = onToggleDrawer
        )

        is ScreenState.Favorites -> DishesToolbar(
            title = currScrSt.title,
            state = currScrSt.dishesState,
            canBack = false,
            cartCount = state.cartCount,
            accept = { vm.accept(Msg.Dishes(it)) },
            onCart = { vm.navigate(NavCmd.ToCart) },
            onDrawer = onToggleDrawer
        )

        is ScreenState.Menu -> DefaultToolbar(
            currScrSt.title,
            state.cartCount,
            canBack = currScrSt.menuState.parent != null,
            onCart = { vm.navigate(NavCmd.ToCart) },
            onDrawer = onToggleDrawer
        )

        is ScreenState.Dish -> DefaultToolbar(
            currScrSt.title,
            state.cartCount,
            canBack = true,
            onCart = { vm.navigate(NavCmd.ToCart) },
            onDrawer = onToggleDrawer
        )

        else -> DefaultToolbar(
            currScrSt.title,
            state.cartCount,
            onCart = { vm.navigate(NavCmd.ToCart) },
            onDrawer = onToggleDrawer
        )
    }
}