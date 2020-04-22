package com.example.fireapp;


import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fireapp.adapters.AdapterPosts;
import com.example.fireapp.models.ModelPost;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.google.firebase.storage.FirebaseStorage.getInstance;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {


    //firebase
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    StorageReference storageReference;

    String storagePath = "Users_Profile_Cover_Imgs/";

    //image views
    ImageView avatarIV, coverIV;
    TextView nameTV, emailTV, phoneTV;
    FloatingActionButton fab;
    RecyclerView postsRecyclerView;

    //progress dialog
    ProgressDialog pd;


    //Uri of picked image
    Uri image_uri;

    //checking profile or cover photo
    String profileOrCoverPhoto;

    //permissions constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    //arrays of permissions to be requested
    String cameraPermissions[];
    String storagePermissions[];

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");
        storageReference = getInstance().getReference(); // posibila probleam



        //init views
        avatarIV = view.findViewById(R.id.avatarIV);
        coverIV = view.findViewById(R.id.coverIV);
        nameTV = view.findViewById(R.id.nameTV);
        emailTV = view.findViewById(R.id.EmailTV);
        phoneTV = view.findViewById(R.id.PhoneTV);
        fab = view.findViewById(R.id.fab);
        postsRecyclerView = view.findViewById(R.id.recyclerview_posts);


        //init arrays of permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE};


        pd = new ProgressDialog(getActivity());

        //retrieve user detail using email
        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
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

        //fab button click
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        postList = new ArrayList<>();


        checkUserStatus();
        loadMyPosts();

        return view;
    }

    private void loadMyPosts() {
        //linear layout for recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager((getActivity()));
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
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void searchMyPosts(final String searchQuery) {
        //linear layout for recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager((getActivity()));
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
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private boolean checkStoragePermission(){
        //check if permission are enabled
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission() {

        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);

    }


    private boolean checkCameraPermission(){
        //check if permission are enabled

        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);


        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void requestCameraPermission() {

        requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);

    }



    private void showEditProfileDialog() {
        //t options in dialog

        String options[] = {"Edit Profile Picture", "Edit Cover Photo", "Edit Name", "Edit Phone"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Choose action");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog item click
                if (which == 0){
                    //profile image
                    pd.setMessage("Updating Profile Picture");
                    profileOrCoverPhoto = "image";

                    showImagePicDialog();

                } else if (which == 1){
                    //cover photo
                    pd.setMessage("Updating Cover Picture");
                    profileOrCoverPhoto = "cover";
                    showImagePicDialog();

                }else if (which == 2){
                    //Name
                    pd.setMessage("Updating Name");
                    showNamePhoneUpdateDialog("name");

                }else if (which == 3){
                    //phone
                    pd.setMessage("Updating Phone Number");
                    showNamePhoneUpdateDialog("phone");


                }
            }
        });
        //create and show dialog
        builder.create().show();

    }

    private void showNamePhoneUpdateDialog(final String key) { //final aici problema p[iuasydfgoiaejdghaidghioadushadoghkoasdjigbeakdgjbaehkbkhs

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update " + key);

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10, 10, 10, 10);

        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter " + key);

        linearLayout.addView(editText);

        builder.setView(linearLayout);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                final String value = editText.getText().toString().trim();

                if(!TextUtils.isEmpty((value))){

                    pd.show();
                    HashMap<String, Object> result = new HashMap<>();
                    result.put(key, value);

                    databaseReference.child(user.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                pd.dismiss();
                                    Toast.makeText(getActivity(), "Updated", Toast.LENGTH_SHORT).show();

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            pd.dismiss();
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });
                    if(key.equals("name")){
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = ref.orderByChild("uid").equalTo(uid);
                        query.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot ds: dataSnapshot.getChildren()){
                                    String child = ds.getKey();
                                    dataSnapshot.getRef().child(child).child("uName").setValue(value);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }else {

                    Toast.makeText(getActivity(), "Please enter " + key, Toast.LENGTH_SHORT).show();

                }


            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {



            }
        });
        builder.create().show();
    }

    private void showImagePicDialog() {


        String options[] = {"Camera", "Gallery"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Upload Image From");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog item click
                if (which == 0){
                    //Camera clicked
                    pd.setMessage("Updating Profile Picture");
                    showImagePicDialog();

                    if(!checkCameraPermission()){
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }


                } else if (which == 1){

                    if(!checkStoragePermission()){
                        requestStoragePermission();
                    } else {
                        pickFromGallery();
                    }
                    //Gallery clicked
                    pd.setMessage("Updating Cover Picture");

                }
            }
        });
        //create and show dialog
        builder.create().show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {

            case CAMERA_REQUEST_CODE:{

                if(grantResults.length > 0) {

                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if(cameraAccepted && writeStorageAccepted){
                        //permission enabled
                        pickFromCamera();

                    } else {
                        //permission denied
                        Toast.makeText(getActivity(), "Please enable camera & storage permission", Toast.LENGTH_SHORT).show();

                    }
                }

            }
            break;

            case STORAGE_REQUEST_CODE:{

                if(grantResults.length > 0) {

                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(writeStorageAccepted){
                        //permission enabled
                        pickFromGallery();

                    } else {
                        //permission denied
                        Toast.makeText(getActivity(), "Please enable storage permission", Toast.LENGTH_SHORT).show();

                    }
                }

            }
            break;

        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(resultCode == RESULT_OK){

            if(requestCode == IMAGE_PICK_GALLERY_CODE){

                image_uri = data.getData();
                uploadProfileCoverPhoto(image_uri);


            }

            if(requestCode == IMAGE_PICK_CAMERA_CODE){
//                if(data.getData() == null){
//
//                }
                image_uri = data.getData();
                uploadProfileCoverPhoto(image_uri);


            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto(Uri uri) {

        pd.show();
        String filePathAndName = storagePath + " " + profileOrCoverPhoto + "_" + user.getUid();

        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while(!uriTask.isSuccessful());

                        final Uri downloadUri = uriTask.getResult();

                        if(uriTask.isSuccessful()){

                            HashMap<String, Object> result = new HashMap<>();
                            result.put(profileOrCoverPhoto, downloadUri.toString());

                            databaseReference.child(user.getUid()).updateChildren(result)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            pd.dismiss();
                                            Toast.makeText(getActivity(), "Image Updated", Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                    pd.dismiss();
                                    Toast.makeText(getActivity(), "Error in updating image", Toast.LENGTH_SHORT).show();

                                }
                            });

                            if(profileOrCoverPhoto.equals("image")){
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                Query query = ref.orderByChild("uid").equalTo(uid);
                                query.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for(DataSnapshot ds: dataSnapshot.getChildren()){
                                            String child = ds.getKey();
                                            dataSnapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }

                        } else {

                            pd.dismiss();
                            Toast.makeText(getActivity(), "ERROR OCCURED", Toast.LENGTH_SHORT).show();

                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                pd.dismiss();
                Toast.makeText(getActivity(), ""+ e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void pickFromCamera() {
        //intent pick from gallery
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        //intent of start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);

    }

    private void pickFromGallery() {

        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);

    }

    private void checkUserStatus() {

        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if(user != null){
            //user signed in
            //set email for logged in user
            uid = user.getUid();

        } else {
            //user not signed in go back to MainActivity
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true); //to show menu option in fragment
        super.onCreate(savedInstanceState);
    }

    /*inflate options menu*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //inflate menu
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called on pres search button
                if(!TextUtils.isEmpty(s)){
                    //search
                    searchMyPosts(s);
                }else {
                    loadMyPosts();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
               //called on text change
                if(!TextUtils.isEmpty(s)){
                    //search
                    searchMyPosts(s);
                }else {
                    loadMyPosts();
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    /*handele menu item clicks*/

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();

        }

        if (id == R.id.action_add_post){
            startActivity(new Intent(getActivity(), AddPostActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}
