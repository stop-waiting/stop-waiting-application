package com.example.stopwaiting.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.stopwaiting.R;
import com.example.stopwaiting.adapter.SettingImageAdapter;
import com.example.stopwaiting.databinding.SettingInfoBinding;
import com.example.stopwaiting.dto.WaitingInfo;
import com.example.stopwaiting.dto.WaitingQueue;
import com.example.stopwaiting.service.MultipartRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SettingInfoActivity extends AppCompatActivity {
    //private EditText edtName, edtDetail, edtInfo, edtPerson;
    //private RecyclerView recyclerView;
    //private RadioGroup rdoGroup;
    //private RadioButton rdoNormal, rdoTime;
    //private Button btnNext;
    private Intent settingInfoIntent;
    private SettingImageAdapter imgListAdapter;
    private ArrayList<Uri> uriList;
    public static Activity setting_info_Activity;
    private int mStatusCode = 0;

    private SettingInfoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SettingInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settingInfoIntent = getIntent();
        setting_info_Activity = SettingInfoActivity.this;
        this.getContentResolver();
//        edtName = findViewById(R.id.edtName);
//        edtDetail = findViewById(R.id.edtLocDetail);
//        edtInfo = findViewById(R.id.edtInfo);
//        edtPerson = findViewById(R.id.edtMaxPerson);
//        recyclerView = findViewById(R.id.lstImg);
//        rdoGroup = findViewById(R.id.rdoGroup);
//        rdoNormal = findViewById(R.id.rdoNormal);
//        rdoTime = findViewById(R.id.rdoTime);
//        btnNext = findViewById(R.id.btnSettingNext);

        uriList = new ArrayList<>();
        imgListAdapter = new SettingImageAdapter(this, uriList);
        binding.lstImg.setAdapter(imgListAdapter);

        imgListAdapter.notifyDataSetChanged();

        binding.rdoNormal.setChecked(true);
        binding.rdoNormal.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.btnSettingNext.setText("완료");
            }
        });
        binding.rdoTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.btnSettingNext.setText("다음");
            }
        });
        bindInsert();
        bindDelete();
        bindNext();
    }

    private void bindInsert() {
        binding.btnAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (data == null) {   // 어떤 이미지도 선택하지 않은 경우
                Toast.makeText(getApplicationContext(), "이미지를 선택하지 않았습니다.", Toast.LENGTH_LONG).show();
            } else {   // 이미지를 하나라도 선택한 경우
                if (data.getClipData() == null) {
                    Uri imageUri = data.getData();
                    getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    uriList.add(imageUri);
                } else {
                    ClipData clipData = data.getClipData();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        Uri imageUri = clipData.getItemAt(i).getUri();
                        try {
                            uriList.add(imageUri);
                        } catch (Exception e) {
                            Log.e("Tag", "File select error", e);
                        }
                    }
                }
                imgListAdapter = new SettingImageAdapter(getApplicationContext(), uriList);
                binding.lstImg.setAdapter(imgListAdapter);
            }
        } else {

        }
    }

    private void bindDelete() {
        binding.btnRemove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Uri recyclerItem = imgListAdapter.getSelected();
                if (recyclerItem == null) {
                    Toast.makeText(SettingInfoActivity.this, "항목을 선택 후 삭제해 주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                uriList.remove(recyclerItem);

                imgListAdapter.notifyDataSetChanged();
                final int checkedPosition = imgListAdapter.getCheckedPosition();
                imgListAdapter.notifyItemRemoved(checkedPosition);

                imgListAdapter.clearSelected();
            }
        });
    }

    private void bindNext() {
        binding.btnSettingNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.rdoGroup.getCheckedRadioButtonId() == R.id.rdoNormal) {
                    addWaitingRequest();
                } else if (binding.rdoGroup.getCheckedRadioButtonId() == R.id.rdoTime) {
                    settingInfoIntent.putExtra("name", binding.edtName.getText().toString());
                    settingInfoIntent.putExtra("detail", binding.edtLocDetail.getText().toString());
                    settingInfoIntent.putExtra("info", binding.edtInfo.getText().toString());
                    settingInfoIntent.putExtra("maxPerson", Integer.valueOf(binding.edtMaxPerson.getText().toString()));
                    if (uriList.size() != 0) {
                        settingInfoIntent.putParcelableArrayListExtra("image", uriList);
                    } else {
                        settingInfoIntent.putParcelableArrayListExtra("image", null);
                    }

                    settingInfoIntent.setClass(SettingInfoActivity.this, SettingTimeActivity.class);

                    startActivity(settingInfoIntent);
                }
            }
        });
    }

    public void addWaitingRequest() {
        if (DataApplication.isTest) {

            ArrayList<Long> tempList = new ArrayList<>();
            tempList.add(DataApplication.qCnt);
            DataApplication.testDBList.add(new WaitingInfo(DataApplication.currentUser.getStudentCode(), 10L,
                    settingInfoIntent.getDoubleExtra("latitude", 0),
                    settingInfoIntent.getDoubleExtra("longitude", 0),
                    binding.edtName.getText().toString(), binding.edtLocDetail.getText().toString(), binding.edtInfo.getText().toString(),
                    "normal", Integer.valueOf(binding.edtMaxPerson.getText().toString()), new ArrayList<>(), tempList));

            DataApplication.testWaitingQueueDBList.add(new WaitingQueue(10L, DataApplication.qCnt++, binding.edtName.getText().toString(),
                    "normal", Integer.valueOf(binding.edtMaxPerson.getText().toString())));

            addImageRequest(10L);
        } else {
            JSONObject jsonBodyObj = new JSONObject();
            try {
                jsonBodyObj.put("adminId", DataApplication.currentUser.getStudentCode());
                jsonBodyObj.put("latitude", settingInfoIntent.getDoubleExtra("latitude", 0));
                jsonBodyObj.put("longitude", settingInfoIntent.getDoubleExtra("longitude", 0));
                jsonBodyObj.put("name", binding.edtName.getText().toString());
                jsonBodyObj.put("locationDetail", binding.edtLocDetail.getText().toString());
                jsonBodyObj.put("information", binding.edtInfo.getText().toString());
                jsonBodyObj.put("maxPerson", Integer.valueOf(binding.edtMaxPerson.getText().toString()));
                jsonBodyObj.put("type", "NORMAL");
                JSONArray timeArray = new JSONArray();
                timeArray.put("NORMAL");

                jsonBodyObj.put("timetables", timeArray);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            final String requestBody = String.valueOf(jsonBodyObj.toString());
            StringRequest request = new StringRequest(Request.Method.POST, DataApplication.serverURL + "/waitinginfo",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String jsonObject) {
                            Log.e("response", jsonObject);
                            if (uriList.size() > 0) {
                                addImageRequest(Long.valueOf(jsonObject));
                            } else {
                                Intent temp = new Intent(SettingInfoActivity.this, MyPageActivity.class);
                                MyPageActivity.myPageActivity.finish();
                                startActivity(temp);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), "등록에 실패하였습니다.", Toast.LENGTH_SHORT).show();
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

            request.setRetryPolicy(new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            request.setShouldCache(false);
            DataApplication.requestQueue.add(request);
        }
    }

    private void addImageRequest(Long waitingId) {
        if (DataApplication.isTest) {
            ArrayList<String> imgList = new ArrayList<>();
            if (uriList.size() > 0) {
                for (Uri img : uriList) {
                    imgList.add(img.toString());
                }
            }
            for (WaitingInfo temp : DataApplication.testDBList) {
                if (temp.getWaitingId().equals(waitingId)) {
                    temp.setUrlList(imgList);
                    break;
                }
            }

            Intent temp = new Intent(SettingInfoActivity.this, MyPageActivity.class);
            MyPageActivity.myPageActivity.finish();
            startActivity(temp);
        } else {
            MultipartRequest multipartRequest = new MultipartRequest(Request.Method.POST, DataApplication.serverURL + "/waitinginfo/" + waitingId + "/images",
                    new Response.Listener<NetworkResponse>() {
                        @Override
                        public void onResponse(NetworkResponse response) {
                            Intent temp = new Intent(SettingInfoActivity.this, MyPageActivity.class);
                            MyPageActivity.myPageActivity.finish();
                            startActivity(temp);

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse networkResponse = error.networkResponse;
                    String errorMessage = "Unknown error";
                    if (networkResponse == null) {
                        if (error.getClass().equals(TimeoutError.class)) {
                            errorMessage = "Request timeout";
                        } else if (error.getClass().equals(NoConnectionError.class)) {
                            errorMessage = "Failed to connect server";
                        }
                    } else {
                        String result = new String(networkResponse.data);
                        try {
                            JSONObject response = new JSONObject(result);
                            String status = response.getString("status");
                            String message = response.getString("message");

                            Log.e("Error Status", status);
                            Log.e("Error Message", message);

                            if (networkResponse.statusCode == 404) {
                                errorMessage = "Resource not found";
                            } else if (networkResponse.statusCode == 401) {
                                errorMessage = message + " Please login again";
                            } else if (networkResponse.statusCode == 400) {
                                errorMessage = message + " Check your inputs";
                            } else if (networkResponse.statusCode == 500) {
                                errorMessage = message + " Something is getting wrong";
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.i("Error", errorMessage);
                    error.printStackTrace();
                }
            }) {
                @Override
                protected Map<String, DataPart[]> getByteData() {
                    Map<String, DataPart[]> params = new HashMap<>();
                    if (uriList != null) {
                        DataPart[] temp = new DataPart[uriList.size()];
                        for (int i = 0; i < uriList.size(); i++) {
                            Bitmap bitmap = null;
                            try {
                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uriList.get(i));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            byte[] imageBytes = baos.toByteArray();

                            temp[i] = new MultipartRequest.DataPart(waitingId + "_" + String.valueOf(i) + ".jpg", imageBytes, "image/jpeg");
                        }
                        params.put("files", temp);
                    }
                    return params;
                }
            };

            DataApplication.requestQueue.add(multipartRequest);
        }

    }
}
