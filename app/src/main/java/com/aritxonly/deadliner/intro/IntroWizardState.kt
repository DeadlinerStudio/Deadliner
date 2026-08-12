package com.aritxonly.deadliner.intro

data class IntroWizardState(
    val currentStep: WizardStep = WizardStep.AddEntry
)

sealed class WizardStep {
    data object AddEntry : WizardStep()
    data object AddEntryInfo : WizardStep()
    data object SwipeRightComplete : WizardStep()
    data object SwipeLeftDelete : WizardStep()
    data object AiEntry : WizardStep()
    data object AiInfo : WizardStep()
    data object Done : WizardStep()
}
