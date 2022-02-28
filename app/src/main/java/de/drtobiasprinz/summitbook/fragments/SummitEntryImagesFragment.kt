import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.stfalcon.imageviewer.StfalconImageViewer
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.PageViewModel
import kotlinx.android.synthetic.*
import org.imaginativeworld.whynotimagecarousel.ImageCarousel
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem


class SummitEntryImagesFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private var database: AppDatabase? = null
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
        val root: View = inflater.inflate(R.layout.fragment_summit_entry_images, container, false)
        database = context?.let { AppDatabase.getDatabase(it) }
        if (summitEntry == null && savedInstanceState != null) {
            val summitEntryId = savedInstanceState.getLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != 0L) {
                summitEntry = database?.summitDao()?.getSummit(summitEntryId)
            }
        }
        val localSummit = summitEntry
        if (localSummit != null) {
            val textViewName = root.findViewById<TextView>(R.id.summit_name)
            textViewName.text = localSummit.name
            val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
            imageViewSportType.setImageResource(localSummit.sportType.imageIdBlack)
            if (localSummit.hasImagePath()) {
                setImages(root, localSummit)
            }
        }
        return root
    }

    private fun setImages(root: View, localSummit: Summit) {
        val metrics = DisplayMetrics()
        val mainActivity = MainActivity.mainActivity
        mainActivity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        val carousel: ImageCarousel = root.findViewById(R.id.carousel)
        val params = carousel.layoutParams
        params.height = (metrics.heightPixels * 0.7).toInt()
        carousel.layoutParams = params
        carousel.invalidate()
        carousel.clearFindViewByIdCache()
        val list = mutableListOf<CarouselItem>()
        for (imageId in localSummit.imageIds) {
            val item = CarouselItem(imageUrl = "file://" + localSummit.getImagePath(imageId))
            list.add(item)
        }
        carousel.setData(list)
        carousel.carouselListener = object : CarouselListener {
            override fun onClick(position: Int, carouselItem: CarouselItem) {
                StfalconImageViewer.Builder(context, summitEntry?.imageIds) { view, imageId ->
                    Glide.with(root)
                            .load("file://" + localSummit.getImagePath(imageId))
                            .fitCenter()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(view)
                }
                        .withStartPosition(position)
                        .show()
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        summitEntry?.id?.let { outState.putLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        database?.close()
    }

    companion object {
        private const val TAG = "SummitImagesFragment"
        fun newInstance(summitEntry: Summit): SummitEntryImagesFragment {
            val fragment = SummitEntryImagesFragment()
            fragment.summitEntry = summitEntry
            return fragment
        }
    }
}