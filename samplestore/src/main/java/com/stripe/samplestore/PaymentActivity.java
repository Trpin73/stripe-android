package com.stripe.samplestore;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.SourceParams;
import com.stripe.android.view.CardInputWidget;

import java.util.Currency;
import java.util.concurrent.Callable;

import retrofit2.Retrofit;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class PaymentActivity extends AppCompatActivity {

    // Put your publishable key here. It should start with "pk_test_"
    private static final String PUBLISHABLE_KEY =
            "put your publishable key here";

    private static final String EXTRA_EMOJI_INT = "EXTRA_EMOJI_INT";
    private static final String EXTRA_PRICE = "EXTRA_PRICE";
    private static final String EXTRA_CURRENCY = "EXTRA_CURRENCY";

    private CardInputWidget mCardInputWidget;
    private CompositeSubscription mCompositeSubscription;
    private Currency mCurrency;
    private int mEmojiUnicode;
    private long mPrice;
    private ProgressDialogFragment mProgressDialogFragment;
    private Stripe mStripe;
    private View mTotalLayoutView;

    public static Intent createIntent(
            @NonNull Context context,
            int emojiUnicode,
            long price,
            @NonNull Currency currency) {
        Intent intent = new Intent(context, PaymentActivity.class);
        intent.putExtra(EXTRA_EMOJI_INT, emojiUnicode);
        intent.putExtra(EXTRA_PRICE, price);
        intent.putExtra(EXTRA_CURRENCY, currency);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        mEmojiUnicode = getIntent().getExtras().getInt(EXTRA_EMOJI_INT);
        TextView emojiDisplay = (TextView) findViewById(R.id.tv_emoji_display);
        emojiDisplay.setText(StoreUtils.getEmojiByUnicode(mEmojiUnicode));

        mTotalLayoutView = findViewById(R.id.payment_main_layout);

        mPrice = getIntent().getExtras().getLong(EXTRA_PRICE);
        mCurrency = (Currency) getIntent().getExtras().getSerializable(EXTRA_CURRENCY);
        TextView priceDisplay = (TextView) findViewById(R.id.tv_price_display);
        priceDisplay.setText(StoreUtils.getPriceString(mPrice, mCurrency));
        mCompositeSubscription = new CompositeSubscription();

        mCardInputWidget = (CardInputWidget) findViewById(R.id.card_input_widget);
        mProgressDialogFragment =
                ProgressDialogFragment.newInstance(R.string.completing_purchase);
        RxView.clicks(findViewById(R.id.btn_purchase))
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        attemptPurchase();
                    }
                });

        mStripe = new Stripe(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCompositeSubscription != null) {
            mCompositeSubscription.unsubscribe();
            mCompositeSubscription = null;
        }
    }

    private void attemptPurchase() {
        Card card = mCardInputWidget.getCard();
        if (card == null) {
            return;
        }

        final SourceParams cardParams = SourceParams.createCardParams(card);
        Observable<Source> cardSourceObservable =
                Observable.fromCallable(new Callable<Source>() {
                    @Override
                    public Source call() throws Exception {
                        return mStripe.createSourceSynchronous(
                                cardParams,
                                PUBLISHABLE_KEY);
                    }
                });

        final FragmentManager fragmentManager = this.getSupportFragmentManager();
        mCompositeSubscription.add(cardSourceObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(
                    new Action0() {
                        @Override
                        public void call() {
                            mProgressDialogFragment.show(fragmentManager, "progress");
                        }
                    })
                .subscribe(
                    new Action1<Source>() {
                        @Override
                        public void call(Source source) {
                            proceedWithPurchaseIf3DSCheckIsNotNecessary(source);
                        }
                    },
                    new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Snackbar snackbar = Snackbar.make(
                                    mTotalLayoutView,
                                    throwable.getLocalizedMessage(),
                                    Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }));
    }

    private void proceedWithPurchaseIf3DSCheckIsNotNecessary(Source source) {
        if (source == null || !Source.CARD.equals(source.getType())) {
            Snackbar snackbar = Snackbar.make(
                    mTotalLayoutView,
                    "Something went wrong - this should be rare.",
                    Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }

        SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
        if (SourceCardData.REQUIRED.equals(cardData.getThreeDSecureStatus())) {
            // In this case, you would need to ask the user to verify the purchase.
            // You can see an example of how to do this in the 3DS example application.
            // In stripe-android/example.
        } else {
            // If 3DS is not required, you can charge the source.
            completePurchase(source);
        }
    }

    private void completePurchase(Source source) {
        Retrofit retrofit = RetrofitFactory.getInstance();
        StripeService stripeService = retrofit.create(StripeService.class);
        Observable<Void> stripeResponse = stripeService.createQueryCharge(mPrice, source.getId());

        mCompositeSubscription.add(stripeResponse
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                if (mProgressDialogFragment != null) {
                                    mProgressDialogFragment.dismiss();
                                }
                            }
                        })
                .subscribe(
                    new Action1<Void>() {
                        @Override
                        public void call(Void aVoid) {
                            finishCharge();
                        }
                    },
                    new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            sendSnackBarMessage(throwable.getLocalizedMessage());
                        }
                    }));
    }

    private void sendSnackBarMessage(String message) {
        Snackbar snackbar = Snackbar.make(
                mTotalLayoutView,
                message,
                Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void finishCharge() {
        Intent data = StoreActivity.createPurchaseCompleteIntent(mEmojiUnicode, mPrice);
        setResult(RESULT_OK, data);
        finish();
    }
}
