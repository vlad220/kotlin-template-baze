package com.template.screens.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.template.presentation.components.SocialShareRow

private data class Topic(val question: String, val answer: String)

private val rules = listOf(
    "Как вызвать машину" to listOf(
        "Отметьте «Откуда» на карте или оставьте точку по умолчанию, введите «Куда» — кнопкой «Найти» или с карты. Можно сказать фразу: «из центра на вокзал за 250».",
        "После «Рассчитать дорогу» появится расстояние, время и ориентир по ставкам рядом. Цену называете вы: ползунок шагает по 10 ₽, любое число от 50 ₽.",
        "«Вызвать машину» отправляет заявку только тем водителям, кому она выгодна при их тарифе: кто первый нажал «Взять» — тот и едет.",
        "Оплата вне приложения — наличными или переводом водителю. Денег на балансе у нас нет."
    ),
    "Как стать водителем" to listOf(
        "Кнопка «Я водитель — переключиться» внизу экрана заказа. В профиле задайте свой тариф ₽ за км и радиус — заявки дальше не приходят.",
        "Отметьте себя на карте и включите геолокацию: пока вы на линии и приложение открыто, точка уходит на сервер каждые 15 секунд. Без точки заявок рядом не будет.",
        "Взяли заказ — ведите его на вкладке «Поездка»: «Я приехал» → «Начать поездку» → «Завершить», там же поставьте клиенту звёзды."
    ),
    "Отмены и споры" to listOf(
        "Отменили до приезда — цена не начисляется, деньги не держатся.",
        "Водитель после выезда отказаться от поездки уже не может: бэкенд запрещает, и кнопка в этом статусе не показывается. Отмены после выезда считаются против водителя.",
        "Цена, на которой стороны договорились, остаётся в истории заказа навсегда — видно обоим."
    )
)

private val faq = listOf(
    Topic(
        "Никто не берёт заказ",
        "Заявку видят только водители, чей тариф укладывается в вашу цену. Посмотрите «Свободных машин рядом» и ставку в расчёте дороги — если машин много, а заказа никто не берёт, нажмите «Поднять свою цену до N ₽» (+10 %). Автоподнятия по умолчанию нет: цену меняет человек."
    ),
    Topic(
        "Водитель предложил другую цену",
        "В карточке заказа появится «Водитель предлагает N ₽» с двумя кнопками. «Согласиться за N ₽» закрепляет поездку именно за этим водителем, «Поднять свою цену» оставляет поиск открытым."
    ),
    Topic(
        "Где посмотреть поездки и сколько ушло",
        "«Мои заказы» в шапке экрана заказа: вкладки по статусам и строка «потрачено N ₽ · M поездок · K км». У водителя то же по деньгам — во вкладке «Заработок»."
    ),
    Topic(
        "Как связаться с водителем",
        "Через чат: номер телефона наружу не отдаётся, поэтому из приложения — сообщение, а звонок только когда он уже у точки подачи."
    ),
    Topic(
        "Уведомления не приходят",
        "Разрешение на уведомления дают на экране: у клиента — кнопка под «Вызвать машину», у водителя — в карточке над списком заявок. Фона мы не ловим: без открытого приложения уведомление не придёт — доставка работает, пока приложение живо."
    ),
    Topic(
        "Водитель не едет",
        "Пока статус «Ищем машину» или «Водитель в пути», заказ можно отменить без последствий. Статусы меняются самими участниками, поэтому машина на карте стоит ровно там, где её передал телефон водителя."
    ),
    Topic(
        "Что видно о водителе до заказа",
        "Ничего персонального: ни имени, ни номера, ни пути. Имя, марка и номер машины появляются только после того, как водитель взял ваш заказ, а его позиция на карте — только во время поездки."
    ),
    Topic(
        "Приложение устарело",
        "Новая версия предлагается сама при запуске — скачивание и установка идут из приложения, отдельный APK искать не нужно."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Помощь") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rules.forEach { (title, lines) ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }

            Text(
                "Частые вопросы",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            faq.forEach { topic ->
                Card {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(topic.question, fontWeight = FontWeight.SemiBold)
                        Text(topic.answer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text(
                "Не нашли ответ — напишите, что именно сломалось",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { navController.navigate("feedback") }) { Text("Сообщить об ошибке") }
                TextButton(onClick = { navController.navigate("chat_list") }) { Text("Написать в чат") }
            }
            SocialShareRow(
                shareText = "AI-Такси: клиент сам называет цену поездки",
                shareUrl = "https://vsem72.ru"
            )
        }
    }
}
