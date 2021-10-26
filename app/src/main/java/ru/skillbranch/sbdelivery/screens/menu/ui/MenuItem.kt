package ru.skillbranch.sbdelivery.screens.menu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import ru.skillbranch.sbdelivery.R
import ru.skillbranch.sbdelivery.domain.CategoryItem
import ru.skillbranch.sbdelivery.screens.components.LazyGrid
import ru.skillbranch.sbdelivery.screens.root.ui.AppTheme

@OptIn(ExperimentalCoilApi::class)
@Composable
fun MenuItem(
    item: CategoryItem,
    modifier: Modifier = Modifier,
    onClick: (CategoryItem) -> Unit
) {
    Card(modifier = modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clickable { onClick(item) }
    ) {
       /* Layout(
            modifier = modifier.padding(bottom = 40.dp),
            content = {
                val painter = rememberImagePainter(
                    data = item.icon, //this is svg icon
                    builder = {
                        crossfade(true)
                        placeholder(R.drawable.img_empty_place_holder)
                    })

                Icon(
                    painter = painter,
                    contentDescription = item.title,
                    tint = MaterialTheme.colors.secondary,
                    modifier = modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)
                    )

                Text(
                    text = item.title,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W700,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                )
            }

        ) { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                measurable.measure(constraints)
            }
            var yPosition = 0
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach { placeable ->
                    placeable.placeRelative(x = 0, y = yPosition)
                    yPosition += placeable.height
                }
            }
        }*/

        Column(
             verticalArrangement = Arrangement.SpaceAround,
             horizontalAlignment = Alignment.CenterHorizontally,
             modifier = modifier.padding(bottom = 16.dp)
         ) {
             val painter = rememberImagePainter(
                 data = item.icon, //this is svg icon
                 builder = {
                     crossfade(true)
                     placeholder(R.drawable.img_empty_place_holder)
                 })

             Icon(
                 painter = painter,
                 contentDescription = item.title,
                 tint = MaterialTheme.colors.secondary,
             )

             Text(
                 text = item.title,
                 textAlign = TextAlign.Center,
                 fontSize = 14.sp,
                 fontWeight = FontWeight.W700,
                 color = MaterialTheme.colors.secondary,
                 modifier = Modifier.fillMaxWidth()
             )
         }

        /*ConstraintLayout {
            val (icon, text) = createRefs()
            val painter = rememberImagePainter(
                data = item.icon, //this is svg icon
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.img_empty_place_holder)
                })

            Icon(
                painter = painter,
                contentDescription = item.title,
                tint = MaterialTheme.colors.secondary,
                modifier = Modifier.aspectRatio(1f)
                    .constrainAs(icon) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(text.top, margin = 8.dp)
                    width = Dimension.preferredWrapContent
                }
            )

            Text(
                text = item.title,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colors.secondary,
                modifier = Modifier.fillMaxWidth()
                    .constrainAs(text) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    }
            )
        }*/
    }

}

@Composable
@Preview
fun MenuPreview() {
    AppTheme {
        Row {
            MenuItem(
                item = CategoryItem("0", "test test ", "null", 0, null),
                modifier = Modifier.requiredWidth(140.dp),
                onClick = {})

            MenuItem(
                item = CategoryItem("0", "test test test test test", "null", 0, null),
                modifier = Modifier.requiredWidth(140.dp),
                onClick = {})
        }

    }
}

@Composable
@Preview
fun MenuGridPreview() {

    val items = listOf(
        CategoryItem("0", "test", null, 0, null),
        CategoryItem("1", "test1", null, 1, null),
        CategoryItem("2", "test2", null, 2, null),
        CategoryItem("3", "test3", null, 2, null),
        CategoryItem("4", "test2", null, 2, null),
    )
    AppTheme {
        LazyGrid(
            items = items,
            itemsInRow = 2,
            cellsPadding = 16.dp
        ) {
            MenuItem(it) { }
        }
    }
}

