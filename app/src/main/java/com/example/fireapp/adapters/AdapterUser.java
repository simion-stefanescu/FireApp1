package com.example.fireapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fireapp.ChatActivity;
import com.example.fireapp.R;
import com.example.fireapp.TheProfileActivity;
import com.example.fireapp.models.ModelUser;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterUser extends RecyclerView.Adapter<AdapterUser.Holder>{

    Context context;
    List<ModelUser> userList;

    public AdapterUser(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        //inflate layout
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, viewGroup, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        //get data
        final String hisUID = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        final String userEmail = userList.get(position).getEmail();

        //set data
        holder.mNameTV.setText(userName);
        holder.mEmailTV.setText(userEmail);

        try{

            Picasso.get().load(userImage)
                    .placeholder(R.drawable.ic_default_img)
                    .into(holder.mAvatarIV);

        }catch(Exception e){

        }
        //handle item click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0){
                            //profile clicked
                            Intent intent = new Intent(context, TheProfileActivity.class);
                            intent.putExtra("uid", hisUID);  //m-a obligat sa fac uid final desi nu trebuia
                            context.startActivity(intent);
                        }
                        if(which == 1){
                            //chat clicked
                            //Click user from list to start messaging activity by putting uid of reciever and identify the user by uid
                            Intent intent = new Intent(context, ChatActivity.class);
                            intent.putExtra("hisUid", hisUID);
                            context.startActivity(intent);
                        }
                    }
                });
                builder.create().show();

            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }
    //view holder class

    class Holder extends RecyclerView.ViewHolder{

        ImageView mAvatarIV;
        TextView mNameTV, mEmailTV;

        public Holder(@NonNull View itemView) {
            super(itemView);

            mAvatarIV = itemView.findViewById(R.id.avatarIV);
            mNameTV = itemView.findViewById(R.id.nameTV);
            mEmailTV = itemView.findViewById(R.id.EmailTV);

        }
    }

}
