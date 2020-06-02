package com.example.pond;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class PostActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ProgressDialog loadingBar;
    private ImageButton SelectPostImage;
    private Button UpdatePostButton;
    private EditText PostDescription;

    private static final int Gallery_Pick = 1;
    private Uri ImageUri;
    private String Description;
    private StorageReference PostImagesReference;
    private DatabaseReference UsersRef, PostsRef;
    private FirebaseAuth mAuth;
    private String saveCurrentDate, saveCurrentTime, postRandomName, downloadUrl, current_user_id;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        //getting current user id
        mAuth = FirebaseAuth.getInstance();
        current_user_id = mAuth.getCurrentUser().getUid();

        PostImagesReference = FirebaseStorage.getInstance().getReference();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        PostsRef = FirebaseDatabase.getInstance().getReference().child("Posts");

        SelectPostImage = (ImageButton) findViewById(R.id.select_post_image);
        UpdatePostButton = (Button) findViewById(R.id.update_post_button);
        PostDescription = (EditText) findViewById(R.id.post_description);
        loadingBar = new ProgressDialog(this);

        mToolbar = (Toolbar) findViewById(R.id.update_post_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Update Post");

        SelectPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                OpenGallery();
            }
        });

        UpdatePostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ValidatePostInfo();
            }
        });

    }

    private void ValidatePostInfo()
    {
        Description = PostDescription.getText().toString();
        if(ImageUri == null) 
        {
            Toast.makeText(this, "Please select an image ", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(Description))
        {
            Toast.makeText(this, "Write a caption ", Toast.LENGTH_SHORT).show();
        }
        else
            {
                loadingBar.setTitle("Add new post");
                loadingBar.setMessage("Please wait, while we updating your new post");
                loadingBar.show();
                loadingBar.setCanceledOnTouchOutside(true);
                StoringImageToFirebaseStorage();
            }

    }

    private void StoringImageToFirebaseStorage()
    {
        //assign date to the post

        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
        saveCurrentDate = currentDate.format(calForDate.getTime());

        //assign time to post

        Calendar calForTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(calForDate.getTime());

        //save rand name to the post so the image won't be replaced by other users images

        postRandomName = saveCurrentDate+saveCurrentTime;


        //store image to the firebase storage

        StorageReference filePath = PostImagesReference.child("Post Images").child(ImageUri.getLastPathSegment() + postRandomName + ".jpg");
        filePath.putFile(ImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override

            // will tell the user if the image is stored successfully or not
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful())
                {
                    // get the link from firebase storage and save inside downloadUrl variable
                    downloadUrl = task.getResult().getStorage().getDownloadUrl().toString();
                    Toast.makeText(PostActivity.this, "Image uploaded successfully", Toast.LENGTH_LONG).show();
                    SavingPostInformationToDatabase();
                }

                else
                    {
                        String message = task.getException().getMessage();
                        Toast.makeText(PostActivity.this, "Error occurred: " + message, Toast.LENGTH_LONG).show();
                    }
            }
        });

    }

    private void SavingPostInformationToDatabase()
    {
        //retrieve profile image and user full name from the user node and store it inside the post node that store the post info
        //child is current user, which we get from a firebase auth
        UsersRef.child(current_user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                //if child exists
                if(dataSnapshot.exists())
                {
                    //get full name,username, post info from user db and store it inside the following variables
                    String userFullName = dataSnapshot.child("fullname").getValue().toString();
                    String userProfileImage = dataSnapshot.child("profileimage").getValue().toString();

                    HashMap postsMap = new HashMap();
                        postsMap.put("uid", current_user_id);
                        postsMap.put("date", saveCurrentDate);
                        postsMap.put("time", saveCurrentTime);
                        postsMap.put("description", Description);
                        postsMap.put("postimage", downloadUrl);
                        postsMap.put("profileimage", userProfileImage);
                        postsMap.put("fullname", userFullName);

                    PostsRef.child(current_user_id + postRandomName).updateChildren(postsMap)
                    .addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task)
                        {
                            if(task.isSuccessful())
                            {
                                SendUserToMainActivity();
                                Toast.makeText(PostActivity.this, "Post is updated", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                            else 
                                
                                {
                                    Toast.makeText(PostActivity.this, "Error occurred while updating your post. ", Toast.LENGTH_SHORT).show();
                                    loadingBar.dismiss();
                                }
                        }
                    });


                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void OpenGallery()
    {
        Intent galleryIntent = new Intent ();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, Gallery_Pick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Gallery_Pick && resultCode==RESULT_OK && data!=null)
        {
            ImageUri = data.getData();
            SelectPostImage.setImageURI(ImageUri);
        }
    }

    //back button


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)

    {
        int id = item.getItemId();

 //if user clicks back button

        if(id == android.R.id.home)

        {
           SendUserToMainActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    private void SendUserToMainActivity() {

        Intent mainIntent = new Intent(PostActivity.this, MainActivity.class);
        startActivity(mainIntent);

    }
}
