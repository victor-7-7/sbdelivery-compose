package ru.skillbranch.sbdelivery.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.skillbranch.sbdelivery.domain.DishItem
import ru.skillbranch.sbdelivery.screens.components.items.ProductItem
import ru.skillbranch.sbdelivery.screens.root.ui.AppTheme

@Composable
fun <T> Grid(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemsInRow: Int = 2,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    cellsPadding: Dp = 8.dp,
    itemContent: @Composable BoxScope.(T) -> Unit
) {
    // List<List<T>>. Делим список на списки по два блюда и затем
    // собираем двухблюдные списки в новый список
    val chunkedList = items.chunked(itemsInRow)
    // A layout composable that places its children in a vertical sequence
    Column(
        verticalArrangement = Arrangement.spacedBy(cellsPadding),
        modifier = modifier.padding(contentPadding)
    ) {
        chunkedList.forEach { rowList ->
            // Для каждого двухблюдного списка строим горизонтальный ряд из его блюд
            Row {
                repeat(rowList.size) { index ->
                    Box(modifier = Modifier.weight(1f / itemsInRow)) {
                        itemContent(rowList[index])
                    }
                    // К блюдам в ряду (кроме последнего) добавляем вертикальный спейсер
                    if (index < rowList.size.dec()) Spacer(modifier = Modifier.width(cellsPadding))
                }
                // Последний чанк-список может быть не полным
                if (rowList.size % itemsInRow > 0) {
                    // Высчитываем число недостающих айтемов в неполном чанке
                    val emptyCols = itemsInRow - rowList.size % itemsInRow
                    // Добавляем паддниг-спейсеры для них
                    repeat(emptyCols) {
                        Spacer(modifier = Modifier.width(cellsPadding))
                    }
                    // Добавляем спейсер, занимающий столько пространства,
                    // сколько заняли бы айтемы на месте недостающих
                    Spacer(modifier = Modifier.weight(emptyCols / itemsInRow.toFloat()))
                }
            }

        }
    }
}

@Composable
fun <T> LazyGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemsInRow: Int = 2,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    cellsPadding: Dp = 8.dp,
    itemContent: @Composable BoxScope.(T) -> Unit
){
    // List<List<T>>. Делим список на списки по два блюда и затем
    // собираем двухблюдные списки в новый список
    val chunkedList = items.chunked(itemsInRow)
    // The vertically scrolling list that only composes and lays out
    // the currently visible items
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(cellsPadding),
        modifier = modifier
    ) {
        items(chunkedList){ rowList ->
            Row {
                // Для каждого двухблюдного списка строим горизонтальный ряд из его блюд
                repeat(rowList.size) { index ->
                    Box(modifier = Modifier.weight(1f / itemsInRow)) {
                        itemContent(rowList[index])
                    }
                    // К блюдам в ряду (кроме последнего) добавляем вертикальный спейсер
                    if (index < rowList.size.dec()) Spacer(modifier = Modifier.width(cellsPadding))
                }
                // Последний чанк-список может быть не полным
                if (rowList.size % itemsInRow > 0) {
                    // Высчитываем число недостающих айтемов в неполном чанке
                    val emptyCols = itemsInRow - rowList.size % itemsInRow
                    // Добавляем паддниг-спейсеры для них
                    repeat(emptyCols) {
                        Spacer(modifier = Modifier.width(cellsPadding))
                    }
                    // Добавляем спейсер, занимающий столько пространства,
                    // сколько заняли бы айтемы на месте недостающих
                    Spacer(modifier = Modifier.weight(emptyCols / itemsInRow.toFloat()))
                }
            }
        }
    }
}

@Preview
@Composable
fun GridPreview() {
    val dishes = listOf(
        DishItem(
            id = "5ed8da011f071c00465b2026",
            image = "https://www.delivery-club.ru/media/cms/relation_product/32350/312372888_m650.jpg",
            price = "170",
            title = "Бургер \"Америка\"",
            isFavorite = true
        ),
        DishItem(
            id = "5ed8da011f071c00465b2027",
            image = "https://www.delivery-club.ru/media/cms/relation_product/32350/312372889_m650.jpg",
            price = "259",
            title = "Бургер \"Мексика\"",
            isSale = true
        ),
        DishItem(
            id = "5ed8da011f071c00465b2028",
            image = "https://www.delivery-club.ru/media/cms/relation_product/32350/312372890_m650.jpg",
            price = "379",
            title = "Бургер \"Русский\""
        ),
    )

    AppTheme {
        Grid(
            items = dishes,
            itemsInRow = 2
        ) {
            ProductItem(dish = it, onToggleLike = {}, onAddToCart = {}, onClick = {})
        }
    }
}