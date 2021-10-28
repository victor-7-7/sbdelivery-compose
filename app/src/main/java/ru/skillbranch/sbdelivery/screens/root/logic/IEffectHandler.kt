package ru.skillbranch.sbdelivery.screens.root.logic

import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel


interface IEffectHandler<E, M> {
    var localJob : Job
    suspend fun handle(effect: E, commit: (M) -> Unit)
    fun cancelJob(){
        Log.e("XXX", "Current job [$localJob] will be canceled")
        localJob.cancel("Cancel current local job")
        localJob = SupervisorJob()
    }
}