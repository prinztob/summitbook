package de.drtobiasprinz.summitbook.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.properties.HasName
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.Before
import org.junit.Test

/**
 * Enforces the layered package structure:
 *
 * core  -> (nothing)
 * data  -> core
 * sync  -> data, core
 * work  -> data, sync, core
 * widget-> data, core
 * ui    -> data, sync, work, core
 * di    -> may access all layers
 *
 * The only cross-layer exception: widget may reference ui.activities.MainActivityCompose
 * (to launch the app) and ui.activities.SummitEntryDetailsComposeActivity
 * (to deep-link a recent summit row straight to its details screen).
 */
class LayeredArchitectureTest {

    private lateinit var classes: JavaClasses

    @Before
    fun importClasses() {
        classes = importMainClasses()
    }

    private fun forbidAccess(layer: String, vararg forbidden: String) {
        noClasses()
            .that(JavaClass.Predicates.resideInAPackage("de.drtobiasprinz.summitbook.$layer.."))
            .should().dependOnClassesThat()
            .resideInAnyPackage(*forbidden)
            .because("layer '$layer' may not depend on ${forbidden.joinToString(", ")}")
            .check(classes)
    }

    @Test
    fun `core must not depend on any other layer`() {
        forbidAccess(
            "core",
            "de.drtobiasprinz.summitbook.data..",
            "de.drtobiasprinz.summitbook.sync..",
            "de.drtobiasprinz.summitbook.work..",
            "de.drtobiasprinz.summitbook.widget..",
            "de.drtobiasprinz.summitbook.ui..",
            "de.drtobiasprinz.summitbook.di.."
        )
    }

    @Test
    fun `data must only depend on core`() {
        forbidAccess(
            "data",
            "de.drtobiasprinz.summitbook.sync..",
            "de.drtobiasprinz.summitbook.work..",
            "de.drtobiasprinz.summitbook.widget..",
            "de.drtobiasprinz.summitbook.ui..",
            "de.drtobiasprinz.summitbook.di.."
        )
    }

    @Test
    fun `sync must only depend on data and core`() {
        forbidAccess(
            "sync",
            "de.drtobiasprinz.summitbook.work..",
            "de.drtobiasprinz.summitbook.widget..",
            "de.drtobiasprinz.summitbook.ui..",
            "de.drtobiasprinz.summitbook.di.."
        )
    }

    @Test
    fun `work must only depend on data, sync and core`() {
        forbidAccess(
            "work",
            "de.drtobiasprinz.summitbook.widget..",
            "de.drtobiasprinz.summitbook.ui..",
            "de.drtobiasprinz.summitbook.di.."
        )
    }

    @Test
    fun `widget must only depend on data and core`() {
        forbidAccess(
            "widget",
            "de.drtobiasprinz.summitbook.sync..",
            "de.drtobiasprinz.summitbook.work..",
            "de.drtobiasprinz.summitbook.di.."
        )
    }

    @Test
    fun `widget may only reference ui to launch activities`() {
        val launchableActivities = listOf(
            "de.drtobiasprinz.summitbook.ui.activities.MainActivityCompose",
            "de.drtobiasprinz.summitbook.ui.activities.SummitEntryDetailsComposeActivity"
        )
        val isLaunchableActivity = launchableActivities
            .map { HasName.Predicates.name(it) }
            .reduce { acc, next -> acc.or(next) }
        noClasses()
            .that(JavaClass.Predicates.resideInAPackage("de.drtobiasprinz.summitbook.widget.."))
            .should().dependOnClassesThat(
                JavaClass.Predicates.resideInAPackage("de.drtobiasprinz.summitbook.ui..")
                    .and(DescribedPredicate.not(isLaunchableActivity))
            )
            .because("widgets may only reference launchable ui activities, not arbitrary ui classes")
            .check(classes)
    }

    @Test
    fun `ui must not depend on widget or di`() {
        forbidAccess(
            "ui",
            "de.drtobiasprinz.summitbook.widget..",
            "de.drtobiasprinz.summitbook.di.."
        )
    }

    @Test
    fun `no layer may depend on the application class`() {
        noClasses()
            .that(JavaClass.Predicates.resideInAPackage("de.drtobiasprinz.summitbook.core.."))
            .or(JavaClass.Predicates.resideInAPackage("de.drtobiasprinz.summitbook.data.."))
            .or(JavaClass.Predicates.resideInAPackage("de.drtobiasprinz.summitbook.sync.."))
            .or(JavaClass.Predicates.resideInAPackage("de.drtobiasprinz.summitbook.work.."))
            .or(JavaClass.Predicates.resideInAPackage("de.drtobiasprinz.summitbook.widget.."))
            .or(JavaClass.Predicates.resideInAPackage("de.drtobiasprinz.summitbook.ui.."))
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("de.drtobiasprinz.summitbook.MyApp")
            .because("the Application class is a composition root; inject dependencies instead of casting")
            .check(classes)
    }
}
