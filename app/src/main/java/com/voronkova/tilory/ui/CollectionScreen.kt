package com.voronkova.tilory.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voronkova.tilory.R
import com.voronkova.tilory.data.CollectionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Tilory collection (#57): "Random from the collection" on top (never shown before it is solved), then a grid
 * of all photos to pick one; solved photos carry a check mark.
 */
@Composable
fun CollectionScreen(vm: TiloryViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val items = vm.collection.items
    Scaffold(topBar = { BackTopBar(stringResource(R.string.collection_title), vm::back) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = vm::startCollectionRandom,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.grass),
                        contentColor = colorResource(R.color.on_river),
                    ),
                ) {
                    Text(stringResource(R.string.collection_random))
                }
            }
            items(items, key = { it.id }) { item ->
                CollectionTile(vm, item, solved = item.id in data.collectionSolved)
            }
        }
    }
}

@Composable
private fun CollectionTile(vm: TiloryViewModel, item: CollectionItem, solved: Boolean) {
    var thumb by remember(item) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(item) {
        thumb = withContext(Dispatchers.IO) { vm.collection.thumbnail(item)?.asImageBitmap() }
    }
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { vm.pickFromCollection(item) },
    ) {
        thumb?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (solved) {
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.collection_solved),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(colorResource(R.color.grass), CircleShape)
                    .padding(3.dp),
            )
        }
    }
}
