package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import android.media.MediaPlayer
import android.util.Log
import com.example.R
import com.example.data.TelegramService
import kotlin.random.Random

@Composable
fun BlockedScreen(onUnblocked: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        var mediaPlayer: MediaPlayer? = null
        try {
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            try {
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC), 0)
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM), 0)
            } catch (e: Exception) {}

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val afd = context.resources.openRawResourceFd(R.raw.system_is_corrupted)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                isLooping = true
                setVolume(1.0f, 1.0f)
                start()
            }
        } catch (e: Exception) {
            Log.e("BlockedScreen", "Error playing audio, falling back", e)
            try {
                mediaPlayer = MediaPlayer.create(context, R.raw.system_is_corrupted)
                mediaPlayer?.isLooping = true
                mediaPlayer?.setVolume(1.0f, 1.0f)
                mediaPlayer?.start()
            } catch (e2: Exception) {
                Log.e("BlockedScreen", "Fallback audio failed", e2)
            }
        }

        onDispose {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                }
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {}
        }
    }



    var glitchTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            glitchTrigger = !glitchTrigger
            delay(Random.nextLong(100, 800))
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val translationX by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0000))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red.copy(alpha = if (glitchTrigger) 0.05f else 0.15f * bgAlpha))
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    this.translationX = if (glitchTrigger) translationX else 0f
                }
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .background(Color(0xFF1A0000))
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = com.example.R.drawable.ic_launcher_foreground), // Default Android Icon
                    contentDescription = "Android Bot",
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        alpha = if (glitchTrigger) 0.8f else 1f
                    },
                    tint = Color.Red
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "SYSTEM CORRUPTED",
                color = Color.Red,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "ACCESS DENIED",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Color.Red).padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "SECURITY BREACH DETECTED.\nUNAUTHORIZED ACCESS LOGGED.",
                color = Color.Red.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Students are strictly prohibited from accessing this application. Only authorized staff, teachers, and developers may proceed. Your device has been flagged.",
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .border(1.dp, Color.Red, RoundedCornerShape(8.dp))
                    .background(Color.Red.copy(alpha = 0.1f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "DEVICE ID: ${TelegramService.DEVICE_ID}",
                    color = Color.Red,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
