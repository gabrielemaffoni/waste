package com.gimaf.waste;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static com.gimaf.waste.Item.BRAND_DB;
import static com.gimaf.waste.Item.EXPIRATION_DATE_DB;
import static com.gimaf.waste.Item.KEY_DB;
import static com.gimaf.waste.Item.MEASURE_DB;
import static com.gimaf.waste.Item.NEW_PRODUCT_DB;
import static com.gimaf.waste.Item.OPTIMAL_TEMPERATURE_DB;
import static com.gimaf.waste.Item.PRODUCT_TYPE_DB;
import static com.gimaf.waste.Item.QUANTITY_DB;
import static com.gimaf.waste.Item.QUANTITY_LEFT_DB;

/**
 * Saves the new product on the database and gives the "Go on" to the Raspberry
 */

public class SetNewProduct extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    private Spinner pack_size_dropdown;
    private String productKey;
    private TextInputLayout pick_a_date_layout;
    private TextInputEditText pick_a_date_text;
    private Button save_button;
    private String measure_selected, product_type_selected;
    private Calendar expiration_date_calendar;
    private FirebaseUser currentUser;
    private FirebaseAuth auth;
    private HorizontalScrollView productTypeScrollView, pack_size_scrollView;

    private LinearLayout pack_size_linear_layout, productTypeLinearLayout, milk_layout, wine_layout, cheese_layout;
    private Toolbar toolbar;
    private Double selectedSize;
    private CardView milk, cheese, wine, small, medium, big;
    private ImageView small_image, medium_image, big_image;
    private TextView small_text, medium_text, big_text, select_product_type;
    private ConstraintLayout container;

    //TODO: add the conversion to selected measure
    //TODO: add conversion from String to Double as the measure. !IMPORTANT


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_new_item);
        Intent data = getIntent();
        productKey = data.getStringExtra(KEY_DB);
        Log.d("EXTRA RECEIVED", productKey);
        expiration_date_calendar = Calendar.getInstance();
        auth = FirebaseAuth.getInstance();
        container = findViewById(R.id.newProductContainer);
        save_button = findViewById(R.id.save_product);
        pick_a_date_layout = findViewById(R.id.pick_a_date_field);
        pick_a_date_text = findViewById(R.id.pick_a_date_edit_text);
        setTodayText();
        productTypeScrollView = findViewById(R.id.product_type_choice_wrapper);
        productTypeLinearLayout = findViewById(R.id.product_type_choice);
        pack_size_linear_layout = findViewById(R.id.pack_size_linear_layout);
        pack_size_scrollView = findViewById(R.id.pack_size_choice);
        toolbar = findViewById(R.id.new_product_navbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white, this.getTheme()));
        milk = findViewById(R.id.milk_button);
        cheese = findViewById(R.id.cheese_button);
        wine = findViewById(R.id.wine_button);
        small = findViewById(R.id.small_card);
        medium = findViewById(R.id.medium_card);
        big = findViewById(R.id.big_card);
        small_image = findViewById(R.id.small_image);
        medium_image = findViewById(R.id.medium_image);
        big_image = findViewById(R.id.big_image);
        small_text = findViewById(R.id.small_label);
        medium_text = findViewById(R.id.medium_label);
        big_text = findViewById(R.id.big_label);
        select_product_type = findViewById(R.id.title_select_product_type);
        pack_size_dropdown = findViewById(R.id.choose_measure);
        milk_layout = findViewById(R.id.milk_layout);
        wine_layout = findViewById(R.id.wine_layout);
        cheese_layout = findViewById(R.id.cheese_layout);
        setSpinner(pack_size_dropdown, R.array.measures);
        pack_size_dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                measure_selected = pack_size_dropdown.getItemAtPosition(i).toString();
                Log.d("Measure selected is", measure_selected);
                if (select_product_type.getVisibility() == View.GONE) {
                    switch (measure_selected) {
                        case "Pints":
                            changeViewToPints();
                            break;
                        case "Ml":
                            changeViewToML();
                            break;
                        case "L":
                            changeViewToL();
                            break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        currentUser = auth.getCurrentUser();

        milk.setOnClickListener(this);
        wine.setOnClickListener(this);
        cheese.setOnClickListener(this);
        small.setOnClickListener(this);
        medium.setOnClickListener(this);
        big.setOnClickListener(this);

        /**
         * Shows a calendar view for the expiration date
         */
        pick_a_date_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCalendarView();
            }
        });
        /**
         * If "Placed" button has been clicked, the method will check few data.
         * First that nothing is empty. If so, it shows a toast
         * Second if the pack size is a number. If not, it shows a toast.
         */
        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check data
                updateFirebase(productKey,selectedSize,pick_a_date_text.getText().toString());
                finish();
                String textToPrint = getResources().getString(R.string.product)+ " "+ product_type_selected +" "+getResources().getString(R.string.product_set);
                Snackbar snackbar = Snackbar
                        .make(container, textToPrint, Snackbar.LENGTH_LONG);
                snackbar.show();



            }
        });



    }

    /**
     * Sets spinners.
     * @param spinner The spinner to set
     * @param resource_array The menu to inflate in the spinners
     */

    public void setSpinner(Spinner spinner, int resource_array) {
        ArrayAdapter<CharSequence> dropdown_adapter = ArrayAdapter.createFromResource(this,
                resource_array, android.R.layout.simple_spinner_dropdown_item);
        dropdown_adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setAdapter(dropdown_adapter);

    }

    /**
     * Checks what is selected in the Spinners and sets them on the field
     * @param parent the adapter view from which has been selected
     * @param view the view that has been clicked
     * @param position the position of the view
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == pack_size_dropdown.getParent()) {
            measure_selected = pack_size_dropdown.getItemAtPosition(position).toString();
            if (select_product_type.getVisibility() == View.GONE) {
                switch (measure_selected) {
                    case "Pints":
                        changeViewToPints();
                        break;
                    case "Ml":
                        changeViewToML();
                        break;
                    case "L":
                        changeViewToL();
                        break;
                }
            }
        }

    }

    private void changeViewToML() {
        switch (product_type_selected){
            case "Milk":
                setImagePackSize(getResources().getStringArray(R.array.milk_ml_pack_size), getResources().getDrawable(R.drawable.ic_milk, this.getTheme()), false);
                break;
            case "Wine":
                setImagePackSize(getResources().getStringArray(R.array.wine_ml_pack_size), getResources().getDrawable(R.drawable.ic_white_wine, this.getTheme()),false);
                break;
            default:
                //DO NOTHING
                Log.d("Do nothing", "DO NOTHING COMMAND");
        }
    }

    private void changeViewToL(){
        switch (product_type_selected){
            case "Milk":
                setImagePackSize(getResources().getStringArray(R.array.milk_l_pack_size), getResources().getDrawable(R.drawable.ic_milk, this.getTheme()), false);
                break;
            case "Wine":
                setImagePackSize(getResources().getStringArray(R.array.wine_l_pack_size), getResources().getDrawable(R.drawable.ic_white_wine, this.getTheme()),false);
                break;
            default:
                //DO NOTHING
                Log.d("Do nothing", "DO NOTHING COMMAND");
        }
    }

    private void changeViewToPints() {
        switch (product_type_selected){
            case "Milk":
                setImagePackSize(getResources().getStringArray(R.array.milk_pints_pack_size), getResources().getDrawable(R.drawable.ic_milk, this.getTheme()), false);
                break;
            default:
                //DO NOTHING

                Log.d("Do nothing", "DO NOTHING COMMAND");
        }
    }

    /**
     * Sets a standard. These are arbitrary values
     * @param parent
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        measure_selected = "Pints";
        product_type_selected = "Milk";
    }

    /**
     * Updates Firebase with the new data coming in
     * @param productKey the product key generated by firebase
     * @param packSize the size of the pack, also called "total_quantity"
     * @param expirationDate the expiration date set by the user
     */
    public void updateFirebase(String productKey, Double packSize,
                               String expirationDate) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("items").child(currentUser.getUid()).child(productKey);
        HashMap<String, Object> toUpdate = new HashMap<>();

        toUpdate.put(NEW_PRODUCT_DB, "placed");
        toUpdate.put(QUANTITY_DB, packSize);
        toUpdate.put(EXPIRATION_DATE_DB, expirationDate);
        toUpdate.put(MEASURE_DB, measure_selected);
        toUpdate.put(PRODUCT_TYPE_DB, product_type_selected);
        toUpdate.put(OPTIMAL_TEMPERATURE_DB, 0);
        toUpdate.put(QUANTITY_LEFT_DB, 0);
        Log.d("PATH", databaseReference.getPath().toString());
        Log.d("DATA", toUpdate.toString());
        databaseReference.updateChildren(toUpdate);
    }

    /**
     * Sets the calendar of the expiration date.
     */
    public void setCalendarView() {
        int mYear = expiration_date_calendar.get(Calendar.YEAR);
        int mMonth = expiration_date_calendar.get(Calendar.MONTH);
        int mDay = expiration_date_calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String month_string = "";
                        String day_string = "";
                        // Converts the value on date set, as it will be
                        // necessary later on for compatibility with the database and Raspberry Pi
                        if (month < 10) {
                            month_string = "0" + String.valueOf(month + 1);
                        } else {
                            month_string = String.valueOf(month + 1);
                        }

                        if (dayOfMonth < 10) {
                            day_string = 0 + String.valueOf(dayOfMonth);
                        } else {
                            day_string = String.valueOf(dayOfMonth);
                        }

                        String chosenDate = day_string + "/" + month_string
                                + "/" + String.valueOf(year);
                        Log.d("EXPIRATION DATE", chosenDate);
                        pick_a_date_text.setText(chosenDate);
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.setTitle("Choose expiration date");
        datePickerDialog.show();
    }

    private void setTodayText(){

        int mYear = expiration_date_calendar.get(Calendar.YEAR);
        int mMonth = expiration_date_calendar.get(Calendar.MONTH);
        int mDay = expiration_date_calendar.get(Calendar.DAY_OF_MONTH) + 2;
        String todayDate = (mDay) + "/" + (mMonth) + "/" + (mYear);

        pick_a_date_text.setText(todayDate);
    }

    /**
     * General onClick method that defines what happens when something in the view is clicked.
     *
     * @param view
     */
    @Override
    public void onClick(View view) {
            int viewID = view.getId();
            Log.d("TAPPED", String.valueOf(view.getId()));
            switch (viewID){
                case R.id.milk_button:
                    defineCardChecker(milk, getResources().getStringArray(R.array.milk_pints_pack_size), getResources().getDrawable(R.drawable.ic_milk, this.getTheme()), "Milk");
                    break;
                case R.id.cheese_button:
                    defineCardChecker(cheese, getResources().getStringArray(R.array.cheese_pack_size), getResources().getDrawable(R.drawable.ic_cheese, this.getTheme()), "Cheese");
                    break;
                case R.id.wine_button:
                    defineCardChecker(wine, getResources().getStringArray(R.array.wine_ml_pack_size), getResources().getDrawable(R.drawable.ic_white_wine, this.getTheme()), "Wine");
                    break;
                case R.id.small_card:
                    getSelectedSize(small, small_text);
                    break;
                case R.id.medium_card:
                    getSelectedSize(medium, medium_text);
                    break;
                case R.id.big_card:
                    getSelectedSize(big, big_text);
                    break;
                case R.id.milk_layout:
                    Log.d("MILK TAPPED", "MILK TAPPED");
                    break;

            }


    }

    private void setImagePackSize(String[] stringArray, Drawable drawable, boolean change_visibility) {
        float small_size = (float) 0.5;
        float medium_size = (float) 1;
        float big_size = (float) 1.5;
        singlePackSize(small_text, small_image, stringArray[0], drawable, small_size);
        singlePackSize(medium_text, medium_image, stringArray[1], drawable, medium_size);
        singlePackSize(big_text, big_image, stringArray[2], drawable, big_size);
        if (change_visibility) {
            pack_size_scrollView.setVisibility(View.VISIBLE);
            select_product_type.setVisibility(View.GONE);
        }
    }

    private void singlePackSize(TextView text, ImageView imageView, String label, Drawable image, float size){
        text.setText(label);
        imageView.setImageDrawable(image);
        imageView.setScaleX(size);
        imageView.setScaleY(size);
    }

    private void setTypeElevationCard(CardView cardToSet){
        cardToSet.setElevation(8);
        if (cardToSet == milk){
            wine.setCardElevation(0);
            cheese.setCardElevation(0);
        } else if (cardToSet == wine){
            milk.setCardElevation(0);
            cheese.setCardElevation(0);
        } else if (cardToSet == cheese){
            wine.setCardElevation(0);
            milk.setCardElevation(0);
        }
    }

    private void setSizeElevationCard(CardView cardToSet){
        if (cardToSet == small){
            big.setCardElevation(0);
            medium.setCardElevation(0);
        } else if (cardToSet == big){
            small.setCardElevation(0);
            medium.setCardElevation(0);
        } else if (cardToSet == medium){
            small.setCardElevation(0);
            big.setCardElevation(0);
        }
    }

    private void revertPackSize(){

        select_product_type.setVisibility(View.VISIBLE);
        pack_size_scrollView.setVisibility(View.GONE);
    }

    private void revertTypeElevationCard(){
        milk.setCardElevation(3);
        cheese.setCardElevation(3);
        wine.setCardElevation(3);

    }

    private void revertSizeElevationCard(){
        small.setCardElevation(3);
        big.setCardElevation(3);
        medium.setCardElevation(3);
    }

    private void defineCardChecker(CardView view, String[] valueArray, Drawable image, String productTypeSelected){
        Log.d("TAPPED ON CARD", view.toString());
            if (view.getCardElevation() < 8) {
                product_type_selected = productTypeSelected;
                setImagePackSize(valueArray, image, true);
                setTypeElevationCard(view);
            } else {
                revertPackSize();
                revertTypeElevationCard();
            }
        }

    private void getSelectedSize(CardView viewSize, TextView labelSize){
        if (viewSize.getCardElevation() < 8){
            selectedSize = Double.parseDouble(labelSize.getText().toString());
            setSizeElevationCard(viewSize);
        } else {
            revertSizeElevationCard();
        }
    }


}
