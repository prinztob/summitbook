package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.model.RangeSliderValues
import java.io.File
import java.util.Date

/**
 * Reusable [Saver]s for dialog/form state so input survives rotation and
 * process death (see rememberSaveable usages).
 *
 * Non-auto-savable types (enums, dates, files, domain objects) are mapped to
 * Bundle-compatible primitives; complex domain objects go through Gson.
 */

private val formStateGson = Gson()

/**
 * Gson-based saver for non-null domain objects. Restore failures (e.g. a
 * class layout change between save and restore) throw, so the caller notices
 * instead of silently operating on nulls.
 */
inline fun <reified T : Any> jsonSaver(): Saver<T, String> = Saver(
    save = { value -> formStateGson.toJson(value) },
    restore = { stored ->
        try {
            formStateGson.fromJson(stored, object : TypeToken<T>() {}.type) as T?
                ?: error("Could not restore $stored to ${T::class.java.name}")
        } catch (_: Exception) {
            null
        } ?: error("Could not restore $stored to ${T::class.java.name}")
    }
)

/**
 * Gson-based saver for nullable domain objects. Null values round-trip as the
 * JSON literal "null". Restore failures yield null so the caller falls back
 * to its initial value instead of crashing.
 */
inline fun <reified T : Any> nullableJsonSaver(): Saver<T?, String> = Saver(
    save = { value -> formStateGson.toJson(value) },
    restore = { stored ->
        try {
            formStateGson.fromJson(stored, object : TypeToken<T>() {}.type) as T?
        } catch (_: Exception) {
            null
        }
    }
)

/** Saver for enum constants, stored by name. */
inline fun <reified T : Enum<T>> enumSaver(): Saver<T, String> = Saver(
    save = { it.name },
    restore = { stored -> enumValueOf<T>(stored) }
)

/** Saver for nullable enum constants. */
inline fun <reified T : Enum<T>> nullableEnumSaver(): Saver<T?, String> = Saver(
    save = { it?.name ?: "" },
    restore = { stored -> stored.ifEmpty { null }?.let { enumValueOf<T>(it) } }
)

/** Saver for java.util.Date stored as epoch millis; null maps to -1. */
val dateSaver: Saver<Date?, Long> = Saver(
    save = { it?.time ?: -1L },
    restore = { stored -> if (stored == -1L) null else Date(stored) }
)

/** Saver for List<String>. */
val stringListSaver: Saver<List<String>, List<String>> = listSaver(
    save = { it.toList() },
    restore = { it }
)

/** Saver for a nullable File stored as its absolute path. */
val fileSaver: Saver<File?, String> = Saver(
    save = { it?.absolutePath ?: "" },
    restore = { stored -> stored.ifEmpty { null }?.let(::File) }
)

/**
 * Saver for [RangeSliderValues]. The value selector lambda cannot be saved,
 * so callers must pass the same selector they used to construct the initial
 * value.
 */
fun rangeSliderSaver(getValue: (Summit) -> Float): Saver<RangeSliderValues, List<Float>> =
    Saver(
        save = { listOf(it.totalMin, it.selectedMin, it.selectedMax, it.totalMax, it.stepSize) },
        restore = { values ->
            RangeSliderValues(getValue, values[0], values[1], values[2], values[3], values[4])
        }
    )
