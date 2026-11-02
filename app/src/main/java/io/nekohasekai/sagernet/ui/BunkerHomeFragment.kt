package io.nekohasekai.sagernet.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.bg.BaseService
import com.google.android.material.card.MaterialCardView

/** Bunker OVERDRIVE landing screen. The existing service FAB remains the single source of truth for connect/disconnect. */
class BunkerHomeFragment : ToolbarFragment() {

    private lateinit var connectionState: TextView
    private lateinit var connectionHint: TextView

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(96))
            setBackgroundColor(Color.rgb(9, 10, 17))
        }
        val scroll = ScrollView(context).apply {
            clipToPadding = false
            addView(root)
        }
        val screen = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 10, 17))
            addView(AppBarLayout(context).apply {
                id = R.id.appbar
                setBackgroundColor(Color.rgb(9, 10, 17))
                addView(Toolbar(context).apply {
                    id = R.id.toolbar
                    setTitleTextColor(Color.WHITE)
                    setBackgroundColor(Color.TRANSPARENT)
                }, AppBarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(scroll, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            ))
        }

        val hero = MaterialCardView(context).apply {
            radius = dp(24).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            setStrokeColor(Color.rgb(113, 28, 255))
            setCardBackgroundColor(Color.rgb(22, 13, 38))
        }
        val heroContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20))
        }
        heroContent.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_bunker_cat_mark)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Маскот Бункера Кота"
        }, LinearLayout.LayoutParams(dp(86), dp(86)))
        heroContent.addView(TextView(context).apply {
            text = "БУНКЕР КОТА"
            setTextColor(Color.WHITE)
            textSize = 25f
            letterSpacing = 0.04f
        })
        heroContent.addView(TextView(context).apply {
            text = "OVERDRIVE"
            setTextColor(Color.rgb(168, 85, 247))
            textSize = 25f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.09f
        })
        heroContent.addView(TextView(context).apply {
            text = "VPN  •  PROXY  •  FREEDOM"
            setTextColor(Color.rgb(0, 229, 155))
            textSize = 11f
            letterSpacing = 0.12f
            setPadding(0, dp(8), 0, dp(4))
        })
        hero.addView(heroContent)
        root.addView(hero, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(14) })

        val stateCard = card(context)
        val stateContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
        }
        connectionState = TextView(context).apply {
            setTextColor(Color.rgb(0, 229, 155))
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        connectionHint = TextView(context).apply {
            setTextColor(Color.rgb(170, 176, 194))
            textSize = 13f
            setPadding(0, dp(4), 0, 0)
        }
        stateContent.addView(connectionState)
        stateContent.addView(connectionHint)
        stateCard.addView(stateContent)
        root.addView(stateCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(14) })

        root.addView(sectionTitle(context, "БЫСТРЫЙ ДОСТУП"))
        addAction(root, "СЕРВЕРЫ И URL-ТЕСТ", "Профили, проверка задержки и быстрый выбор", R.id.nav_configuration)
        addAction(root, "ПАРСЕР КОНФИГУРАЦИЙ", "Импорт ссылок, подписок и TXT", R.id.nav_bunker_parser)
        addAction(root, "ПОДПИСКИ И ГРУППЫ", "Обновление и управление источниками", R.id.nav_group)
        addAction(root, "СТАТИСТИКА", "Трафик и состояние подключения", R.id.nav_traffic)

        root.addView(TextView(context).apply {
            text = "Бункер Кота • OVERDRIVE"
            setTextColor(Color.rgb(105, 108, 128))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, dp(12))
        })
        refreshConnectionState()
        return screen
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar?.title = "Бункер Кота"
    }

    fun onServiceStateChanged() = refreshConnectionState()

    private fun refreshConnectionState() {
        if (!::connectionState.isInitialized) return
        when (DataStore.serviceState) {
            BaseService.State.Connected -> {
                connectionState.text = "●  ПОДКЛЮЧЕНО"
                connectionHint.text = "VPN активен. Управление — кнопкой подключения."
            }
            BaseService.State.Connecting -> {
                connectionState.text = "◌  ПОДКЛЮЧЕНИЕ…"
                connectionHint.text = "Поднимаем защищённое соединение."
            }
            BaseService.State.Stopping -> {
                connectionState.text = "◌  ОТКЛЮЧЕНИЕ…"
                connectionHint.text = "Завершаем VPN-сессию."
            }
            else -> {
                connectionState.text = "●  НЕ ПОДКЛЮЧЕНО"
                connectionHint.text = "Выбери сервер и нажми кнопку подключения."
            }
        }
    }

    private fun addAction(root: LinearLayout, title: String, detail: String, destination: Int) {
        val context = root.context
        val item = card(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15))
        }
        content.addView(TextView(context).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.025f
        })
        content.addView(TextView(context).apply {
            text = detail
            setTextColor(Color.rgb(160, 165, 184))
            textSize = 12f
            setPadding(0, dp(4), 0, 0)
        })
        item.addView(content)
        item.setOnClickListener {
            (activity as? MainActivity)?.displayFragmentWithId(destination)
        }
        root.addView(item, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(9) })
    }

    private fun sectionTitle(context: android.content.Context, value: String) =
        TextView(context).apply {
            text = value
            setTextColor(Color.rgb(168, 85, 247))
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
            setPadding(dp(2), dp(5), 0, dp(9))
        }

    private fun card(context: android.content.Context) = MaterialCardView(context).apply {
        radius = dp(17).toFloat()
        cardElevation = 0f
        strokeWidth = dp(1)
        setStrokeColor(Color.rgb(48, 38, 68))
        setCardBackgroundColor(Color.rgb(18, 19, 30))
        isClickable = true
        isFocusable = true
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
