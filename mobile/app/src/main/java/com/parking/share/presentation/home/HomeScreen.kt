package com.parking.share.presentation.home

import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMyPage: () -> Unit = {},
    onNavigateToHost: () -> Unit = {},
    onNavigateToGuest: () -> Unit = {}
) {
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }

    // x86 에뮬레이터만 차단 (ARM 에뮬레이터는 카카오맵 지원)
    val isEmulator = remember {
        val isX86 = Build.SUPPORTED_ABIS.none { it.startsWith("arm") }
        val isVirtualDevice = Build.FINGERPRINT.contains("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")
        isVirtualDevice && isX86
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                onMenuClick = { /* TODO: 메뉴 열기 */ },
                onProfileClick = onNavigateToMyPage
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 카카오맵 또는 플레이스홀더
            if (isEmulator || mapError != null) {
                // 에뮬레이터이거나 맵 에러 시 플레이스홀더 표시
                MapPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    message = if (isEmulator) {
                        "카카오맵은 실제 기기에서만\n사용 가능합니다.\n\n(x86 에뮬레이터 미지원)"
                    } else {
                        "지도를 불러올 수 없습니다.\n$mapError"
                    }
                )
            } else {
                KakaoMapView(
                    modifier = Modifier.fillMaxSize(),
                    onMapReady = { map -> kakaoMap = map },
                    onMapError = { error -> mapError = error }
                )
            }

            // 하단 Host/Guest 버튼
            BottomButtons(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                onHostClick = onNavigateToHost,
                onGuestClick = onNavigateToGuest
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "PARKING APP",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "메뉴"
                )
            }
        },
        actions = {
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "마이페이지"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
private fun MapPlaceholder(
    modifier: Modifier = Modifier,
    message: String
) {
    Box(
        modifier = modifier.background(Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

@Composable
private fun KakaoMapView(
    modifier: Modifier = Modifier,
    onMapReady: (KakaoMap) -> Unit,
    onMapError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = mapView.resume()
            override fun onPause(owner: LifecycleOwner) = mapView.pause()
        }

        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {
                    Log.d("KakaoMap", "Map destroyed")
                }
                override fun onMapError(error: Exception?) {
                    Log.e("KakaoMap", "Map error: ${error?.message}")
                    error?.let { onMapError(it.message ?: "Unknown error") }
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    Log.d("KakaoMap", "Map ready")
                    onMapReady(map)
                }
            }
        )

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    AndroidView(modifier = modifier, factory = { mapView })
}

@Composable
private fun BottomButtons(
    modifier: Modifier = Modifier,
    onHostClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Host 버튼
        Button(
            onClick = onHostClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A1A2E)
            )
        ) {
            Text(
                text = "Host",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Guest 버튼
        Button(
            onClick = onGuestClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6B7FD7)
            )
        ) {
            Text(
                text = "Guest",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
