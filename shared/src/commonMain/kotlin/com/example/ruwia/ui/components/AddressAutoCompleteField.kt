package com.example.ruwia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.ruwia.data.AddressSuggestion
import com.example.ruwia.data.getLocationService
import com.example.ruwia.ui.dashboard.NTColors
import kotlinx.coroutines.delay

@Composable
fun AddressAutoCompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    onSuggestionSelected: (AddressSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "Type address, area, or landmark…",
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val locationService = remember { getLocationService() }
    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }
    var lastSelectedText by remember { mutableStateOf("") }

    LaunchedEffect(value) {
        val trimmed = value.trim()
        if (trimmed == lastSelectedText || trimmed.length < 2) {
            suggestions = emptyList()
            showDropdown = false
            isSearching = false
            return@LaunchedEffect
        }

        // Debounce typing by 300ms
        delay(300)
        isSearching = true
        try {
            val results = locationService.searchAddressSuggestions(trimmed)
            suggestions = results
            showDropdown = results.isNotEmpty()
        } catch (_: Exception) {
            suggestions = emptyList()
            showDropdown = false
        } finally {
            isSearching = false
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                if (it.trim() != lastSelectedText) {
                    showDropdown = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = label?.let { { Text(it) } },
            placeholder = { Text(placeholder, fontSize = 13.sp, color = NTColors.TextTertiary) },
            singleLine = true,
            leadingIcon = leadingIcon ?: {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = NTColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.8.dp,
                            color = NTColors.Primary
                        )
                        Spacer(Modifier.width(6.dp))
                    } else if (value.isNotBlank() && enabled) {
                        IconButton(
                            onClick = {
                                onValueChange("")
                                suggestions = emptyList()
                                showDropdown = false
                                lastSelectedText = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                tint = NTColors.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    trailingContent?.invoke()
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NTColors.Primary,
                unfocusedBorderColor = NTColors.Border,
                focusedContainerColor = NTColors.Surface,
                unfocusedContainerColor = NTColors.Surface,
                focusedTextColor = NTColors.TextPrimary,
                unfocusedTextColor = NTColors.TextPrimary,
                cursorColor = NTColors.Primary,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    showDropdown = false
                    onImeAction()
                },
                onNext = {
                    showDropdown = false
                    onImeAction()
                }
            )
        )

        DropdownMenu(
            expanded = showDropdown && suggestions.isNotEmpty(),
            onDismissRequest = { showDropdown = false },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(NTColors.Surface, RoundedCornerShape(14.dp)),
            properties = PopupProperties(focusable = false)
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NTColors.Primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = NTColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = suggestion.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NTColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (suggestion.subtitle.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = suggestion.subtitle,
                                        fontSize = 11.sp,
                                        color = NTColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        lastSelectedText = suggestion.fullAddress
                        showDropdown = false
                        onSuggestionSelected(suggestion)
                    }
                )
            }
        }
    }
}
