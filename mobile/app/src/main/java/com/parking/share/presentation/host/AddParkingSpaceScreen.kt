package com.parking.share.presentation.host

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.util.Calendar

private val BRAND_COLOR = Color(0xFF1A1A2E)

private val POSTCODE_HTML = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
*{margin:0;padding:0;box-sizing:border-box;}
body{overflow:hidden;}
#wrap{position:fixed;top:0;left:0;right:0;bottom:0;}
</style>
</head>
<body>
<div id="wrap"></div>
<script src="//t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
<script>
new kakao.Postcode({
    width:'100%',
    height:'100%',
    oncomplete:function(data){
        AndroidBridge.onAddressSelected(data.roadAddress||data.address);
    }
}).embed(document.getElementById('wrap'));
</script>
</body>
</html>"""

private fun todayStr(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

private class AddressBridge(private val callback: (String) -> Unit) {
    @JavascriptInterface
    fun onAddressSelected(address: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            callback(address)
        }
    }
}

private val VEHICLE_TYPES = listOf(
    "sedan" to "승용차",
    "suv" to "SUV",
    "van" to "승합차",
    "truck" to "화물차",
    "motorcycle" to "오토바이"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddParkingSpaceScreen(
    viewModel: AddParkingSpaceViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onSuccess()
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("주차 공간 등록", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 위치
                FormSection("위치") {
                    AddressSearchField(
                        address = uiState.address,
                        onSearchClick = { viewModel.onShowAddressSearch(true) }
                    )
                }

                // 운영 날짜
                FormSection("운영 날짜 및 요금") {
                    Text("운영할 날짜를 탭하여 선택하세요", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    MonthCalendar(
                        year = uiState.calendarYear,
                        month = uiState.calendarMonth,
                        selectedDates = uiState.selectedDates.keys,
                        todayStr = todayStr(),
                        onPrevMonth = viewModel::onPrevMonth,
                        onNextMonth = viewModel::onNextMonth,
                        onDateToggle = viewModel::onDateToggle
                    )
                    if (uiState.selectedDates.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color(0xFFE0E0E0))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("선택한 날짜별 운영 시간 및 요금", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        uiState.selectedDates.entries.sortedBy { it.key }.forEach { (dateStr, slot) ->
                            SelectedDateRow(
                                dateStr = dateStr,
                                slot = slot,
                                onStartTimeChange = { viewModel.onStartTimeChange(dateStr, it) },
                                onEndTimeChange = { viewModel.onEndTimeChange(dateStr, it) },
                                onHourlyRateChange = { viewModel.onHourlyRateChange(dateStr, it) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                // 허용 차종
                FormSection("허용 차종 (선택)") {
                    Text("주차 가능한 차종을 선택하세요 (미선택 시 모두 허용)", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VEHICLE_TYPES.forEach { (code, label) ->
                            FilterChip(
                                selected = code in uiState.selectedVehicleTypes,
                                onClick = { viewModel.onVehicleTypeToggle(code) },
                                label = { Text(label, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BRAND_COLOR,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // 사진
                FormSection("사진 (선택)") {
                    ImagePickerSection(
                        imageUris = uiState.selectedImageUris,
                        onImagesSelected = viewModel::onImagesSelected
                    )
                }

                // 상세 설명
                FormSection("상세 설명 (선택)") {
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("설명") },
                        placeholder = { Text("주차 공간에 대한 추가 설명을 입력하세요") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                uiState.error?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = error, color = Color(0xFFC62828), fontSize = 14.sp, modifier = Modifier.padding(12.dp))
                    }
                }

                Button(
                    onClick = viewModel::createParkingSpace,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BRAND_COLOR)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("등록하기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.isLoading) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.3f)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
        } // end Scaffold

        if (uiState.showAddressSearch) {
            PostcodeOverlay(
                onAddressSelected = viewModel::onAddressSelected,
                onDismiss = { viewModel.onShowAddressSearch(false) }
            )
        }
    }
}

@Composable
private fun ImagePickerSection(
    imageUris: List<Uri>,
    onImagesSelected: (List<Uri>) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) onImagesSelected(uris)
    }

    OutlinedButton(
        onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BRAND_COLOR)
    ) {
        Text(if (imageUris.isEmpty()) "사진 선택" else "${imageUris.size}장 선택됨 (변경하기)")
    }

    if (imageUris.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(imageUris) { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = BRAND_COLOR)
        Divider(color = Color(0xFFE0E0E0))
        content()
    }
}

@Composable
private fun AddressSearchField(address: String, onSearchClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onSearchClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BRAND_COLOR)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("주소 검색")
        }
        if (address.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(text = address, fontSize = 14.sp, color = Color.DarkGray)
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    year: Int,
    month: Int,
    selectedDates: Set<String>,
    todayStr: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateToggle: (String) -> Unit
) {
    val cal = remember(year, month) {
        Calendar.getInstance().apply { set(year, month - 1, 1) }
    }
    val firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "이전 달")
            }
            Text("${year}년 ${month}월", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "다음 달")
            }
        }

        val dayNames = listOf("일", "월", "화", "수", "목", "금", "토")
        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEachIndexed { index, name ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        color = when (index) { 0 -> Color.Red; 6 -> Color(0xFF1565C0); else -> Color.Gray },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val totalCells = firstDow + daysInMonth
        val weeks = (totalCells + 6) / 7
        var dayCounter = 1

        for (week in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = week * 7 + col
                    if (cellIndex < firstDow || dayCounter > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).height(40.dp))
                    } else {
                        val day = dayCounter
                        val dateStr = "%04d-%02d-%02d".format(year, month, day)
                        val isSelected = dateStr in selectedDates
                        val isPast = dateStr < todayStr

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) BRAND_COLOR else Color.Transparent)
                                .clickable(enabled = !isPast) { onDateToggle(dateStr) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$day",
                                fontSize = 14.sp,
                                color = when {
                                    isSelected -> Color.White
                                    isPast -> Color(0xFFBDBDBD)
                                    col == 0 -> Color.Red
                                    col == 6 -> Color(0xFF1565C0)
                                    else -> Color.Black
                                }
                            )
                        }
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDateRow(
    dateStr: String,
    slot: DateTimeSlot,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onHourlyRateChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = dateStr.substring(5).replace("-", "/"),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = BRAND_COLOR
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("시간", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(36.dp))
            TimePickerButton(time = slot.startTime, onTimeSelected = onStartTimeChange)
            Text(" ~ ", color = Color.Gray, fontSize = 13.sp)
            TimePickerButton(time = slot.endTime, onTimeSelected = onEndTimeChange)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("요금", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(36.dp))
            OutlinedTextField(
                value = slot.hourlyRate,
                onValueChange = onHourlyRateChange,
                placeholder = { Text("시간당 요금", fontSize = 12.sp) },
                modifier = Modifier.weight(1f).height(48.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(6.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                trailingIcon = {
                    Text("원/시간", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                }
            )
        }
    }
}

@Composable
private fun TimePickerButton(time: String, onTimeSelected: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val hour = remember(time) { time.split(":")[0].toInt() }

    TextButton(
        onClick = { showPicker = true },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(time, color = BRAND_COLOR, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }

    if (showPicker) {
        HourPickerDialog(
            currentHour = hour,
            onHourSelected = { h ->
                onTimeSelected("%02d:00".format(h))
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourPickerDialog(
    currentHour: Int,
    onHourSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentHour - 2).coerceAtLeast(0))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("시간 선택", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(state = listState, modifier = Modifier.height(240.dp)) {
                items((0..23).toList()) { h ->
                    val isSelected = h == currentHour
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) BRAND_COLOR else Color.Transparent)
                            .clickable { onHourSelected(h) }
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = "%02d:00".format(h),
                            color = if (isSelected) Color.White else Color.Black,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PostcodeOverlay(
    onAddressSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("주소 검색", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = onDismiss) { Text("닫기") }
                }
                Divider()
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(false)
                            }
                            webViewClient = WebViewClient()
                            webChromeClient = android.webkit.WebChromeClient()
                            addJavascriptInterface(
                                AddressBridge { address -> onAddressSelected(address) },
                                "AndroidBridge"
                            )
                            loadDataWithBaseURL(
                                "https://postcode.map.kakao.com",
                                POSTCODE_HTML,
                                "text/html",
                                "utf-8",
                                null
                            )
                        }
                    }
                )
            }
        }
    }
}
