package ru.skillbranch.sbdelivery.screens.dishes.ui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import ru.skillbranch.sbdelivery.R
import ru.skillbranch.sbdelivery.screens.components.LazyGrid
import ru.skillbranch.sbdelivery.screens.components.items.ProductItem
import ru.skillbranch.sbdelivery.screens.dishes.data.DishesUiState
import ru.skillbranch.sbdelivery.screens.dishes.logic.DishesState
import ru.skillbranch.sbdelivery.screens.root.logic.Msg

@ExperimentalCoilApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun DishesScreen(state: DishesState, accept: (Msg) -> Unit) {

    when (state.uiState) {
        is DishesUiState.Value -> LazyGrid(items = state.uiState.dishes) { dish ->
            ProductItem(
                dish = dish,
                onToggleLike = { accept(Msg.ToggleLike(dish.id, !dish.isFavorite)) },
                onAddToCart = { accept(Msg.AddToCart(dish.id, dish.title)) },
                onClick = { accept(Msg.ClickDish(dish.id, dish.title)) },
            )
        }

        is DishesUiState.Loading -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(color = MaterialTheme.colors.secondary)
        }

        is DishesUiState.Empty -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_ufo_not_found),
                    contentDescription = "Empty Icon",
                    modifier = Modifier.requiredSize(200.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "Ничего не найдено :(", style = MaterialTheme.typography.body1)
            }
        }

        DishesUiState.Error -> {
            TODO()
        }
    }
}


