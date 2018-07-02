package com.example.tronku.location;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, ImageAdapter.ImageClickListener {

    CardView photo;
    Dialog dialog;
    private int requestCode = 100;
    private static final int REQ_PERMISSION = 1;
    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    List<Bitmap> imageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //location_autocomplete
        PlaceAutocompleteFragment places= (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        places.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                Toast.makeText(getApplicationContext(),place.getName(),Toast.LENGTH_SHORT).show();
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
                checkCameraPermission();
            }
        });

        dialog = new Dialog(this);
        recyclerView = findViewById(R.id.recyclerview);
        adapter = new ImageAdapter(imageList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        Spinner category_spinner = findViewById(R.id.category);
        category_spinner.setOnItemSelectedListener(this);

        List<String> data = new ArrayList<>();
        data.add("Category");
        data.add("Food");
        data.add("Travel");
        data.add("Shopping");
        data.add("Religious");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, data);
        category_spinner.setAdapter(adapter);

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

    public void checkCameraPermission(){
        if(ActivityCompat.checkSelfPermission(getApplicationContext(), CAMERA) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{CAMERA}, REQ_PERMISSION);
            }
        }
        else
            openCameraIntent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQ_PERMISSION:
                if(grantResults.length>0){
                    openCameraIntent();
                }
                else{
                    Toast.makeText(this,"Permission Denied",Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public void openCameraIntent() {
        Intent pictureIntent = new Intent(
                MediaStore.ACTION_IMAGE_CAPTURE);
        if(pictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(pictureIntent,
                    requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (this.requestCode == requestCode &&
                resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                imageList.add(imageBitmap);
                adapter.setBitmapList(imageList);
                adapter.updateList();
            }
        }
    }

    @Override
    public void onImageClick(int position) {
        //Pop-up for bigger view
        ImageView image;
        dialog.setContentView(R.layout.image__big_view);

        image = dialog.findViewById(R.id.singlePlaceImage);
        image.setImageBitmap(imageList.get(position));

        dialog.show();
    }
}
