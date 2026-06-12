package com.karyar.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scaleAnim = remember { Animatable(0.5f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, animationSpec = tween(350, easing = EaseOutBack))
        alphaAnim.animateTo(1f, animationSpec = tween(250))
        delay(400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "کاریار",
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                        clip = true
                        shape = RoundedCornerShape(24.dp)
                    }
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "کاریار",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(alphaAnim.value)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "مدیریت کارهای روزانه",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alphaAnim.value)
            )
        }
    }
}
