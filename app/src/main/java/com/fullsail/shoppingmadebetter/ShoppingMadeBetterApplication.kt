package com.fullsail.shoppingmadebetter

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp

/**
 * Configures Coil's process-wide [ImageLoader] via [SingletonImageLoader.Factory].
 *
 * Product thumbnails come from an external vendor host, so we cache aggressively:
 * a large on-disk LRU cache means each image is fetched from the network at most
 * once and served from disk on every later launch, keeping load on the vendor
 * minimal. Coil evicts the least-recently-used entries once the cache fills, so
 * the cache is self-bounding without any extra ejection logic.
 */
@HiltAndroidApp
class ShoppingMadeBetterApplication : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, percent = 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(IMAGE_DISK_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()

    private companion object {
        /** Upper bound for the on-disk image cache (512 MB). */
        const val IMAGE_DISK_CACHE_BYTES = 512L * 1024 * 1024
    }
}
