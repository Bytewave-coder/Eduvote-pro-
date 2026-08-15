package com.example.ui.screens.voter

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.ui.theme.PrimaryBlue

@Composable
fun VotingSuccessScreen(
    useRealEvmSound: Boolean,
    onComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val scale = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val alpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        if (!useRealEvmSound) {
            com.example.ui.SoundPlayer.playSuccess(context)
        }
        delay(4000) // Increase delay a bit to let the user enjoy the animation
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F5132)), // Dark green background
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale.value * pulseScale)
                    .background(Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(90.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                com.example.ui.Translator.tr("Vote Cast Successfully!"),
                color = Color.White.copy(alpha = alpha.value),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.scale(scale.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                com.example.ui.Translator.tr("Thank you for voting."),
                color = Color.White.copy(alpha = alpha.value * 0.9f),
                fontSize = 18.sp,
                modifier = Modifier.scale(scale.value)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                com.example.ui.Translator.tr("You have successfully cast your vote.\nYou cannot vote again."),
                color = Color.White.copy(alpha = alpha.value * 0.7f),
                fontSize = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.scale(scale.value)
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
                    .scale(scale.value),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(com.example.ui.Translator.tr("Return to Login"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
