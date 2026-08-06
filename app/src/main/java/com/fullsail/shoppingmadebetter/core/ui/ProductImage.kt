package com.fullsail.shoppingmadebetter.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * A square, rounded-crop product thumbnail backed by Coil.
 *
 * Usable anywhere in the app by providing product [imageUrl].
 * Images are cached process-wide by the app's Coil `ImageLoader`.
 * Each URL is fetched at most once.
 *
 * A [fallbackIconRes] glyph stands in whenever there's no usable image: a blank
 * or null [imageUrl], or while the real image is loading or errors.
 *
 * @param imageUrl the URL of the image to display.
 * @param contentDescription spoken label for the image; `null` marks it decorative.
 * @param modifier optional styling.
 * @param size the size of the thumbnail.
 * @param cornerRadius the corner radius of the thumbnail.
 * @param fallbackIconRes the resource to use for the fallback glyph.
 */
@Composable
fun ProductImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 12.dp,
    @DrawableRes fallbackIconRes: Int = R.drawable.ic_pantry,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            FallbackGlyph(iconRes = fallbackIconRes, thumbnailSize = size)
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { FallbackGlyph(iconRes = fallbackIconRes, thumbnailSize = size) },
                error = { FallbackGlyph(iconRes = fallbackIconRes, thumbnailSize = size) },
            )
        }
    }
}

/** The centered placeholder glyph shown when there is no product image to display. */
@Composable
private fun FallbackGlyph(
    @DrawableRes iconRes: Int,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(thumbnailSize / 2),
    )
}

@Preview(showBackground = true, name = "No image (fallback)")
@Composable
private fun ProductImageFallbackPreview() {
    ShoppingMadeBetterTheme {
        ProductImage(imageUrl = null, contentDescription = null)
    }
}
