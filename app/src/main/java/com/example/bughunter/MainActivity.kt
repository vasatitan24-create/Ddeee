package com.example.bughunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
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
                    color = Color(0xFF1E1E1E)
                ) {
                    GridFactorioGame()
                }
            }
        }
    }
}

// Типы предметов на конвейерах и заводах
enum class ItemType(val emoji: String) {
    IRON_ORE("🪨"),
    COPPER_ORE("🟫"),
    IRON_PLATE("🪙"),
    COPPER_PLATE("🟧"),
    CIRCUIT("🔌")
}

// Типы строений и клеток
enum class TileType(val displayName: String, val icon: String) {
    EMPTY("Пусто", "⬛"),
    ORE_IRON("Залежи железа", "🪨"),
    ORE_COPPER("Залежи меди", "🟫"),
    MINER("Бур", "⛏️"),
    FURNACE("Печь", "🔥"),
    ASSEMBLER("Сборщик", "🏭"),
    LAB("Лаборатория", "🧪"),
    BELT_UP("Конвейер Вверх", "⬆️"),
    BELT_DOWN("Конвейер Вниз", "⬇️"),
    BELT_LEFT("Конвейер Влево", "⬅️"),
    BELT_RIGHT("Конвейер Вправо", "➡️")
}

// Класс клетки игрового поля
data class GridTile(
    val x: Int,
    val y: Int,
    val baseType: TileType, // Ресурс под клеткой (EMPTY, ORE_IRON, ORE_COPPER)
    var building: TileType = TileType.EMPTY,
    val inventory: MutableMap<ItemType, Int> = mutableStateMapOf()
)

