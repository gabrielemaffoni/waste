package com.gimaf.waste;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

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

public class SetNewProduct extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Spinner product_type_dropdown;
    private Spinner measure_dropdown;
    private EditText expiration_date_text;
    private EditText brand_text;
    private EditText pack_size_text;
    private Button save_button;
    private String measure_selected;
    private String product_type_selected;
    private Calendar expiration_date_calendar;
    private FirebaseUser currentUser;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_new_item);
        Intent data = getIntent();
        final String productKey = data.getStringExtra(KEY_DB);
        expiration_date_calendar = Calendar.getInstance();
        auth = FirebaseAuth.getInstance();

        product_type_dropdown = findViewById(R.id.product_type_spinner);
        measure_dropdown = findViewById(R.id.measure_choicer);
        expiration_date_text = findViewById(R.id.expiration_date_date);
        brand_text = findViewById(R.id.brand_value);
        pack_size_text = findViewById(R.id.amount_value);
        save_button = findViewById(R.id.save_product);
        setSpinner(product_type_dropdown, R.array.product_types);
        setSpinner(measure_dropdown, R.array.measures);

        currentUser = auth.getCurrentUser();

        /**
         * Shows a calendar view for the expiration date
         */
        expiration_date_text.setOnClickListener(new View.OnClickListener() {
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
                Double pack_size = Double.parseDouble(pack_size_text.getText().toString());
                String brand_final_text = brand_text.getText().toString();
                String expiration_date = expiration_date_text.getText().toString();

                measure_selected = measure_dropdown.getSelectedItem().toString();
                product_type_selected = product_type_dropdown.getSelectedItem().toString();


                if (pack_size == 0 || brand_final_text.isEmpty() || expiration_date.isEmpty() || measure_selected.isEmpty() || product_type_selected.isEmpty()) {
                    Toast.makeText(SetNewProduct.this, "Please, fill all the field first!", Toast.LENGTH_SHORT).show();
                } else {
                    if (pack_size_text.getText().toString().matches("[0-9]+")) {
                        updateFirebase(productKey, pack_size, brand_final_text, expiration_date);
                        Toast.makeText(SetNewProduct.this, "Product uploaded!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(SetNewProduct.this, "Please, insert numbers only!!", Toast.LENGTH_SHORT).show();
                        pack_size_text.setBackgroundColor(getResources().getColor(R.color.red_error, getTheme()));
                    }

                }


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
                resource_array, android.R.layout.simple_spinner_item);
        dropdown_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dropdown_adapter);

    }

    /**
     *  Checks what is selected in the Spinners and sets them on the field
     * @param parent the adapter view from which has been selected
     * @param view the view that has been clicked
     * @param position the position of the view
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == product_type_dropdown.getParent()) {
            product_type_selected = product_type_dropdown.getItemAtPosition(position).toString();
        }
        if (parent == measure_dropdown.getParent()) {
            measure_selected = measure_dropdown.getItemAtPosition(position).toString();
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
     * @param brandText the brand of the product
     * @param expirationDate the expiration date set by the user
     */
    public void updateFirebase(String productKey, Double packSize, String brandText,
                               String expirationDate) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("items").child(currentUser.getUid()).child(productKey);
        HashMap<String, Object> toUpdate = new HashMap<>();

        toUpdate.put(NEW_PRODUCT_DB, "placed");
        toUpdate.put(QUANTITY_DB, packSize);
        toUpdate.put(BRAND_DB, brandText);
        toUpdate.put(EXPIRATION_DATE_DB, expirationDate);
        toUpdate.put(MEASURE_DB, measure_selected);
        toUpdate.put(PRODUCT_TYPE_DB, product_type_selected);
        toUpdate.put(OPTIMAL_TEMPERATURE_DB, 0);
        toUpdate.put(QUANTITY_LEFT_DB, 0);
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
                        expiration_date_text.setText(chosenDate);
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.setTitle("Choose expiration date");
        datePickerDialog.show();
    }


}
