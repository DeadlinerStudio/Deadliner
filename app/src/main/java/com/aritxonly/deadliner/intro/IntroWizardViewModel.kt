package com.aritxonly.deadliner.intro

import androidx.lifecycle.ViewModel

/** Android lifecycle owner for the platform-neutral KMP walkthrough state machine. */
class IntroWizardViewModel : ViewModel() {
    private val shared = com.deadliner.viewmodel.intro.IntroWizardViewModel()

    val state = shared.state

    fun onAddEntryClicked() = shared.onAddEntryClicked()

    fun onAddEntryInfoNext() = shared.onAddEntryInfoNext()

    fun onSwipeRightComplete() = shared.onSwipeRightComplete()

    fun onSwipeLeftDelete() = shared.onSwipeLeftDelete()

    fun onAiEntryTriggered() = shared.onAiEntryTriggered()

    fun onAiInfoNext() = shared.onAiInfoNext()

    fun next() = shared.next()
}
