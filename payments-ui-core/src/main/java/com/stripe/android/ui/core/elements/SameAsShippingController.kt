package com.stripe.android.ui.core.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.elements.SectionFieldComposable
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SameAsShippingController(
    initialValue: Boolean
) : InputController, SectionFieldComposable {
    override val label: Flow<Int> =
        MutableStateFlow(R.string.stripe_paymentsheet_address_element_same_as_shipping)
    private val _value = MutableStateFlow(initialValue)
    val value: Flow<Boolean> = _value
    override val fieldValue: Flow<String> = value.map { it.toString() }
    override val rawFieldValue: Flow<String?> = fieldValue

    override val error: Flow<FieldError?> = flowOf(null)
    override val showOptionalLabel: Boolean = false
    override val isComplete: Flow<Boolean> = flowOf(true)
    override val formFieldValue: Flow<FormFieldEntry> =
        rawFieldValue.map { value ->
            FormFieldEntry(value, true)
        }

    fun onValueChange(value: Boolean) {
        _value.value = value
    }

    override fun onRawValueChange(rawValue: String) {
        onValueChange(rawValue.toBooleanStrictOrNull() ?: true)
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
        nextFocusDirection: FocusDirection,
        previousFocusDirection: FocusDirection
    ) {
        SameAsShippingElementUI(this)
    }
}
