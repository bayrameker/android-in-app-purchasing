package com.bayram.inappexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.List;

public class Consumable extends AppCompatActivity {

    private final String PRODUCT_PREMIUM = "lifetime";
    private final String ConsumeProductID = "consumabletest";
    private final ArrayList<String> purchaseItemIDs = new ArrayList<String>() {{
        add(PRODUCT_PREMIUM);
        add(ConsumeProductID);
    }};

    private final String TAG = "iapSample";

    private BillingClient billingClient;
    private int tries=1;
    private int maxTries= 3;

    private int connectionAttempts = 0;

    boolean isConnectionEstablished=false;
    Button btn_premium, btn_restore;
    TextView tv_status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumable);

        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(
                        (billingResult, list) -> {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                                for (Purchase purchase : list) {
                                    String orderId = purchase.getOrderId();
                                    Log.d(TAG, "Response is OK");
                                    handlePurchase(purchase);
                                }
                            } else {

                                Log.d(TAG, "Response NOT OK");
                            }
                        }
                ).build();

        //start the connection after initializing the billing client
        establishConnection();
        init();
    }



    void init() {
        btn_premium = this.findViewById(R.id.btn_premium);
        btn_restore = this.findViewById(R.id.btn_restore);
        tv_status = this.findViewById(R.id.tv_status);

        btn_premium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetSingleInAppDetail();
            }
        });

        btn_restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restorePurchases();
            }
        });
    }



    private void establishConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.e("err", String.valueOf(billingResult.getResponseCode()));
                    Log.e("err","baglamtı başarılı");

                    // Bağlantı başarıyla kuruldu
                    // The BillingClient is ready. You can query purchases here.
                    //Use any of function below to get details upon successful connection
                    // GetSingleInAppDetail();
                    //GetListsInAppDetail();
                } else {
                    // Bağlantı başarısız oldu 3 kere tekrar dene
                    retryBillingServiceConnection();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Bağlantı kesildiğinde yeniden bağlanmak için yeniden bağlanmayı deneyin
                // TODO: 11.04.2023 alttaki yerine retryBillingServiceConnection() eklenecek ve 3 deneme olacak
                retryBillingServiceConnection();
            }
        });
    }

    void retryBillingServiceConnection(){
        tries=1;
        maxTries= 3;
        isConnectionEstablished = false;
        do{
            try {
                billingClient.startConnection(new BillingClientStateListener() {
                    @Override
                    public void onBillingServiceDisconnected() {
                        retryBillingServiceConnection();
                    }

                    @Override
                    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                        tries++;
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            isConnectionEstablished = true;

                        }
                        else if(tries==maxTries){
                            handleBillingError(billingResult.getResponseCode());
                        }
                    }
                });
            }
            catch (Exception e){
                tries++;
            }}
        while (tries <= maxTries && !isConnectionEstablished);

        if(isConnectionEstablished==false){
            handleBillingError(-1);
        }

    }




    /*
     *
     * The official examples use an ImmutableList for some reason to build the query,
     * but you don't actually need to use that.
     * The setProductList method just takes List<Product> as its input, it does not require ImmutableList.
     *
     * */

    /*
     * If you have API < 24, you could just make an ArrayList instead.
     * */

    void GetSingleInAppDetail() {
        ArrayList<QueryProductDetailsParams.Product> productList = new ArrayList<>();

        //Set your In App Product ID in setProductId()
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ConsumeProductID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {

            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {

                //Do Anything that you want with requested product details

                //Calling this function here so that once products are verified we can start the purchase behavior.
                //You can save this detail in separate variable or list to call them from any other location
                //Create another function if you want to call this in establish connections' success state
                LaunchPurchaseFlow(list.get(0));


            }
        });
    }

    void GetListsInAppDetail() {
        ArrayList<QueryProductDetailsParams.Product> productList = new ArrayList<>();

        //Set your In App Product ID in setProductId()
        for (String ids : purchaseItemIDs) {
            productList.add(
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(ids)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build());
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {

                for (ProductDetails li : list) {
                    Log.d(TAG, "IN APP item Price" + li.getOneTimePurchaseOfferDetails().getFormattedPrice());
                }
                //Do Anything that you want with requested product details
            }
        });
    }

    //This function will be called in handlepurchase() after success of any consumeable purchase
    void ConsumePurchase(Purchase purchase) {

        ConsumeParams params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.consumeAsync(params, new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {

                Log.d("TAG", "Consuming Successful: "+s);
                tv_status.setText("Product Consumed");
            }
        });
    }

    void LaunchPurchaseFlow(ProductDetails productDetails) {
        ArrayList<BillingFlowParams.ProductDetailsParams> productList = new ArrayList<>();

        productList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build());

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productList)
                .build();

        billingClient.launchBillingFlow(this, billingFlowParams);
    }

    void handlePurchase(Purchase purchases) {

        if (!purchases.isAcknowledged()) {

            billingClient.acknowledgePurchase(AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchases.getPurchaseToken())
                    .build(), billingResult -> {

         /*       ConsumeResponseListener listener = new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String purchaseToken) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            // Handle the success of the consume operation.
                        }
                    }
                }; */

          /*      ConsumeParams consumeParams =
                        ConsumeParams.newBuilder()
                                .setPurchaseToken(purchases.getPurchaseToken())
                                .build();
                                *
           */

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (String pur : purchases.getProducts()) {
                        if (pur.equalsIgnoreCase(ConsumeProductID)) {
                            Log.d("TAG", "Purchase is successful");
                            tv_status.setText("Yay! Purchased");

                            //Calling Consume to consume the current purchase
                            // so user will be able to buy same product again
                            //   billingClient.consumeAsync(consumeParams, listener); // you should have listener method !


                            // TODO: 11.04.2023 bu kısım backend tarafında yapılacak
                            ConsumePurchase(purchases);
                        }
                    }
                }
                else{
                    handleBillingError(billingResult.getResponseCode());
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    void restorePurchases() {

        billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener((billingResult, list) -> {
        }).build();
        final BillingClient finalBillingClient = billingClient;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                Log.e("erorr","geldi");
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    finalBillingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (billingResult1, list) -> {
                                if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    if (list.size() > 0) {

                                        Log.d("TAG", "IN APP SUCCESS RESTORE: " + list);
                                        for (int i = 0; i < list.size(); i++) {

                                            if (list.get(i).getProducts().contains(PRODUCT_PREMIUM)) {
                                                tv_status.setText("Premium Restored");
                                                Log.d("TAG", "Product id "+PRODUCT_PREMIUM+" will restore here");
                                            }

                                        }
                                    } else {
                                        tv_status.setText("Nothing found to Restored");
                                        Log.d("TAG", "In APP Not Found To Restore");
                                    }
                                }
                            });
                }
                else{
                    handleBillingError(billingResult.getResponseCode());
                }

            }
        });
    }


    private void handleBillingError(int responseCode) {
        String errorMessage = "";
        switch (responseCode) {
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                errorMessage = "Billing service is currently unavailable. Please try again later.";
                break;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                errorMessage = "An error occurred while processing the request. Please try again later.";
                break;
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                errorMessage = "This feature is not supported on your device.";
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                errorMessage = "You already own this item.";
                break;
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
                errorMessage = "You do not own this item.";
                break;
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                errorMessage = "This item is not available for purchase.";
                break;
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                errorMessage = "Billing service has been disconnected. Please try again later.";
                break;
            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                errorMessage = "Billing service timed out. Please try again later.";
                break;
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                errorMessage = "Billing service is currently unavailable. Please try again later.";
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                errorMessage = "The purchase has been canceled.";
                break;
            default:
                errorMessage = "An unknown error occurred.";
                break;
        }
        Log.e("BillingError", errorMessage);
    }

}