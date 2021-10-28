package ru.skillbranch.sbdelivery.screens.root

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import ru.skillbranch.sbdelivery.screens.root.logic.*
import javax.inject.Inject


@FlowPreview
@HiltViewModel
class RootViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val dispatcher: EffDispatcher
) : ViewModel() {
    private val rootFeature: RootFeature = RootFeature(handle.get<RootState>("state"))

    val stateFlow
        get() = rootFeature.state

    val commands
        get() = dispatcher.commands

    val notifications
        get() = dispatcher.notifications

    init {
        // При запуске приложения метод get вернет null
        val initState = handle.get<RootState>("state")
        Log.e("XXX", "RootViewModel. initState: $initState")
        // Собираем реактивную систему
        rootFeature.listen(viewModelScope, dispatcher, initState)
    }

    fun navigate(cmd: NavCmd) {
        rootFeature.mutate(Msg.Navigate(cmd))
    }

    fun accept(msg: Msg) {
        rootFeature.mutate(msg)
    }

    fun saveState(){
        val state = stateFlow.value
        handle.set("state", state)
        Log.e("XXX", "RootViewModel. Save state: $state")
    }

}
