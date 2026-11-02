package io.nekohasekai.sagernet.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.bunker.BunkerParser
import io.nekohasekai.sagernet.database.ProfileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Bunker-specific import screen. Parsing is delegated to the existing protocol importer;
 * this screen only adds Bunker naming, de-duplication and batch insertion.
 */
class BunkerParserFragment : ToolbarFragment() {

    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var importButton: Button

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        val scroll = ScrollView(context).apply {
            setBackgroundColor(Color.rgb(9, 10, 17))
            clipToPadding = false
            setPadding(dp(18), dp(12), dp(18), dp(28))
        }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(column)

        val hero = TextView(context).apply {
            text = "🐈‍⬛  БУНКЕР КОТА\nПАРСЕР КОНФИГУРАЦИЙ"
            setTextColor(Color.WHITE)
            textSize = 23f
            setPadding(0, dp(12), 0, dp(8))
            letterSpacing = 0.035f
        }
        column.addView(hero)

        val subtitle = TextView(context).apply {
            text = "Вставь ключи, ссылку или содержимое подписки. Парсер разберёт поддерживаемые форматы, уберёт дубли и присвоит имена Бункера."
            setTextColor(Color.rgb(164, 169, 187))
            textSize = 14f
            setPadding(0, 0, 0, dp(16))
        }
        column.addView(subtitle)

        val card = MaterialCardView(context).apply {
            radius = dp(18).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.rgb(19, 21, 32))
            strokeWidth = dp(1)
            setStrokeColor(Color.rgb(54, 42, 83))
        }
        input = EditText(context).apply {
            hint = "vless://…\nvmess://…\ntrojan://…\nss://…"
            setHintTextColor(Color.rgb(112, 116, 137))
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.TOP or Gravity.START
            minLines = 8
            maxLines = 16
            setPadding(dp(14))
            setBackgroundColor(Color.TRANSPARENT)
            setSelectAllOnFocus(false)
        }
        card.addView(input, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        column.addView(card, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(14) })

        importButton = Button(context).apply {
            text = "РАЗОБРАТЬ И ДОБАВИТЬ"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.rgb(4, 15, 14))
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(0, 220, 139)))
            setOnClickListener { parseAndImport() }
        }
        column.addView(importButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(54)
        ).apply { bottomMargin = dp(14) })

        status = TextView(context).apply {
            text = "ГОТОВ К РАБОТЕ\nПоддерживаются форматы, которые понимает сетевое ядро приложения."
            setTextColor(Color.rgb(170, 176, 194))
            textSize = 13f
            setPadding(dp(14))
            setBackgroundColor(Color.rgb(19, 21, 32))
        }
        column.addView(status, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar?.title = "Парсер Бункера"
    }

    private fun parseAndImport() {
        val raw = input.text?.toString()?.trim().orEmpty()
        if (raw.isBlank()) {
            input.error = "Вставь конфигурации или ссылку"
            return
        }
        importButton.isEnabled = false
        importButton.text = "ОБРАБОТКА…"
        status.text = "ПАРСИНГ\nПроверяю формат и удаляю дубликаты…"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sourceText = withContext(Dispatchers.IO) {
                    if (raw.startsWith("https://", ignoreCase = true) || raw.startsWith("http://", ignoreCase = true)) {
                        downloadSource(raw)
                    } else raw
                }
                val result = withContext(Dispatchers.Default) { BunkerParser.parse(sourceText) }
                if (result.profiles.isEmpty()) {
                    status.text = "НИЧЕГО НЕ НАЙДЕНО\nПроверь содержимое. Ошибок разбора: ${result.rejectedMessages.size}"
                } else {
                    val added = withContext(Dispatchers.IO) {
                        ProfileManager.addProfileBatch(result.profiles)
                    }
                    status.text = "ГОТОВО  ✓\n\nНайдено: ${result.parsedCount}\nДобавлено: ${added.size}\nДубликаты удалены: ${result.duplicateCount}\nОшибки разбора: ${result.rejectedMessages.size}\n\nПрофили добавлены в текущую группу."
                    input.text?.clear()
                }
            } catch (error: Exception) {
                status.text = "ОШИБКА ПАРСИНГА\n${error.message ?: error.javaClass.simpleName}"
            } finally {
                importButton.isEnabled = true
                importButton.text = "РАЗОБРАТЬ И ДОБАВИТЬ"
            }
        }
    }

    private fun downloadSource(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "BunkerCat-Overdrive/0.1")
        return try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Сервер вернул HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val body = reader.readText()
                if (body.length > 8 * 1024 * 1024) {
                    throw IllegalStateException("Файл больше 8 МБ — раздели источник на части")
                }
                body
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
