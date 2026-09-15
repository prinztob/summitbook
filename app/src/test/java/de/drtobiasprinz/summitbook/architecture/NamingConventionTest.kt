package de.drtobiasprinz.summitbook.architecture

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.Test

/**
 * Keeps naming conventions stable so the role of a class is recognizable
 * from its name (activities, workers, view models).
 */
class NamingConventionTest {

    private val importedClasses = importMainClasses()

    @Test
    fun `activities end with Activity or Compose`() {
        classes()
            .that().areTopLevelClasses()
            .and().areAssignableTo("android.app.Activity")
            .should().haveSimpleNameEndingWith("Activity")
            .orShould().haveSimpleNameEndingWith("Compose")
            .because("activity classes should be recognizable by name")
            .check(importedClasses)
    }

    @Test
    fun `workers end with Worker`() {
        classes()
            .that().areTopLevelClasses()
            .and().areAssignableTo("androidx.work.ListenableWorker")
            .should().haveSimpleNameEndingWith("Worker")
            .because("background worker classes should be recognizable by name")
            .check(importedClasses)
    }

    @Test
    fun `view models end with ViewModel`() {
        classes()
            .that().areTopLevelClasses()
            .and().areAssignableTo("androidx.lifecycle.ViewModel")
            .should().haveSimpleNameEndingWith("ViewModel")
            .because("view model classes should be recognizable by name")
            .check(importedClasses)
    }
}
