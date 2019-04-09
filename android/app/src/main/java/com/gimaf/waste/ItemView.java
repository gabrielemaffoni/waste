package com.gimaf.waste;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.gimaf.waste.Item.KEY_DB;

/**
 * On opening, this view shows the data saved in the database in real time.
 * It also updates the animation
 *
 * TODO: Convert from Pints to Milliliters
 */
public class ItemView extends AppCompatActivity {
    private DatabaseReference databaseReference;
    private TextView brand_text;
    private TextView product_type;
    private TextView quantity_left;
    private TextView brand_field;
    private TextView int_temperature;
    private TextView ext_temperature;
    private TextView expiration_date;
    private LottieAnimationView animationView;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private TextView optimal_temperature;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_item);
        auth = FirebaseAuth.getInstance();
        Intent intent = getIntent();
        String productKey = intent.getStringExtra(KEY_DB);
        Log.d("EXTRA GOT", productKey);
        currentUser = auth.getCurrentUser();
        brand_text = findViewById(R.id.brand_text);
        product_type = findViewById(R.id.type_and_total_quantity);
        quantity_left = findViewById(R.id.quantity_left_value);
        int_temperature = findViewById(R.id.int_temperature_value);
        ext_temperature = findViewById(R.id.ext_temperature_value);
        expiration_date = findViewById(R.id.expiration_date_text);
        animationView = findViewById(R.id.item_animation);
        optimal_temperature = findViewById(R.id.opt_temperature_value);
        setValues(productKey);

    }

    private void attachValues(@NonNull DataSnapshot dataSnapshot, String productKey) {
        Item item = new Item();
        item.setFromDataSnapshot(dataSnapshot, getApplicationContext());
        try {
            Log.d("NUMBER OF SNAPS", dataSnapshot.getValue().toString());
            optimal_temperature.setText(String.format(getResources().getString(R.string.double_format), item.getOptimal_temperature()));
            brand_text.setText(String.format(getResources().getString(R.string.double_format), item.getTotal_quantity()));
            product_type.setText(item.getProduct_type());
            quantity_left.setText(String.format(getResources().getString(R.string.double_format), item.getCurrent_quantity()));
            brand_field.setText(item.getBrand());
            int_temperature.setText(String.format(getResources().getString(R.string.double_format), item.getInternal_temperature()));
            ext_temperature.setText(String.format(getResources().getString(R.string.double_format), item.getExternal_temperature()));
            expiration_date.setText(item.getExpiration_date());
            this.setTitle(item.getProduct_type());
        } catch (NullPointerException exception) {
            Log.d("Exception", exception.getMessage());
        }

        calculateAnimation(item.getTotal_quantity(), item.getCurrent_quantity());
    }


    private void setValues(final String productKey) {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("items").child(currentUser.getUid()).child(productKey);
        Log.d("Reference", databaseReference.getPath().toString());

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                attachValues(dataSnapshot, productKey);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                finish();
            }
        });
    }

    /**
     * Calculates which animation is required.
     * @param totalQuantity The size of the packaging
     * @param currentQuantity The quantity left
     */

    private void calculateAnimation(double totalQuantity, double currentQuantity) {
        double calculation = currentQuantity / totalQuantity;

        if (calculation > 0.7) {
            animationView.setAnimation(R.raw.milk_full);
        } else if (calculation > 0.5) {
            animationView.setAnimation(R.raw.milk_75);
        } else if (calculation > 0.3) {
            animationView.setAnimation(R.raw.milk_50);
        } else {
            animationView.setAnimation(R.raw.milk_low);
        }

        animationView.playAnimation();

    }
}
