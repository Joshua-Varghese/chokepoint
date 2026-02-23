package com.joshua.chokepoint.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun ProductImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    if (imageUrl != null && imageUrl.startsWith("data:image")) {
        val base64String = imageUrl.substringAfter("base64,")
        val bitmap = remember(base64String) {
            try {
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
