package com.example.pond;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private RecyclerView postList;
    private Toolbar mToolbar;
    private ImageButton AddNewPostButton;

    private CircleImageView NavProfileImage;
    private TextView NavProfileUserName;
    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef, PostsRef;

    String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        PostsRef = FirebaseDatabase.getInstance().getReference().child("Posts");

        mToolbar = (Toolbar) findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Home");
        AddNewPostButton = (ImageButton) findViewById(R.id.add_new_post_button);



        drawerLayout = (DrawerLayout) findViewById(R.id.drawable_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, drawerLayout,  R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);


        // displaying the feed
        postList = (RecyclerView) findViewById(R.id.all_users_post_list);
        postList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        postList.setLayoutManager(linearLayoutManager);


        View navView = navigationView.inflateHeaderView(R.layout.navigation_header);
        NavProfileImage = (CircleImageView) navView.findViewById(R.id.nav_profile_image);
        NavProfileUserName = (TextView) navView.findViewById(R.id.nav_user_full_name);



        UsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    if(dataSnapshot.hasChild("fullname"))

                    {
                        String fullname = dataSnapshot.child("fullname").getValue().toString();
                        NavProfileUserName.setText(fullname);
                    }
                    if(dataSnapshot.hasChild("profileimage"))
                    {
                        String image = dataSnapshot.child("profileimage").getValue().toString();
                        Picasso.get().load(image).placeholder(R.drawable.profile).into(NavProfileImage);
                    } else
                    {
                        Toast.makeText(MainActivity.this, "This profile does nor exist", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                UserMenuSelector(item);

                return false;
            }
        });

        AddNewPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendUserToPostActivity();
            }
        });

        DisplayAllUsersPosts();
    }

    private void DisplayAllUsersPosts()
    {
        FirebaseRecyclerOptions<Posts> options =
                new FirebaseRecyclerOptions.Builder<Posts>()
                        .setQuery(PostsRef, Posts.class)
                        .build();

        FirebaseRecyclerAdapter firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Posts, PostsViewHolder>(options) {
            @Override
            public PostsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from( parent.getContext() ).inflate( R.layout.all_posts_layout,parent,false );
                return new PostsViewHolder( view );
            }

            protected void onBindViewHolder(@NonNull PostsViewHolder viewHolder, int position, @NonNull Posts model) {

                final String PostKey= getRef(position).getKey();

                viewHolder.setFullname(model.getFullname());
                viewHolder.setTime(model.getTime());
                viewHolder.setDate(model.getDate());
                viewHolder.setDescription(model.getDescription());
                viewHolder.setProfileimage(getApplicationContext(), model.getProfileimage());
                viewHolder.setPostimage(getApplicationContext(), model.getPostimage());


                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        Intent clickPostIntent = new Intent(MainActivity.this, ClickPostActivity.class);
                        clickPostIntent.putExtra("PostKey",PostKey);
                        startActivity(clickPostIntent);
                    }
                });

            }

        };
        firebaseRecyclerAdapter.startListening();
        postList.setAdapter(firebaseRecyclerAdapter);


    }

    public static class PostsViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        public PostsViewHolder(@NonNull View itemView)
        {
            super(itemView);
            mView = itemView;
        }

        //display profile image, profile name, time and date
        public void setFullname(String fullname)
        {
            //textview to display the username
            TextView username = (TextView) mView.findViewById(R.id.post_user_name);
            username.setText(fullname);
        }

        //set the profile image of the user

        public void setProfileimage(Context ctx, String profileimage)
        {
            CircleImageView image = (CircleImageView) mView.findViewById(R.id.post_profile_image);
            Picasso.get().load(profileimage).into(image);
        }

        public void setTime(String time)
        {
            TextView PostTime = (TextView) mView.findViewById(R.id.post_time);
            PostTime.setText("   " + time);
        }

        public void setDate(String date)
        {
            TextView PostDate = (TextView) mView.findViewById(R.id.post_date);
            PostDate.setText("   " + date);
        }
        public void setDescription(String description)

        {
            TextView PostDescription = (TextView) mView.findViewById(R.id.post_description);
            PostDescription.setText(description);
        }

        public void setPostimage(Context applicationContext, String postimage)
        {
            ImageView PostImage = (ImageView) mView.findViewById(R.id.post_image);
            Picasso.get().load(postimage).into(PostImage);
        }
        }


    private void SendUserToPostActivity() {
        Intent addNewPostIntent = new Intent(MainActivity.this, PostActivity.class);
        startActivity(addNewPostIntent);
        finish();
    }


    @Override
    protected void onStart() {

        super.onStart();
       FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null) 
        {
            
            SendUserToLoginActivity();
        }
        
        else {
            
            CheckUserExistence();
        }

    }

    private void CheckUserExistence() {

        final String current_user_id = mAuth.getCurrentUser().getUid();

        UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(!dataSnapshot.hasChild(current_user_id)) {
                    SendUserToSetupActivity();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private void SendUserToSetupActivity() {

        Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);

        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(setupIntent );

        finish();


    }


    private void SendUserToLoginActivity() {

        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);

        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(loginIntent);

        finish();

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(actionBarDrawerToggle.onOptionsItemSelected(item))

        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void UserMenuSelector(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.nav_post:

                SendUserToPostActivity();
                break;

            case R.id.nav_profile:

                Toast.makeText(this, "profile", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_home:

                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_friends:

                Toast.makeText(this, "Friends", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_find_friends:

                Toast.makeText(this, "Find Friends", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_messages:

                Toast.makeText(this, "Messages", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_settings:

                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_logout:

                mAuth.signOut();
                SendUserToLoginActivity();

                break;

        }
    }
}
