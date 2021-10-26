package ru.skillbranch.sbdelivery.screens.cart.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import ru.skillbranch.sbdelivery.screens.cart.logic.CartFeature
import ru.skillbranch.sbdelivery.screens.cart.data.CartUiState
import ru.skillbranch.sbdelivery.screens.cart.data.ConfirmDialogState
import ru.skillbranch.sbdelivery.screens.root.logic.Msg

@ExperimentalCoilApi
@Composable
fun CartScreen(state: CartFeature.State, accept: (Msg) -> Unit) {
    when (state.uiState) {
        is CartUiState.Value -> {
            Column {
                LazyColumn(
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = {
                        val items = state.uiState.dishes
                        items(items = items, key = { it.id }) {
                            CartListItem(it,
                                onProductClick = { dishId: String, title: String ->
                                    CartFeature.Msg.ClickOnDish(dishId, title)
                                        .let(Msg::Cart)
                                        .also(accept)
                                },
                                onIncrement = { dishId ->
                                    CartFeature.Msg.IncrementCount(dishId)
                                        .let(Msg::Cart)
                                        .also(accept)
                                },
                                onDecrement = { dishId ->
                                    CartFeature.Msg.DecrementCount(dishId)
                                        .let(Msg::Cart)
                                        .also(accept)
                                },
                                onRemove = { dishId, title ->
                                    // Предлагаем юзеру подтвердить удаление из корзины
                                    CartFeature.Msg.ShowConfirm(dishId, title)
                                        .let(Msg::Cart)
                                        .also(accept)
                                }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row {
                        val total = state.uiState.dishes.sumOf { it.count * it.price }
                        Text(
                            "Итого",
                            fontSize = 24.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "$total Р",
                            fontSize = 24.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colors.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val order = state.uiState.dishes.associate { it.id to it.count }
                            CartFeature.Msg.SendOrder(order)
                                .let(Msg::Cart)
                                .also(accept)
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = MaterialTheme.colors.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Оформить заказ", style = TextStyle(fontWeight = FontWeight.Bold))
                    }
                }
            }

        }

        is CartUiState.Empty -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = "Пока ничего нет")
        }

        is CartUiState.Loading -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(color = MaterialTheme.colors.secondary)
        }
    }

    if (state.confirmDialog is ConfirmDialogState.Show) {
        AlertDialog(
            // onDismissRequest -> Executes when the user tries to dismiss the Dialog
            // by clicking outside or pressing the back button. This is not called
            // when the dismiss button is clicked
            onDismissRequest = {
                CartFeature.Msg.HideConfirm
                    .let(Msg::Cart)
                    .also(accept)
            },
            backgroundColor = Color.White,
            contentColor = MaterialTheme.colors.primary,
            title = { Text(text = "Вы уверены?", color = MaterialTheme.colors.secondary) },
            text = { Text(text = "Вы точно хотите удалить ${state.confirmDialog.title} из корзины") },
            buttons = {
                Row {
                    TextButton(
                        onClick = {
                            CartFeature.Msg.HideConfirm
                                .let(Msg::Cart)
                                .also(accept)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Нет", color = MaterialTheme.colors.secondary)
                    }
                    TextButton(
                        onClick = {
                            CartFeature.Msg.RemoveFromCart(state.confirmDialog.id)
                                .let(Msg::Cart)
                                .also(accept)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Да", color = MaterialTheme.colors.secondary)
                    }
                }
            }
        )
    }
}