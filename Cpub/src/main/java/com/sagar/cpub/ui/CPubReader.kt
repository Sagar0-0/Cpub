package com.sagar.cpub.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sagar.cpub.models.EpubBook
import com.sagar.cpub.utils.BookTextMapper
import com.sagar.cpub.utils.EpubParser
import com.sagar.cpub.utils.chunkText
import kotlinx.coroutines.async

@Composable
fun CPubReader(
    filePath: String,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    loadingAnimation: @Composable () -> Unit = { CircularProgressIndicator() }
) {
    var epubBook by remember {
        mutableStateOf<EpubBook?>(null)
    }
    var isLoading by rememberSaveable {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        isLoading = true
        async {
            epubBook = EpubParser().createEpubBook(filePath)
        }.await()
        isLoading = false
    }

    Crossfade(isLoading, label = "CrossFade") {
        if (it || epubBook == null) {
            loadingAnimation.invoke()
        } else {
            SelectionContainer(modifier) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState
                ) {
                    items(
                        count = epubBook!!.chapters.size,
                        key = { index -> epubBook!!.chapters[index].hashCode() }
                    ) { index ->
                        ChapterLazyItem(
                            epubBook = epubBook!!,
                            index = index,
                            onClick = { }
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun ChapterLazyItem(epubBook: EpubBook, index: Int, onClick: () -> Unit) {
    val chapter = epubBook.chapters[index]
    val paragraphs = remember { chunkText(chapter.body) }
    val targetFontSize = (18 / 10) * 1.8f//TODO: Make this changeable with observable value
    val fontSize by animateFloatAsState(
        targetValue = targetFontSize,
        animationSpec = tween(durationMillis = 300),
        label = "fontSize"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp),
            text = chapter.title,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        paragraphs.forEach { para ->
            val imgEntry = BookTextMapper.ImgEntry.fromXMLString(para)
            when {
                imgEntry == null -> {
                    Text(
                        text = para,
                        fontSize = fontSize.sp,
                        lineHeight = 1.3.em,
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                    )
                }

                else -> {
                    val image = epubBook.images.find { it.absPath == imgEntry.path }
                    image?.let {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(image.image)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            }

        }

        Divider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        )
    }
}
