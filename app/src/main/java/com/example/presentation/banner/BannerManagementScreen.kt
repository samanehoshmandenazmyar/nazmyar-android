package com.example.presentation.banner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.banner.BannerManager
import com.example.domain.model.HomeBanner
import com.example.domain.model.SchoolClasses
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.BannerImage
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.SchoolBluePrimary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerManagementScreen(
    user: User?,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val banners by BannerManager.banners.collectAsState()

    var showAddBottomSheet by remember { mutableStateOf(false) }
    var bannerToDelete by remember { mutableStateOf<HomeBanner?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مدیریت بنرهای صفحه اول",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "سایز ۱۶:۹، تفکیک پایه‌ها و لینک مستقیم",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("banner_management_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    if (banners.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                BannerManager.clearAllBanners()
                                scope.launch {
                                    snackbarHostState.showSnackbar("تمام بنرها حذف شدند.")
                                }
                            },
                            modifier = Modifier.testTag("clear_all_banners_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف همه بنرها",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBottomSheet = true },
                containerColor = SchoolBluePrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_banner_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "افزودن بنر",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("banner_management_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SchoolBluePrimary.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SchoolBluePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = SchoolBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "بنرهای فعال: ${banners.size.toPersianDigits()} بنر",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "بنرها در نسبت ۱۶:۹ در صفحه اول نمایش داده می‌شوند و با کلیک روی آنها مستقیم وارد لینک می‌شوند.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            if (banners.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "هیچ بنری در صفحه اول وجود ندارد",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "با زدن دکمه «افزودن بنر» می‌توانید اولین بنر ۱۶:۹ خود را اضافه کنید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddBottomSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                            ) {
                                Text("افزودن بنر جدید")
                            }
                        }
                    }
                }
            } else {
                items(banners, key = { it.id }) { banner ->
                    BannerManagementItemCard(
                        banner = banner,
                        onDeleteClick = { bannerToDelete = banner }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (bannerToDelete != null) {
        val banner = bannerToDelete!!
        AlertDialog(
            onDismissRequest = { bannerToDelete = null },
            title = {
                Text(
                    text = "حذف بنر از صفحه خانه",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("آیا از حذف بنر «${banner.title}» اطمینان دارید؟ این بنر بلافاصله از صفحه اصلی خانه برداشته خواهد شد.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        BannerManager.deleteBanner(banner.id)
                        bannerToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("بنر با موفقیت حذف شد.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncidentRed)
                ) {
                    Text("حذف بنر")
                }
            },
            dismissButton = {
                TextButton(onClick = { bannerToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Add Banner Bottom Sheet
    if (showAddBottomSheet) {
        AddBannerBottomSheet(
            onDismiss = { showAddBottomSheet = false },
            onBannerAdded = { newBanner ->
                BannerManager.addBanner(newBanner)
                showAddBottomSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("بنر جدید با موفقیت اضافه شد.")
                }
            }
        )
    }
}

@Composable
fun BannerManagementItemCard(
    banner: HomeBanner,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("management_banner_item_${banner.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Clean 16:9 Aspect Ratio Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // Strictly 16:9
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                BannerImage(
                    imageUrl = banner.imageUrl,
                    contentDescription = banner.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = banner.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "مخاطبان: ${SchoolClasses.formatClassesDisplay(banner.targetClasses)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SchoolBluePrimary,
                        fontWeight = FontWeight.Medium
                    )

                    if (!banner.linkUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = banner.linkUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_banner_button_${banner.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف بنر",
                        tint = IncidentRed
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBannerBottomSheet(
    onDismiss: () -> Unit,
    onBannerAdded: (HomeBanner) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var selectedImageUrl by remember { mutableStateOf("res:img_home_banner_olympiad") }
    var customUrlInput by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }

    // Multi-selected classes from the 6 options
    // If empty or all 6 selected, it means all classes can see it
    var selectedClasses by remember { mutableStateOf<Set<String>>(SchoolClasses.ALL_CLASSES.toSet()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUrl = uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "افزودن بنر جدید (سایز ۱۶:۹)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "بستن")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 16:9 Live Preview Box (Clean, no overlays on image)
            Text(
                text = "پیش‌نمایش تصویر بنر (سایز ۱۶:۹):",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // Strictly 16:9
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            ) {
                BannerImage(
                    imageUrl = selectedImageUrl,
                    contentDescription = "پیش‌نمایش بنر",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Choose Image Source
            Text(
                text = "انتخاب تصویر بنر:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("انتخاب از گالری گوشی", fontSize = 12.sp)
                }

                FilterChip(
                    selected = selectedImageUrl == "res:img_home_banner_olympiad",
                    onClick = { selectedImageUrl = "res:img_home_banner_olympiad" },
                    label = { Text("المپیاد", fontSize = 12.sp) }
                )

                FilterChip(
                    selected = selectedImageUrl == "res:img_home_banner_sports",
                    onClick = { selectedImageUrl = "res:img_home_banner_sports" },
                    label = { Text("ورزشی", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Optional custom direct URL
            OutlinedTextField(
                value = customUrlInput,
                onValueChange = {
                    customUrlInput = it
                    if (it.isNotBlank()) selectedImageUrl = it.trim()
                },
                label = { Text("یا وارد کردن آدرس مستقیم اینترنتی عکس (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان بنر *") },
                placeholder = { Text("مثلاً: مسابقات علمی یا اطلاعیه آموزشگاه") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-grade selection (هفتم یک، هفتم دو، هشتم یک، هشتم دو، نهم یک، نهم دو)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "پایه‌ها و کلاس‌های مجاز برای مشاهده:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = {
                        selectedClasses = if (selectedClasses.size == SchoolClasses.ALL_CLASSES.size) {
                            emptySet()
                        } else {
                            SchoolClasses.ALL_CLASSES.toSet()
                        }
                    }
                ) {
                    Text(
                        text = if (selectedClasses.size == SchoolClasses.ALL_CLASSES.size) "لغو همه" else "انتخاب همه",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // FlowRow of the 6 classes
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SchoolClasses.ALL_CLASSES.forEach { className ->
                    val isSelected = selectedClasses.contains(className)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedClasses = if (isSelected) {
                                selectedClasses - className
                            } else {
                                selectedClasses + className
                            }
                        },
                        label = { Text(className, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SchoolBluePrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "وضعیت نمایش: ${
                    when {
                        selectedClasses.isEmpty() -> "هیچ کلاسی انتخاب نشده (لطفاً حداقل یک کلاس انتخاب کنید)"
                        selectedClasses.size == SchoolClasses.ALL_CLASSES.size -> "همه پایه‌ها و کلاس‌ها بنر را می‌بینند"
                        else -> "فقط دانش‌آموزان ${selectedClasses.joinToString(" و ")} بنر را می‌بینند"
                    }
                }",
                style = MaterialTheme.typography.labelSmall,
                color = if (selectedClasses.isEmpty()) IncidentRed else SchoolBluePrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Link URL
            OutlinedTextField(
                value = linkUrl,
                onValueChange = { linkUrl = it },
                label = { Text("لینک بنر (اختیاری)") },
                placeholder = { Text("مثلاً: https://shad.ir یا سایت ثبت‌نام") },
                supportingText = { Text("وقتی کاربر روی عکس بنر کلیک کند، مستقیم وارد این لینک می‌شود.") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedClasses.isNotEmpty()) {
                        val targetList = if (selectedClasses.size == SchoolClasses.ALL_CLASSES.size) {
                            emptyList() // Empty means all classes
                        } else {
                            selectedClasses.toList()
                        }

                        val newBanner = HomeBanner(
                            id = "banner_${UUID.randomUUID()}",
                            title = title.trim(),
                            imageUrl = selectedImageUrl,
                            targetClasses = targetList,
                            linkUrl = linkUrl.trim().ifBlank { null },
                            createdAt = System.currentTimeMillis()
                        )
                        onBannerAdded(newBanner)
                    }
                },
                enabled = title.isNotBlank() && selectedClasses.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_banner_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
            ) {
                Text(
                    text = "ثبت و انتشار بنر در صفحه خانه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
