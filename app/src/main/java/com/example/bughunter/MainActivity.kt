package com.example.bughunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E1E) // Темная индустриальная тема
                ) {
                    FactorioGame()
                }
            }
        }
    }
}

@Composable
fun FactorioGame() {
    // Ресурсы
    var ironOre by remember { mutableStateOf(0) }
    var copperOre by remember { mutableStateOf(0) }
    var ironPlates by remember { mutableStateOf(0) }
    var copperPlates by remember { mutableStateOf(0) }
    var circuits by remember { mutableStateOf(0) }
    var sciencePacks by remember { mutableStateOf(0) }

    // Здания автоматизации
    var minersIron by remember { mutableStateOf(0) }
    var minersCopper by remember { mutableStateOf(0) }
    var furnacesIron by remember { mutableStateOf(0) }
    var furnacesCopper by remember { mutableStateOf(0) }
    var assemblersCircuits by remember { mutableStateOf(0) }
    var assemblersScience by remember { mutableStateOf(0) }

    // Состояние космоса
    var hasSilo by remember { mutableStateOf(false) }
    var gameWon by remember { mutableStateOf(false) }

    // Выбранная вкладка (0 - Сбор, 1 - Автоматизация, 2 - Космос)
    var selectedTab by remember { mutableStateOf(0) }

    // Игровой цикл фабрики (работает раз в секунду)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (!gameWon) {
                // 1. Плавление руды в печи
                val ironToSmelt = minOf(furnacesIron, ironOre)
                ironOre -= ironToSmelt
                ironPlates += ironToSmelt

                val copperToSmelt = minOf(furnacesCopper, copperOre)
                copperOre -= copperToSmelt
                copperPlates += copperToSmelt

                // 2. Сборка микросхем (требует 1 железную и 1 медную плиту на штуку)
                val circuitsToCraft = minOf(assemblersCircuits, ironPlates, copperPlates)
                ironPlates -= circuitsToCraft
                copperPlates -= circuitsToCraft
                circuits += circuitsToCraft

                // 3. Сборка колб (требует 1 железную плиту и 1 микросхему)
                val scienceToCraft = minOf(assemblersScience, ironPlates, circuits)
                ironPlates -= scienceToCraft
                circuits -= scienceToCraft
                sciencePacks += scienceToCraft

                // 4. Автоматическая добыча руды бурами
                ironOre += minersIron
                copperOre += minersCopper
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Верхняя панель ресурсов
        ResourcePanel(
            ironOre, copperOre, ironPlates, copperPlates, circuits, sciencePacks
        )

        // Переключатель вкладок
        TabSelector(selectedTab) { selectedTab = it }

        Spacer(modifier = Modifier.height(8.dp))

        // Содержимое вкладок
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            if (gameWon) {
                VictoryScreen {
                    // Сброс прогресса
                    ironOre = 0; copperOre = 0; ironPlates = 0; copperPlates = 0
                    circuits = 0; sciencePacks = 0
                    minersIron = 0; minersCopper = 0; furnacesIron = 0; furnacesCopper = 0
                    assemblersCircuits = 0; assemblersScience = 0
                    hasSilo = false; gameWon = false; selectedTab = 0
                }
            } else {
                when (selectedTab) {
                    0 -> ManualMiningTab(
                        ironOre, copperOre, ironPlates, copperPlates, circuits,
                        onMineIron = { ironOre++ },
                        onMineCopper = { copperOre++ },
                        onSmeltIron = { if (ironOre >= 1) { ironOre--; ironPlates++ } },
                        onSmeltCopper = { if (copperOre >= 1) { copperOre--; copperPlates++ } },
                        onCraftCircuit = { if (ironPlates >= 1 && copperPlates >= 1) { ironPlates--; copperPlates--; circuits++ } },
                        onCraftScience = { if (ironPlates >= 1 && circuits >= 1) { ironPlates--; circuits--; sciencePacks++ } }
                    )
                    1 -> AutomationTab(
                        ironPlates, copperPlates, circuits,
                        minersIron, minersCopper, furnacesIron, furnacesCopper, assemblersCircuits, assemblersScience,
                        onBuyIronMiner = { ironPlates -= 10; minersIron++ },
                        onBuyCopperMiner = { ironPlates -= 10; minersCopper++ },
                        onBuyIronSmelter = { ironPlates -= 15; furnacesIron++ },
                        onBuyCopperSmelter = { ironPlates -= 15; copperPlates -= 5; furnacesCopper++ },
                        onBuyCircuitAssembler = { ironPlates -= 30; copperPlates -= 10; assemblersCircuits++ },
                        onBuyScienceAssembler = { ironPlates -= 50; circuits -= 20; assemblersScience++ }
                    )
                    2 -> SpaceTab(
                        ironPlates, circuits, sciencePacks, hasSilo,
                        onBuildSilo = { sciencePacks -= 100; hasSilo = true },
                        onLaunchRocket = {
                            ironPlates -= 200
                            circuits -= 150
                            sciencePacks -= 100
                            gameWon = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ResourceItem(icon: String, name: String, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xFF2C2C2C), shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
            .width(90.dp)
    ) {
        Text(text = icon, fontSize = 24.sp)
        Text(text = name, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(text = count.toString(), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ResourcePanel(
    ironOre: Int, copperOre: Int, ironPlates: Int, copperPlates: Int, circuits: Int, sciencePacks: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151515))
            .padding(16.dp)
    ) {
        Text(
            text = "⚡ СКЛАД ФАБРИКИ",
            color = Color(0xFFE67E22), // Оранжевый цвет Factorio
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ResourceItem("🪨", "Жел. руда", ironOre)
            ResourceItem("🟫", "Мед. руда", copperOre)
            ResourceItem("🪙", "Жел. плита", ironPlates)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ResourceItem("🟧", "Мед. плита", copperPlates)
            ResourceItem("🔌", "Схемы", circuits)
            ResourceItem("🧪", "Наука", sciencePacks)
        }
    }
}

@Composable
fun TabSelector(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151515))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val tabs = listOf("⛏ Сбор", "⚙ Фабрика", "🚀 Космос")
        tabs.forEachIndexed { index, title ->
            Button(
                onClick = { onTabSelected(index) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == index) Color(0xFFE67E22) else Color(0xFF333333)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text(text = title, fontSize = 12.sp, maxLines = 1, color = Color.White)
            }
        }
    }
}

@Composable
fun ManualMiningTab(
    ironOre: Int, copperOre: Int, ironPlates: Int, copperPlates: Int, circuits: Int,
    onMineIron: () -> Unit,
    onMineCopper: () -> Unit,
    onSmeltIron: () -> Unit,
    onSmeltCopper: () -> Unit,
    onCraftCircuit: () -> Unit,
    onCraftScience: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Ручная добыча и крафт",
            color = Color.LightGray,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onMineIron,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                modifier = Modifier.weight(1f).height(60.dp)
            ) {
                Text("Добыть Жел. руду\n(+1 🪨)", color = Color.White, fontSize = 11.sp)
            }
            Button(
                onClick = onMineCopper,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5A2B)),
                modifier = Modifier.weight(1f).height(60.dp)
            ) {
                Text("Добыть Мед. руду\n(+1 🟫)", color = Color.White, fontSize = 11.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSmeltIron,
                enabled = ironOre >= 1,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E5D6C)),
                modifier = Modifier.weight(1f).height(60.dp)
            ) {
                Text("Переплавить Жел.\n(Нужно: 1 🪨)", fontSize = 11.sp)
            }
            Button(
                onClick = onSmeltCopper,
                enabled = copperOre >= 1,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E5E38)),
                modifier = Modifier.weight(1f).height(60.dp)
            ) {
                Text("Переплавить Медь\n(Нужно: 1 🟫)", fontSize = 11.sp)
            }
        }

        Button(
            onClick = onCraftCircuit,
            enabled = ironPlates >= 1 && copperPlates >= 1,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Собрать Эл. схему 🔌 (Нужно: 1 🪙 + 1 🟧)")
        }

        Button(
            onClick = onCraftScience,
            enabled = ironPlates >= 1 && circuits >= 1,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Собрать Научную колбу 🧪 (Нужно: 1 🪙 + 1 🔌)")
        }
    }
}

