package com.lenovo.levoice.caption

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryAdapter(private val items: List<GalleryItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TITLE = 0
        private const val TYPE_IMAGE_GRID = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is GalleryItem.Title -> TYPE_TITLE
            is GalleryItem.ImageGrid -> TYPE_IMAGE_GRID
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TITLE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_title, parent, false)
                TitleViewHolder(view)
            }
            TYPE_IMAGE_GRID -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image_grid, parent, false)
                ImageGridViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is GalleryItem.Title -> {
                (holder as TitleViewHolder).bind(item)
            }
            is GalleryItem.ImageGrid -> {
                (holder as ImageGridViewHolder).bind(item)
            }
        }
    }

    override fun getItemCount() = items.size

    class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)

        fun bind(item: GalleryItem.Title) {
            titleText.text = item.text
        }
    }

    class ImageGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gridRecyclerView: RecyclerView = itemView.findViewById(R.id.gridRecyclerView)

        fun bind(item: GalleryItem.ImageGrid) {
            gridRecyclerView.layoutManager = GridLayoutManager(itemView.context, 4)
            gridRecyclerView.adapter = ImageGridAdapter(item.images)
        }
    }
}

class ImageGridAdapter(private val images: List<Int>) :
    RecyclerView.Adapter<ImageGridAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount() = images.size

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        init {
            // 确保图片为正方形
            itemView.post {
                val width = itemView.width
                itemView.layoutParams.height = width
                itemView.requestLayout()
            }
        }

        fun bind(imageRes: Int) {
            imageView.setImageResource(imageRes)
        }
    }
}