package com.example.bughunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E24)
                ) {
                    BugHunterGame()
                }
            }
        }
    }
}

@Composable
fun BugHunterGame() {
    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(30) }
    var isPlaying by remember { mutableStateOf(false) }

    var bugX by remember { mutableStateOf(0f) }
    var bugY by remember { mutableStateOf(0f) }

    // Игровой цикл для перемещения бага
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (timeLeft > 0) {
                delay(800)
                // Ограничиваем координаты от -0.8 до 0.8, чтобы баг не вылетал за края экрана
                bugX = Random.nextFloat() * 1.6f - 0.8f
                bugY = Random.nextFloat() * 1.6f - 0.8f
            }
        }
    }

    // Игровой таймер
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            isPlaying = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isPlaying) {
            // Начальный экран и экран окончания игры
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (timeLeft == 0) "Игра окончена!" else "Охотник за багами",
                    fontSize = 32.sp,
                    color = Color.White
                )
                if (timeLeft == 0) {
                    Text(
                        text = "Поймано багов: $score",
                        fontSize = 24.sp,
                        color = Color.Green,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Button(
                    onClick = {
                        score = 0
                        timeLeft = 30
                        isPlaying = true
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = if (timeLeft == 0) "Играть снова" else "Старт", fontSize = 20.sp)
                }
            }
        } else {
            // Игровой процесс
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Баги: $score", fontSize = 24.sp, color = Color.White)
                Text(text = "Время: $timeLeft сек", fontSize = 24.sp, color = Color.Red)
            }

            // Мишень (Баг)
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(BiasAlignment(bugX, bugY))
                        .size(70.dp)
                        .background(Color(0xFFFF5252), shape = CircleShape)
                        .clickable {
                            score++
                            // Мгновенный перенос при успешном тапе
                            bugX = Random.nextFloat() * 1.6f - 0.8f
                            bugY = Random.nextFloat() * 1.6f - 0.8f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🐛", fontSize = 36.sp)
                }
            }
        }
    }
}
