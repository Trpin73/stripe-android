package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal object VerticalModeInitialScreenFactory {
    fun create(viewModel: BaseSheetViewModel): PaymentSheetScreen {
        val savedPaymentMethods = viewModel.savedPaymentMethodMutator.paymentMethods.value
        val paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value)
        val supportedPaymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes()

        if (supportedPaymentMethodTypes.size == 1 && savedPaymentMethods.isEmpty()) {
            return PaymentSheetScreen.VerticalModeForm(
                interactor = DefaultVerticalModeFormInteractor.create(
                    selectedPaymentMethodCode = supportedPaymentMethodTypes.first(),
                    viewModel = viewModel,
                ),
                showsWalletHeader = true,
            )
        }
        return PaymentSheetScreen.VerticalMode(DefaultPaymentMethodVerticalLayoutInteractor.create(viewModel))
    }
}