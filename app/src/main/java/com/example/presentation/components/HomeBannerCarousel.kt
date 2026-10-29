package com.example.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.core.network.NetworkMonitor
import com.example.core.network.rememberIsOnline
import com.example.domain.model.HomeBanner
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.delay

@Composable
fun HomeBannerCarousel(
    banners: List<HomeBanner>,
    modifier: Modifier = Modifier,
    canManage: Boolean = false,
    onManageClick: (() -> Unit)? = null
) {
    if (banners.isEmpty()) return

    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { banners.size })
    val isAutoScrollEnabled = banners.size > 1

    // Smooth auto-scroll when untouched (only if more than 1 banner)
    LaunchedEffect(banners.size, pagerState.currentPage, pagerState.isScrollInProgress) {
        if (isAutoScrollEnabled && !pagerState.isScrollInProgress) {
            delay(4500L) // Peaceful, gentle auto slide
            val nextPage = (pagerState.currentPage + 1) % banners.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(durationMillis = 800)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_banner_carousel")
    ) {
        // Section Header (with optional Manage button for Creator/Manager)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = SchoolBluePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "اطلاعیه‌ها و رویدادهای آموزشگاه",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (canManage && onManageClick != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SchoolBluePrimary.copy(alpha = 0.1f),
                    modifier = Modifier.clickable(onClick = onManageClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "مدیریت بنرها",
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "مدیریت بنرها",
                            style = MaterialTheme.typography.labelSmall,
                            color = SchoolBluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal Pager strictly maintaining 16:9 Aspect Ratio
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { page ->
            val banner = banners[page]
            BannerItemCard(
                banner = banner,
                onClick = {
                    openBannerLink(context, banner.linkUrl)
                }
            )
        }

        // Animated Page Indicator Dots (Only if more than 1 banner)
        if (banners.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(banners.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (isSelected) 20.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) SchoolBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun BannerItemCard(
    banner: HomeBanner,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f) // Strictly 16:9 aspect ratio
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("banner_item_${banner.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Clean 16:9 Banner Image - No link badges or text overlays on the picture
            BannerImage(
                imageUrl = banner.imageUrl,
                contentDescription = banner.title,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private enum class BannerLoadStatus {
    LOADING,
    SUCCESS,
    ERROR
}

@Composable
fun BannerImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnline by rememberIsOnline()
    var retryCount by remember { mutableIntStateOf(0) }

    // When internet connection is established / restored, automatically retry loading
    LaunchedEffect(isOnline) {
        if (isOnline) {
            retryCount++
        }
    }

    val resId = when (imageUrl) {
        "res:img_home_banner_olympiad" -> R.drawable.img_home_banner_olympiad
        "res:img_home_banner_sports" -> R.drawable.img_home_banner_sports
        else -> null
    }

    if (!isOnline) {
        // When disconnected from internet, show gray "تصویر لود نشد"
        BannerOfflineBox(
            modifier = modifier,
            onRetry = { retryCount++ }
        )
    } else if (resId != null) {
        // Preset banner: show spinning circle indicator based on server reachability
        var loadStatus by remember(imageUrl, retryCount) {
            mutableStateOf(BannerLoadStatus.LOADING)
        }

        LaunchedEffect(imageUrl, retryCount, isOnline) {
            if (!isOnline) {
                loadStatus = BannerLoadStatus.ERROR
                return@LaunchedEffect
            }
            loadStatus = BannerLoadStatus.LOADING
            val isReachable = NetworkMonitor.checkRealInternetAccess(context)
            if (isReachable || NetworkMonitor.isOnline(context)) {
                delay(300)
                loadStatus = BannerLoadStatus.SUCCESS
            } else {
                loadStatus = BannerLoadStatus.ERROR
            }
        }

        when (loadStatus) {
            BannerLoadStatus.LOADING -> BannerLoadingBox(modifier = modifier)
            BannerLoadStatus.ERROR -> BannerOfflineBox(modifier = modifier, onRetry = { retryCount++ })
            BannerLoadStatus.SUCCESS -> {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = ContentScale.Crop
                )
            }
        }
    } else {
        // Cloud, Web or Uploaded Base64 banner:
        // Loads from server based on user's internet speed with loading spinner
        val modelData: Any = remember(imageUrl) {
            if (imageUrl.startsWith("data:image/", ignoreCase = true)) {
                try {
                    val base64 = imageUrl.substringAfter("base64,")
                    Base64.decode(base64, Base64.DEFAULT)
                } catch (_: Exception) {
                    imageUrl
                }
            } else {
                imageUrl
            }
        }

        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(modelData)
                .crossfade(300)
                .setParameter("retry", retryCount, null)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = {
                BannerLoadingBox(modifier = Modifier.fillMaxSize())
            },
            error = {
                BannerOfflineBox(
                    modifier = Modifier.fillMaxSize(),
                    onRetry = { retryCount++ }
                )
            }
        )
    }
}

@Composable
private fun BannerLoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp),
            color = SchoolBluePrimary
        )
    }
}

@Composable
private fun BannerOfflineBox(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onRetry),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "تصویر لود نشد",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = VazirmatnFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun openBannerLink(context: Context, linkUrl: String?) {
    if (linkUrl.isNullOrBlank()) return

    val cleanUrl = if (!linkUrl.startsWith("http://") && !linkUrl.startsWith("https://")) {
        "https://$linkUrl"
    } else {
        linkUrl
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "امکان باز کردن پیوند وجود ندارد: $cleanUrl", Toast.LENGTH_SHORT).show()
    }
}
