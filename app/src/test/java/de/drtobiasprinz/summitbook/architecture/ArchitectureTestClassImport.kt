package de.drtobiasprinz.summitbook.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption

/**
 * Imports only classes from the main sourceset. AGP compiles unit tests to
 * directories like `debugUnitTest`, which ArchUnit's predefined
 * DO_NOT_INCLUDE_TESTS option does not match, so we filter manually.
 */
fun importMainClasses() = ClassFileImporter()
    .withImportOption(ImportOption { location ->
        !location.contains("UnitTest") &&
            !location.contains("androidTest") &&
            !location.contains("/test/")
    })
    .importPackages("de.drtobiasprinz.summitbook")
