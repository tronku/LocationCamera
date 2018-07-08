package com.example.tronku.location;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, ImageAdapter.ImageClickListener {

    private CardView photo;
    private static final int GET_IMAGE = 2;
    private String geoUrl, geoLabel;
    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private TextView add_place;
    private EditText name, about, tags, city, weburl, category;
    private List<Uri> imageUriList = new ArrayList<>();
    private Uri imageUri = null;
    private Map<String, ArrayList<String>> cities = new HashMap<>();

    //Firebase references
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private DatabaseReference cityReference;
    private DatabaseReference placeReference;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name = findViewById(R.id.name);
        about = findViewById(R.id.about);
        tags = findViewById(R.id.tags);
        city = findViewById(R.id.city);
        add_place = findViewById(R.id.add_place);
        weburl = findViewById(R.id.website);
        category = findViewById(R.id.category);

        //Firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        cityReference = firebaseDatabase.getReference();
        placeReference = firebaseDatabase.getReference();
        storageReference = firebaseStorage.getReference();

        if(savedInstanceState !=null){
            imageUriList = savedInstanceState.getParcelableArrayList("uri");
        }

        //location_autocomplete
        final PlaceAutocompleteFragment autocompleteFragment= (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                geoLabel = place.getName().toString();
                geoUrl = "http://maps.google.com/maps?q=loc:" + place.getLatLng().latitude + "," + place.getLatLng().longitude + "(" + geoLabel + ")";
                Uri websiteUri = place.getWebsiteUri();
                if(websiteUri!=null){
                    weburl.setText(websiteUri.toString());
                }
            }

            @Override
            public void onError(Status status) {

                Toast.makeText(getApplicationContext(),status.toString(),Toast.LENGTH_SHORT).show();

            }
        });

        photo = findViewById(R.id.photo_button);
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getImage();
            }
        });

        recyclerView = findViewById(R.id.recyclerview);
        adapter = new ImageAdapter(imageUriList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        add_place.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addData();
                imageUriList.clear();
                adapter.setUriList(imageUriList);
                adapter.updateList();
                name.setText("");
                city.setText("");
                weburl.setText("");
                tags.setText("");
                about.setText("");
                category.setText("");
                autocompleteFragment.setText("");
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String category_selected = parent.getItemAtPosition(position).toString();
        if(position!=0){
            Toast.makeText(this, "Category: " + category_selected, Toast.LENGTH_LONG).show();
        }
        //storing data in database
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Toast.makeText(this, "Please select a category.", Toast.LENGTH_LONG).show();
    }

    public void getImage() {
        Intent photo = new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photo.setType("images/jpeg");
        photo.putExtra(Intent.EXTRA_LOCAL_ONLY, "false");
        startActivityForResult(Intent.createChooser(photo, "Complete action using..."), GET_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == GET_IMAGE && resultCode == RESULT_OK) {
            imageUri = data.getData();
            imageUriList.add(imageUri);
            adapter.setUriList(imageUriList);
            adapter.updateList();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("uri", (ArrayList<? extends Parcelable>) imageUriList);
    }

    @Override
    public void onImageClick(int position) {
        //Pop-up for bigger view
        Intent bigger = new Intent(this, full_image.class);
        bigger.putExtra("position", position);
        bigger.putParcelableArrayListExtra("imageList", (ArrayList<? extends Parcelable>) imageUriList);
        startActivity(bigger);
    }

    //Adding data to Firebase database
    public void addData(){
        String tag = tags.getText().toString();
        String placeName = name.getText().toString();
        String categ = category.getText().toString();
        String desc = about.getText().toString();
        String cityName = city.getText().toString();
        String website = weburl.getText().toString();
        String key = placeReference.child("places").push().getKey();
        Places places = new Places(placeName, desc, categ, tag, cityName, geoUrl, geoLabel, website);

        Map<String, Object> placeValues = places.toMap();
        Map<String, Object> values = new HashMap<>();
        values.put("/places/" + key, placeValues);
        placeReference.updateChildren(values);

        //cities
        ArrayList<String> pushIds;
        boolean city_present = false;
        if(cities.containsKey(cityName)){
            pushIds = cities.get(cityName);
            city_present = true;
        }
        else{
            pushIds = new ArrayList<>();
            city_present = false;
        }
        pushIds.add(key);
        cities.put(cityName, pushIds);

        setCityData(cityName, city_present);
        setDownloadUri(key);
    }

    public void setDownloadUri(final String key){

        for(int i=0;i<imageUriList.size();i++){

            //high res picture
            final StorageReference photoUriHigh = storageReference.child(key).child("images").child("highres").child(imageUriList.get(i).getLastPathSegment());
            UploadTask uploadTask = photoUriHigh.putFile(imageUri);
            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return photoUriHigh.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        DatabaseReference imageRef = firebaseDatabase.getReference().child(key).child("images").child("highres");
                        String imageKey = imageRef.push().getKey();
                        Map<String, Object> images = new HashMap<>();
                        images.put("/places/" + key + "/images/highres/" + imageKey, downloadUri.toString());
                        placeReference.updateChildren(images);
                    } else {
                        Toast.makeText(MainActivity.this, "Error!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    public void setCityData(String cityName, boolean city_present){
        String key = cityReference.child("cities").push().getKey();
        DatabaseReference cityData = firebaseDatabase.getReference().child(key);
        ArrayList<String> cityPlaces = cities.get(cityName);

        Map<String, Object> city = new HashMap<>();
        city.put("/cities/" + key + "/title", cityName);
        cityReference.updateChildren(city);
        Map<String, Object> places = new HashMap<>();


        for(int i=0;i<cityPlaces.size();i++){
            String placeKey = cityData.child("places").push().getKey();
            places.put("/cities/" + key + "/places/" + placeKey, cityPlaces.get(i));
            cityReference.updateChildren(places);
        }
    }
}
