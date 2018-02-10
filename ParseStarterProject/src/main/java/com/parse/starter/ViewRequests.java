package com.parse.starter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.Manifest;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.gms.analytics.internal.zzy.r;
import static com.google.android.gms.analytics.internal.zzy.s;

public class ViewRequests extends AppCompatActivity {

    ListView requestListView;
    ArrayList<String> request = new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    LocationManager locationManager;
    LocationListener locationListener;
    ArrayList<Double> requestLatitudes = new ArrayList<Double>();
    ArrayList<Double> requestLongitudes = new ArrayList<Double>();
    ArrayList<String> usernames = new ArrayList<String>();

    public void updateListView(Location location)
    {
        //is there is a location given
        if(location != null) {
            //query all locations
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
            final ParseGeoPoint geoPointLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            //order them ascending to nearest location
            query.whereNear("location", geoPointLocation);
            //will not give request that have drivers that have accepted the request
            query.whereDoesNotExist("driverUsername");
            query.setLimit(10);

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        //clear the array list in case
                        request.clear();
                        requestLongitudes.clear();
                        requestLatitudes.clear();

                        //if there is request in the query
                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                //get the geopoint of the Request class
                                ParseGeoPoint requestLocation = (ParseGeoPoint) object.get("location");
                                //if there is a geopoint
                                if (requestLocation != null) {
                                    //convert it into kilometers
                                    Double distanceKM = geoPointLocation.distanceInKilometersTo(requestLocation);
                                    Double distanceOneDP = (double) Math.round((distanceKM * 10) / 10);

                                    //add the distance into array list request
                                    request.add(distanceOneDP.toString() + " kilometers");

                                    //add specifics to their array list
                                    requestLatitudes.add(requestLocation.getLatitude());
                                    requestLongitudes.add(requestLocation.getLongitude());
                                    //adds the riders username into a list
                                    usernames.add(object.getString("username"));
                                }
                            }
                        } else //if there are no request
                        {
                            request.add("No active request nearby ");
                        }
                        //update array list
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        setTitle("Nearby Requests");

        requestListView  = (ListView) findViewById(R.id.requestListView);

        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,request);

        request.clear();
        request.add("Getting Nearby request");
        requestListView.setAdapter(arrayAdapter);

        //when a request is clicked
        requestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("Intent clicked", "1");

                //check if version is less than 23 or if permission is given
                if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(ViewRequests.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    Log.i("Intent clicked", "2");
                    //gets the lastknown location of user which is driver
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    //if there are contents in request, transfer data into intent
                    if(requestLatitudes.size() > i && requestLongitudes.size() > i && usernames.size() > i && lastKnownLocation != null)
                    {
                        Log.i("Intent clicked", "wat");
                        Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
                        intent.putExtra("requestLatitude", requestLatitudes.get(i));
                        intent.putExtra("requestLongitude", requestLongitudes.get(i));
                        intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());
                        intent.putExtra("driverLongitude", lastKnownLocation.getLongitude());
                        intent.putExtra("username", usernames.get(i));

                        startActivity(intent);
                    }
                }

            }
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListView(location);

                //puts the driver's location in USER table
                ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(),location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        //if before marshmellow
        if(Build.VERSION.SDK_INT < 23)
        {
            //set location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }else
        {
            //if permission is not granted
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                //ask for permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }else
            {
                //if not marshmellow and permission is already granted, find the last known location and update list if there is
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                //makes the app update the distance if there is a last known location for driver
                if(lastKnownLocation != null)
                {
                    updateListView(lastKnownLocation);
                }

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1)
        {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    //same as above
                    locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER,0,0,locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    updateListView(lastKnownLocation);
                }
            }
        }
    }


}
