package com.example.stopwaiting.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.stopwaiting.R;
import com.example.stopwaiting.databinding.MainpageBinding;
import com.example.stopwaiting.dto.UserInfo;
import com.example.stopwaiting.dto.WaitingInfo;
import com.example.stopwaiting.dto.WaitingQueue;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, Overlay.OnClickListener {
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private ArrayList<Marker> markers;

    public static Activity mainActivity;
    public static Context context_main;
    private Intent mainIntent;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final int CAMERA_MOVE_REQUEST_CODE = 2000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private MainpageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MainpageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //setContentView(R.layout.mainpage);

        mainIntent = getIntent();
        mainActivity = MainActivity.this;
        context_main = this;

        //TextView userId = findViewById(R.id.txtUser);
        binding.txtUser.setText(DataApplication.currentUser.getName() + " 님(" + String.valueOf(DataApplication.currentUser.getStudentCode()) + ")");

        markers = new ArrayList<>();
        ((DataApplication) getApplication()).waitingList = new ArrayList<>();

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        binding.btnMypage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
                Intent intent = mainIntent;
                intent.setClass(MainActivity.this, MyPageActivity.class);

                startActivityForResult(intent, CAMERA_MOVE_REQUEST_CODE);
            }
        });

        binding.btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(mainActivity, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
                refresh();
            }
        });

        binding.btnList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = mainIntent;
                intent.setClass(MainActivity.this, ShowListActivity.class);

                startActivityForResult(intent, CAMERA_MOVE_REQUEST_CODE);
            }
        });

    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);  //현재위치 표시
        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);

        waitingInfoAllRequest();
        myWaitingRequest();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
                return;
            } else {
                naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onClick(@NonNull Overlay overlay) {
        if (overlay instanceof InfoWindow) {
            InfoWindow infoWindow = (InfoWindow) overlay;
            if (infoWindow.getAdapter() != null) {
                Intent infoIntent;
                WaitingInfo temp = new WaitingInfo();
                temp.setWaitingId(Long.valueOf(infoWindow.getMarker().getTag().toString()));
                temp = ((DataApplication) getApplication()).waitingList.get(((DataApplication) getApplication()).waitingList.indexOf(temp));

                if (temp.getType().equals("TIME")) {
                    infoIntent = new Intent(MainActivity.this, WaitingSelectTimeActivity.class);
                } else {
                    infoIntent = new Intent(MainActivity.this, WaitingNormalActivity.class);
                }
                infoIntent.putExtra("id", temp.getWaitingId());

                startActivity(infoIntent);
            }
            return true;
        }
        return false;
    }

    public void setInfo(WaitingInfo waitingInfo) {
        InfoWindow infoWindow = new InfoWindow();
        Marker marker = new Marker();

        marker.setPosition(new LatLng(waitingInfo.getLatitude(), waitingInfo.getLongitude()));
//        Log.e("loc-----------", waitingInfo.getLatitude() + "/" + waitingInfo.getLongitude());
        marker.setMap(naverMap);
        marker.setWidth(1);
        marker.setHeight(1);

        marker.setTag(String.valueOf(waitingInfo.getWaitingId()));

        markers.add(marker);

        infoWindow.open(marker);
        infoWindow.setOnClickListener(this);

        setInfoWindowText(infoWindow, waitingInfo.getName());
    }

    public void setInfoWindowText(InfoWindow info, String str) {
        info.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return str;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        refresh();

        if (requestCode == CAMERA_MOVE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Long selectId = data.getLongExtra("id", 0);
                switch (data.getIntExtra("case", 0)) {
                    case 1:
                        for (WaitingInfo temp : DataApplication.waitingList) {
                            if (temp.getWaitingId().equals(selectId)) {
                                CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(
                                        new LatLng(temp.getLatitude(), temp.getLongitude()), 15).animate(CameraAnimation.Fly, 1000);
                                naverMap.moveCamera(cameraUpdate);
                                break;
                            }
                        }
                        break;
                }

            } else {

            }
        } else if (requestCode == 1) {

        }
    }

    public void refresh() {
        for (Marker marker : markers) {
            marker.setMap(null);
        }
        markers = new ArrayList<>();

        waitingInfoAllRequest();
        myWaitingRequest();
        setWearOS();
    }

    public void myWaitingRequest() {
        DataApplication.myWaiting = new ArrayList<>();
        if (DataApplication.isTest) {
            for (WaitingQueue tempQueue : DataApplication.testWaitingQueueDBList) {
                for (UserInfo tempUser : tempQueue.getWaitingPersonList()) {
                    if (tempUser.getStudentCode().equals(DataApplication.currentUser.getStudentCode())
                            && !(DataApplication.myWaiting.contains(tempQueue))) {
                        DataApplication.myWaiting.add(tempQueue);
                    }
                }
            }
        } else {
            JSONObject jsonBodyObj = new JSONObject();
            try {
                jsonBodyObj.put("id", DataApplication.currentUser.getStudentCode());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            final String requestBody = String.valueOf(jsonBodyObj.toString());

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
                    ((DataApplication) getApplication()).serverURL + "/user/" + DataApplication.currentUser.getStudentCode() + "/queue", null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
//                            Log.e("my", jsonObject.toString());
                            Toast.makeText(getApplicationContext(), "신청한 웨이팅 조회.", Toast.LENGTH_SHORT).show();
                            try {
                                JSONArray dataArray = jsonObject.getJSONArray("data");
                                for (int i = 0; i < dataArray.length(); i++) {
                                    WaitingQueue data = new WaitingQueue();
                                    JSONObject dataObject = dataArray.getJSONObject(i);
                                    data.setQId(dataObject.getJSONObject("waitingQueue").getLong("id"));

                                    JSONObject timeObject = dataObject.getJSONObject("waitingQueue").getJSONObject("timetable");
                                    data.setTime(timeObject.getString("time"));

                                    JSONObject waitingObject = timeObject.getJSONObject("waitingInfo");
                                    data.setWId(waitingObject.getLong("id"));
                                    data.setQueueName(waitingObject.getString("name"));
                                    data.setMaxPerson(waitingObject.getInt("maxPerson"));

                                    ArrayList<UserInfo> tempUserList = new ArrayList<>();
                                    JSONArray userArray = dataObject.getJSONObject("waitingQueue").getJSONArray("userQueues");
                                    for (int j = 0; j < userArray.length(); j++) {
                                        UserInfo tempUser = new UserInfo();
                                        JSONObject userObject = userArray.getJSONObject(j).getJSONObject("user");
                                        tempUser.setStudentCode(userObject.getLong("id"));

                                        tempUserList.add(tempUser);
                                    }
                                    data.setWaitingPersonList(tempUserList);

                                    DataApplication.myWaiting.add(data);
                                }
                            } catch (JSONException e) {
                                Log.e("error", e.toString());
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), "신청한 웨이팅 조회 실패.", Toast.LENGTH_SHORT).show();
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }

                @Override
                public byte[] getBody() {
                    try {
                        if (requestBody != null && requestBody.length() > 0 && !requestBody.equals("")) {
                            return requestBody.getBytes("utf-8");
                        } else {
                            return null;
                        }
                    } catch (UnsupportedEncodingException uee) {
                        return null;
                    }
                }
            };

            request.setShouldCache(false);
            DataApplication.requestQueue.add(request);
        }

    }

    public void waitingInfoAllRequest() {
        ((DataApplication) getApplication()).waitingList = new ArrayList<>();
        if (((DataApplication) getApplication()).isTest) {
            ((DataApplication) getApplication()).waitingList = ((DataApplication) getApplication()).getTestDBList();

            for (int i = 0; i < ((DataApplication) getApplication()).waitingList.size(); i++) {
                setInfo(((DataApplication) getApplication()).waitingList.get(i));
            }
        } else {
            JSONObject jsonBodyObj = new JSONObject();
            final String requestBody = String.valueOf(jsonBodyObj.toString());
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, ((DataApplication) getApplication()).serverURL + "/waitinginfo/confirmed", null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            try {
//                                Log.e("temp", jsonObject.toString());
                                JSONArray dataArray = jsonObject.getJSONArray("data");
                                for (int i = 0; i < dataArray.length(); i++) {
                                    WaitingInfo data = new WaitingInfo();

                                    JSONObject dataObject = dataArray.getJSONObject(i);

                                    data.setWaitingId(dataObject.getLong("id"));
                                    data.setAdminId(dataObject.getLong("adminId"));
                                    data.setLatitude(dataObject.getDouble("latitude"));
                                    data.setLongitude(dataObject.getDouble("longitude"));
                                    data.setName(dataObject.getString("name"));
                                    data.setLocDetail(dataObject.getString("locationDetail"));
                                    data.setInfo(dataObject.getString("information"));
                                    data.setType(dataObject.getString("type"));
                                    data.setMaxPerson(dataObject.getInt("maxPerson"));

                                    ArrayList<String> timetable = new ArrayList();
                                    JSONArray timeArr = dataObject.getJSONArray("timetables");
                                    for (int j = 0; j < timeArr.length(); j++) {
                                        JSONObject timeObj = timeArr.getJSONObject(j);
                                        timetable.add(timeObj.getString("time"));
                                    }
                                    data.setTimetable(timetable);

                                    ArrayList<String> urlList = new ArrayList();
                                    if (dataObject.getJSONArray("images") != null) {
                                        JSONArray imageArray = dataObject.getJSONArray("images");
                                        for (int j = 0; j < imageArray.length(); j++) {
                                            JSONObject imgInfo = imageArray.getJSONObject(j);

                                            urlList.add(((DataApplication) getApplication()).imgURL + imgInfo.getString("fileurl"));

                                        }
                                    }
                                    data.setUrlList(urlList);

                                    setInfo(data);
                                    ((DataApplication) getApplication()).waitingList.add(data);

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), "로딩에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }

                @Override
                public byte[] getBody() {
                    try {
                        if (requestBody != null && requestBody.length() > 0 && !requestBody.equals("")) {
                            return requestBody.getBytes("utf-8");
                        } else {
                            return null;
                        }
                    } catch (UnsupportedEncodingException uee) {
                        return null;
                    }
                }
            };

            request.setShouldCache(false);
            DataApplication.requestQueue.add(request);
        }
    }

    public void setWearOS() {
        ((DataApplication) getApplication()).sendRefresh();
        ((DataApplication) getApplication()).sendUserInfo();

        ArrayList<WaitingInfo> tempList = new ArrayList<>();
        for (WaitingInfo tempInfo : DataApplication.waitingList) {
            for (WaitingQueue tempQueue : ((DataApplication) getApplication()).myWaiting) {
                WaitingInfo temp = new WaitingInfo();
                temp.setName(tempQueue.getQueueName());
                if (tempInfo.getName().equals(tempQueue.getQueueName()) && !tempList.contains(temp.getName())) {
                    tempList.add(tempInfo);
                    Log.e("tempInfo", String.valueOf(tempInfo.getWaitingId()));
                    break;
                }
            }
        }

        ((DataApplication) getApplication()).sendMyQueueInfo(tempList);
    }
}
