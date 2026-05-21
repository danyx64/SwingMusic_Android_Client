package dev.swingmusic.android

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.google.android.material.card.MaterialCardView
import kotlin.math.roundToInt

class LibraryAdapter(
    private val onClick: (LibraryRow, Int) -> Unit,
    private val onLongClick: (LibraryRow, Int) -> Unit,
    private val onPlayClick: (LibraryRow, Int) -> Unit,
    private val canAcceptInput: () -> Boolean = { true }
) : RecyclerView.Adapter<LibraryAdapter.RowHolder>() {

    private val rows = mutableListOf<LibraryRow>()
    private var session: Session? = null
    private val coverArtLoader = CoverArtLoader()
    private var accentColor: Int = Color.WHITE
    private var onAccentColor: Int = Color.BLACK
    private var cardColor: Int = Color.rgb(17, 17, 17)
    private var edgeColor: Int = Color.rgb(36, 36, 36)

    fun submit(next: List<LibraryRow>) {
        rows.clear()
        rows.addAll(next)
        notifyDataSetChanged()
    }

    fun setSession(value: Session?) {
        session = value
        notifyDataSetChanged()
    }

    fun setAccent(accent: Int, onAccent: Int) {
        accentColor = accent
        onAccentColor = onAccent
        notifyDataSetChanged()
    }

    fun setThemeColors(surface: Int, edge: Int) {
        cardColor = surface
        edgeColor = edge
        notifyDataSetChanged()
    }

    fun currentRows(): List<LibraryRow> = rows.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_library, parent, false)
        return RowHolder(view)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        holder.bind(rows[position])
    }

    override fun getItemCount(): Int = rows.size

    inner class RowHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card: MaterialCardView = view.findViewById(R.id.card)
        private val coverImage: ImageView = view.findViewById(R.id.coverImage)
        private val typeMark: TextView = view.findViewById(R.id.typeMark)
        private val primaryText: TextView = view.findViewById(R.id.primaryText)
        private val secondaryText: TextView = view.findViewById(R.id.secondaryText)
        private val metaText: TextView = view.findViewById(R.id.metaText)
        private val rowPlayButton: ImageButton = view.findViewById(R.id.rowPlayButton)
        private val selectableForegroundState = card.foreground?.constantState

        fun bind(row: LibraryRow) {
            val isTrack = row.kind == RowKind.TRACK
            updateRowSpacing(row)
            card.isPressed = false
            card.isSelected = false
            card.isActivated = false
            card.isFocusable = false
            card.isFocusableInTouchMode = false
            card.foreground = if (isTrack) {
                null
            } else {
                selectableForegroundState?.newDrawable(card.resources)?.mutate()
            }
            card.setCardBackgroundColor(if (isTrack) Color.TRANSPARENT else cardColor)
            card.strokeColor = if (isTrack) Color.TRANSPARENT else edgeColor
            card.strokeWidth = if (isTrack) 0 else 1
            typeMark.text = when (row.kind) {
                RowKind.FOLDER -> "F"
                RowKind.TRACK -> "T"
                RowKind.PLAYLIST -> "P"
                RowKind.ALBUM -> "A"
                RowKind.SETTINGS -> "S"
                RowKind.MESSAGE -> "i"
            }
            primaryText.text = row.title
            secondaryText.text = row.subtitle
            metaText.text = row.meta
            typeMark.setTextColor(accentColor)
            coverArtLoader.load(coverImage, typeMark, session, row.coverKind, row.image)
            rowPlayButton.visibility = if (row.playable && !isTrack) View.VISIBLE else View.GONE
            rowPlayButton.backgroundTintList = ColorStateList.valueOf(accentColor)
            rowPlayButton.setColorFilter(onAccentColor)

            itemView.isEnabled = row.kind != RowKind.MESSAGE
            itemView.alpha = if (row.kind == RowKind.MESSAGE) 0.72f else 1f
            itemView.setOnClickListener {
                @Suppress("DEPRECATION")
                val position = adapterPosition
                if (position != NO_POSITION && canAcceptInput()) onClick(row, position)
            }
            itemView.setOnLongClickListener {
                @Suppress("DEPRECATION")
                val position = adapterPosition
                if (position != NO_POSITION && canAcceptInput()) {
                    onLongClick(row, position)
                    true
                } else {
                    false
                }
            }
            rowPlayButton.setOnClickListener {
                @Suppress("DEPRECATION")
                val position = adapterPosition
                if (position != NO_POSITION && canAcceptInput()) onPlayClick(row, position)
            }
        }

        private fun updateRowSpacing(row: LibraryRow) {
            val verticalMargin = when (row.kind) {
                RowKind.PLAYLIST,
                RowKind.ALBUM -> dp(5)
                else -> dp(1)
            }
            val params = card.layoutParams as? ViewGroup.MarginLayoutParams ?: return
            if (params.topMargin == verticalMargin && params.bottomMargin == verticalMargin) return
            params.topMargin = verticalMargin
            params.bottomMargin = verticalMargin
            card.layoutParams = params
        }

        private fun dp(value: Int): Int {
            return (value * itemView.resources.displayMetrics.density).roundToInt()
        }
    }
}
