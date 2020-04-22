package com.example.fireapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fireapp.adapters.AdapterPosts;
import com.example.fireapp.models.ModelPost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class TheProfileActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;

    //image views
    ImageView avatarIV, coverIV;
    TextView nameTV, emailTV, phoneTV;

    RecyclerView postsRecyclerView;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_the_profile);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        avatarIV = findViewById(R.id.avatarIV);
        coverIV = findViewById(R.id.coverIV);
        nameTV = findViewById(R.id.nameTV);
        emailTV = findViewById(R.id.EmailTV);
        phoneTV = findViewById(R.id.PhoneTV);

        postsRecyclerView = findViewById(R.id.recyclerview_posts);

        firebaseAuth = FirebaseAuth.getInstance();

        //get uid of clicked user to retrieve his posts
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");

        //retrieve user detail using email
        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //check until get required data
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    //get data
                    String name = "" + ds.child("name").getValue();
                    String email = "" + ds.child("email").getValue();
                    String phone = "" + ds.child("phone").getValue();
                    String image = "" + ds.child("image").getValue();
                    String cover = "" + ds.child("cover").getValue();

                    nameTV.setText(name);
                    emailTV.setText(email);
                    phoneTV.setText(phone);
                    try {
                        //if image is recieved set
                        Picasso.get().load(image).into(avatarIV);

                    } catch (Exception e){
                        //if image is recieved set
                        Picasso.get().load(R.drawable.ic_default_img_white).into(avatarIV);

                    }

                    try {
                        //if image is recieved set
                        Picasso.get().load(cover).into(coverIV);

                    } catch (Exception e){
                        //if image is recieved set

                    }

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        postList = new ArrayList<>();

        checkUserStatus();
        loadHistPosts();

    }

    private void loadHistPosts()    {
        //linear layout for recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //show newest posts first, for this load from last ??
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set this layout to recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);

        //init posts list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load posts
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelPost myPost = ds.getValue(ModelPost.class);

                    //add to list
                    postList.add(myPost);

                    //adapter
                    adapterPosts = new AdapterPosts(TheProfileActivity.this, postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TheProfileActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void searchHistPosts(final String searchQuery){
        //linear layout for recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(TheProfileActivity.this);
        //show newest posts first, for this load from last ??
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set this layout to recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);

        //init posts list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load posts
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelPost myPost = ds.getValue(ModelPost.class);

                    if(myPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            myPost.getpDescr().toLowerCase().contains(searchQuery.toLowerCase())){

                        postList.add(myPost);
                    }


                    //adapter
                    adapterPosts = new AdapterPosts(TheProfileActivity.this, postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TheProfileActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void checkUserStatus() {

        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if(user != null){
            //user signed in
            //set email for logged in user

        } else {
            //user not signed in go back to MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_add_post).setVisible(false); //hide add post


        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called on pres search button
                if(!TextUtils.isEmpty(s)){
                    //search
                    searchHistPosts(s);
                }else {
                    loadHistPosts();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called on text change
                if(!TextUtils.isEmpty(s)){
                    //search
                    searchHistPosts(s);
                }else {
                    loadHistPosts();
                }
                return false;
            }
        });


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();

        }
        return super.onOptionsItemSelected(item);
    }
}
