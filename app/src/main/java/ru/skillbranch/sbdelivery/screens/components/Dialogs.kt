package ru.skillbranch.sbdelivery.screens.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.skillbranch.sbdelivery.R
import ru.skillbranch.sbdelivery.screens.dish.logic.DishFeature

// https://stackoverflow.com/questions/68852110/show-custom-alert-dialog-in-jetpack-compose

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        // onDismissRequest -> Executes when the user tries to dismiss the Dialog
        // by clicking outside or pressing the back button. This is not called
        // when the dismiss button is clicked
        onDismissRequest = { onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "О приложении SBDelivery",
                    color = MaterialTheme.colors.secondary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        tint = MaterialTheme.colors.onBackground,
                        painter = painterResource(R.drawable.ic_baseline_close_24),
                        contentDescription = "Close"
                    )
                }
            }
        },
        text = {
            Text(
                text = "Приложение создано студентом Victor Ivantsov при прохождении " +
                        "курса \"Middle Android Developer на Kotlin\" на образовательном портале " +
                        "Skill-Branch (https://skill-branch.ru). В приложении используется " +
                        "технология Google Jetpack Compose",
                color = MaterialTheme.colors.onSurface
            )
        },
        buttons = {
            Divider(color = MaterialTheme.colors.onSurface, thickness = 1.dp)
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(all = 8.dp)
                ) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onDismiss() },
                ) {
                    Text("Ok", color = MaterialTheme.colors.secondary)
                }
            }
        }
    )
}



