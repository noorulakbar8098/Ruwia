package com.example.ruwia.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun formKeyboardActions(onDone: (() -> Unit)? = null): KeyboardActions {
    val focusManager = LocalFocusManager.current
    return KeyboardActions(
        onNext = {
            if (!focusManager.moveFocus(FocusDirection.Next)) {
                focusManager.clearFocus()
            }
        },
        onDone = {
            if (onDone == null) focusManager.clearFocus() else onDone()
        },
    )
}
