package com.gimaf.waste;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This simple viewHolder class will get all the data and find them in the view of the single card
 * Ready to be inglated
 */

public class ViewHolder extends RecyclerView.ViewHolder {
    public LinearLayout right;
    public LinearLayout left;
    public LinearLayout wrapper;
    public TextView type_and_quantity;
    public TextView quantity_left;
    public TextView message;
    public Item item;

    public ViewHolder(View itemView) {
        super(itemView);
        right = itemView.findViewById(R.id.right_data);
        left = itemView.findViewById(R.id.left_data);
        wrapper = itemView.findViewById(R.id.wrapper);
        type_and_quantity = itemView.findViewById(R.id.type_and_total_quantity);
        quantity_left = itemView.findViewById(R.id.current_quantity);
        message = itemView.findViewById(R.id.brand_table_view);
        item = new Item();
    }


    public void setType_and_quantity(String type_and_quantity) {
        this.type_and_quantity.setText(type_and_quantity);
    }


    public void setQuantity_left(String quantity_left) {
        this.quantity_left.setText(quantity_left);
    }


    public void setMessage(String message) {
        this.message.setText(message);
    }

    public void setItem(Item item){
        this.item = item;
    }
}
