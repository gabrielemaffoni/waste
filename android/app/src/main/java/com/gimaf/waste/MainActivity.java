package com.gimaf.waste;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.*;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Map;

import static com.gimaf.waste.Item.EXPIRATION_DATE_DB;
import static com.gimaf.waste.Item.EXPIRATION_DAYS_DB;
import static com.gimaf.waste.Item.KEY_DB;
import static com.gimaf.waste.Item.MEASURE_DB;
import static com.gimaf.waste.Item.PRODUCT_TYPE_DB;
import static com.gimaf.waste.Item.QUANTITY_DB;
import static com.gimaf.waste.Item.QUANTITY_LEFT_DB;

/**
 * This activity will show the list of monitored products. It uses FirebaseRecyclerAdapter which should be faster for a prototype.
 *
 */

public class MainActivity extends AppCompatActivity {
    private DatabaseReference database;
    private Button update_button;
    private TextView update_message;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private FirebaseUser currentUser;
    private Toolbar bar;
    private FirebaseRecyclerAdapter recyclerAdapter;
    private FirebaseAuth auth;
    private ConstraintLayout container;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        /**
         * This is the method required to make the nav bar items work
         * @param item The single item in the navbar
         * @return false
         */
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
            }
            return false;
        }
    };


    /**
     * The activity will start checking for data updates
     */
    @Override
    protected void onStart() {
        super.onStart();
        recyclerAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        recyclerAdapter.stopListening();
    }

    /**
     * On creating the activity, the code will check if there is new data on the database.
     * Meanwhile, it will create also on the top right of the navbar a menu item in which "Log out" button is available.
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        BottomNavigationView navigation = findViewById(R.id.navigation);
        Log.d("GABRIELE", "Activity On Create");
        bar = findViewById(R.id.bar);
        setSupportActionBar(bar);
        container = findViewById(R.id.container);
        //Inflates the menu
        bar.inflateMenu(R.menu.right_bar);

        // Sets the list
        recyclerView = findViewById(R.id.products_list);
        update_button = findViewById(R.id.item_updated_button);
        update_message = findViewById(R.id.message_updated);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        //Creates the layout of the list
        createLayoutManager();
        enableSwipeToDeleteAndUndo();
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d("GABRIELE", "On post resume");
        listenToChanged();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.d("GABRIELE", "On post create");
        get_token();
        listenToChanged();
    }

    /**
     * Gets the token of the device and uploads it on the database.
     * It is required to send notifications from Firebase Cloud Messaging.
     */
    public void get_token() {
        Log.d("TOKEN", "Getting token");
        //Firebase builtin method
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("TOKEN", "getInstanceId failed", task.getException());
                            return;
                        }
                        String token = "";
                        try {
                            // Get new Instance ID token
                            token = task.getResult().getToken();
                        } catch (NullPointerException exception) {
                            // If there is any null pointer exception it will show it as a toast.
                            Toast.makeText(getApplicationContext(), exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                        //If everything goes right, it will upload on the database the token using saveToken(Map) method.
                        Map<String, Object> toUpload = new ArrayMap<>();
                        toUpload.put("token", token);
                        saveToken(toUpload);

                    }

                    //If there is a new token, it will be uploaded
                    public void onNewToken(String token) {
                        Log.d("Tokens!", "Refreshed token: " + token);

                        // If you want to send messages to this application instance or
                        // manage this apps subscriptions on the server side, send the
                        // Instance ID token to your app server.
                        Map<String, Object> toUpload = new ArrayMap<>();
                        toUpload.put("token", token);
                        saveToken(toUpload);
                    }
                });
    }

    /**
     * Uploads the token on firebase under "users"-
     *
     * @param token A map that contains "token": the token to upload
     */
    //Saves the token on firebase
    public void saveToken(Map<String, Object> token) {
        //Upload on Firebase
        database = FirebaseDatabase.getInstance().getReference();
        database.child("users").child(currentUser.getUid()).updateChildren(token);
    }

    /**
     * Required for the recyclerview
     */
    private void createLayoutManager() {
        Log.d("GABRIELE", "Creating layout manager");
        layoutManager = new LinearLayoutManager(this.getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        fetch();
    }

    /**
     * Converts the value from ml to pints and liters
     *
     * @param measure_pack          The measure decided by the user
     * @param total_quantity_amount The total amount
     * @return The converted value, if any.
     */

    private double convert_value_by_measure(String measure_pack, Double total_quantity_amount) {
        Double converted_value = 0.0;
        if (measure_pack.equals("Pints")) {
            converted_value = total_quantity_amount / 568;
        } else if (measure_pack.equals("L")) {
            converted_value = total_quantity_amount / 1000;
        } else {
            converted_value = total_quantity_amount;
        }
        return converted_value;
    }

    /**
     * The code simply queries the Database and asks it if there is a new Item there. If so it will display a card for each item
     * TODO: Empty screen
     */

    private void fetch() {
        Log.d("GABRIELE", "Fetching");
        String uid = currentUser.getUid();
        Query query = FirebaseDatabase.getInstance()
                .getReference()
                .child("items").child(uid);
        Log.d("GABRIELE", "Path to the database: " + query.toString());

        FirebaseRecyclerOptions<Item> options = new FirebaseRecyclerOptions.Builder<Item>().setQuery(query, new SnapshotParser<Item>() {

            @NonNull
            @Override
            public Item parseSnapshot(@NonNull DataSnapshot snapshot) {
                Item newItem = new Item();
                Log.d("Fetch", "Found item. Creating item");

                if (!snapshot.child("new_product").getValue().toString().equals("yes")) {
                    try {
                        // Assigns to the new item the right amount of data
                        newItem.setMeasure(snapshot.child(MEASURE_DB).getValue().toString());
                        newItem.stringToDouble(QUANTITY_DB, snapshot.child(QUANTITY_DB).getValue().toString());
                        Double total_quantity_converted = convert_value_by_measure(newItem.getMeasure(), newItem.getTotal_quantity());
                        newItem.setTotal_quantity(total_quantity_converted);
                        newItem.setProduct_type(snapshot.child(PRODUCT_TYPE_DB).getValue().toString());
                        newItem.setExpiration_date(snapshot.child(EXPIRATION_DATE_DB).getValue().toString());
                        newItem.stringToDouble(QUANTITY_LEFT_DB, snapshot.child(QUANTITY_LEFT_DB).getValue().toString());
                        Double quantity_converted = convert_value_by_measure(newItem.getMeasure(), newItem.getCurrent_quantity());
                        newItem.setCurrent_quantity(quantity_converted);
                        if (snapshot.hasChild(EXPIRATION_DAYS_DB)) {
                            newItem.setExpiration_days(Integer.parseInt(snapshot.child(EXPIRATION_DAYS_DB).getValue().toString()));
                        }
                        newItem.setItem_key(snapshot.getKey());

                    } catch (NullPointerException exception) {
                        Log.d("Item list empty!", exception.getLocalizedMessage());
                    }
                }
                return newItem;

            }
        }).build();

        // Adds the data to the recycler view using custom ViewHolder class.
        recyclerAdapter = new FirebaseRecyclerAdapter<Item, ViewHolder>(options) {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                Log.d("GABRIELE", "Inflating in the adapter");
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.single_item_in_list, parent, false);

                return new ViewHolder(view);
            }

            /**
             * This class will check whether a single item is clicked. If so, it will send to the detailed view page which will show the items detail
             * @param holder the Viewholder
             * @param position The position of the clicked item
             * @param single_item The single item on which we will save the data
             */

            @Override
            protected void onBindViewHolder(@NonNull ViewHolder holder, final int position, @NonNull final Item single_item) {
                if (single_item.getCurrent_quantity() != null) {

                    holder.setType_and_quantity(single_item.getProduct_type() + " " + single_item.getTotal_quantity() + " " + single_item.getMeasure());
                    holder.setQuantity_left(String.format(getResources().getString(R.string.double_format), single_item.getCurrent_quantity()));
                    if (single_item.getCurrent_quantity() <= 0) {
                        holder.setQuantity_left(getString(R.string.finished_product));
                        holder.quantity_left.setTextColor(getColor(R.color.red_error));
                    }
                    holder.setMessage(single_item.getExpiration_date());
                    if (single_item.getExpiration_days() >= 5) {
                        holder.message.setTextColor(getColor(R.color.colorAccent));
                    } else if (single_item.getExpiration_days() > 2) {
                        holder.message.setTextColor(getColor(R.color.yellow));
                    } else if (single_item.getExpiration_days() <= 2) {
                        holder.message.setTextColor(getColor(R.color.red_error));
                    }

                    holder.wrapper.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //If the item is clicked it will send also the item key, which will be useful to gather data from the database
                            Intent intent = new Intent(view.getContext(), ItemView.class);
                            intent.putExtra(KEY_DB, single_item.getItem_key());
                            Log.d("EXTRA", single_item.getItem_key());
                            startActivity(intent);
                        }
                    });
                }


            }


        };
        //Shows the list
        recyclerView.setAdapter(recyclerAdapter);
    }

    /**
     * If there is a new item, the view will show the button.
     */
    private void listenToChanged() {
        database = FirebaseDatabase.getInstance().getReference();
        String uid = currentUser.getUid();
        database.child("items").child(uid).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull final DataSnapshot dataSnapshot, @Nullable String s) {
                if (dataSnapshot.hasChild("new_product")) {


                    if (dataSnapshot.child("new_product").getValue().equals("yes")) {
                        update_button.setVisibility(View.VISIBLE);
                        update_message.setVisibility(View.VISIBLE);
                        /**
                         * If the button is clicked, the view will change to "SetNewProduct".
                         */
                        update_button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String product_key = dataSnapshot.getKey();
                                update_button.setVisibility(View.GONE);
                                update_message.setVisibility(View.GONE);
                                Intent intent = new Intent(getApplicationContext(), SetNewProduct.class);
                                intent.putExtra(KEY_DB, product_key); // The key will be useful later on to save the data on the database
                                Log.d("EXTRA PUT", product_key);
                                startActivity(intent);
                            }
                        });

                    }
                } else if (!dataSnapshot.hasChildren()) {
                    //Do nothing
                    Log.d("Listen to changed", "No items");
                }

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Inflates the menu on the right top angle
     *
     * @param menu the menu to be inflated
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.right_bar, menu);
        return true;
    }

    /**
     * In case people tap on "Logout" it will log out from the main user
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout_bar:
                logout();
                break;
        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    /**
     * Logs out the user and goes back to Login page
     */
    public void logout() {
        Intent logIn = new Intent(MainActivity.this, Login.class);
        FirebaseAuth.getInstance().signOut();
        startActivity(logIn);
        finish();
    }

    private void enableSwipeToDeleteAndUndo() {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {


                final int position = viewHolder.getAdapterPosition();
                final String keyToDelete = recyclerAdapter.getRef(position).getKey();
                final String itemName = recyclerAdapter.getRef(position).child("product_type").toString();
                if (!( keyToDelete== null)) {
                    database.child("items").child(currentUser.getUid()).child(keyToDelete).removeValue(new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@androidx.annotation.Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            String textToPrint = itemName + getResources().getString(R.string.item_removed);
                            Snackbar snackbar = Snackbar
                                    .make(container, textToPrint, Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }



            }

        };
        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
    }


}