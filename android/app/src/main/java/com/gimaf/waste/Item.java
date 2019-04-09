package com.gimaf.waste;


import android.content.Context;
import android.util.ArrayMap;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * The Item class in Java has several variables.
 * It mainly deals with the showing and creation of a single item all around the app.
 */

public class Item {
    //CONSTANTS REQUIRED FOR THE DATABASE
    public static final String EXTERNAL_TEMPERATURE_DB = "ext_temperature";
    public static final String INTERNAL_TEMPERATURE_DB = "int_temperature";
    public static final String QUANTITY_DB = "total_quantity"; // The size of the packaging
    public static final String BRAND_DB = "brand";
    public static final String QUANTITY_LEFT_DB = "quantity_left";
    public static final String OPTIMAL_TEMPERATURE_DB = "opt_temperature";
    public static final String EXPIRATION_DATE_DB = "expiration_date";
    public static final String PRODUCT_TYPE_DB = "product_type";
    public static final String MEASURE_DB = "measure";
    public static final String KEY_DB = "item_key";
    public static final String NEW_PRODUCT_DB = "new_product";
    private Double current_quantity;
    private Double total_quantity;
    private String measure;
    private String product_type;
    private String expiration_date;
    private String item_key;
    private String brand_product;
    private Double internal_temperature;
    private Double external_temperature;
    private Double optimal_temperature;

    public Item() {
        //Default required for Datasnapshot
    }

    public Item(String item_key, Double current_quantity, Double total_quantity, String measure, String product_type, String expiration_date, String brand, Double internal_temperature, Double external_temperature, Double optimal_temperature) {
        this.item_key = item_key;
        this.current_quantity = current_quantity;
        this.total_quantity = total_quantity;
        this.measure = measure;
        this.product_type = product_type;
        this.expiration_date = expiration_date;
        this.brand_product = brand;
        this.internal_temperature = internal_temperature;
        this.external_temperature = external_temperature;
        this.optimal_temperature = optimal_temperature;

    }

    /**
     * Setters and getters
     */


    public Double getCurrent_quantity() {
        return current_quantity;
    }

    public void setCurrent_quantity(Double current_quantity) {
        this.current_quantity = current_quantity;
    }

    public Double getTotal_quantity() {
        return total_quantity;
    }

    public void setTotal_quantity(Double total_quantity) {
        this.total_quantity = total_quantity;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public String getProduct_type() {
        return product_type;
    }

    public void setProduct_type(String product_type) {
        this.product_type = product_type;
    }

    public String getExpiration_date() {
        return expiration_date;
    }

    public void setExpiration_date(String expiration_date) {
        this.expiration_date = expiration_date;
    }

    public Double getOptimal_temperature() {
        return optimal_temperature;
    }

    public void setOptimal_temperature(Double optimal_temperature) {
        this.optimal_temperature = optimal_temperature;
    }

    public String getItem_key() {
        return item_key;
    }

    public void setItem_key(String item_key) {
        this.item_key = item_key;
    }

    public Double getInternal_temperature() {
        return internal_temperature;
    }

    public void setInternal_temperature(Double internal_temperature) {
        this.internal_temperature = internal_temperature;
    }

    public Double getExternal_temperature() {
        return external_temperature;
    }

    public void setExternal_temperature(Double external_temperature) {
        this.external_temperature = external_temperature;
    }

    public String getBrand() {
        return brand_product;
    }

    public void setBrand(String brand) {
        this.brand_product = brand;
    }

    /**
     * Converts input Strings to double when required
     * @param thingToSet What is the key of the value
     * @param value The actual value
     */
    public void stringToDouble(String thingToSet, String value) {
        Double convertedValue = Double.parseDouble(value);
        switch (thingToSet) {
            case QUANTITY_DB:
                this.setTotal_quantity(convertedValue);
                break;
            case QUANTITY_LEFT_DB:
                this.setCurrent_quantity(convertedValue);
                break;
            case EXTERNAL_TEMPERATURE_DB:
                this.setExternal_temperature(convertedValue);
                break;
            case INTERNAL_TEMPERATURE_DB:
                this.setInternal_temperature(convertedValue);
                break;
            case OPTIMAL_TEMPERATURE_DB:
                this.setOptimal_temperature(convertedValue);
                break;

        }

    }

    /**
     * Required from Firebase to convert the data
     * @return
     */
    @Exclude
    public Map<String, Object> toMap() {

        HashMap<String, Object> all_data = new HashMap<>();

        all_data.put(PRODUCT_TYPE_DB, this.product_type);
        all_data.put(EXPIRATION_DATE_DB, this.expiration_date);
        all_data.put(INTERNAL_TEMPERATURE_DB, this.internal_temperature);
        all_data.put(EXTERNAL_TEMPERATURE_DB, this.external_temperature);
        all_data.put(OPTIMAL_TEMPERATURE_DB, this.optimal_temperature);
        all_data.put(BRAND_DB, this.brand_product);
        all_data.put(QUANTITY_LEFT_DB, this.current_quantity);
        all_data.put(MEASURE_DB, this.measure);
        all_data.put(QUANTITY_DB, this.total_quantity);


        return all_data;
    }

    /**
     * Sets the data of the item from a Datasnashot from Firebase
     * @param dataSnapshot What is generated by a Query
     * @param context The context of the app
     */
    public void setFromDataSnapshot(DataSnapshot dataSnapshot, Context context) {
        this.setItem_key(dataSnapshot.getKey());
        //Log.d("SNAPSHOT", dataSnapshot.getRef().getPath().toString());
        //Log.d("BRAND", dataSnapshot.child(BRAND_DB).getValue().toString());
        try {
            this.setBrand(dataSnapshot.child(BRAND_DB).getValue().toString());
            this.setMeasure(dataSnapshot.child(MEASURE_DB).getValue().toString());
            //this.setProduct_name(dataSnapshot.child(PRODUCT_NAME_DB).getValue().toString());
            this.setProduct_type(dataSnapshot.child(PRODUCT_TYPE_DB).getValue().toString());
            this.setExpiration_date(dataSnapshot.child(EXPIRATION_DATE_DB).getValue().toString());
            this.stringToDouble(QUANTITY_DB, dataSnapshot.child(QUANTITY_DB).getValue().toString());
            this.stringToDouble(QUANTITY_LEFT_DB, dataSnapshot.child(QUANTITY_LEFT_DB).getValue().toString());
            this.stringToDouble(INTERNAL_TEMPERATURE_DB, dataSnapshot.child(INTERNAL_TEMPERATURE_DB).getValue().toString());
            this.stringToDouble(EXTERNAL_TEMPERATURE_DB, dataSnapshot.child(EXTERNAL_TEMPERATURE_DB).getValue().toString());
            this.stringToDouble(OPTIMAL_TEMPERATURE_DB, dataSnapshot.child(OPTIMAL_TEMPERATURE_DB).getValue().toString());
        } catch (NullPointerException exception) {
            Toast.makeText(context, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
