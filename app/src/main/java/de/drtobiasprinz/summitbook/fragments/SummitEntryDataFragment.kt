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
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.GarminData
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.PageViewModel
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor


class SummitEntryDataFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private var database: AppDatabase? = null
    private lateinit var root: View
    private var summitEntry: Summit? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java)
        pageViewModel?.setIndex(TAG)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        root = inflater.inflate(R.layout.fragment_summit_entry_data, container, false)
        database = context?.let { AppDatabase.getDatabase(it) }
        if (summitEntry == null && savedInstanceState != null) {
            val summitEntryId = savedInstanceState.getLong(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != 0L) {
                summitEntry = database?.summitDao()?.getSummit(summitEntryId)
            }
        }
        val localSummit = summitEntry
        if (localSummit != null) {
            val extrema = MainActivity.extremaValuesAllSummits
            val textViewDate = root.findViewById<TextView>(R.id.tour_date)
            textViewDate.text = localSummit.getDateAsString()
            val textViewName = root.findViewById<TextView>(R.id.summit_name)
            textViewName.text = localSummit.name
            val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
            imageViewSportType.setImageResource(localSummit.sportType.imageIdBlack)
            setText(localSummit.elevationData.elevationGain, "hm", root.findViewById(R.id.height_meterText), root.findViewById(R.id.height_meter),
                    Pair(extrema?.heightMetersMinMax?.first?.elevationData?.elevationGain
                            ?: 0, extrema?.heightMetersMinMax?.second?.elevationData?.elevationGain
                            ?: 0))
            setText(localSummit.kilometers, "km", root.findViewById(R.id.kilometersText), root.findViewById(R.id.kilometers),
                    Pair(floor(extrema?.kilometersMinMax?.first?.kilometers
                            ?: 0.0).toInt(), ceil(extrema?.kilometersMinMax?.second?.kilometers
                            ?: 0.0).toInt()))
            setText(localSummit.elevationData.maxElevation, "hm", root.findViewById(R.id.top_elevationText), root.findViewById(R.id.top_elevation),
                    Pair(extrema?.topElevationMinMax?.first?.elevationData?.maxElevation
                            ?: 0, extrema?.topElevationMinMax?.second?.elevationData?.maxElevation
                            ?: 0))
            setText(localSummit.elevationData.maxVerticalVelocity1Min, "m", root.findViewById(R.id.top_verticalVelocity1MinText), root.findViewById(R.id.top_verticalVelocity1Min),
                    Pair(extrema?.topVerticalVelocity1MinMinMax?.first?.elevationData?.maxVerticalVelocity1Min
                            ?: 0.0, extrema?.topVerticalVelocity1MinMinMax?.second?.elevationData?.maxVerticalVelocity1Min
                            ?: 0.0), factor = 60, digits = 0)
            setText(localSummit.elevationData.maxVerticalVelocity10Min, "m", root.findViewById(R.id.top_verticalVelocity10MinText), root.findViewById(R.id.top_verticalVelocity10Min),
                    Pair(extrema?.topVerticalVelocity10MinMinMax?.first?.elevationData?.maxVerticalVelocity10Min
                            ?: 0.0, extrema?.topVerticalVelocity10MinMinMax?.second?.elevationData?.maxVerticalVelocity10Min
                            ?: 0.0), factor = 600, digits = 0)
            setText(localSummit.elevationData.maxVerticalVelocity1h, "m", root.findViewById(R.id.top_verticalVelocity1hText), root.findViewById(R.id.top_verticalVelocity1h),
                    Pair(extrema?.topVerticalVelocity1hMinMax?.first?.elevationData?.maxVerticalVelocity1h
                            ?: 0.0, extrema?.topVerticalVelocity1hMinMax?.second?.elevationData?.maxVerticalVelocity1h
                            ?: 0.0), factor = 3600, digits = 0)
            setText(localSummit.elevationData.maxSlope, "%", root.findViewById(R.id.top_slopeText), root.findViewById(R.id.top_slope),
                    Pair(extrema?.topSlopeMinMax?.first?.elevationData?.maxSlope
                            ?: 0.0, extrema?.topSlopeMinMax?.second?.elevationData?.maxSlope
                            ?: 0.0))
            setText(localSummit.velocityData.avgVelocity, "km/h", root.findViewById(R.id.paceText), root.findViewById(R.id.pace),
                    Pair(floor(extrema?.averageSpeedMinMax?.first?.velocityData?.avgVelocity
                            ?: 0.0).toInt(), ceil(extrema?.averageSpeedMinMax?.second?.velocityData?.avgVelocity
                            ?: 0.0).toInt()))
            setText(localSummit.velocityData.maxVelocity, "km/h", root.findViewById(R.id.top_speedText), root.findViewById(R.id.top_speed),
                    Pair(floor(extrema?.topSpeedMinMax?.first?.velocityData?.maxVelocity
                            ?: 0.0).toInt(), ceil(extrema?.topSpeedMinMax?.second?.velocityData?.maxVelocity
                            ?: 0.0).toInt()))

            setAdditionalSpeedData(localSummit, extrema)
            val expandMoreSpeedData: TextView = root.findViewById(R.id.expand_more_speed_data)
            if (localSummit.velocityData.hasAdditionalData()) {
                expandMoreSpeedData.visibility = View.VISIBLE
                expandMoreSpeedData.setOnClickListener {
                    if (expandMoreSpeedData.text == getString(R.string.more_speed)) {
                        expandMoreSpeedData.text = getString(R.string.less_speed)
                        expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                        setAdditionalSpeedData(localSummit, extrema, View.VISIBLE)
                    } else {
                        expandMoreSpeedData.text = getString(R.string.more_speed)
                        expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                        root.findViewById<TextView>(R.id.power1sec).visibility = View.GONE
                        setAdditionalSpeedData(localSummit, extrema)
                    }
                }
            } else {
                expandMoreSpeedData.visibility = View.GONE
            }

            setText(localSummit.duration, "h", root.findViewById(R.id.durationText), root.findViewById(R.id.duration),
                    Pair(floor(extrema?.durationMinMax?.first?.duration
                            ?: 0.0).toInt(), ceil(extrema?.durationMinMax?.second?.duration
                            ?: 0.0).toInt()), toHHms = true)
            setText(localSummit.comments, root.findViewById(R.id.comments), root.findViewById(R.id.comments))
            database?.let { setChipsText(R.id.places, localSummit.getPlacesWithConnectedEntryString(requireContext(), it), R.drawable.ic_place_black_24dp) }
            setChipsText(R.id.countries, localSummit.countries, R.drawable.ic_baseline_flag_24)
            setChipsText(R.id.participants, localSummit.participants, R.drawable.ic_baseline_people_24)
            val garminData = localSummit.garminData
            val expandMorePowerData: TextView = root.findViewById(R.id.expand_more_power_data)
            if (garminData != null) {
                val textView = root.findViewById(R.id.link) as TextView
                textView.isClickable = true
                textView.movementMethod = LinkMovementMethod.getInstance()
                val text = "<a href=\"${garminData.url}\">${requireContext().getString(R.string.sensor_data)}</a>"
                textView.text = Html.fromHtml(text, 0)
                root.findViewById<View>(R.id.garminData).visibility = View.VISIBLE
                setText(garminData.averageHR.toInt(), "bpm", root.findViewById(R.id.averageHrText), root.findViewById(R.id.averageHr),
                        Pair(extrema?.averageHRMinMax?.first?.garminData?.averageHR
                                ?: 0f, extrema?.averageHRMinMax?.second?.garminData?.averageHR
                                ?: 0f), true)
                setText(garminData.maxHR.toInt(), "bpm", root.findViewById(R.id.maxHrText), root.findViewById(R.id.maxHr),
                        Pair(extrema?.maxHRMinMax?.first?.garminData?.maxHR
                                ?: 0f, extrema?.maxHRMinMax?.second?.garminData?.maxHR ?: 0f), true)
                setText(garminData.power.maxPower.toInt(), "W", root.findViewById(R.id.maxPowerText), root.findViewById(R.id.maxPower),
                        Pair(extrema?.maxPowerMinMax?.first?.garminData?.power?.maxPower
                                ?: 0f, extrema?.maxPowerMinMax?.second?.garminData?.power?.maxPower
                                ?: 0f))
                setText(garminData.calories.toInt(), "kcal", root.findViewById(R.id.caloriesText), root.findViewById(R.id.calories),
                        Pair(extrema?.caloriesMinMax?.first?.garminData?.calories
                                ?: 0f, extrema?.caloriesMinMax?.second?.garminData?.calories ?: 0f))
                setText(garminData.power.avgPower.toInt(), "W", root.findViewById(R.id.averagePowerText), root.findViewById(R.id.averagePower),
                        Pair(extrema?.averagePowerMinMax?.first?.garminData?.power?.avgPower
                                ?: 0f, extrema?.averagePowerMinMax?.second?.garminData?.power?.avgPower
                                ?: 0f))
                setText(garminData.power.normPower.toInt(), "W", root.findViewById(R.id.normPowerText), root.findViewById(R.id.normPower),
                        Pair(extrema?.normPowerMinMax?.first?.garminData?.power?.normPower
                                ?: 0f, extrema?.normPowerMinMax?.second?.garminData?.power?.normPower
                                ?: 0f))

                setAdditionalPowerData(garminData, extrema)
                if (localSummit.garminData?.power?.oneSec != null && (localSummit.garminData?.power?.oneSec
                                ?: 0) > 0) {
                    expandMorePowerData.visibility = View.VISIBLE
                    expandMorePowerData.setOnClickListener {
                        if (expandMorePowerData.text == getString(R.string.more_power)) {
                            expandMorePowerData.text = getString(R.string.less_power)
                            expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                            setAdditionalPowerData(garminData, extrema, View.VISIBLE)
                        } else {
                            expandMorePowerData.text = getString(R.string.more_power)
                            expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                            root.findViewById<TextView>(R.id.power1sec).visibility = View.GONE
                            setAdditionalPowerData(garminData, extrema)
                        }
                    }
                } else {
                    expandMorePowerData.visibility = View.GONE
                }

                setText(garminData.aerobicTrainingEffect.toDouble(), "", root.findViewById(R.id.aerobicTrainingEffectText), root.findViewById(R.id.aerobicTrainingEffect), Pair(0, 5))
                setText(garminData.anaerobicTrainingEffect.toDouble(), "", root.findViewById(R.id.anaerobicTrainingEffectText), root.findViewById(R.id.anaerobicTrainingEffect), Pair(0, 5))
                setText(garminData.grit.toDouble(), "", root.findViewById(R.id.gritText), root.findViewById(R.id.grit),
                        Pair(extrema?.gritMinMax?.first?.garminData?.grit
                                ?: 0f, extrema?.gritMinMax?.second?.garminData?.grit ?: 0f))
                setText(garminData.flow.toDouble(), "", root.findViewById(R.id.flowText), root.findViewById(R.id.flow),
                        Pair(extrema?.flowMinMax?.first?.garminData?.flow
                                ?: 0f, extrema?.flowMinMax?.second?.garminData?.flow ?: 0f))
                setText(garminData.trainingLoad.toDouble(), "", root.findViewById(R.id.trainingLoadText), root.findViewById(R.id.trainingLoad),
                        Pair(extrema?.trainingsLoadMinMax?.first?.garminData?.trainingLoad
                                ?: 0f, extrema?.trainingsLoadMinMax?.second?.garminData?.trainingLoad
                                ?: 0f))
                setText(garminData.vo2max.toDouble(), "", root.findViewById(R.id.vo2MaxText), root.findViewById(R.id.vo2Max),
                        Pair(extrema?.vo2maxMinMax?.first?.garminData?.vo2max
                                ?: 0, extrema?.vo2maxMinMax?.second?.garminData?.vo2max ?: 0))
                setText(garminData.ftp.toDouble(), "", root.findViewById(R.id.FTPText), root.findViewById(R.id.FTP),
                        Pair(extrema?.ftpMinMax?.first?.garminData?.ftp
                                ?: 0, extrema?.ftpMinMax?.second?.garminData?.ftp ?: 0))

            } else {
                root.findViewById<View>(R.id.garminData).visibility = View.GONE
                expandMorePowerData.visibility = View.GONE
            }

        }
        return root
    }

    private fun setAdditionalSpeedData(localSummit: Summit, extrema: ExtremaValuesSummits?, visibility: Int = View.GONE) {
        setText(localSummit.velocityData.oneKilometer, "km/h", root.findViewById(R.id.oneKM_top_speedText), root.findViewById(R.id.oneKM_top_speed),
                Pair(floor(extrema?.oneKmMinMax?.first?.velocityData?.oneKilometer
                        ?: 0.0).toInt(), ceil(extrema?.oneKmMinMax?.second?.velocityData?.fiveKilometer
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummit.velocityData.fiveKilometer, "km/h", root.findViewById(R.id.fiveKM_top_speedText), root.findViewById(R.id.fiveKM_top_speed),
                Pair(floor(extrema?.fiveKmMinMax?.first?.velocityData?.fiveKilometer
                        ?: 0.0).toInt(), ceil(extrema?.fiveKmMinMax?.second?.velocityData?.fiveKilometer
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummit.velocityData.tenKilometers, "km/h", root.findViewById(R.id.tenKM_top_speedText), root.findViewById(R.id.tenKM_top_speed),
                Pair(floor(extrema?.tenKmMinMax?.first?.velocityData?.tenKilometers
                        ?: 0.0).toInt(), ceil(extrema?.tenKmMinMax?.second?.velocityData?.tenKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummit.velocityData.fifteenKilometers, "km/h", root.findViewById(R.id.fifteenKM_top_speedText), root.findViewById(R.id.fifteenKM_top_speed),
                Pair(floor(extrema?.fifteenKmMinMax?.first?.velocityData?.fifteenKilometers
                        ?: 0.0).toInt(), ceil(extrema?.fifteenKmMinMax?.second?.velocityData?.fifteenKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummit.velocityData.twentyKilometers, "km/h", root.findViewById(R.id.twentyKM_top_speedText), root.findViewById(R.id.twentyKM_top_speed),
                Pair(floor(extrema?.twentyKmMinMax?.first?.velocityData?.twentyKilometers
                        ?: 0.0).toInt(), ceil(extrema?.twentyKmMinMax?.second?.velocityData?.twentyKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummit.velocityData.thirtyKilometers, "km/h", root.findViewById(R.id.thirtyKM_top_speedText), root.findViewById(R.id.thirtyKM_top_speed),
                Pair(floor(extrema?.thirtyKmMinMax?.first?.velocityData?.thirtyKilometers
                        ?: 0.0).toInt(), ceil(extrema?.thirtyKmMinMax?.second?.velocityData?.thirtyKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummit.velocityData.fortyKilometers, "km/h", root.findViewById(R.id.fourtyKM_top_speedText), root.findViewById(R.id.fourtyKM_top_speed),
                Pair(floor(extrema?.fortyKmMinMax?.first?.velocityData?.fortyKilometers
                        ?: 0.0).toInt(), ceil(extrema?.fortyKmMinMax?.second?.velocityData?.fortyKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummit.velocityData.fiftyKilometers, "km/h", root.findViewById(R.id.fiftyKM_top_speedText), root.findViewById(R.id.fiftyKM_top_speed),
                Pair(floor(extrema?.fiftyKmMinMax?.first?.velocityData?.fiftyKilometers
                        ?: 0.0).toInt(), ceil(extrema?.fiftyKmMinMax?.second?.velocityData?.fiftyKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummit.velocityData.seventyFiveKilometers, "km/h", root.findViewById(R.id.seventyfiveKM_top_speedText), root.findViewById(R.id.seventyfiveKM_top_speed),
                Pair(floor(extrema?.seventyFiveKmMinMax?.first?.velocityData?.seventyFiveKilometers
                        ?: 0.0).toInt(), ceil(extrema?.seventyFiveKmMinMax?.second?.velocityData?.seventyFiveKilometers
                        ?: 0.0).toInt()), visibility = visibility)
        setText(localSummit.velocityData.hundredKilometers, "km/h", root.findViewById(R.id.hundretKM_top_speedText), root.findViewById(R.id.hundretKM_top_speed),
                Pair(floor(extrema?.hundredKmMinMax?.first?.velocityData?.hundredKilometers
                        ?: 0.0).toInt(), ceil(extrema?.hundredKmMinMax?.second?.velocityData?.hundredKilometers
                        ?: 0.0).toInt()), visibility = visibility)
    }

    private fun setAdditionalPowerData(garminData: GarminData, extrema: ExtremaValuesSummits?, visibility: Int = View.GONE) {
        setText(garminData.power.oneSec, "W", root.findViewById(R.id.power1secText), root.findViewById(R.id.power1sec),
                Pair(extrema?.power1sMinMax?.first?.garminData?.power?.oneSec
                        ?: 0, extrema?.power1sMinMax?.second?.garminData?.power?.oneSec
                        ?: 0), visibility = visibility)
        setText(garminData.power.twoSec, "W", root.findViewById(R.id.power2secText), root.findViewById(R.id.power2sec),
                Pair(extrema?.power2sMinMax?.first?.garminData?.power?.twoSec
                        ?: 0, extrema?.power2sMinMax?.second?.garminData?.power?.twoSec
                        ?: 0), visibility = visibility)
        setText(garminData.power.fiveSec, "W", root.findViewById(R.id.power5secText), root.findViewById(R.id.power5sec),
                Pair(extrema?.power5sMinMax?.first?.garminData?.power?.fiveSec
                        ?: 0, extrema?.power5sMinMax?.second?.garminData?.power?.fiveSec
                        ?: 0), visibility = visibility)
        setText(garminData.power.tenSec, "W", root.findViewById(R.id.power10secText), root.findViewById(R.id.power10sec),
                Pair(extrema?.power10sMinMax?.first?.garminData?.power?.tenSec
                        ?: 0, extrema?.power10sMinMax?.second?.garminData?.power?.tenSec
                        ?: 0), visibility = visibility)
        setText(garminData.power.twentySec, "W", root.findViewById(R.id.power20secText), root.findViewById(R.id.power20sec),
                Pair(extrema?.power20sMinMax?.first?.garminData?.power?.twentySec
                        ?: 0, extrema?.power20sMinMax?.second?.garminData?.power?.twentySec
                        ?: 0), visibility = visibility)
        setText(garminData.power.thirtySec, "W", root.findViewById(R.id.power30secText), root.findViewById(R.id.power30sec),
                Pair(extrema?.power30sMinMax?.first?.garminData?.power?.thirtySec
                        ?: 0, extrema?.power30sMinMax?.second?.garminData?.power?.thirtySec
                        ?: 0), visibility = visibility)

        setText(garminData.power.oneMin, "W", root.findViewById(R.id.power1minText), root.findViewById(R.id.power1min),
                Pair(extrema?.power1minMinMax?.first?.garminData?.power?.oneMin
                        ?: 0, extrema?.power1minMinMax?.second?.garminData?.power?.oneMin
                        ?: 0), visibility = visibility)
        setText(garminData.power.twoMin, "W", root.findViewById(R.id.power2minText), root.findViewById(R.id.power2min),
                Pair(extrema?.power2minMinMax?.first?.garminData?.power?.twoMin
                        ?: 0, extrema?.power2minMinMax?.second?.garminData?.power?.twoMin
                        ?: 0), visibility = visibility)
        setText(garminData.power.fiveMin, "W", root.findViewById(R.id.power5minText), root.findViewById(R.id.power5min),
                Pair(extrema?.power5minMinMax?.first?.garminData?.power?.fiveMin
                        ?: 0, extrema?.power5minMinMax?.second?.garminData?.power?.fiveMin
                        ?: 0), visibility = visibility)
        setText(garminData.power.tenMin, "W", root.findViewById(R.id.power10minText), root.findViewById(R.id.power10min),
                Pair(extrema?.power10minMinMax?.first?.garminData?.power?.tenMin
                        ?: 0, extrema?.power10minMinMax?.second?.garminData?.power?.tenMin
                        ?: 0), visibility = visibility)
        setText(garminData.power.twentyMin, "W", root.findViewById(R.id.power20minText), root.findViewById(R.id.power20min),
                Pair(extrema?.power20minMinMax?.first?.garminData?.power?.twentyMin
                        ?: 0, extrema?.power20minMinMax?.second?.garminData?.power?.twentyMin
                        ?: 0), visibility = visibility)
        setText(garminData.power.thirtyMin, "W", root.findViewById(R.id.power30minText), root.findViewById(R.id.power30min),
                Pair(extrema?.power30minMinMax?.first?.garminData?.power?.thirtyMin
                        ?: 0, extrema?.power30minMinMax?.second?.garminData?.power?.thirtyMin
                        ?: 0), visibility = visibility)

        setText(garminData.power.oneHour, "W", root.findViewById(R.id.power1hText), root.findViewById(R.id.power1h),
                Pair(extrema?.power1hMinMax?.first?.garminData?.power?.oneHour
                        ?: 0, extrema?.power1hMinMax?.second?.garminData?.power?.oneHour
                        ?: 0), visibility = visibility)
        setText(garminData.power.twoHours, "W", root.findViewById(R.id.power2hText), root.findViewById(R.id.power2h),
                Pair(extrema?.power2hMinMax?.first?.garminData?.power?.twoHours
                        ?: 0, extrema?.power2hMinMax?.second?.garminData?.power?.twoHours
                        ?: 0), visibility = visibility)
        setText(garminData.power.fiveHours, "W", root.findViewById(R.id.power5hText), root.findViewById(R.id.power5h),
                Pair(extrema?.power5hMinMax?.first?.garminData?.power?.fiveHours
                        ?: 0, extrema?.power5hMinMax?.second?.garminData?.power?.fiveHours
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

    private fun setText(
            value: Double, unit: String, info: TextView, textView: TextView,
            minMax: Pair<Number?, Number?> = Pair(null, null), reverse: Boolean = false,
            toHHms: Boolean = false, visibility: Int = View.VISIBLE, digits: Int = 1, factor: Int = 1,
    ) {
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
                textView.text = String.format(Locale.US, "%.${digits}f %s", value * factor, unit)
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
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        summitEntry?.id?.let { savedInstanceState.putLong(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
    }

    companion object {
        private const val TAG = "SummitDataFragement"

        fun newInstance(summitEntry: Summit): SummitEntryDataFragment {
            val fragment = SummitEntryDataFragment()
            fragment.summitEntry = summitEntry
            return fragment
        }
    }
}
