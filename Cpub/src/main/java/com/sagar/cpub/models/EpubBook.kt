package com.sagar.cpub.models

import android.graphics.Bitmap

data class EpubBook(
    val fileName: String = "",
    val title: String = "",
    val coverImage: Bitmap? = null,
    val chapters: List<EpubChapter> = listOf(),
    val images: List<EpubImage> = listOf()
)