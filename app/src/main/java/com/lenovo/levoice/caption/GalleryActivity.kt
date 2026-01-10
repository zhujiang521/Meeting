package com.lenovo.levoice.caption

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 准备数据
        val items = mutableListOf<GalleryItem>()

        // 标题一
        items.add(GalleryItem.Title("标题一"))
        // 3张图片
        items.add(GalleryItem.ImageGrid(List(3) { R.drawable.ic_demo }))

        // 标题二
        items.add(GalleryItem.Title("标题二"))
        // 4张图片
        items.add(GalleryItem.ImageGrid(List(4) { R.drawable.ic_demo }))

        // 标题三
        items.add(GalleryItem.Title("标题三"))
        // 100张图片
        items.add(GalleryItem.ImageGrid(List(100) { R.drawable.ic_demo }))

        adapter = GalleryAdapter(items)
        recyclerView.adapter = adapter
    }
}

sealed class GalleryItem {
    data class Title(val text: String) : GalleryItem()
    data class ImageGrid(val images: List<Int>) : GalleryItem()
}