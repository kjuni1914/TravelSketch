package com.travelsketch.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.*
import com.travelsketch.R

@Composable
fun CelebrationDialog(
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }

    if (isPlaying) {
        Dialog(
            onDismissRequest = {
                isPlaying = false
                onDismiss()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.8f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val backgroundComposition by rememberLottieComposition(
                            LottieCompositionSpec.RawRes(R.raw.confetti)
                        )

                        val backgroundAnimatable by animateLottieCompositionAsState(
                            composition = backgroundComposition,
                            iterations = LottieConstants.IterateForever,
                            isPlaying = true,
                            restartOnPlay = true,
                            speed = 0.8f
                        )

                        LottieAnimation(
                            composition = backgroundComposition,
                            progress = { backgroundAnimatable },
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(y = 50.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Spacer(modifier = Modifier.height(32.dp))

//                            val foregroundComposition by rememberLottieComposition(
//                                LottieCompositionSpec.RawRes(R.raw.confetti)
//                            )
//
//                            val foregroundAnimatable by animateLottieCompositionAsState(
//                                composition = foregroundComposition,
//                                iterations = LottieConstants.IterateForever,
//                                isPlaying = true,
//                                restartOnPlay = false,
//                                speed = 1f
//                            )
//
//                            LottieAnimation(
//                                composition = foregroundComposition,
//                                progress = { foregroundAnimatable },
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .weight(1f)
//                                    .offset(y = 50.dp)
//                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Welcome to TravelSketch!",
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Start your journey with us",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(56.dp),
                                    onClick = {
                                        isPlaying = false
                                        onDismiss()
                                    }
                                ) {
                                    Text(
                                        "Let's Begin!",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}