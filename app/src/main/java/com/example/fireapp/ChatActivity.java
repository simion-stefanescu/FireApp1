package com.example.fireapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fireapp.adapters.AdapterChat;
import com.example.fireapp.models.ModelChat;
import com.example.fireapp.models.ModelUser;
import com.example.fireapp.notifications.APIService;
import com.example.fireapp.notifications.Client;
import com.example.fireapp.notifications.Data;
import com.example.fireapp.notifications.Response;
import com.example.fireapp.notifications.Sender;
import com.example.fireapp.notifications.Token;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;

public class ChatActivity extends AppCompatActivity {


    //views
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileIv;
    TextView nameTv, userStatusTv;
    EditText messageEt;
    ImageButton sendBtn;

    //firebase auth
    FirebaseAuth firebaseAuth;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersDbRef;
    //for checking user has sent messages or not
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;

    List<ModelChat> chatList;
    AdapterChat adapterChat;

    String hisUid;
    String myUid;
    String hisImage;

    APIService apiService;
    boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        //init views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        recyclerView = findViewById(R.id.chat_recyclerView);
        profileIv = findViewById(R.id.profileIV);
        nameTv = findViewById(R.id.nameTV);
        userStatusTv = findViewById(R.id.userStatusTV);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);

        //Layout (Linear layout) for Recyclerviewer
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);

        //recyclerviewer properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        //create api service
        apiService = Client.getRetrofit("https://fcm.googleapis.com/").create(APIService.class);


        //get uid here to get name picture and start chat
        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        //firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();

        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDbRef = firebaseDatabase.getReference("Users");

        //search to get user's info
        Query userQuery = usersDbRef.orderByChild("uid").equalTo(hisUid);
        //get user picture and name
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //check until required info is recieved
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    //get data
                    String name ="" +  ds.child("name").getValue();
                    hisImage ="" +  ds.child("image").getValue();
                    String typingStatus = "" +  ds.child("typingTo").getValue();

                    //check typing status
                    if(typingStatus.equals(myUid)){
                        userStatusTv.setText("typing...");
                    }
                    else {
                        //get value of onlinestatus
                        String onlineStatus = "" + ds.child("onlineStatus").getValue();
                        if(onlineStatus.equals("online")){
                            userStatusTv.setText(onlineStatus);
                        }else {
                            //convert timestamp
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();
                            userStatusTv.setText("Last Seen: " + dateTime);
                        }
                    }

                    //set data
                    nameTv.setText(name);
                    try {
                        //image recieved put it in toolbar
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img_white).into(profileIv);

                    } catch(Exception e){
                        //put placeholder in toolbar
                        Picasso.get().load(R.drawable.ic_default_img_white).into(profileIv);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //click button to send image
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                // get text from edit text
                String message = messageEt.getText().toString().trim();
                //check if text is empty or not
                if(TextUtils.isEmpty(message)){
                    //text is empty
                    Toast.makeText(ChatActivity.this, "Write a message...", Toast.LENGTH_SHORT).show();
                }else {
                    //text is not empty
                    sendMessage(message);
                    
                }

                //resset edit text after sending message
                messageEt.setText("");
            }
        });

        //check edit text change listener
        messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().length() == 0){
                    checkTypingStatus("noOne");
                } else {
                    checkTypingStatus(hisUid); //uid of receiver
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        readMessages();
        seenMessage();

    }

    @Override
    protected void onPause() {
        super.onPause();
        //set ofline with last online timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        checkOnlineStatus(timestamp);
        checkTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        checkOnlineStatus("online");
        super.onResume();
    }

    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)){ //am interschimbat hisUid cu myUid
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void readMessages() {

        chatList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) ||
                            chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)){
                        chatList.add(chat);
                    }

                    //adapter
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    adapterChat.notifyDataSetChanged();
                    //set adapter to recyclerview
                    recyclerView.setAdapter(adapterChat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {


            }
        });

    }

    private void sendMessage(final String message) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timeStamp", timestamp);
        hashMap.put("isSeen", false);
        databaseReference.child("Chats").push().setValue(hashMap);


        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid); //final??
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelUser user = dataSnapshot.getValue(ModelUser.class);

                if(notify){
                    sendNotificagtion(hisUid, user.getName(), message);
                }

                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotificagtion(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid, name + " : " + message, "New Message", hisUid, R.drawable.ic_default_img);

                    Sender sender = new Sender(data, token.getToken());
                    apiService.sendNotification(sender)
                            .enqueue(new Callback<Response>() {
                                @Override
                                public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                                    Toast.makeText(ChatActivity.this, "" + response.message(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Call<Response> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkUserStatus() {

        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if(user != null){
            //user signed in
            //set email for logged in user
            myUid = user.getUid();

        } else {
            //user not signed in go back to MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }

    private void checkOnlineStatus(String status){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus",status);
        //update value of onlineStatus of current user
        dbRef.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo",typing);
        //update value of onlineStatus of current user
        dbRef.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        //set online
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide searchview, ad post
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();

        }

        return super.onOptionsItemSelected(item);
    }
}
