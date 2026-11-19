package com.nebula.browser.feature.ai

data class AgentAction(
    val action: String,
    val title: String,
    val urls: List<String> = emptyList(),
)

enum class AgentStepStatus { PENDING, RUNNING, COMPLETE, FAILED, CANCELLED }

data class AgentStep(
    val id: String,
    val title: String,
    val status: AgentStepStatus = AgentStepStatus.PENDING,
    val detail: String = "Waiting",
)
