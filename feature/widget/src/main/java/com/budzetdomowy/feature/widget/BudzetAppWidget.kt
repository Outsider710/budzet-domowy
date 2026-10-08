package com.budzetdomowy.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val BrandGreen = Color(0xFF1B5E4B)
private val OnBrand = Color(0xFFFFFFFF)
private val OnBrandMuted = Color(0xFFD7EBE2)
private val AlertAmber = Color(0xFFFFB74D)
private val AddButtonBg = Color(0xFF4C8C74)

class BudzetAppWidget : GlanceAppWidget(), KoinComponent {
    private val loader: MonthSnapshotLoader by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = runCatching { loader.load() }.getOrElse {
            MonthSnapshot(
                monthLabel = "",
                balanceLabel = "—",
                incomeLabel = "—",
                expenseLabel = "—",
                hasOverBudget = false,
                hasRecurringDueToday = false
            )
        }
        val incomePrefix = context.getString(R.string.widget_income_short)
        val expensePrefix = context.getString(R.string.widget_expense_short)

        provideContent {
            GlanceTheme {
                WidgetContent(
                    snapshot = snapshot,
                    incomeLine = "$incomePrefix ${snapshot.incomeLabel}",
                    expenseLine = "$expensePrefix ${snapshot.expenseLabel}",
                    homeIntent = WidgetNav.homeIntent(context),
                    addIntent = WidgetNav.addExpenseIntent(context)
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    snapshot: MonthSnapshot,
    incomeLine: String,
    expenseLine: String,
    homeIntent: Intent,
    addIntent: Intent
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BrandGreen, BrandGreen))
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(homeIntent)),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snapshot.monthLabel,
                style = TextStyle(
                    color = ColorProvider(OnBrandMuted, OnBrandMuted),
                    fontSize = 12.sp
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            if (snapshot.hasRecurringDueToday) {
                Box(
                    modifier = GlanceModifier
                        .size(22.dp)
                        .cornerRadius(11.dp)
                        .background(ColorProvider(AddButtonBg, AddButtonBg)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "zł",
                        style = TextStyle(
                            color = ColorProvider(OnBrand, OnBrand),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                Spacer(GlanceModifier.width(4.dp))
            }
            if (snapshot.hasOverBudget) {
                Box(
                    modifier = GlanceModifier
                        .size(22.dp)
                        .cornerRadius(11.dp)
                        .background(ColorProvider(AlertAmber, AlertAmber)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        style = TextStyle(
                            color = ColorProvider(BrandGreen, BrandGreen),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                Spacer(GlanceModifier.width(4.dp))
            }
        }

        Spacer(GlanceModifier.height(4.dp))

        Text(
            text = snapshot.balanceLabel,
            style = TextStyle(
                color = ColorProvider(OnBrand, OnBrand),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )

        Spacer(GlanceModifier.height(6.dp))

        Text(
            text = incomeLine,
            style = TextStyle(
                color = ColorProvider(OnBrandMuted, OnBrandMuted),
                fontSize = 11.sp
            ),
            maxLines = 1
        )
        Text(
            text = expenseLine,
            style = TextStyle(
                color = ColorProvider(OnBrandMuted, OnBrandMuted),
                fontSize = 11.sp
            ),
            maxLines = 1
        )

        Spacer(GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .cornerRadius(18.dp)
                    .background(ColorProvider(AddButtonBg, AddButtonBg))
                    .clickable(actionStartActivity(addIntent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = ColorProvider(OnBrand, OnBrand),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

class BudzetAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudzetAppWidget()
}
