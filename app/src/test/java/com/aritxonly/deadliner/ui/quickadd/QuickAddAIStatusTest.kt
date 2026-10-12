package com.aritxonly.deadliner.ui.quickadd

import com.deadliner.domain.ai.error.AIError
import org.junit.Assert.*
import org.junit.Test
import java.net.UnknownHostException

class QuickAddAIStatusTest {
    @Test fun missingKeyAndMalformedResponseAreNotHiddenAsGenericFailures() {
        assertTrue(quickAddAIError(AIError.MissingAPIKey).contains("API Key"))
        assertTrue(quickAddAIError(AIError.ParsingFailed).contains("格式"))
        assertTrue(quickAddAIError(AIError.EmptyResponse).contains("为空"))
        assertTrue(quickAddAIError(UnknownHostException()).contains("网络"))
    }
}
