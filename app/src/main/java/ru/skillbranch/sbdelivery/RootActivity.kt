package ru.skillbranch.sbdelivery

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.lifecycleScope
import coil.annotation.ExperimentalCoilApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import ru.skillbranch.sbdelivery.screens.root.RootViewModel
import ru.skillbranch.sbdelivery.screens.root.logic.Command
import ru.skillbranch.sbdelivery.screens.root.logic.NavCmd
import ru.skillbranch.sbdelivery.screens.root.ui.AppTheme
import ru.skillbranch.sbdelivery.screens.root.ui.RootScreen


@ExperimentalCoilApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@FlowPreview
@AndroidEntryPoint
class RootActivity : AppCompatActivity() {

    private val viewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenCreated {
            viewModel.commands
                .collect { handleCommands(it) }
        }

        setContent {
            AppTheme {
                RootScreen(viewModel)
            }
            // Video-2 t.c. 01:53:40.
            // Calling this in your composable adds the given lambda to the
            // OnBackPressedDispatcher of the LocalOnBackPressedDispatcherOwner
            BackHandler {
                viewModel.navigate(NavCmd.Back)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.e("XXX", "RootActivity. Save instant state")
        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }

    private fun handleCommands(cmd: Command) {
        //Handle Android specific command (Activity.finish, ActivityResult e.t.c "
        Log.e("XXX", "HANDLE CMD: $cmd")
        when (cmd) {
            // Если поступила команда закрыть активити
            Command.Finish -> finish()
        }
    }
}