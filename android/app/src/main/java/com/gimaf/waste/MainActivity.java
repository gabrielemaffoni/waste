package com.gimaf.waste;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Map;

import static com.gimaf.waste.Item.EXPIRATION_DATE_DB;
import static com.gimaf.waste.Item.KEY_DB;
import static com.gimaf.waste.Item.MEASURE_DB;
import static com.gimaf.waste.Item.PRODUCT_TYPE_DB;
import static com.gimaf.waste.Item.QUANTITY_DB;
import static com.gimaf.waste.Item.QUANTITY_LEFT_DB;


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
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
            }
            return false;
        }
    };

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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent extras = getIntent();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        BottomNavigationView navigation = findViewById(R.id.navigation);
        Log.d("GABRIELE", "Activity On Create");
        bar = findViewById(R.id.bar);
        setSupportActionBar(bar);

        bar.inflateMenu(R.menu.right_bar);


        recyclerView = findViewById(R.id.products_list);
        update_button = findViewById(R.id.item_updated_button);
        update_message = findViewById(R.id.message_updated);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        createLayoutManager();


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

    public void get_token() {
        Log.d("GABRIELE", "Getting token");
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("Hello TAG", "getInstanceId failed", task.getException());
                            return;
                        }
                        String token = "";
                        try {
                            // Get new Instance ID token
                            token = task.getResult().getToken();
                        } catch (NullPointerException exception) {
                            Toast.makeText(getApplicationContext(), exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                        Map<String, Object> toUpload = new ArrayMap<>();
                        toUpload.put("token", token);
                        saveToken(toUpload);

                    }


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


    public void saveToken(Map<String, Object> token) {
        //UPload to firebase
        database = FirebaseDatabase.getInstance().getReference();
        database.child("users").child(currentUser.getUid()).updateChildren(token);
    }

    private void createLayoutManager() {
        Log.d("GABRIELE", "Creating layout manager");
        layoutManager = new LinearLayoutManager(this.getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        fetch();
    }

    private void fetch() {
        Log.d("GABRIELE", "Fetching");
        String uid = currentUser.getUid();
        Query query = FirebaseDatabase.getInstance()
                .getReference()
                .child("items").child(uid);
        Log.d("GABRIELE", "Path to the database: " + query.toString());
        //FirebaseRecyclerOptions<Item> options = new FirebaseRecyclerOptions.Builder<Item>().setQuery(query, Item.class).build();


        FirebaseRecyclerOptions<Item> options = new FirebaseRecyclerOptions.Builder<Item>().setQuery(query, new SnapshotParser<Item>() {

            @NonNull
            @Override
            public Item parseSnapshot(@NonNull DataSnapshot snapshot) {
                Item newItem = new Item();
                                Log.d("GABRIELE", "Found item. Creating item");
                                try {
                                    newItem.stringToDouble(QUANTITY_DB, snapshot.child(QUANTITY_DB).getValue().toString());
                                    newItem.stringToDouble(QUANTITY_LEFT_DB, snapshot.child(QUANTITY_LEFT_DB).getValue().toString());
                                    newItem.setProduct_type(snapshot.child(PRODUCT_TYPE_DB).getValue().toString());
                                    newItem.setExpiration_date(snapshot.child(EXPIRATION_DATE_DB).getValue().toString());
                                    newItem.setMeasure(snapshot.child(MEASURE_DB).getValue().toString());
                                    newItem.setItem_key(snapshot.getKey());
                                } catch (NullPointerException exception) {
                                    Log.d("Item list empty!", exception.getLocalizedMessage());
                                }
                return newItem;
            }
        }).build();




        recyclerAdapter = new FirebaseRecyclerAdapter<Item, ViewHolder>(options) {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                Log.d("GABRIELE", "Inflating in the adapter");
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.single_item_in_list, parent, false);

                return new ViewHolder(view);
            }


            @Override
            protected void onBindViewHolder(@NonNull ViewHolder holder, final int position, @NonNull final Item single_item) {
                holder.setType_and_quantity(single_item.getProduct_type() + " " + single_item.getTotal_quantity() + single_item.getMeasure());
                holder.setQuantity_left(String.format(getResources().getString(R.string.double_format), single_item.getCurrent_quantity()));
                holder.setMessage(single_item.getExpiration_date());
                holder.wrapper.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(view.getContext(), ItemView.class);
                        intent.putExtra(KEY_DB, single_item.getItem_key());
                        Log.d("EXTRA", intent.getExtras().getString(KEY_DB));
                        startActivity(intent);
                    }
                });
            }


        };
        recyclerView.setAdapter(recyclerAdapter);
    }

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

                        update_button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String product_key = dataSnapshot.getKey();
                                update_button.setVisibility(View.GONE);
                                update_message.setVisibility(View.GONE);
                                Intent intent = new Intent(getApplicationContext(), SetNewProduct.class);
                                intent.putExtra(KEY_DB, product_key);
                                startActivity(intent);
                            }
                        });

                    }
                } else if (!dataSnapshot.hasChildren()) {
                    //Do nothing
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.right_bar, menu);
        return true;
    }


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

    public void logout() {
        Intent logIn = new Intent(MainActivity.this, Login.class);
        FirebaseAuth.getInstance().signOut();
        startActivity(logIn);
        finish();
    }

}
