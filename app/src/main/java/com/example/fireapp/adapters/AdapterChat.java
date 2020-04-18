package com.example.fireapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fireapp.R;
import com.example.fireapp.models.ModelChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    Context context;
    List<ModelChat> chatList;
    String imageUrl;

    FirebaseUser fUser;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //inflate layouts: row left for  receiver and row right for sender
        if(i == MSG_TYPE_RIGHT){
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, viewGroup, false);
            return new MyHolder(view);
        }else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, viewGroup, false);
            return new MyHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        //get data
        String message = chatList.get(position).getMessage();
        String timeStamp = chatList.get(position).getTimestamp();
        boolean seen = chatList.get(position).isSeen();

        //convert timestamp
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
    //    cal.setTimeInMillis(Long.parseLong(timeStamp));  //aici il aduce null pe timestamp desi nu e null trebuie investigat
        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();

        //set data
        holder.messageTV.setText(message);
        holder.timeTV.setText(dateTime);
        try {
            Picasso.get().load(imageUrl).into(holder.profileIV);

        }catch(Exception e){

        }

        //click to show delete dialog
        holder.messageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show delete message confirm dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure ?");
                //delete button
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMessage(position);
                    }
                });
                //cancel delete button
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //dismiss dialog
                        dialog.dismiss();
                    }
                });
                //create and show dialog
                builder.create().show();

            }
        });

        //set seend/delivered status of message
        if (position == chatList.size() - 1){
            if (chatList.get(position).isSeen()){  //isSeen vine false chiar daca in database vine true
                holder.isSeenTV.setText("Seen");
            }else {
                holder.isSeenTV.setText("Delivered");
            }
        } else {
            holder.isSeenTV.setVisibility(View.GONE);
        }


    }

    private void deleteMessage(int position) {
        final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        /* get time of clicked message and compare it with time of all messages and where values match deleteit */
        String msgTimeStamp = chatList.get(position).getTimestamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    //if we want to allow only the sender to delete compare the uid of who is deleting with sender uid

                    if(ds.child("sender").getValue().equals(myUid)){
                    //doar sterg
                      //  ds.getRef().removeValue();

                        //we can display message was removed instead
                    HashMap<String ,Object> hashMap = new HashMap<>();
                    hashMap.put("message", "Message is deleted");
                    ds.getRef().updateChildren(hashMap);
                        Toast.makeText(context, "message deleted...", Toast.LENGTH_SHORT).show();
                    } else{

                        Toast.makeText(context, "You can delete only your own messages...", Toast.LENGTH_SHORT).show();

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        //get currently logged in user
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        if(chatList.get(position).getSender().equals(fUser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        //views
        ImageView profileIV;
        TextView messageTV, timeTV, isSeenTV;
        LinearLayout messageLayout;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //inti views
            profileIV = itemView.findViewById(R.id.profileIV);
            messageTV = itemView.findViewById(R.id.messageTV);
            timeTV = itemView.findViewById(R.id.timeTV);
            isSeenTV = itemView.findViewById(R.id.isSeenTV);
            messageLayout = itemView.findViewById(R.id.messageLayout);

        }
    }
}
