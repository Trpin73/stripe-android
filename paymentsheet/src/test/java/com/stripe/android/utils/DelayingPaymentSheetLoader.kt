package com.stripe.android.utils

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.channels.Channel

internal class DelayingPaymentSheetLoader : PaymentSheetLoader {

    private val results = Channel<Result<PaymentSheetState.Full>>(capacity = 1)

    fun enqueueSuccess(
        stripeIntent: StripeIntent = PaymentIntentFactory.create(),
    ) {
        enqueue(
            Result.success(
                PaymentSheetState.Full(
                    stripeIntent = stripeIntent,
                    customerPaymentMethods = emptyList(),
                    config = null,
                    isGooglePayReady = false,
                    paymentSelection = null,
                    linkState = null,
                ),
            )
        )
    }

    fun enqueueFailure() {
        val error = RuntimeException("whoops")
        enqueue(Result.failure<PaymentSheetState.Full>(error))
    }

    private fun enqueue(result: Result<PaymentSheetState.Full>) {
        results.trySend(result)
    }

    override suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ): Result<PaymentSheetState.Full> {
        return results.receive()
    }
}
