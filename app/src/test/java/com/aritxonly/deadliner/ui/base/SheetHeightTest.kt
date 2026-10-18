package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class SheetHeightTest {
    @Test fun fullIncludesHandleAndStopsEightDpBelowTheSystemSafeArea() {
        assertEquals(32.dp, SheetHeight.FULL.topInset(24.dp))
        assertEquals(859.dp, SheetHeight.FULL.bodyHeight(899.dp, 24.dp))
        assertEquals(899.dp - 24.dp - 8.dp,
            SheetHeight.FULL.bodyHeight(899.dp, 24.dp) + 8.dp)
    }
    @Test fun mediumKeepsTheExistingBodyHeightAndFitsSmallWindows() {
        assertEquals(600.dp, SheetHeight.MEDIUM.bodyHeight(899.dp, 24.dp))
        assertEquals(368.dp, SheetHeight.MEDIUM.bodyHeight(400.dp, 24.dp))
        assertEquals(24.dp, SheetHeight.MEDIUM.topInset(24.dp))
    }
    @Test fun fullRecomputesForLandscapeAndDoesNotProduceNegativeConstraints() {
        assertEquals(360.dp, SheetHeight.FULL.bodyHeight(400.dp, 24.dp))
        assertEquals(0.dp, SheetHeight.FULL.bodyHeight(30.dp, 24.dp))
    }
}