@Composable
fun GridFactorioGame() {
    // Кошелек игрока (Рюкзак)
    var walletIronOre by remember { mutableStateOf(0) }
    var walletCopperOre by remember { mutableStateOf(0) }
    var walletIronPlates by remember { mutableStateOf(15) } // Даем стартовые плиты для первых построек
    var walletCopperPlates by remember { mutableStateOf(10) }
    var walletCircuits by remember { mutableStateOf(0) }
    var globalScience by remember { mutableStateOf(0) }

    // Выбранная клетка для постройки/управления
    var selectedTile by remember { mutableStateOf<GridTile?>(null) }

    // Генерация карты 5x5
    val gridState = remember {
        mutableStateListOf<GridTile>().apply {
            for (y in 0 until 5) {
                for (x in 0 until 5) {
                    val baseType = when {
                        (x == 0 && y == 1) || (x == 1 && y == 0) -> TileType.ORE_IRON
                        (x == 3 && y == 4) || (x == 4 && y == 3) -> TileType.ORE_COPPER
                        else -> TileType.EMPTY
                    }
                    add(GridTile(x, y, baseType))
                }
            }
        }
    }

    // Вспомогательная функция проверки: принимает ли здание данный ресурс
    fun canAcceptItem(building: TileType, item: ItemType, currentCount: Int): Boolean {
        if (currentCount >= 5) return false // Ограничение буфера входа в 5 единиц
        return when (building) {
            TileType.FURNACE -> item == ItemType.IRON_ORE || item == ItemType.COPPER_ORE
            TileType.ASSEMBLER -> item == ItemType.IRON_PLATE || item == ItemType.COPPER_PLATE
            TileType.LAB -> item == ItemType.CIRCUIT || item == ItemType.IRON_PLATE
            TileType.BELT_UP, TileType.BELT_DOWN, TileType.BELT_LEFT, TileType.BELT_RIGHT -> true
            else -> false
        }
    }

    // Игровой такт (автоматизация раз в секунду)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)

            // 1. Производство на фабриках
            gridState.forEach { tile ->
                when (tile.building) {
                    TileType.MINER -> {
                        if (tile.baseType == TileType.ORE_IRON && (tile.inventory[ItemType.IRON_ORE] ?: 0) < 5) {
                            tile.inventory[ItemType.IRON_ORE] = (tile.inventory[ItemType.IRON_ORE] ?: 0) + 1
                        }
                        if (tile.baseType == TileType.ORE_COPPER && (tile.inventory[ItemType.COPPER_ORE] ?: 0) < 5) {
                            tile.inventory[ItemType.COPPER_ORE] = (tile.inventory[ItemType.COPPER_ORE] ?: 0) + 1
                        }
                    }
                    TileType.FURNACE -> {
                        val oresIron = tile.inventory[ItemType.IRON_ORE] ?: 0
                        val oresCopper = tile.inventory[ItemType.COPPER_ORE] ?: 0
                        if (oresIron > 0 && (tile.inventory[ItemType.IRON_PLATE] ?: 0) < 5) {
                            tile.inventory[ItemType.IRON_ORE] = oresIron - 1
                            tile.inventory[ItemType.IRON_PLATE] = (tile.inventory[ItemType.IRON_PLATE] ?: 0) + 1
                        } else if (oresCopper > 0 && (tile.inventory[ItemType.COPPER_PLATE] ?: 0) < 5) {
                            tile.inventory[ItemType.COPPER_ORE] = oresCopper - 1
                            tile.inventory[ItemType.COPPER_PLATE] = (tile.inventory[ItemType.COPPER_PLATE] ?: 0) + 1
                        }
                    }
                    TileType.ASSEMBLER -> {
                        val platesIron = tile.inventory[ItemType.IRON_PLATE] ?: 0
                        val platesCopper = tile.inventory[ItemType.COPPER_PLATE] ?: 0
                        if (platesIron > 0 && platesCopper > 0 && (tile.inventory[ItemType.CIRCUIT] ?: 0) < 5) {
                            tile.inventory[ItemType.IRON_PLATE] = platesIron - 1
                            tile.inventory[ItemType.COPPER_PLATE] = platesCopper - 1
                            tile.inventory[ItemType.CIRCUIT] = (tile.inventory[ItemType.CIRCUIT] ?: 0) + 1
                        }
                    }
                    TileType.LAB -> {
                        val circuitsCount = tile.inventory[ItemType.CIRCUIT] ?: 0
                        val platesIron = tile.inventory[ItemType.IRON_PLATE] ?: 0
                        val craftedScience = minOf(circuitsCount, platesIron)
                        if (craftedScience > 0) {
                            tile.inventory[ItemType.CIRCUIT] = circuitsCount - craftedScience
                            tile.inventory[ItemType.IRON_PLATE] = platesIron - craftedScience
                            globalScience += craftedScience
                        }
                    }
                    else -> {}
                }
            }

            // 2. Расчет логики движения конвейеров и выгрузки заводов (подготовка перемещений)
            val transfers = mutableListOf<Triple<GridTile, GridTile, ItemType>>()

            gridState.forEach { tile ->
                // А. Логика движения предметов по конвейерной ленте
                val isBelt = tile.building in listOf(TileType.BELT_UP, TileType.BELT_DOWN, TileType.BELT_LEFT, TileType.BELT_RIGHT)
                if (isBelt) {
                    val dir = when (tile.building) {
                        TileType.BELT_UP -> Pair(0, -1)
                        TileType.BELT_DOWN -> Pair(0, 1)
                        TileType.BELT_LEFT -> Pair(-1, 0)
                        TileType.BELT_RIGHT -> Pair(1, 0)
                        else -> Pair(0, 0)
                    }
                    val tx = tile.x + dir.first
                    val ty = tile.y + dir.second
                    if (tx in 0..4 && ty in 0..4) {
                        val target = gridState[ty * 5 + tx]
                        val itemToMove = tile.inventory.filter { it.value > 0 }.keys.firstOrNull()
                        if (itemToMove != null) {
                            val isTargetBelt = target.building in listOf(TileType.BELT_UP, TileType.BELT_DOWN, TileType.BELT_LEFT, TileType.BELT_RIGHT)
                            val targetTotalItems = target.inventory.values.sum()

                            if (isTargetBelt && targetTotalItems < 3) {
                                transfers.add(Triple(tile, target, itemToMove))
                            } else if (!isTargetBelt && canAcceptItem(target.building, itemToMove, target.inventory[itemToMove] ?: 0)) {
                                transfers.add(Triple(tile, target, itemToMove))
                            }
                        }
                    }
                }

                // Б. Логика автовыталкивания (заводы сами отдают готовую продукцию на соседние конвейеры/заводы)
                val outputItem = when (tile.building) {
                    TileType.MINER -> if (tile.baseType == TileType.ORE_IRON) ItemType.IRON_ORE else ItemType.COPPER_ORE
                    TileType.FURNACE -> if ((tile.inventory[ItemType.IRON_PLATE] ?: 0) > 0) ItemType.IRON_PLATE else if ((tile.inventory[ItemType.COPPER_PLATE] ?: 0) > 0) ItemType.COPPER_PLATE else null
                    TileType.ASSEMBLER -> if ((tile.inventory[ItemType.CIRCUIT] ?: 0) > 0) ItemType.CIRCUIT else null
                    else -> null
                }

                if (outputItem != null && (tile.inventory[outputItem] ?: 0) > 0) {
                    val directions = listOf(Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0))
                    for (dir in directions) {
                        val tx = tile.x + dir.first
                        val ty = tile.y + dir.second
                        if (tx in 0..4 && ty in 0..4) {
                            val target = gridState[ty * 5 + tx]
                            val isTargetBelt = target.building in listOf(TileType.BELT_UP, TileType.BELT_DOWN, TileType.BELT_LEFT, TileType.BELT_RIGHT)
                            val targetTotalItems = target.inventory.values.sum()

                            if (isTargetBelt && targetTotalItems < 3) {
                                transfers.add(Triple(tile, target, outputItem))
                                break
                            } else if (!isTargetBelt && canAcceptItem(target.building, outputItem, target.inventory[outputItem] ?: 0)) {
                                transfers.add(Triple(tile, target, outputItem))
                                break
                            }
                        }
                    }
                }
            }

            // 3. Выполнение перемещений ресурсов
            transfers.forEach { (src, dest, item) ->
                val srcCount = src.inventory[item] ?: 0
                if (srcCount > 0) {
                    src.inventory[item] = srcCount - 1
                    dest.inventory[item] = (dest.inventory[item] ?: 0) + 1
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Панель статуса и рюкзака игрока
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141414))
                .padding(12.dp)
        ) {
            Text(
                text = "🎒 ВАШ РЮКЗАК И СТАТУС",
                color = Color(0xFFE67E22),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🪙 Плиты Fe: $walletIronPlates", color = Color.White, fontSize = 12.sp)
                Text("🟧 Плиты Cu: $walletCopperPlates", color = Color.White, fontSize = 12.sp)
                Text("🔌 Схемы: $walletCircuits", color = Color.White, fontSize = 12.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🪨 Руда Fe: $walletIronOre", color = Color.Gray, fontSize = 12.sp)
                Text("🟫 Руда Cu: $walletCopperOre", color = Color.Gray, fontSize = 12.sp)
                Text("🧪 НАУКА: $globalScience / 50", color = Color(0xFF9B59B6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Рендеринг 2D Сетки 5x5
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("КАРТА ЗАВОДА (Нажмите на клетку)", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            for (y in 0 until 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (x in 0 until 5) {
                        val tile = gridState[y * 5 + x]
                        val isSelected = selectedTile?.x == x && selectedTile?.y == y

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    when (tile.baseType) {
                                        TileType.ORE_IRON -> Color(0xFF333A42)
                                        TileType.ORE_COPPER -> Color(0xFF4C3E35)
                                        else -> Color(0xFF2B2B2B)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Color(0xFFE67E22) else Color(0xFF444444)
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { selectedTile = tile },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Показываем конвейеры или другие здания
                                if (tile.building != TileType.EMPTY) {
                                    Text(text = tile.building.icon, fontSize = 20.sp)
                                } else if (tile.baseType != TileType.EMPTY) {
                                    Text(text = tile.baseType.icon, fontSize = 16.sp) // Рисуем руду под ногами
                                }

                                // Показываем текущее содержимое инвентаря на клетке
                                val items = tile.inventory.filter { it.value > 0 }
                                if (items.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.Center) {
                                        items.forEach { (type, count) ->
                                            Text(text = "${type.emoji}$count", fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Интерактивная нижняя панель управления
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (globalScience >= 50) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF27AE60)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ПОБЕДА! 🎉", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Вы построили полностью автоматическую систему научных исследований и прошли Mini-Factorio!", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Button(
                                onClick = {
                                    // Сброс
                                    walletIronOre = 0; walletCopperOre = 0; walletIronPlates = 15; walletCopperPlates = 10; walletCircuits = 0; globalScience = 0
                                    gridState.forEach { it.building = TileType.EMPTY; it.inventory.clear() }
                                    selectedTile = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Играть заново")
                            }
                        }
                    }
                }

                if (selectedTile != null) {
                    val tile = selectedTile!!
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF252525)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Управление клеткой (${tile.x}, ${tile.y})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "На клетке построено: ${tile.building.displayName}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )

                            // Показ внутренних ресурсов клетки и кнопка "Забрать все"
                            val items = tile.inventory.filter { it.value > 0 }
                            if (items.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Внутри лежит: " + items.map { "${it.value}x ${it.key.emoji}" }.joinToString(", "), color = Color.Green, fontSize = 12.sp)
                                    Button(
                                        onClick = {
                                            tile.inventory.forEach { (type, count) ->
                                                when (type) {
                                                    ItemType.IRON_ORE -> walletIronOre += count
 
