package com.dinopig.piglauncher

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
internal fun PigLauncherApp(status: ModuleStatus) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val darkTheme = isSystemInDarkTheme()

    MiuixTheme(
        colors = if (darkTheme) darkColorScheme() else lightColorScheme(),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = stringResource(R.string.app_name),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp),
            ) {
                item {
                    ActivationStatusCard(
                        status = status,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(720.dp))
                }
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
        ModuleStatus.RESTART_REQUIRED -> stringResource(R.string.status_activated)
        ModuleStatus.DISABLED -> stringResource(R.string.status_not_activated)
    }

    val subtitle = when (status) {
        ModuleStatus.RESTART_REQUIRED -> stringResource(R.string.status_restart_scope)
        else -> null
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
                color = if (status == ModuleStatus.ACTIVE) {
                    MiuixTheme.colorScheme.onSurface
                } else {
                    accentColor
                },
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 48.dp),
                    fontSize = 15.sp,
                    color = accentColor,
                )
            }

            Icon(
                imageVector = when (status) {
                    ModuleStatus.ACTIVE -> Icons.Rounded.CheckCircleOutline
                    ModuleStatus.RESTART_REQUIRED -> Icons.Rounded.Warning
                    ModuleStatus.DISABLED -> Icons.Rounded.Block
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 27.dp, y = 31.dp)
                    .size(110.dp),
            )
        }
    }
}
