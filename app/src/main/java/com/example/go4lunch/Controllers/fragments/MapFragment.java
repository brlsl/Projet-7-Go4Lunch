package com.example.go4lunch.controllers.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.go4lunch.R;
import com.example.go4lunch.models.apiGooglePlace.MyPlaces;
import com.example.go4lunch.models.apiGooglePlace.Result;
import com.example.go4lunch.utils.GooglePlaceStreams;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.MarkerOptions;



import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;



public class MapFragment extends androidx.fragment.app.Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1234;

    private static final float DEFAULT_ZOOM = 15f;

    private static final String PLACE_API_KEY = "AIzaSyAK366wqKIdy-Td7snXrjIRaI9MkXb2VZE";

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private int mLocationPermissionGranted = 0; // refused by default

    private GoogleMap mMap;

    private LocationCallback locationCallback;



    private Disposable mDisposable;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null)
                    return;
                for(Location location: locationResult.getLocations()){
                    String stringLatitude = String.valueOf(location.getLatitude());
                    String stringLongitude = String.valueOf(location.getLongitude());
                    executeHttpRequestNearbySearchWithRetrofit(stringLatitude,stringLongitude);
                }
            }
        };
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout for this fragment
        View mView = inflater.inflate(R.layout.map_fragment, container, false);
        MapView mMapView = mView.findViewById(R.id.map);
        if (mMapView != null) {
            mMapView.onCreate(null);
            mMapView.onResume();
            mMapView.getMapAsync(this);
        }
        getLocationPermission();
        return mView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdate();

    }

    private void startLocationUpdate() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.disposeWhenDestroy();
    }

    private void disposeWhenDestroy() {
        if (this.mDisposable != null && !this.mDisposable.isDisposed())
            this.mDisposable.dispose();

    }

    @AfterPermissionGranted(LOCATION_PERMISSION_REQUEST) // made with Easy Permission
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        if (EasyPermissions.hasPermissions(requireContext(), permissions)) {
            mLocationPermissionGranted = 1;
        } else {
            EasyPermissions.requestPermissions(this, "Go4Lunch needs location permission", LOCATION_PERMISSION_REQUEST, permissions);
        }
    }

    // ask User permission with a dialog
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = 0;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = 1;
                    Toast.makeText(requireContext(), "Permission granted", Toast.LENGTH_SHORT).show();
                    getDeviceLocation();
                } else {
                    Toast.makeText(requireContext(), "Permission refused", Toast.LENGTH_SHORT).show();
                    EasyPermissions.requestPermissions(this, "Go4Lunch needs location permission", LOCATION_PERMISSION_REQUEST, permissions);
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mLocationPermissionGranted == 1) {
            getDeviceLocation();
        }
    }



    void getDeviceLocation() {
        mFusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);


                        Location mLastKnownLocation = location;
                        Double test = mLastKnownLocation.getLatitude();
                        String stringLatitude = String.valueOf(mLastKnownLocation.getLatitude());
                        String stringLongitude = String.valueOf(mLastKnownLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mLastKnownLocation.getLatitude(),
                                        mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));

                        executeHttpRequestNearbySearchWithRetrofit(stringLatitude,stringLongitude);
                    }
                });
    }



    private void executeHttpRequestNearbySearchWithRetrofit(String latitude, String longitude) {
        this.mDisposable = GooglePlaceStreams.streamFetchNearbySearch(latitude+","+longitude,500,"restaurant", PLACE_API_KEY)
                .subscribeWith(new DisposableObserver<MyPlaces>() {
            @Override
            public void onNext(MyPlaces myPlaces) {
                mMap.clear();
                for (int i = 0; i < myPlaces.getResults().size(); i++) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    Result googlePlace = myPlaces.getResults().get(i);
                    double lat = googlePlace.getGeometry().getLocation().getLat();
                    double lng = googlePlace.getGeometry().getLocation().getLng();
                    String placeName = googlePlace.getName();
                    LatLng latLng = new LatLng(lat, lng);
                    markerOptions.position(latLng);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
                    markerOptions.title(placeName);

                    mMap.addMarker(markerOptions);

                    }
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });
    }
}