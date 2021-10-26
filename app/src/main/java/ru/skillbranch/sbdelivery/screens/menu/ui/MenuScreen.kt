package ru.skillbranch.sbdelivery.screens.menu.ui

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import ru.skillbranch.sbdelivery.screens.components.LazyGrid
import ru.skillbranch.sbdelivery.screens.menu.logic.MenuFeature
import ru.skillbranch.sbdelivery.screens.root.logic.Msg

@ExperimentalCoilApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun MenuScreen(state: MenuFeature.State, accept: (Msg) -> Unit) {
    // Video-2 t.c. 01:50:00.
    val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher

    val backCallback = remember {
        object : OnBackPressedCallback(false) {
            //default not handle back pressed
            override fun handleOnBackPressed() {
                Log.e("XXX", "MenuScreen. Press back")
                // Video-2 t.c. 01:50:40.
                MenuFeature.Msg.PopCategory
                    .let(Msg::Menu)
                    .also(accept)
            }
        }
    }
    // Video-2 t.c. 01:52:30.
    // Schedule effect to run when the current composition completes
    // successfully and applies changes.
    //call if RenderFunction args changed
    SideEffect {
        Log.e("XXX", "MenuScreen. Side effect")
        // Нам локальный колбэк нужен, только когда мы на экране с дочерними категориями
        //if not top level menu -> add handle back pressed
        backCallback.isEnabled = state.parent != null
    }

    // A side effect of composition that must run for any new unique value
    // of key1 and must be reversed or cleaned up if key1 changes or
    // if the DisposableEffect leaves the composition
    DisposableEffect(key1 = dispatcher){ // first launch after compose RenderFunction
        Log.e("XXX", "MenuScreen. Add back callback")
        dispatcher.addCallback(backCallback)
        // Video-2 t.c. 01:51:00.
        onDispose {  // call if RenderFunction remove or key changed
            Log.e("XXX", "MenuScreen. Remove back callback")
            backCallback.remove()
        }
    }

    LazyGrid(
        items = state.current,
        /*itemsInRow = 3,*/
        cellsPadding = 16.dp
    ) { category ->
        MenuItem(item = category, onClick = {
            MenuFeature.Msg.ClickCategory(category.id, category.title)
                .let(Msg::Menu)
                .also(accept)
        })
    }
}