@Composable
fun AutomationTab(
    ironPlates: Int, copperPlates: Int, circuits: Int,
    minersIron: Int, minersCopper: Int, furnacesIron: Int, furnacesCopper: Int, assemblersCircuits: Int, assemblersScience: Int,
    onBuyIronMiner: () -> Unit,
    onBuyCopperMiner: () -> Unit,
    onBuyIronSmelter: () -> Unit,
    onBuyCopperSmelter: () -> Unit,
    onBuyCircuitAssembler: () -> Unit,
    onBuyScienceAssembler: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Автоматизация производства",
            color = Color.LightGray,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        AutomationCard(
            title = "Буровая установка (Железо)",
            desc = "Добывает 1 Железную руду в сек.",
            count = minersIron,
            costText = "Цена: 10 Железных плит 🪙",
            enabled = ironPlates >= 10,
            onBuy = onBuyIronMiner
        )

        AutomationCard(
            title = "Буровая установка (Медь)",
            desc = "Добывает 1 Медную руду в сек.",
            count = minersCopper,
            costText = "Цена: 10 Железных плит 🪙",
            enabled = ironPlates >= 10,
            onBuy = onBuyCopperMiner
        )

        AutomationCard(
            title = "Каменная печь (Железо)",
            desc = "Плавит до 1 Железной руды в Плиту в сек.",
            count = furnacesIron,
            costText = "Цена: 15 Железных плит 🪙",
            enabled = ironPlates >= 15,
            onBuy = onBuyIronSmelter
        )

        AutomationCard(
            title = "Каменная печь (Медь)",
            desc = "Плавит до 1 Медной руды в Плиту в сек.",
            count = furnacesCopper,
            costText = "Цена: 15 Жел. плит 🪙 + 5 Мед. плит 🟧",
            enabled = ironPlates >= 15 && copperPlates >= 5,
            onBuy = onBuyCopperSmelter
        )

        AutomationCard(
            title = "Сборочный автомат (Схемы)",
            desc = "Собирает Схемы из 1 Жел. и 1 Мед. плиты в сек.",
            count = assemblersCircuits,
            costText = "Цена: 30 Жел. плит 🪙 + 10 Мед. плит 🟧",
            enabled = ironPlates >= 30 && copperPlates >= 10,
            onBuy = onBuyCircuitAssembler
        )

        AutomationCard(
            title = "Сборочный автомат (Наука)",
            desc = "Собирает Колбы из 1 Жел. плиты и 1 Схемы в сек.",
            count = assemblersScience,
            costText = "Цена: 50 Жел. плит 🪙 + 20 Схем 🔌",
            enabled = ironPlates >= 50 && circuits >= 20,
            onBuy = onBuyScienceAssembler
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AutomationCard(
    title: String, desc: String, count: Int, costText: String, enabled: Boolean, onBuy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF292929), shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = desc, color = Color.Gray, fontSize = 11.sp)
                Text(text = costText, color = Color(0xFFE67E22), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Активно: $count", color = Color.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onBuy,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD35400)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Купить", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun SpaceTab(
    ironPlates: Int, circuits: Int, sciencePacks: Int, hasSilo: Boolean,
    onBuildSilo: () -> Unit,
    onLaunchRocket: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasSilo) {
            Text(
                text = "Постройте ракетную шахту!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Шахта необходима для сборки и запуска исследовательской ракеты.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
            )
            Button(
                onClick = onBuildSilo,
                enabled = sciencePacks >= 100,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE67E22)),
                modifier = Modifier.height(60.dp).fillMaxWidth(0.8f)
            ) {
                Text("Построить шахту (100 Колб 🧪)", fontSize = 14.sp)
            }
        } else {
            Text(
                text = "Шахта построена! 🚀",
                color = Color.Green,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Подготовьте ракету и отправьте ее к звездам.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)
            )
            Button(
                onClick = onLaunchRocket,
                enabled = ironPlates >= 200 && circuits >= 150 && sciencePacks >= 100,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                modifier = Modifier.height(70.dp).fillMaxWidth(0.9f)
            ) {
                Text(
                    text = "ЗАПУСТИТЬ РАКЕТУ!\n(Цена: 200 🪙, 150 🔌, 100 🧪)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VictoryScreen(onRestart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🎉 РАКЕТА ЗАПУЩЕНА! 🎉", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Green)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Вы успешно автоматизировали фабрику и покорили космос. Миссия Factorio завершена!",
            color = Color.LightGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRestart,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE67E22))
        ) {
            Text("Начать заново", fontSize = 16.sp)
        }
    }
}
