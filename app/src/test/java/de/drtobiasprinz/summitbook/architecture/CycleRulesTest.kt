package de.drtobiasprinz.summitbook.architecture

import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
import org.junit.Test

/**
 * Guards against dependency cycles between the top-level packages
 * (core, data, sync, work, widget, di, ui). Cycles between layers make it
 * impossible to reason about initialization order and test layers in isolation.
 */
class CycleRulesTest {

    private val classes = importMainClasses()

    @Test
    fun `top-level packages must be free of cycles`() {
        SlicesRuleDefinition.slices()
            .matching("de.drtobiasprinz.summitbook.(*)..")
            .should().beFreeOfCycles()
            .check(classes)
    }
}
