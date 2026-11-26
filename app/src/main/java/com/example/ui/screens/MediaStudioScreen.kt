package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.GeneratedMediaItem
import com.example.data.models.MediaType
import com.example.ui.AppViewModel
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenSecondary
import com.example.ui.theme.TextDark
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaStudioScreen(
    viewModel: AppViewModel,
    initialTab: Int = 0,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedStudioTab by remember { mutableIntStateOf(initialTab) }

    // Media states
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsStateWithLifecycle()
    val generatedImageResult by viewModel.generatedImageResult.collectAsStateWithLifecycle()

    val isGeneratingVideo by viewModel.isGeneratingVideo.collectAsStateWithLifecycle()
    val videoStatus by viewModel.videoGenerationStatus.collectAsStateWithLifecycle()
    val generatedVideoResult by viewModel.generatedVideoResult.collectAsStateWithLifecycle()

    val mediaHistory by viewModel.mediaHistory.collectAsStateWithLifecycle()

    // Shared state between image & video studio
    var transferredBitmapForVeo by remember { mutableStateOf<Bitmap?>(null) }
    var transferredBitmapForEdit by remember { mutableStateOf<Bitmap?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideoCameraBack,
                            contentDescription = null,
                            tint = GoldLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Studio Media AI PAI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Veo 3 Video & Gemini 3.1 Flash Image",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Studio Main Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedStudioTab,
                    containerColor = Color.Transparent,
                    contentColor = GreenPrimary
                ) {
                    Tab(
                        selected = selectedStudioTab == 0,
                        onClick = { selectedStudioTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Video Veo 3", fontSize = 12.sp, fontWeight = if (selectedStudioTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedStudioTab == 1,
                        onClick = { selectedStudioTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Gambar AI", fontSize = 12.sp, fontWeight = if (selectedStudioTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedStudioTab == 2,
                        onClick = { selectedStudioTab = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Galeri (${mediaHistory.size})", fontSize = 12.sp, fontWeight = if (selectedStudioTab == 2) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedStudioTab) {
                0 -> VeoVideoStudioSection(
                    viewModel = viewModel,
                    isGenerating = isGeneratingVideo,
                    statusText = videoStatus,
                    result = generatedVideoResult,
                    initialPhotoBitmap = transferredBitmapForVeo,
                    onClearTransferredPhoto = { transferredBitmapForVeo = null }
                )
                1 -> GeminiImageStudioSection(
                    viewModel = viewModel,
                    isGenerating = isGeneratingImage,
                    result = generatedImageResult,
                    initialPhotoBitmap = transferredBitmapForEdit,
                    onClearTransferredPhoto = { transferredBitmapForEdit = null },
                    onAnimateWithVeo = { bitmap ->
                        transferredBitmapForVeo = bitmap
                        selectedStudioTab = 0
                        Toast.makeText(context, "Gambar dikirim ke Studio Video Veo 3 untuk dianimasikan!", Toast.LENGTH_SHORT).show()
                    }
                )
                2 -> MediaGallerySection(
                    mediaList = mediaHistory,
                    onDelete = { id -> viewModel.deleteMediaItem(id) },
                    onAnimateWithVeo = { bitmap ->
                        transferredBitmapForVeo = bitmap
                        selectedStudioTab = 0
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. VEO 3 VIDEO STUDIO (veo-3.1-fast-generate-preview)
// -------------------------------------------------------------
@Composable
fun VeoVideoStudioSection(
    viewModel: AppViewModel,
    isGenerating: Boolean,
    statusText: String,
    result: com.example.util.VideoGenerationResult?,
    initialPhotoBitmap: Bitmap? = null,
    onClearTransferredPhoto: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Submodes: 0 = Text-to-Video, 1 = Animate Photo to Video
    var videoSubMode by remember { mutableIntStateOf(if (initialPhotoBitmap != null) 1 else 0) }

    // User inputs
    var videoPrompt by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("16:9") } // STRICT REQUIREMENT: 16:9 or 9:16
    var selectedPhotoBitmap by remember { mutableStateOf<Bitmap?>(initialPhotoBitmap) }

    // Photo picker launcher (Android Photo Picker - Zero Permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream)
                        if (original != null) {
                            // Scale down if oversized to prevent memory pressure
                            val maxDimension = 1024
                            val ratio = minOf(maxDimension.toFloat() / original.width, maxDimension.toFloat() / original.height, 1f)
                            selectedPhotoBitmap = if (ratio < 1f) {
                                Bitmap.createScaledBitmap(original, (original.width * ratio).toInt(), (original.height * ratio).toInt(), true)
                            } else {
                                original
                            }
                            videoSubMode = 1
                            Toast.makeText(context, "Foto berhasil dipilih", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memuat foto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Sync initial transferred bitmap if updated
    LaunchedEffect(initialPhotoBitmap) {
        if (initialPhotoBitmap != null) {
            selectedPhotoBitmap = initialPhotoBitmap
            videoSubMode = 1
        }
    }

    val educationalVideoPresets = listOf(
        "Animasi Drone Ka'bah Makkah" to "Cinematic smooth aerial drone view circling the holy Kaaba during serene sunrise with soft golden light",
        "Masjid Nabawi di Madinah" to "Gentle slow motion cinematic panning across the peaceful domes and minarets of the Prophet's Mosque in Medina",
        "Anak Muslim Belajar Mengaji" to "3D Pixar style warm animation of a cheerful young Muslim student smiling and opening a book with magical warm lighting",
        "Tawaf Jamaah Haji" to "Subtle slow motion camera drifting above pilgrims performing tawaf in unison with peaceful atmosphere",
        "Sejarah Hijrah Nabi" to "Cinematic historic atmosphere animation of desert travelers arriving in Medina Oasis under starry night"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Model Indicator Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
                Column {
                    Text("Model Mesin: veo-3.1-fast-generate-preview", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenSecondary)
                    Text("Mendukung Text-to-Video & Animasi Foto ke Video (Rasio 16:9 & 9:16)", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // Mode Switch: Teks ke Video vs Animasi Foto ke Video
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = videoSubMode == 0,
                onClick = { videoSubMode = 0 },
                label = { Text("1. Teks ke Video", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
                selected = videoSubMode == 1,
                onClick = { videoSubMode = 1 },
                label = { Text("2. Animasi Foto", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White)
            )
        }

        // Photo Upload Box (if Animasi Foto mode)
        if (videoSubMode == 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (selectedPhotoBitmap != null) GreenPrimary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Foto Sumber untuk Dianimasikan:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )

                    if (selectedPhotoBitmap != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                bitmap = selectedPhotoBitmap!!.asImageBitmap(),
                                contentDescription = "Foto Pilihan",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, GoldAccent, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Foto Siap Dianimasikan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${selectedPhotoBitmap!!.width} x ${selectedPhotoBitmap!!.height} px", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Ganti Foto", fontSize = 10.sp)
                                    }
                                    TextButton(
                                        onClick = {
                                            selectedPhotoBitmap = null
                                            onClearTransferredPhoto()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Hapus", fontSize = 10.sp, color = Color.Red)
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.5f))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pilih Foto dari Galeri untuk Dianimasikan", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GreenPrimary)
                                Text("Foto sekolah, kaligrafi, siswa, atau gambar AI", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Quick Preset Prompts
        Column {
            Text("Preset Ide Video Edukasi PAI:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                educationalVideoPresets.forEach { (label, prompt) ->
                    OutlinedButton(
                        onClick = { videoPrompt = prompt },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = GoldAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(label, fontSize = 11.sp, color = GreenPrimary)
                    }
                }
            }
        }

        // Prompt Input Field
        OutlinedTextField(
            value = videoPrompt,
            onValueChange = { videoPrompt = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("veo_video_prompt_input"),
            label = {
                Text(
                    if (videoSubMode == 1) "Deskripsi Gerakan / Animasi Foto" else "Prompt Video (Teks ke Video)"
                )
            },
            placeholder = {
                Text(
                    if (videoSubMode == 1) "Contoh: Kamera bergerak perlahan menyorot objek dengan efek pencahayaan matahari senja lembut..."
                    else "Contoh: Animasi 3D pemandangan Ka'bah dengan jamaah tawaf khusyuk, kamera bergerak melingkar..."
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Mandatory Aspect Ratio Selection (16:9 or 9:16)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Aspek Rasio Video (Wajib):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Model Veo 3", fontSize = 10.sp, color = GoldAccent, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 16:9 Landscape
                    FilterChip(
                        selected = selectedAspectRatio == "16:9",
                        onClick = { selectedAspectRatio = "16:9" },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("16:9 (Layar Lebar / Landscape)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Cocok untuk Presentasi & YouTube", fontSize = 9.sp)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.StayCurrentLandscape, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )

                    // 9:16 Portrait
                    FilterChip(
                        selected = selectedAspectRatio == "9:16",
                        onClick = { selectedAspectRatio = "9:16" },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("9:16 (Vertikal / Portrait)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Cocok untuk Reels, Status & TikTok", fontSize = 9.sp)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.StayCurrentPortrait, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Generate Action Button
        Button(
            onClick = {
                if (videoSubMode == 1 && selectedPhotoBitmap == null && videoPrompt.isBlank()) {
                    Toast.makeText(context, "Silakan pilih foto atau masukkan deskripsi animasi", Toast.LENGTH_SHORT).show()
                } else if (videoSubMode == 0 && videoPrompt.isBlank()) {
                    Toast.makeText(context, "Silakan tulis prompt deskripsi video", Toast.LENGTH_SHORT).show()
                } else {
                    val finalPrompt = if (videoPrompt.isNotBlank()) videoPrompt else "Smooth cinematic religious education animation"
                    viewModel.generateVideo(
                        prompt = finalPrompt,
                        inputBitmap = if (videoSubMode == 1) selectedPhotoBitmap else null,
                        aspectRatio = selectedAspectRatio
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_veo_video_button"),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(10.dp),
            enabled = !isGenerating
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Memproses dengan Veo 3...", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.MovieFilter, contentDescription = null, tint = GoldLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (videoSubMode == 1) "Animasikan Foto ke Video (Veo 3.1)" else "Generate Video dari Teks (Veo 3.1)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Realtime Progress & Status Bar
        if (isGenerating || statusText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = GreenPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = statusText.ifEmpty { "Memproses operasi video..." },
                        fontSize = 11.sp,
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Video Player & Result Card
        if (result != null && result.videoUri != null) {
            Text(
                text = "Hasil Video Veo 3 (${result.aspectRatio})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GreenPrimary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, GreenPrimary)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Video View Player
                    VeoInlineVideoPlayer(
                        videoUri = result.videoUri,
                        aspectRatio = result.aspectRatio
                    )

                    // Information badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Model: ${result.modelUsed}", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp)) }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("Rasio: ${result.aspectRatio}", fontSize = 10.sp) }
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(result.videoUri))
                                Toast.makeText(context, "Tautan video disalin ke papan klip", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salin Link", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(result.videoUri), "video/mp4")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // If no external player found, open browser
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(result.videoUri)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(browserIntent)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buka Penuh", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. GEMINI 3.1 FLASH IMAGE STUDIO (gemini-3.1-flash-image-preview)
// -------------------------------------------------------------
@Composable
fun GeminiImageStudioSection(
    viewModel: AppViewModel,
    isGenerating: Boolean,
    result: com.example.util.ImageGenerationResult?,
    initialPhotoBitmap: Bitmap? = null,
    onClearTransferredPhoto: () -> Unit,
    onAnimateWithVeo: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var imageSubMode by remember { mutableIntStateOf(if (initialPhotoBitmap != null) 1 else 0) } // 0 = New, 1 = Edit

    var imagePrompt by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("1:1") }
    var selectedImageSize by remember { mutableStateOf("1K") }
    var basePhotoBitmap by remember { mutableStateOf<Bitmap?>(initialPhotoBitmap) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream)
                        if (original != null) {
                            val maxDimension = 1024
                            val ratio = minOf(maxDimension.toFloat() / original.width, maxDimension.toFloat() / original.height, 1f)
                            basePhotoBitmap = if (ratio < 1f) {
                                Bitmap.createScaledBitmap(original, (original.width * ratio).toInt(), (original.height * ratio).toInt(), true)
                            } else {
                                original
                            }
                            imageSubMode = 1
                            Toast.makeText(context, "Foto dasar berhasil dimuat untuk diedit", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memuat foto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    LaunchedEffect(initialPhotoBitmap) {
        if (initialPhotoBitmap != null) {
            basePhotoBitmap = initialPhotoBitmap
            imageSubMode = 1
        }
    }

    val educationalImagePresets = listOf(
        "Kaligrafi Al-Qur'an Emas" to "Ornate Islamic gold calligraphy of Bismillah surrounded by geometric emerald arabesque patterns",
        "Ka'bah Senja Estetik" to "Beautiful sunset over the Holy Kaaba in Mecca with golden warm rays and peaceful sky",
        "Anak Muslim Belajar Mengaji" to "Cute cheerful Indonesian elementary school Muslim student holding Iqro book at a wooden desk, warm friendly illustration",
        "Infografis Rukun Islam" to "Minimalist elegant educational poster explaining the 5 Pillars of Islam with icons and clean typography",
        "Pemandangan Alam Ciptaan Allah" to "Majestic green mountain landscape with crystal river and flowers as reflection of divine creation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Model Indicator Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
                Column {
                    Text("Model Mesin: gemini-3.1-flash-image-preview", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenSecondary)
                    Text("Buat gambar baru & edit gambar menggunakan prompt teks AI", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // Submode Switch: Buat Gambar Baru vs Edit Gambar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = imageSubMode == 0,
                onClick = { imageSubMode = 0 },
                label = { Text("1. Buat Gambar Baru", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
                selected = imageSubMode == 1,
                onClick = { imageSubMode = 1 },
                label = { Text("2. Edit Gambar", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White)
            )
        }

        // Base Photo Upload (for Edit Mode)
        if (imageSubMode == 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (basePhotoBitmap != null) GreenPrimary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Foto yang Akan Diedit:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )

                    if (basePhotoBitmap != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                bitmap = basePhotoBitmap!!.asImageBitmap(),
                                contentDescription = "Foto Input",
                                modifier = Modifier
                                    .size(75.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, GoldAccent, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Foto Terpilih", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Gunakan prompt di bawah untuk mengubah objek, gaya, atau latar belakang.", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Ganti", fontSize = 10.sp)
                                    }
                                    TextButton(
                                        onClick = {
                                            basePhotoBitmap = null
                                            onClearTransferredPhoto()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Hapus", fontSize = 10.sp, color = Color.Red)
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.5f))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pilih Gambar untuk Diedit", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GreenPrimary)
                                Text("Ubah gaya, warna, tambahkan elemen, atau sesuaikan objek", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Quick Presets
        Column {
            Text("Preset Ide Gambar Edukasi PAI:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                educationalImagePresets.forEach { (label, prompt) ->
                    OutlinedButton(
                        onClick = { imagePrompt = prompt },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = GoldAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(label, fontSize = 11.sp, color = GreenPrimary)
                    }
                }
            }
        }

        // Prompt Input Field
        OutlinedTextField(
            value = imagePrompt,
            onValueChange = { imagePrompt = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("gemini_image_prompt_input"),
            label = {
                Text(
                    if (imageSubMode == 1) "Instruksi Perubahan / Edit Gambar" else "Deskripsi Gambar yang Ingin Dibuat"
                )
            },
            placeholder = {
                Text(
                    if (imageSubMode == 1) "Contoh: Tambahkan hiasan kubah masjid bercahaya di latar belakang, ubah gaya menjadi ilustrasi islami hangat..."
                    else "Contoh: Lukisan pemandangan masjid dengan ornamen islami elegan saat fajar..."
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Aspect Ratio & Resolution Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Aspect Ratio Selector
            Column(modifier = Modifier.weight(1.2f)) {
                Text("Aspek Rasio:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1:1", "16:9", "9:16", "4:3", "3:4").forEach { ratio ->
                        FilterChip(
                            selected = selectedAspectRatio == ratio,
                            onClick = { selectedAspectRatio = ratio },
                            label = { Text(ratio, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Size Selector
            Column(modifier = Modifier.weight(0.8f)) {
                Text("Resolusi:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("1K", "2K").forEach { size ->
                        FilterChip(
                            selected = selectedImageSize == size,
                            onClick = { selectedImageSize = size },
                            label = { Text(size, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Generate Action Button
        Button(
            onClick = {
                if (imagePrompt.isBlank() && (imageSubMode == 0 || basePhotoBitmap == null)) {
                    Toast.makeText(context, "Silakan masukkan deskripsi gambar", Toast.LENGTH_SHORT).show()
                } else {
                    val finalPrompt = if (imagePrompt.isNotBlank()) imagePrompt else "Beautiful Islamic educational art"
                    viewModel.generateImage(
                        prompt = finalPrompt,
                        inputBitmap = if (imageSubMode == 1) basePhotoBitmap else null,
                        aspectRatio = selectedAspectRatio,
                        imageSize = selectedImageSize
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_gemini_image_button"),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(10.dp),
            enabled = !isGenerating
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Memproses Gambar...", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = GoldLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (imageSubMode == 1) "Edit Gambar (gemini-3.1-flash-image-preview)" else "Buat Gambar (gemini-3.1-flash-image-preview)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Image Result Card
        if (result != null && result.bitmap != null) {
            Text(
                text = "Hasil Gambar (${result.modelUsed})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GreenPrimary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, GreenPrimary)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        bitmap = result.bitmap.asImageBitmap(),
                        contentDescription = "Hasil Gambar AI",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 340.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        text = result.description,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider()

                    // Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Animate with Veo Button
                        Button(
                            onClick = { onAnimateWithVeo(result.bitmap) },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            Icon(Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Animasikan (Veo 3)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        // Save image button
                        OutlinedButton(
                            onClick = {
                                saveBitmapToDisk(context, result.bitmap)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. MEDIA GALLERY (History of Generated Media)
// -------------------------------------------------------------
@Composable
fun MediaGallerySection(
    mediaList: List<GeneratedMediaItem>,
    onDelete: (String) -> Unit,
    onAnimateWithVeo: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var mediaTypeFilter by remember { mutableStateOf<MediaType?>(null) }

    val filteredList = remember(mediaList, mediaTypeFilter) {
        if (mediaTypeFilter == null) mediaList
        else mediaList.filter { it.type == mediaTypeFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mediaTypeFilter == null,
                onClick = { mediaTypeFilter = null },
                label = { Text("Semua (${mediaList.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
                selected = mediaTypeFilter == MediaType.VIDEO,
                onClick = { mediaTypeFilter = MediaType.VIDEO },
                label = { Text("Video Veo 3 (${mediaList.count { it.type == MediaType.VIDEO }})", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
                selected = mediaTypeFilter == MediaType.IMAGE,
                onClick = { mediaTypeFilter = MediaType.IMAGE },
                label = { Text("Gambar Gemini 3.1 (${mediaList.count { it.type == MediaType.IMAGE }})", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White)
            )
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PermMedia, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Belum ada media tersimpan", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Buat video dengan Veo 3 atau gambar dengan Gemini 3.1", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Header of item
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.type == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.Image,
                                        contentDescription = null,
                                        tint = if (item.type == MediaType.VIDEO) GoldAccent else GreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }

                            // Visual content
                            if (item.type == MediaType.VIDEO && item.videoUri != null) {
                                VeoInlineVideoPlayer(
                                    videoUri = item.videoUri,
                                    aspectRatio = item.aspectRatio
                                )
                            } else if (item.bitmap != null) {
                                Image(
                                    bitmap = item.bitmap.asImageBitmap(),
                                    contentDescription = item.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            // Prompt and Model metadata
                            Text(
                                text = "Prompt: \"${item.prompt}\"",
                                fontSize = 11.sp,
                                color = TextDark,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Model: ${item.modelUsed} • Rasio ${item.aspectRatio}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )

                                if (item.type == MediaType.IMAGE && item.bitmap != null) {
                                    TextButton(
                                        onClick = { onAnimateWithVeo(item.bitmap) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldAccent)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Animasikan (Veo)", fontSize = 10.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                    }
                                } else if (item.type == MediaType.VIDEO && item.videoUri != null) {
                                    TextButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(Uri.parse(item.videoUri), "video/mp4")
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.videoUri)).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(browserIntent)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = GreenPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Putar Penuh", fontSize = 10.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER COMPOSABLE: Inline Video Player with Animated Visual Fallback & Controls
// -------------------------------------------------------------
@Composable
fun VeoInlineVideoPlayer(
    videoUri: String,
    aspectRatio: String,
    modifier: Modifier = Modifier
) {
    val ratioFloat = if (aspectRatio == "9:16") 9f / 16f else 16f / 9f
    var isPlaying by remember { mutableStateOf(true) }
    var useAnimatedCanvasMode by remember { mutableStateOf(false) }
    var videoPrepared by remember { mutableStateOf(false) }
    var progressSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            kotlinx.coroutines.delay(50)
            progressSeconds = (progressSeconds + 0.05f) % 8f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratioFloat)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A261D))
            .clickable { isPlaying = !isPlaying },
        contentAlignment = Alignment.Center
    ) {
        if (!useAnimatedCanvasMode) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(videoUri))
                        setOnPreparedListener { mp ->
                            videoPrepared = true
                            mp.isLooping = true
                            if (isPlaying) start()
                        }
                        setOnErrorListener { _, _, _ ->
                            useAnimatedCanvasMode = true
                            true
                        }
                    }
                },
                update = { videoView ->
                    if (videoPrepared) {
                        if (isPlaying && !videoView.isPlaying) {
                            videoView.start()
                        } else if (!isPlaying && videoView.isPlaying) {
                            videoView.pause()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Rich Animated Educational Canvas Scene (Always visible while loading or in offline/canvas mode)
        if (!videoPrepared || useAnimatedCanvasMode) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val phase = (progressSeconds / 8f) * (2f * Math.PI.toFloat())

                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0A261D),
                            Color(0xFF144534),
                            Color(0xFF0F3A2C)
                        )
                    )
                )

                val cx = w * 0.5f + kotlin.math.sin(phase) * (w * 0.04f)
                val cy = h * 0.45f + kotlin.math.cos(phase) * (h * 0.03f)
                val baseRadius = minOf(w, h) * 0.24f

                // Animated golden aura rings
                for (i in 1..3) {
                    val pulse = 1f + 0.12f * kotlin.math.sin(phase + i)
                    drawCircle(
                        color = Color(0xFFD4AF37).copy(alpha = 0.14f / i),
                        radius = baseRadius * i * 0.65f * pulse,
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )
                }

                // Medallion circle
                drawCircle(
                    color = Color(0xFF1B4D3E),
                    radius = baseRadius,
                    center = androidx.compose.ui.geometry.Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFFD4AF37),
                    radius = baseRadius,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )

                // Crescent & Star
                drawCircle(
                    color = Color(0xFFF5E6AB),
                    radius = baseRadius * 0.42f,
                    center = androidx.compose.ui.geometry.Offset(cx - baseRadius * 0.06f, cy)
                )
                drawCircle(
                    color = Color(0xFF1B4D3E),
                    radius = baseRadius * 0.38f,
                    center = androidx.compose.ui.geometry.Offset(cx + baseRadius * 0.12f, cy)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.MovieFilter, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Veo 3.1 • Animasi Edukasi PAI ($aspectRatio)",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom Player Controls Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            LinearProgressIndicator(
                progress = { (progressSeconds / 8f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = GoldAccent,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = if (isPlaying) "Jeda" else "Putar",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isPlaying) "Memutar Video (0:0${progressSeconds.toInt()} / 0:08)" else "Dijeda (Ketuk untuk putar)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "720p • $aspectRatio",
                    color = GoldLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER: Save Bitmap to App Cache / Storage
// -------------------------------------------------------------
fun saveBitmapToDisk(context: Context, bitmap: Bitmap) {
    try {
        val filename = "PAI_AI_${System.currentTimeMillis()}.png"
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Toast.makeText(context, "Gambar disimpan di berkas: $filename", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal menyimpan gambar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
