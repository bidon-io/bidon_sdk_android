package org.bidon.demoapp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.bidon.demoapp.component.AppTextButton
import org.bidon.demoapp.component.CaptionText
import org.bidon.demoapp.component.HorizontalItemSelector
import org.bidon.demoapp.component.NumberSelector
import org.bidon.demoapp.component.Subtitle1Text
import org.bidon.sdk.segment.models.Gender

/**
 * Created by Aleksei Cherniaev on 13/07/2023.
 */
@Composable
internal fun SegmentSettingsView(
    genders: List<String> = Gender.values().map { it.code } + "Not set",
) {
    val isHidden = remember {
        mutableStateOf(true)
    }
    Column {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Subtitle1Text(
                text = "Segment",
                modifier = Modifier.clickable {
                    isHidden.value = !isHidden.value
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            AppTextButton(
                modifier = Modifier.padding(top = 0.dp), text = "Random values"
            ) {
                SegmentInfo.setGender(Gender.values().random())
                SegmentInfo.setAge((50 * Math.random()).toInt() + 18)
                SegmentInfo.setLevel((80 * Math.random()).toInt())
                SegmentInfo.setTotalInAppAmount(100.0 * Math.random())
                SegmentInfo.setPaying(true.takeIf { Math.random() > 0.5 } ?: false)
            }
            AppTextButton(
                modifier = Modifier.padding(top = 0.dp), text = "Reset"
            ) {
                SegmentInfo.setGender(null)
                SegmentInfo.setAge(0)
                SegmentInfo.setLevel(0)
                SegmentInfo.setTotalInAppAmount(0.0)
                SegmentInfo.setPaying(false)
                SegmentInfo.clearCustomAttribute()
            }
        }
        AnimatedVisibility(visible = isHidden.value) {
            val text = buildString {
                append(SegmentInfo.gender.collectAsState().value?.code ?: "Not set")
                append(", ")
                appendLine("${SegmentInfo.age.collectAsState().value} years")
                appendLine("Game level ${SegmentInfo.level.collectAsState().value}, ")
                append("In-app amount $${SegmentInfo.inAppAmount.collectAsState().value}, ")
                append("Paying: ${SegmentInfo.isPaying.collectAsState().value}")
                append("\nCustom attr ${SegmentInfo.customAttributes.collectAsState().value}")
            }
            CaptionText(
                text = text, color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.clickable {
                    isHidden.value = !isHidden.value
                }
            )
        }
        AnimatedVisibility(visible = !isHidden.value) {
            Surface(
                shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.background,
                border = BorderStroke(
                    width = 1.dp, color = MaterialTheme.colorScheme.secondary
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    HorizontalItemSelector(
                        modifier = Modifier.padding(top = 0.dp),
                        title = "Gender",
                        items = genders,
                        selectedItem = SegmentInfo.gender.collectAsState().value?.code ?: "Not set",
                        getItemTitle = { genderString ->
                            genderString
                        },
                        onItemClicked = { genderString ->
                            val newGender = Gender.values().firstOrNull { it.code == genderString }
                            SegmentInfo.setGender(newGender)
                        }
                    )
                    NumberSelector(
                        title = "Age",
                        modifier = Modifier.padding(top = 4.dp),
                        value = SegmentInfo.age.collectAsState().value,
                        onPlusClicked = {
                            SegmentInfo.setAge(SegmentInfo.age.value + 7)
                        },
                        onMinusClicked = {
                            SegmentInfo.setAge(SegmentInfo.age.value - 7)
                        }
                    )
                    NumberSelector(
                        title = "Game Level",
                        modifier = Modifier.padding(top = 0.dp),
                        value = SegmentInfo.level.collectAsState().value,
                        onPlusClicked = {
                            SegmentInfo.setLevel(SegmentInfo.level.value + 1)
                        },
                        onMinusClicked = {
                            SegmentInfo.setLevel(SegmentInfo.level.value - 1)
                        }
                    )
                    NumberSelector(
                        title = "In-App Amount",
                        modifier = Modifier.padding(top = 0.dp),
                        value = SegmentInfo.inAppAmount.collectAsState().value,
                        onPlusClicked = {
                            SegmentInfo.setTotalInAppAmount(SegmentInfo.inAppAmount.value + 10.24)
                        },
                        onMinusClicked = {
                            SegmentInfo.setTotalInAppAmount(SegmentInfo.inAppAmount.value - 10.24)
                        }
                    )
                    HorizontalItemSelector(
                        modifier = Modifier.padding(top = 0.dp),
                        title = "Is paying",
                        items = listOf(true, false),
                        selectedItem = SegmentInfo.isPaying.collectAsState().value,
                        getItemTitle = { isPaying ->
                            "True".takeIf { isPaying } ?: "False"
                        },
                        onItemClicked = { testMode ->
                            SegmentInfo.setPaying(testMode)
                        }
                    )
                    CustomAttributesView()
                }
            }
        }
    }
}

@Composable
private fun CustomAttributesView() {
    val isCustomAdding = remember {
        mutableStateOf<AddingType?>(null)
    }
    val isAdding = isCustomAdding.value != null

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier,
                text = "Custom attributes",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            AppTextButton(
                modifier = Modifier.padding(top = 0.dp),
                text = "Clear"
            ) {
                SegmentInfo.clearCustomAttribute()
            }
        }
        CaptionText(
            text = SegmentInfo.customAttributes.collectAsState().value.toString(),
            color = Color.White.copy(alpha = 0.4f)
        )
        AnimatedVisibility(
            visible = !isAdding
        ) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AddingType.values().forEach {
                    AppTextButton(
                        modifier = Modifier.padding(top = 0.dp), text = "+ ${it.name}"
                    ) {
                        isCustomAdding.value = it
                    }
                }
            }
        }
        AnimatedVisibility(visible = isAdding) {
            val key = remember {
                mutableStateOf("")
            }
            val value = remember {
                mutableStateOf("true".takeIf { isCustomAdding.value == AddingType.Boolean } ?: "")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KeyValueTextField(
                    text = key,
                    hint = "Key"
                )
                KeyValueTextField(
                    text = value,
                    hint = "Value",
                    keyboardType = when (isCustomAdding.value) {
                        AddingType.Int -> KeyboardType.Decimal
                        AddingType.Boolean -> KeyboardType.Text
                        AddingType.Double -> KeyboardType.Number
                        AddingType.String -> KeyboardType.Text
                        null -> KeyboardType.Text
                    }
                )
                AppTextButton(text = "Add") {
                    val pair = when (isCustomAdding.value) {
                        AddingType.Int -> key.value to (value.value.toIntOrNull() ?: 0)
                        AddingType.Boolean -> key.value to value.value.toBoolean()
                        AddingType.Double -> key.value to (value.value.toDoubleOrNull() ?: 0.0)
                        AddingType.String -> key.value to value.value
                        null -> error("unexpected")
                    }
                    SegmentInfo.addCustomAttribute(pair.first, pair.second)
                    isCustomAdding.value = null
                }
                IconButton(onClick = {
                    isCustomAdding.value = null
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RowScope.KeyValueTextField(
    text: MutableState<String>,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = text.value,
        onValueChange = { newValue ->
            text.value = newValue
        },
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onPrimary,
            background = MaterialTheme.colorScheme.background
        ),
        label = {
            Text(
                text = hint,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Light
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        maxLines = 1
    )
}

internal enum class AddingType {
    Int,
    Boolean,
    Double,
    String
}
