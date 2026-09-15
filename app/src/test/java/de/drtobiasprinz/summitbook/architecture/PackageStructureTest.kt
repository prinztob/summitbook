package de.drtobiasprinz.summitbook.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Restricts the code base to the agreed package structure. New top-level
 * packages or hand-written files in the root package are rejected so utility
 * dumping grounds do not creep back in. Generated classes (Dagger/Hilt
 * components, view bindings, R, BuildConfig) are tolerated.
 */
class PackageStructureTest {

    private lateinit var classes: JavaClasses

    @Before
    fun importClasses() {
        classes = importMainClasses()
    }

    private companion object {
        const val BASE = "de.drtobiasprinz.summitbook"
        val ALLOWED_TOP_LEVEL =
            setOf("core", "data", "sync", "work", "widget", "di", "ui", "databinding")
        val ALLOWED_ROOT = Regex(
            "^(MyApp(_HiltComponents|_GeneratedInjector|_ComponentTreeDeps|_MembersInjector)?|" +
                "Hilt_MyApp|DaggerMyApp.*|R(\\$.*)?|BuildConfig)(\\$.*)?$"
        )
    }

    @Test
    fun `only the agreed top-level packages exist`() {
        val violations = classes
            .filter { it.packageName != BASE }
            .map { it.name }
            .filter { it.startsWith("$BASE.") }
            .map { it.removePrefix("$BASE.") }
            .filter { it.isNotEmpty() }
            .map { relative -> relative.split('.').first() to relative }
            .filter { (topLevel, _) -> topLevel !in ALLOWED_TOP_LEVEL }
            .map { (_, relative) -> "$BASE.$relative" }
        assertTrue(
            "Unexpected top-level packages: $violations (allowed: $ALLOWED_TOP_LEVEL)",
            violations.isEmpty()
        )
    }

    @Test
    fun `root package only contains the application and generated classes`() {
        val violations = classes
            .filter { it.packageName == BASE }
            .filterNot { javaClass -> ALLOWED_ROOT.matches(javaClass.name.removePrefix("$BASE.")) }
        assertTrue(
            "Root package must only contain MyApp and generated classes, found: ${violations.map { it.name }}",
            violations.isEmpty()
        )
    }
}
