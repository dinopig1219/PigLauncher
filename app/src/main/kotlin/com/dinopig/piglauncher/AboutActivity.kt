package com.dinopig.piglauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinopig.piglauncher.effect.BgEffectBackground
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkTheme = isSystemInDarkTheme()

            MiuixTheme(
                colors = if (darkTheme) darkColorScheme() else lightColorScheme(),
            ) {
                AboutPage(
                    onBack = { finish() },
                )
            }
        }
    }
}

@Composable
private fun AboutPage(
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    var logoSpacerHeightPx by remember { mutableStateOf(0) }
    val backdrop = rememberLayerBackdrop()
    val blurSupported = remember { isRuntimeShaderSupported() }

    val scrollProgress by remember {
        derivedStateOf {
            if (logoSpacerHeightPx <= 0) {
                0f
            } else {
                val index = lazyListState.firstVisibleItemIndex
                val offset = lazyListState.firstVisibleItemScrollOffset

                if (index > 0) {
                    1f
                } else {
                    (offset.toFloat() / logoSpacerHeightPx).coerceIn(0f, 1f)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface),
    ) {
        BgEffectBackground(
            dynamicBackground = true,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 1f - scrollProgress
                }
                .layerBackdrop(backdrop),
        ) { }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = stringResource(R.string.about_title),
                    largeTitle = "",
                    scrollBehavior = scrollBehavior,
                    color = if (scrollProgress > 0.99f) {
                        MiuixTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    titleColor = MiuixTheme.colorScheme.onSurface.copy(
                        alpha = scrollProgress,
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        }
                    },
                )
            },
        ) { padding ->
            AboutScreen(
                scrollBehavior = scrollBehavior,
                padding = padding,
                lazyListState = lazyListState,
                scrollProgress = scrollProgress,
                onLogoSpacerHeightChanged = {
                    logoSpacerHeightPx = it
                },
                backdrop = backdrop,
                blurSupported = blurSupported,
            )
        }
    }
}

@OptIn(ExperimentalScrollBarApi::class)
@Composable
private fun AboutScreen(
    scrollBehavior: ScrollBehavior,
    padding: PaddingValues,
    lazyListState: LazyListState,
    scrollProgress: Float,
    onLogoSpacerHeightChanged: (Int) -> Unit,
    backdrop: LayerBackdrop,
    blurSupported: Boolean,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(
            context.packageName,
            0,
        )
    }
    val versionName = packageInfo.versionName.orEmpty()
    val versionCode = packageInfo.longVersionCode
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val logoBlend = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(
                    Color(0xE6A1A1A1),
                    BlurBlendMode.ColorDodge,
                ),
                BlendColorEntry(
                    Color(0x4DE6E6E6),
                    BlurBlendMode.LinearLight,
                ),
            )
        } else {
            listOf(
                BlendColorEntry(
                    Color(0xCC4A4A4A),
                    BlurBlendMode.ColorBurn,
                ),
                BlendColorEntry(
                    Color(0xFF4F4F4F),
                    BlurBlendMode.LinearLight,
                ),
            )
        }
    }
    var logoHeightDp by remember { mutableStateOf(0.dp) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = padding.calculateTopPadding() + 40.dp,
            )
            .onSizeChanged { size ->
                with(density) {
                    logoHeightDp = size.height.toDp()
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    val iconProgress = (
                        (scrollProgress - 0.35f) / 0.15f
                    ).coerceIn(0f, 1f)

                    clip = true
                    shape = RoundedCornerShape(24.dp)
                    alpha = 1f - iconProgress
                    scaleX = 1f - iconProgress * 0.05f
                    scaleY = 1f - iconProgress * 0.05f
                }
                .background(Color.White),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(74.dp),
            )
        }

        Text(
            text = stringResource(R.string.app_name),
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.Bold,
            fontSize = 35.sp,
            modifier = Modifier
                .padding(
                    top = 12.dp,
                    bottom = 5.dp,
                )
                .graphicsLayer {
                    val projectNameProgress = (
                        (scrollProgress - 0.20f) / 0.15f
                    ).coerceIn(0f, 1f)

                    alpha = 1f - projectNameProgress
                    scaleX = 1f - projectNameProgress * 0.05f
                    scaleY = 1f - projectNameProgress * 0.05f
                }
                .then(
                    if (blurSupported) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(16.dp),
                            blurRadius = 150f,
                            noiseCoefficient = BlurDefaults.NoiseCoefficient,
                            colors = BlurColors(
                                blendColors = logoBlend,
                            ),
                            contentBlendMode = BlendMode.DstIn,
                        )
                    } else {
                        Modifier
                    },
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val versionProgress = (
                        (scrollProgress - 0.05f) / 0.15f
                    ).coerceIn(0f, 1f)

                    alpha = 1f - versionProgress
                    scaleX = 1f - versionProgress * 0.05f
                    scaleY = 1f - versionProgress * 0.05f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$versionName ($versionCode)",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = 16.dp,
            ),
        ) {
            item(key = "logoSpacer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            logoHeightDp + 52.dp + 40.dp + 82.dp,
                        )
                        .onSizeChanged { size ->
                            onLogoSpacerHeightChanged(size.height)
                        },
                )
            }

            item(key = "aboutContent") {
                Column(
                    modifier = Modifier
                        .fillParentMaxHeight()
                        .padding(bottom = 16.dp),
                ) {
                    SmallTitle(
                        text = stringResource(R.string.about_links),
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.about_view_source),
                            summary = stringResource(R.string.about_view_source_summary),
                            endActions = {
                                Text(
                                    text = stringResource(R.string.github),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            },
                            onClick = {
                                uriHandler.openUri(
                                    "https://github.com/dinopig1219/PigLauncher",
                                )
                            },
                        )

                        ArrowPreference(
                            title = stringResource(R.string.about_check_update),
                            summary = stringResource(R.string.about_check_update_summary),
                            onClick = {
                                uriHandler.openUri(
                                    "https://github.com/dinopig1219/PigLauncher/releases",
                                )
                            },
                        )
                    }
                }
            }
        }

        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(lazyListState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
        )
    }
}
