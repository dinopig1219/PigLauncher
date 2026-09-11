package com.dinopig.piglauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import io.github.libxposed.service.XposedService

@Composable
internal fun PigLauncherApp(status: ModuleStatus, service: XposedService?) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val darkTheme = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()
    var showRestartDialog by remember { mutableStateOf(false) }
    var showRootRequiredDialog by remember { mutableStateOf(false) }
    var restarting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var featureSettings by remember(service) {
        mutableStateOf(FeatureSettings.load(context, service))
    }

    fun updateFeature(
        key: String,
        value: Boolean,
        update: (FeatureSettingsState) -> FeatureSettingsState,
    ) {
        featureSettings = update(featureSettings)
        FeatureSettings.set(
            context = context,
            service = service,
            key = key,
            value = value,
        )
    }

    MiuixTheme(
        colors = if (darkTheme) darkColorScheme() else lightColorScheme(),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = stringResource(R.string.app_name),
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(
                            onClick = {
                                showRestartDialog = true
                            },
                            enabled = !restarting,
                            holdDownState = showRestartDialog,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Refresh,
                                contentDescription = stringResource(R.string.restart_poco_launcher),
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(top = 8.dp),
            ) {
                item {
                    ActivationStatusCard(
                        status = status,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    )
                }
                item {
                    SmallTitle(
                        text = stringResource(R.string.feature_section_title),
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.feature_folder_dark_mode),
                            summary = stringResource(R.string.feature_folder_dark_mode_summary),
                            checked = featureSettings.folderDarkMode,
                            onCheckedChange = { checked ->
                                updateFeature(
                                    key = FeatureKeys.FOLDER_DARK_MODE,
                                    value = checked,
                                ) {
                                    it.copy(folderDarkMode = checked)
                                }
                            },
                        )

                        SwitchPreference(
                            title = stringResource(R.string.feature_advanced_textures),
                            summary = stringResource(R.string.feature_advanced_textures_summary),
                            checked = featureSettings.advancedTextures,
                            onCheckedChange = { checked ->
                                updateFeature(
                                    key = FeatureKeys.ADVANCED_TEXTURES,
                                    value = checked,
                                ) {
                                    it.copy(advancedTextures = checked)
                                }
                            },
                        )

                        SwitchPreference(
                            title = stringResource(R.string.feature_folder_adapt_icon_size),
                            summary = stringResource(R.string.feature_folder_adapt_icon_size_summary),
                            checked = featureSettings.folderAdaptIconSize,
                            onCheckedChange = { checked ->
                                updateFeature(
                                    key = FeatureKeys.FOLDER_ADAPT_ICON_SIZE,
                                    value = checked,
                                ) {
                                    it.copy(folderAdaptIconSize = checked)
                                }
                            },
                        )

                        SwitchPreference(
                            title = stringResource(R.string.feature_predictive_back_progress),
                            summary = stringResource(R.string.feature_predictive_back_progress_summary),
                            checked = featureSettings.predictiveBackProgress,
                            onCheckedChange = { checked ->
                                updateFeature(
                                    key = FeatureKeys.PREDICTIVE_BACK_PROGRESS,
                                    value = checked,
                                ) {
                                    it.copy(predictiveBackProgress = checked)
                                }
                            },
                        )

                        SwitchPreference(
                            title = stringResource(R.string.feature_all_widget_animation),
                            summary = stringResource(R.string.feature_all_widget_animation_summary),
                            checked = featureSettings.allWidgetAnimation,
                            onCheckedChange = { checked ->
                                updateFeature(
                                    key = FeatureKeys.ALL_WIDGET_ANIMATION,
                                    value = checked,
                                ) {
                                    it.copy(allWidgetAnimation = checked)
                                }
                            },
                        )
                    }
                }
            }

            OverlayDialog(
                title = stringResource(R.string.restart_poco_launcher),
                summary = stringResource(R.string.restart_poco_launcher_message),
                show = showRestartDialog,
                onDismissRequest = {
                    if (!restarting) {
                        showRestartDialog = false
                    }
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = {
                            showRestartDialog = false
                        },
                        enabled = !restarting,
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    TextButton(
                        text = stringResource(R.string.confirm),
                        onClick = {
                            showRestartDialog = false
                            restarting = true

                            coroutineScope.launch {
                                val success = withContext(Dispatchers.IO) {
                                    RootShell.restartPocoLauncher()
                                }

                                restarting = false

                                if (!success) {
                                    showRootRequiredDialog = true
                                }
                            }
                        },
                        enabled = !restarting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }

            OverlayDialog(
                title = stringResource(R.string.root_required_title),
                summary = stringResource(R.string.root_required_message),
                show = showRootRequiredDialog,
                onDismissRequest = {
                    showRootRequiredDialog = false
                },
            ) {
                TextButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        showRootRequiredDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

@Composable
private fun ActivationStatusCard(
    status: ModuleStatus,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val versionText = remember(context) {
        runCatching {
            val packageInfo = context.packageManager
                .getPackageInfo(context.packageName, 0)

            "${packageInfo.versionName.orEmpty()} (${packageInfo.longVersionCode})"
        }.getOrDefault("")
    }

    val cardColor = when (status) {
        ModuleStatus.ACTIVE -> if (darkTheme) Color(0xFF173923) else Color(0xFFDFFAE4)
        ModuleStatus.RESTART_REQUIRED -> if (darkTheme) Color(0xFF463907) else Color(0xFFFFF0C7)
        ModuleStatus.DISABLED -> if (darkTheme) Color(0xFF430D11) else Color(0xFFF8E2E2)
    }

    val accentColor = when (status) {
        ModuleStatus.ACTIVE -> if (darkTheme) Color(0xFF62D783) else Color(0xFF36D167)
        ModuleStatus.RESTART_REQUIRED -> if (darkTheme) Color(0xFFFFB83E) else Color(0xFFE89900)
        ModuleStatus.DISABLED -> if (darkTheme) Color(0xFFFF4D57) else Color(0xFFD93643)
    }

    val title = when (status) {
        ModuleStatus.ACTIVE -> stringResource(R.string.status_activated)
        ModuleStatus.RESTART_REQUIRED -> stringResource(R.string.status_restart_scope)
        ModuleStatus.DISABLED -> stringResource(R.string.status_not_activated)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.defaultColors(color = cardColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 14.dp),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
            )

            if (versionText.isNotEmpty()) {
                Text(
                    text = versionText,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 43.dp),
                    fontSize = 15.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = stringResource(R.string.xposed_api_version, 102),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 12.dp),
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )

            StatusSymbol(
                status = status,
                color = accentColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 27.dp, y = 31.dp)
                    .size(110.dp),
            )
        }
    }
}

@Composable
private fun StatusSymbol(
    status: ModuleStatus,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = size.minDimension * 0.075f

        when (status) {
            ModuleStatus.ACTIVE -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.34f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.31f, h * 0.51f),
                    end = Offset(w * 0.44f, h * 0.63f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.44f, h * 0.63f),
                    end = Offset(w * 0.70f, h * 0.36f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            ModuleStatus.RESTART_REQUIRED -> {
                val path = Path().apply {
                    moveTo(w * 0.50f, h * 0.17f)
                    lineTo(w * 0.84f, h * 0.78f)
                    lineTo(w * 0.16f, h * 0.78f)
                    close()
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = stroke,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.50f, h * 0.37f),
                    end = Offset(w * 0.50f, h * 0.56f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = color,
                    radius = stroke * 0.55f,
                    center = Offset(w * 0.50f, h * 0.68f),
                )
            }

            ModuleStatus.DISABLED -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.34f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.28f, h * 0.72f),
                    end = Offset(w * 0.72f, h * 0.28f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
