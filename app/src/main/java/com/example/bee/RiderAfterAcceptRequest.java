package com.example.bee;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is a class that shows the situation after the rider confirms the driver's acceptance
 */

public class RiderAfterAcceptRequest extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "accept_request_activity";

    GoogleMap request_accepted_map;
    TextView driver_name;


    MarkerOptions ori, dest;
    private static ArrayList<String> points = new ArrayList<>();

    private FirebaseUser user;
    FirebaseDatabase db;

    FloatingActionButton fabConfirm, fabCancel;
    private static final String rq_id = "PAvxlWke8KfOtRbuXuqo6TheIrw1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_after_accept_request);
//        user = FirebaseAuth.getInstance().getCurrentUser();
//        String userID = user.getUid();
        db = FirebaseDatabase.getInstance();
//        db.setPersistenceEnabled(true);
        initMap();
        driver_name = (TextView)findViewById(R.id.driver_name);
        driver_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RiderAfterAcceptRequest.this, DriverBasicInformation.class);
                startActivity(intent);
            }
        });

        fabCancel = findViewById(R.id.my_cancel);
        fabConfirm = findViewById(R.id.my_confirm);
        fabCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCancelDialog();
            }
        });
        fabConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmDialog();
            }
        });



    }

    /**
     * This is a method to initialize the map
     */

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_requestAccepted);
        mapFragment.getMapAsync(RiderAfterAcceptRequest.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");

        request_accepted_map = googleMap;
        request_accepted_map.setMyLocationEnabled(true);
        //request_accepted_map.getUiSettings().setCompassEnabled(true);
        setOriDest();
        drawPointsList();

    }


    private void setOriDest() {
        Context mcontext = RiderAfterAcceptRequest.this;
        DatabaseReference ref = db.getReference("requests");
        ref.child(rq_id)
                .child("request")
                .child("originLatlng")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String part_list = dataSnapshot.getValue(String.class);
                        String[]parts = part_list.split(",");
                        Double lat = Double.parseDouble(parts[0]);
                        Double lng = Double.parseDouble(parts[1]);
                        ori = new MarkerOptions().position(new LatLng(lat,lng)).title("Pick up location");
                        request_accepted_map.addMarker(ori
                                .position(ori.getPosition())
                                .icon(bitmapDescriptorFromVector(mcontext, R.drawable.ic_green_placeholder)));

                        if (ori != null){
                            ref.child(rq_id)
                                    .child("request")
                                    .child("destLatlng")
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            String part_list = dataSnapshot.getValue(String.class);
                                            String[]parts = part_list.split(",");
                                            Double lat = Double.parseDouble(parts[0]);
                                            Double lng = Double.parseDouble(parts[1]);
                                            dest = new MarkerOptions().position(new LatLng(lat,lng)).title("Destination");
                                            request_accepted_map.addMarker(dest
                                                    .position(dest.getPosition())
                                                    .icon(bitmapDescriptorFromVector(mcontext, R.drawable.ic_red_placeholder)));

                                            if(dest != null){
                                                LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                                        .include(ori.getPosition())
                                                        .include(dest.getPosition())
                                                        .build();
                                                //Move camera to include both points
                                                request_accepted_map.setPadding(     0,      350,      0,     0);
                                                request_accepted_map.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 200));
                                            }else{
                                                Toast.makeText(mcontext, "Lack of destination", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }else{
                            Toast.makeText(mcontext, "Lack of origination", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

        /**
     * This is a method to set the model of marker
     */

    // Convert vector drawable to bitmap
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

//    /**
//     * This is a method to draw route between two positions
//     * @param p1
//     * first place
//     * @param p2
//     * second place
//     */

    private void drawRoute(ArrayList<LatLng> pointList) {
        PolylineOptions polylineOptions = DirectionConverter
                .createPolyline(RiderAfterAcceptRequest.this, pointList, 5,
                        getResources().getColor(R.color.yellow));
        request_accepted_map.addPolyline(polylineOptions);
    }

    private void drawPointsList(){
        DatabaseReference ref = db.getReference("requests");
            ref.child(rq_id)
                .child("request")
                .child("points")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> items = dataSnapshot.getChildren();
                for(DataSnapshot child: items){
                    String latlng_str = child.getValue(String.class);
                    points.add(latlng_str);
                }
                ArrayList<LatLng> formal_points = new ArrayList<>();
                for(String point : points){
                    String[] parts = point.split(",");
                    Double lat = Double.parseDouble(parts[0]);
                    Double lng = Double.parseDouble(parts[1]);
                    formal_points.add(new LatLng(lat,lng));
                }
                drawRoute(formal_points);


            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showCancelDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(RiderAfterAcceptRequest.this);
        View view = getLayoutInflater().inflate(R.layout.activity_rider_after_accept_request_dialog,null);
        TextView title = (TextView) view.findViewById(R.id.dialog_title);
        title.setText("Are you sure you want to cancel the ride? ");
        Button not_cancelBtn = view.findViewById(R.id.not_cancel_btn);
        Button cancelBtn = view.findViewById(R.id.do_cancel_btn);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(RiderAfterAcceptRequest.this, "hohoho", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                DatabaseReference ref = db.getReference("requests").child(rq_id).child("request").child("cancel");
                ref.setValue(true);
                finish();
            }
        });
        not_cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


    }

    private void showConfirmDialog(){

        Context mcontext = RiderAfterAcceptRequest.this;
        AlertDialog.Builder builder = new AlertDialog.Builder(RiderAfterAcceptRequest.this);
        View view = getLayoutInflater().inflate(R.layout.activity_rider_after_accept_request_dialog,null);
        TextView title = (TextView) view.findViewById(R.id.dialog_title);
        title.setText("Are you sure you reach the destination? ");
        Button cancelBtn = view.findViewById(R.id.not_cancel_btn);
        Button confirmBtn = view.findViewById(R.id.do_cancel_btn);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference ref = db.getReference("requests");
                ref.child(rq_id)
                        .child("request")
                        .child("reached").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean is_reach = (boolean)dataSnapshot.getValue();
                        if (!is_reach){
                            Toast.makeText(mcontext, "driver has not reach the destination!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }else{
                            DatabaseReference ref2 = db.getReference("requests").child(rq_id).child("request").child("finished");
                            ref2.setValue(true);
                            Intent intent = new Intent(RiderAfterAcceptRequest.this, MainActivity.class);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

    }






}