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

@PublishedApi
internal val formStateGson = Gson()

/**
 * Gson-based saver for non-null domain objects. Restore failures (e.g. a
 * class layout change between save and restore) log and yield null so the
 * caller falls back to its initial value instead of crashing on restore.
 */
inline fun <reified T : Any> jsonSaver(): Saver<T, String> = Saver(
    save = { value -> formStateGson.toJson(value) },
    restore = { stored ->
        try {
            formStateGson.fromJson(stored, object : TypeToken<T>() {}.type) as T?
        } catch (e: Exception) {
            android.util.Log.w(
                "FormStateSavers",
                "Could not restore ${T::class.java.name} from saved state",
                e
            )
            null
        }
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

/** Saver for enum constants, stored by name. Unknown names (e.g. after a
 *  rename or removal of a constant) restore to null so the caller falls back
 *  to its initial value instead of crashing. */
inline fun <reified T : Enum<T>> enumSaver(): Saver<T, String> = Saver(
    save = { it.name },
    restore = { stored ->
        try {
            enumValueOf<T>(stored)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
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

/** Saver for a non-null java.util.Date stored as epoch millis. */
val nonNullDateSaver: Saver<Date, Long> = Saver(
    save = { it.time },
    restore = { stored -> Date(stored) }
)

/** Saver for List<String>. */
val stringListSaver = listSaver<List<String>, String>(
    save = { list: List<String> -> list.toList() },
    restore = { stored: List<String> -> stored }
)

/** Saver for List<Long>. */
val longListSaver = listSaver<List<Long>, Long>(
    save = { list: List<Long> -> list.toList() },
    restore = { stored: List<Long> -> stored }
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
