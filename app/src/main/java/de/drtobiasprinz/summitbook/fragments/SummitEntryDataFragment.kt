import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.models.GarminActivityData
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.PageViewModel
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor


class SummitEntryDataFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private var helper: SummitBookDatabaseHelper? = null
    private var database: SQLiteDatabase? = null
    private lateinit var root: View
    private var summitEntry: SummitEntry? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java)
        pageViewModel?.setIndex(TAG)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_summit_entry_data, container, false)
        helper = SummitBookDatabaseHelper(requireContext())
        database = helper?.writableDatabase
        if (summitEntry == null && savedInstanceState != null) {
            val summitEntryId = savedInstanceState.getInt(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != 0) {
                summitEntry = helper?.getSummitsWithId(summitEntryId, database)
            }
        }
        val localSummitEntry = summitEntry
        if (localSummitEntry != null) {
            val extrema = MainActivity.extremaValuesAllSummits
            val textViewDate = root.findViewById<TextView>(R.id.tour_date)
            textViewDate.text = localSummitEntry.getDateAsString()
            val textViewName = root.findViewById<TextView>(R.id.summit_name)
            textViewName.text = localSummitEntry.name
            val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
            imageViewSportType.setImageResource(localSummitEntry.sportType.imageId)
            setText(localSummitEntry.elevationData.elevationGain, "hm", root.findViewById(R.id.height_meterText), root.findViewById(R.id.height_meter),
                    Pair(extrema?.heightMetersMinMax?.first?.elevationData?.elevationGain ?: 0, extrema?.heightMetersMinMax?.second?.elevationData?.elevationGain ?: 0))
            setText(localSummitEntry.kilometers, "km", root.findViewById(R.id.kilometersText), root.findViewById(R.id.kilometers),
                    Pair(floor(extrema?.kilometersMinMax?.first?.kilometers ?: 0.0).toInt(), ceil(extrema?.kilometersMinMax?.second?.kilometers ?: 0.0).toInt()))
            setText(localSummitEntry.elevationData.maxElevation, "hm", root.findViewById(R.id.top_elevationText), root.findViewById(R.id.top_elevation),
                    Pair(extrema?.topElevationMinMax?.first?.elevationData?.maxElevation ?: 0, extrema?.topElevationMinMax?.second?.elevationData?.maxElevation ?: 0))
            setText(localSummitEntry.elevationData.maxSlope, "%", root.findViewById(R.id.top_slopeText), root.findViewById(R.id.top_slope),
                    Pair(extrema?.topSlopeMinMax?.first?.elevationData?.maxSlope ?: 0.0, extrema?.topSlopeMinMax?.second?.elevationData?.maxSlope ?: 0.0))
            setText(localSummitEntry.velocityData.avgVelocity, "km/h", root.findViewById(R.id.paceText), root.findViewById(R.id.pace),
                    Pair(floor(extrema?.averageSpeedMinMax?.first?.velocityData?.avgVelocity ?: 0.0).toInt(), ceil(extrema?.averageSpeedMinMax?.second?.velocityData?.avgVelocity ?: 0.0).toInt()))
            setText(localSummitEntry.velocityData.maxVelocity, "km/h", root.findViewById(R.id.top_speedText), root.findViewById(R.id.top_speed),
                    Pair(floor(extrema?.topSpeedMinMax?.first?.velocityData?.maxVelocity ?: 0.0).toInt(), ceil(extrema?.topSpeedMinMax?.second?.velocityData?.maxVelocity ?: 0.0).toInt()))

            setAdditionalSpeedData(localSummitEntry, extrema)
            val expandMoreSpeedData: TextView = root.findViewById(R.id.expand_more_speed_data)
            if (localSummitEntry.velocityData.oneKilometer > 0.0) {
                expandMoreSpeedData.visibility = View.VISIBLE
                expandMoreSpeedData.setOnClickListener {
                    if (expandMoreSpeedData.text == getString(R.string.more_speed)) {
                        expandMoreSpeedData.text = getString(R.string.less_speed)
                        expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                        setAdditionalSpeedData(localSummitEntry, extrema, View.VISIBLE)
                    } else {
                        expandMoreSpeedData.text = getString(R.string.more_speed)
                        expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                        root.findViewById<TextView>(R.id.power1sec).visibility = View.GONE
                        setAdditionalSpeedData(localSummitEntry, extrema)
                    }
                }
            } else {
                expandMoreSpeedData.visibility = View.GONE
            }

            setText(localSummitEntry.duration, "h", root.findViewById(R.id.durationText), root.findViewById(R.id.duration),
                    Pair(floor(extrema?.durationMinMax?.first?.duration ?: 0.0).toInt(), ceil(extrema?.durationMinMax?.second?.duration ?: 0.0).toInt()), toHHms = true)
            setText(localSummitEntry.comments, root.findViewById(R.id.comments), root.findViewById(R.id.comments))
            setChipsText(R.id.places, localSummitEntry.getPlacesWithConnectedEntryString(requireContext(), database!!, helper!!), R.drawable.ic_place_black_24dp)
            setChipsText(R.id.countries, localSummitEntry.countries, R.drawable.ic_baseline_flag_24)
            setChipsText(R.id.participants, localSummitEntry.participants, R.drawable.ic_baseline_people_24)
            val activityData = localSummitEntry.activityData
            val expandMorePowerData: TextView = root.findViewById(R.id.expand_more_power_data)
            if (activityData != null) {
                val textView = root.findViewById(R.id.link) as TextView
                textView.isClickable = true
                textView.movementMethod = LinkMovementMethod.getInstance()
                val text = "<a href=\"${activityData.url}\">${requireContext().getString(R.string.sensor_data)}</a>"
                textView.text = Html.fromHtml(text, 0)
                root.findViewById<View>(R.id.garminData).visibility = View.VISIBLE
                setText(activityData.averageHR.toInt(), "bpm", root.findViewById(R.id.averageHrText), root.findViewById(R.id.averageHr),
                        Pair(extrema?.averageHRMinMax?.first?.activityData?.averageHR ?: 0f, extrema?.averageHRMinMax?.second?.activityData?.averageHR ?: 0f), true)
                setText(activityData.maxHR.toInt(), "bpm", root.findViewById(R.id.maxHrText), root.findViewById(R.id.maxHr),
                        Pair(extrema?.maxHRMinMax?.first?.activityData?.maxHR ?: 0f, extrema?.maxHRMinMax?.second?.activityData?.maxHR ?: 0f), true)
                setText(activityData.power.maxPower.toInt(), "W", root.findViewById(R.id.maxPowerText), root.findViewById(R.id.maxPower),
                        Pair(extrema?.maxPowerMinMax?.first?.activityData?.power?.maxPower ?: 0f, extrema?.maxPowerMinMax?.second?.activityData?.power?.maxPower ?: 0f))
                setText(activityData.calories.toInt(), "kcal", root.findViewById(R.id.caloriesText), root.findViewById(R.id.calories),
                        Pair(extrema?.caloriesMinMax?.first?.activityData?.calories ?: 0f, extrema?.caloriesMinMax?.second?.activityData?.calories ?: 0f))
                setText(activityData.power.avgPower.toInt(), "W", root.findViewById(R.id.averagePowerText), root.findViewById(R.id.averagePower),
                        Pair(extrema?.averagePowerMinMax?.first?.activityData?.power?.avgPower ?: 0f, extrema?.averagePowerMinMax?.second?.activityData?.power?.avgPower ?: 0f))
                setText(activityData.power.normPower.toInt(), "W", root.findViewById(R.id.normPowerText), root.findViewById(R.id.normPower),
                        Pair(extrema?.normPowerMinMax?.first?.activityData?.power?.normPower ?: 0f, extrema?.normPowerMinMax?.second?.activityData?.power?.normPower ?: 0f))

                setAdditionalPowerData(activityData, extrema)
                if (localSummitEntry.activityData?.power?.oneSec != null && (localSummitEntry.activityData?.power?.oneSec ?: 0) > 0) {
                    expandMorePowerData.visibility = View.VISIBLE
                    expandMorePowerData.setOnClickListener {
                        if (expandMorePowerData.text == getString(R.string.more_power)) {
                            expandMorePowerData.text = getString(R.string.less_power)
                            expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                            setAdditionalPowerData(activityData, extrema, View.VISIBLE)
                        } else {
                            expandMorePowerData.text = getString(R.string.more_power)
                            expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                            root.findViewById<TextView>(R.id.power1sec).visibility = View.GONE
                            setAdditionalPowerData(activityData, extrema)
                        }
                    }
                } else {
                    expandMorePowerData.visibility = View.GONE
                }

                setText(activityData.aerobicTrainingEffect.toDouble(), "", root.findViewById(R.id.aerobicTrainingEffectText), root.findViewById(R.id.aerobicTrainingEffect), Pair(0,5))
                setText(activityData.anaerobicTrainingEffect.toDouble(), "", root.findViewById(R.id.anaerobicTrainingEffectText), root.findViewById(R.id.anaerobicTrainingEffect), Pair(0,5))
                setText(activityData.grit.toDouble(), "", root.findViewById(R.id.gritText), root.findViewById(R.id.grit),
                        Pair(extrema?.gritMinMax?.first?.activityData?.grit ?: 0f, extrema?.gritMinMax?.second?.activityData?.grit ?: 0f))
                setText(activityData.flow.toDouble(), "", root.findViewById(R.id.flowText), root.findViewById(R.id.flow),
                        Pair(extrema?.flowMinMax?.first?.activityData?.flow ?: 0f, extrema?.flowMinMax?.second?.activityData?.flow ?: 0f))
                setText(activityData.trainingLoad.toDouble(), "", root.findViewById(R.id.trainingLoadText), root.findViewById(R.id.trainingLoad),
                        Pair(extrema?.trainingsLoadMinMax?.first?.activityData?.trainingLoad ?: 0f, extrema?.trainingsLoadMinMax?.second?.activityData?.trainingLoad ?: 0f))
                setText(activityData.vo2max.toDouble(), "", root.findViewById(R.id.vo2MaxText), root.findViewById(R.id.vo2Max),
                        Pair(extrema?.vo2maxMinMax?.first?.activityData?.vo2max ?: 0, extrema?.vo2maxMinMax?.second?.activityData?.vo2max ?: 0))
                setText(activityData.ftp.toDouble(), "", root.findViewById(R.id.FTPText), root.findViewById(R.id.FTP),
                        Pair(extrema?.ftpMinMax?.first?.activityData?.ftp ?: 0, extrema?.ftpMinMax?.second?.activityData?.ftp ?: 0))

            } else {
                root.findViewById<View>(R.id.garminData).visibility = View.GONE
                expandMorePowerData.visibility = View.GONE
            }

        }
        return root
    }

    private fun setAdditionalSpeedData(localSummitEntry: SummitEntry, extrema: ExtremaValuesSummits?, visibility: Int = View.GONE) {
        setText(localSummitEntry.velocityData.oneKilometer, "km/h", root.findViewById(R.id.oneKM_top_speedText), root.findViewById(R.id.oneKM_top_speed),
                Pair(floor(extrema?.oneKmMinMax?.first?.velocityData?.oneKilometer
                        ?: 0.0).toInt(), ceil(extrema?.oneKmMinMax?.second?.velocityData?.fiveKilometer
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummitEntry.velocityData.fiveKilometer, "km/h", root.findViewById(R.id.fiveKM_top_speedText), root.findViewById(R.id.fiveKM_top_speed),
                Pair(floor(extrema?.fiveKmMinMax?.first?.velocityData?.fiveKilometer
                        ?: 0.0).toInt(), ceil(extrema?.fiveKmMinMax?.second?.velocityData?.fiveKilometer
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummitEntry.velocityData.tenKilometers, "km/h", root.findViewById(R.id.tenKM_top_speedText), root.findViewById(R.id.tenKM_top_speed),
                Pair(floor(extrema?.tenKmMinMax?.first?.velocityData?.tenKilometers
                        ?: 0.0).toInt(), ceil(extrema?.tenKmMinMax?.second?.velocityData?.tenKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummitEntry.velocityData.fifteenKilometers, "km/h", root.findViewById(R.id.fifteenKM_top_speedText), root.findViewById(R.id.fifteenKM_top_speed),
                Pair(floor(extrema?.fifteenKmMinMax?.first?.velocityData?.fifteenKilometers
                        ?: 0.0).toInt(), ceil(extrema?.fifteenKmMinMax?.second?.velocityData?.fifteenKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummitEntry.velocityData.twentyKilometers, "km/h", root.findViewById(R.id.twentyKM_top_speedText), root.findViewById(R.id.twentyKM_top_speed),
                Pair(floor(extrema?.twentyKmMinMax?.first?.velocityData?.twentyKilometers
                        ?: 0.0).toInt(), ceil(extrema?.twentyKmMinMax?.second?.velocityData?.twentyKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummitEntry.velocityData.thirtyKilometers, "km/h", root.findViewById(R.id.thirtyKM_top_speedText), root.findViewById(R.id.thirtyKM_top_speed),
                Pair(floor(extrema?.thirtyKmMinMax?.first?.velocityData?.thirtyKilometers
                        ?: 0.0).toInt(), ceil(extrema?.thirtyKmMinMax?.second?.velocityData?.thirtyKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummitEntry.velocityData.fortyKilometers, "km/h", root.findViewById(R.id.fourtyKM_top_speedText), root.findViewById(R.id.fourtyKM_top_speed),
                Pair(floor(extrema?.fortyKmMinMax?.first?.velocityData?.fortyKilometers
                        ?: 0.0).toInt(), ceil(extrema?.fortyKmMinMax?.second?.velocityData?.fortyKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummitEntry.velocityData.fiftyKilometers, "km/h", root.findViewById(R.id.fiftyKM_top_speedText), root.findViewById(R.id.fiftyKM_top_speed),
                Pair(floor(extrema?.fiftyKmMinMax?.first?.velocityData?.fiftyKilometers
                        ?: 0.0).toInt(), ceil(extrema?.fiftyKmMinMax?.second?.velocityData?.fiftyKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummitEntry.velocityData.seventyFiveKilometers, "km/h", root.findViewById(R.id.seventyfiveKM_top_speedText), root.findViewById(R.id.seventyfiveKM_top_speed),
                Pair(floor(extrema?.seventyFiveKmMinMax?.first?.velocityData?.seventyFiveKilometers
                        ?: 0.0).toInt(), ceil(extrema?.seventyFiveKmMinMax?.second?.velocityData?.seventyFiveKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummitEntry.velocityData.hundredKilometers, "km/h", root.findViewById(R.id.hundretKM_top_speedText), root.findViewById(R.id.hundretKM_top_speed),
                Pair(floor(extrema?.hundredKmMinMax?.first?.velocityData?.hundredKilometers
                        ?: 0.0).toInt(), ceil(extrema?.hundredKmMinMax?.second?.velocityData?.hundredKilometers
                        ?: 0.0).toInt()), visibility = visibility)
    }

    private fun setAdditionalPowerData(activityData: GarminActivityData, extrema: ExtremaValuesSummits?, visibility: Int = View.GONE) {
        setText(activityData.power.oneSec, "W", root.findViewById(R.id.power1secText), root.findViewById(R.id.power1sec),
                Pair(extrema?.power1sMinMax?.first?.activityData?.power?.oneSec
                        ?: 0, extrema?.power1sMinMax?.second?.activityData?.power?.oneSec
                        ?: 0), visibility = visibility)
        setText(activityData.power.twoSec, "W", root.findViewById(R.id.power2secText), root.findViewById(R.id.power2sec),
                Pair(extrema?.power2sMinMax?.first?.activityData?.power?.twoSec
                        ?: 0, extrema?.power2sMinMax?.second?.activityData?.power?.twoSec
                        ?: 0), visibility = visibility)
        setText(activityData.power.fiveSec, "W", root.findViewById(R.id.power5secText), root.findViewById(R.id.power5sec),
                Pair(extrema?.power5sMinMax?.first?.activityData?.power?.fiveSec
                        ?: 0, extrema?.power5sMinMax?.second?.activityData?.power?.fiveSec
                        ?: 0), visibility = visibility)
        setText(activityData.power.tenSec, "W", root.findViewById(R.id.power10secText), root.findViewById(R.id.power10sec),
                Pair(extrema?.power10sMinMax?.first?.activityData?.power?.tenSec
                        ?: 0, extrema?.power10sMinMax?.second?.activityData?.power?.tenSec
                        ?: 0), visibility = visibility)
        setText(activityData.power.twentySec, "W", root.findViewById(R.id.power20secText), root.findViewById(R.id.power20sec),
                Pair(extrema?.power20sMinMax?.first?.activityData?.power?.twentySec
                        ?: 0, extrema?.power20sMinMax?.second?.activityData?.power?.twentySec
                        ?: 0), visibility = visibility)
        setText(activityData.power.thirtySec, "W", root.findViewById(R.id.power30secText), root.findViewById(R.id.power30sec),
                Pair(extrema?.power30sMinMax?.first?.activityData?.power?.thirtySec
                        ?: 0, extrema?.power30sMinMax?.second?.activityData?.power?.thirtySec
                        ?: 0), visibility = visibility)

        setText(activityData.power.oneMin, "W", root.findViewById(R.id.power1minText), root.findViewById(R.id.power1min),
                Pair(extrema?.power1minMinMax?.first?.activityData?.power?.oneMin
                        ?: 0, extrema?.power1minMinMax?.second?.activityData?.power?.oneMin
                        ?: 0), visibility = visibility)
        setText(activityData.power.twoMin, "W", root.findViewById(R.id.power2minText), root.findViewById(R.id.power2min),
                Pair(extrema?.power2minMinMax?.first?.activityData?.power?.twoMin
                        ?: 0, extrema?.power2minMinMax?.second?.activityData?.power?.twoMin
                        ?: 0), visibility = visibility)
        setText(activityData.power.fiveMin, "W", root.findViewById(R.id.power5minText), root.findViewById(R.id.power5min),
                Pair(extrema?.power5minMinMax?.first?.activityData?.power?.fiveMin
                        ?: 0, extrema?.power5minMinMax?.second?.activityData?.power?.fiveMin
                        ?: 0), visibility = visibility)
        setText(activityData.power.tenMin, "W", root.findViewById(R.id.power10minText), root.findViewById(R.id.power10min),
                Pair(extrema?.power10minMinMax?.first?.activityData?.power?.tenMin
                        ?: 0, extrema?.power10minMinMax?.second?.activityData?.power?.tenMin
                        ?: 0), visibility = visibility)
        setText(activityData.power.twentyMin, "W", root.findViewById(R.id.power20minText), root.findViewById(R.id.power20min),
                Pair(extrema?.power20minMinMax?.first?.activityData?.power?.twentyMin
                        ?: 0, extrema?.power20minMinMax?.second?.activityData?.power?.twentyMin
                        ?: 0), visibility = visibility)
        setText(activityData.power.thirtyMin, "W", root.findViewById(R.id.power30minText), root.findViewById(R.id.power30min),
                Pair(extrema?.power30minMinMax?.first?.activityData?.power?.thirtyMin
                        ?: 0, extrema?.power30minMinMax?.second?.activityData?.power?.thirtyMin
                        ?: 0), visibility = visibility)

        setText(activityData.power.oneHour, "W", root.findViewById(R.id.power1hText), root.findViewById(R.id.power1h),
                Pair(extrema?.power1hMinMax?.first?.activityData?.power?.oneHour
                        ?: 0, extrema?.power1hMinMax?.second?.activityData?.power?.oneHour
                        ?: 0), visibility = visibility)
        setText(activityData.power.twoHours, "W", root.findViewById(R.id.power2hText), root.findViewById(R.id.power2h),
                Pair(extrema?.power2hMinMax?.first?.activityData?.power?.twoHours
                        ?: 0, extrema?.power2hMinMax?.second?.activityData?.power?.twoHours
                        ?: 0), visibility = visibility)
        setText(activityData.power.fiveHours, "W", root.findViewById(R.id.power5hText), root.findViewById(R.id.power5h),
                Pair(extrema?.power5hMinMax?.first?.activityData?.power?.fiveHours
                        ?: 0, extrema?.power5hMinMax?.second?.activityData?.power?.fiveHours
                        ?: 0), visibility = visibility)
    }

    private fun setText(text: String, info: TextView, textView: TextView) {
        if (text == "") {
            info.visibility = View.GONE
            textView.visibility = View.GONE
        } else {
            info.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            textView.text = text
        }
    }

    private fun setAdditionalPowerDataVisibility(visibility: Int) {
        root.findViewById<TextView>(R.id.power1sec).visibility = visibility
        root.findViewById<TextView>(R.id.power1secText).visibility = visibility
        root.findViewById<TextView>(R.id.power2sec).visibility = visibility
        root.findViewById<TextView>(R.id.power2secText).visibility = visibility
        root.findViewById<TextView>(R.id.power5sec).visibility = visibility
        root.findViewById<TextView>(R.id.power5secText).visibility = visibility
        root.findViewById<TextView>(R.id.power10sec).visibility = visibility
        root.findViewById<TextView>(R.id.power10secText).visibility = visibility
        root.findViewById<TextView>(R.id.power20sec).visibility = visibility
        root.findViewById<TextView>(R.id.power20secText).visibility = visibility
        root.findViewById<TextView>(R.id.power30sec).visibility = visibility
        root.findViewById<TextView>(R.id.power30secText).visibility = visibility

        root.findViewById<TextView>(R.id.power1min).visibility = visibility
        root.findViewById<TextView>(R.id.power1minText).visibility = visibility
        root.findViewById<TextView>(R.id.power2min).visibility = visibility
        root.findViewById<TextView>(R.id.power2minText).visibility = visibility
        root.findViewById<TextView>(R.id.power5min).visibility = visibility
        root.findViewById<TextView>(R.id.power5minText).visibility = visibility
        root.findViewById<TextView>(R.id.power10min).visibility = visibility
        root.findViewById<TextView>(R.id.power10minText).visibility = visibility
        root.findViewById<TextView>(R.id.power20min).visibility = visibility
        root.findViewById<TextView>(R.id.power20minText).visibility = visibility
        root.findViewById<TextView>(R.id.power30min).visibility = visibility
        root.findViewById<TextView>(R.id.power30minText).visibility = visibility

        root.findViewById<TextView>(R.id.power1h).visibility = visibility
        root.findViewById<TextView>(R.id.power1hText).visibility = visibility
        root.findViewById<TextView>(R.id.power2h).visibility = visibility
        root.findViewById<TextView>(R.id.power2hText).visibility = visibility
        root.findViewById<TextView>(R.id.power5h).visibility = visibility
        root.findViewById<TextView>(R.id.power5hText).visibility = visibility
    }

    private fun setText(value: Double, unit: String, info: TextView, textView: TextView, minMax: Pair<Number?, Number?> = Pair(null, null), reverse: Boolean = false, toHHms: Boolean = false, visibility: Int = View.VISIBLE) {
        if (value <= 0.0) {
            info.visibility = View.GONE
            textView.visibility = View.GONE
        } else {
            info.visibility = visibility
            textView.visibility = visibility
            if (toHHms) {
                val valueInMs = (value * 3600000).toLong()
                textView.text = String.format("%02dh %02dm", TimeUnit.MILLISECONDS.toHours(valueInMs),
                        TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1))
            } else {
                textView.text = String.format(Locale.US, "%.1f %s", value, unit)
            }
            drawCircleWithIndication(textView, minMax, value, reverse)
        }
    }

    private fun setText(value: Int, unit: String, info: TextView, textView: TextView, minMax: Pair<Number?, Number?> = Pair(null, null), reverse: Boolean = false, visibility: Int = View.VISIBLE) {
        if (value == 0) {
            info.visibility = View.GONE
            textView.visibility = View.GONE
        } else {
            info.visibility = visibility
            textView.visibility = visibility
            textView.text = String.format("%s %s", value, unit)
            drawCircleWithIndication(textView, minMax, value.toDouble(), reverse)
        }
    }

    private fun drawCircleWithIndication(textView: TextView, minMax: Pair<Number?, Number?>, value: Double, reverse: Boolean) {
        textView.compoundDrawablePadding = 20
        var drawable = R.drawable.filled_circle_white
        if (minMax.first != null && minMax.second != null) {
            val percent = if (reverse) (minMax.second!!.toDouble() - value) / (minMax.second!!.toDouble() - minMax.first!!.toDouble()) else
                    (value - minMax.first!!.toDouble()) / (minMax.second!!.toDouble() - minMax.first!!.toDouble())
            drawable = when (percent) {
                in 0.0..0.2 -> R.drawable.filled_circle_red
                in 0.2..0.4 -> R.drawable.filled_circle_orange
                in 0.4..0.6 -> R.drawable.filled_circle_yellow
                in 0.6..0.8 -> R.drawable.filled_circle_blue
                in 0.8..1.0 -> R.drawable.filled_circle_green
                else -> R.drawable.filled_circle_white
            }
        }
        textView.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0)
    }

    private fun setChipsText(id: Int, list: List<String>, imageId: Int) {
        val chipGroup: ChipGroup = root.findViewById(id)
        if (list.isEmpty()) {
            chipGroup.visibility = View.GONE
        } else {
            for (entry in list) {
                val chip = Chip(requireContext())
                chip.text = entry
                chip.isClickable = false
                chip.chipIcon = ResourcesCompat.getDrawable(resources, imageId, null)
                chipGroup.addView(chip)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        database?.close()
        helper?.close()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        summitEntry?._id?.let { savedInstanceState.putInt(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
    }

    companion object {
        private const val TAG = "SummitEntryDataFragement"

        fun newInstance(summitEntry: SummitEntry): SummitEntryDataFragment {
            val fragment = SummitEntryDataFragment()
            fragment.summitEntry = summitEntry
            return fragment
        }
    }
}
