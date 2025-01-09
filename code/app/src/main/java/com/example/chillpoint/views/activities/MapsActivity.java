package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.example.chillpoint.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLocation;
    private String selectedAddress; // [새로 추가됨] 선택된 주소 저장

    // [새로 추가됨] 검색 기능 관련 변수
    private AutoCompleteTextView searchField;
    private ListView searchResultsListView;
    private Button clearResultsButton;
    private List<Address> searchResults;
    private ArrayAdapter<String> searchResultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // 기본 뷰 초기화
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.confirmLocationButton).setOnClickListener(v -> {
            if (selectedLocation != null && selectedAddress != null) { // [수정됨] 주소도 null 체크
                Intent intent = new Intent();
                intent.putExtra("latitude", selectedLocation.latitude);
                intent.putExtra("longitude", selectedLocation.longitude);
                intent.putExtra("address", selectedAddress); // [새로 추가됨] 주소 반환
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            }
        });

        // [새로 추가됨] 검색 관련 뷰 초기화
        searchField = findViewById(R.id.searchField);
        searchResultsListView = findViewById(R.id.searchResultsListView);
        clearResultsButton = findViewById(R.id.clearResultsButton);

        searchResults = new ArrayList<>();
        searchResultsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        searchResultsListView.setAdapter(searchResultsAdapter);

        searchField.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                String query = searchField.getText().toString().trim();
                if (!TextUtils.isEmpty(query)) {
                    searchLocation(query);
                }
                return true;
            }
            return false;
        });

        searchResultsListView.setOnItemClickListener(this::onSearchResultSelected);
        clearResultsButton.setOnClickListener(v -> clearSearchResults());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Set the default location to RMIT Vietnam (Ho Chi Minh City)
        LatLng defaultLocation = new LatLng(10.729511, 106.693359); // RMIT Vietnam's coordinates
        selectedLocation = defaultLocation; // [새로 추가됨] 초기 선택 위치를 기본값으로 설정
        selectedAddress = getAddressFromLatLng(defaultLocation); // [새로 추가됨] 기본 위치 주소 설정
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15)); // Adjust zoom level
        mMap.addMarker(new MarkerOptions().position(defaultLocation).title("RMIT Vietnam"));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            selectedLocation = latLng;
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));

            selectedAddress = getAddressFromLatLng(latLng); // [새로 추가됨] 새로 선택된 위치 주소 설정
            if (selectedAddress != null) {
                Toast.makeText(MapsActivity.this, "Location Selected: " + selectedAddress, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // [새로 추가됨] LatLng로부터 주소를 가져오는 메서드
    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // [새로 추가됨] 검색 기능: 입력된 텍스트로 위치 검색
    private void searchLocation(String query) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 5); // 최대 5개 결과 검색
            searchResults.clear();
            searchResultsAdapter.clear();

            if (addresses != null && !addresses.isEmpty()) {
                for (Address address : addresses) {
                    searchResults.add(address);
                    searchResultsAdapter.add(address.getAddressLine(0));
                }
                searchResultsAdapter.notifyDataSetChanged();
                searchResultsListView.setVisibility(View.VISIBLE);
                clearResultsButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to search location", Toast.LENGTH_SHORT).show();
        }
    }

    // [새로 추가됨] 검색 결과 클릭 시 지도에 표시
    private void onSearchResultSelected(AdapterView<?> parent, View view, int position, long id) {
        Address selectedResult = searchResults.get(position);
        LatLng latLng = new LatLng(selectedResult.getLatitude(), selectedResult.getLongitude());
        selectedLocation = latLng;
        selectedAddress = selectedResult.getAddressLine(0);

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        Toast.makeText(this, "Selected: " + selectedAddress, Toast.LENGTH_SHORT).show();
    }

    // [새로 추가됨] 검색 결과 초기화
    private void clearSearchResults() {
        searchResults.clear();
        searchResultsAdapter.clear();
        searchResultsAdapter.notifyDataSetChanged();
        searchResultsListView.setVisibility(View.GONE);
        clearResultsButton.setVisibility(View.GONE);
    }
}
