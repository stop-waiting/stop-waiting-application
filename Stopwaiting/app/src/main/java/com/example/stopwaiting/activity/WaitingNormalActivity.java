package com.example.stopwaiting.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.example.stopwaiting.R;
import com.example.stopwaiting.databinding.WaitingNormalBinding;
import com.example.stopwaiting.dto.UserInfo;
import com.example.stopwaiting.dto.WaitingInfo;
import com.example.stopwaiting.dto.WaitingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WaitingNormalActivity extends AppCompatActivity {
    private int pivot, mStatusCode;
    private ArrayList<String> imgItems;
    private WaitingInfo mWaitingInfo;
    private WaitingQueue mWaitingQueue;

    private WaitingNormalBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = WaitingNormalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent = getIntent();


        mWaitingInfo = new WaitingInfo();
        for (int i = 0; i < DataApplication.waitingList.size(); i++) {
            if (DataApplication.waitingList.get(i).getWaitingId().equals(intent.getLongExtra("id", 0))) {
                mWaitingInfo = DataApplication.waitingList.get(i);
            }
        }

        mWaitingQueue = new WaitingQueue();
        queueRequest(mWaitingInfo.getWaitingId(), mWaitingInfo.getTimetable().get(0));
        imageRequest();

        pivot = 0;
        String content = "";
        if (imgItems.size() > 0) {
            for (int i = 0; i < imgItems.size(); i++) {
                content = content + "·";
            }
            binding.txtImgCnt.setText(content);
            setImg();
        } else {
            content = "·";
            binding.txtImgCnt.setText(content);
            binding.imageView.setImageResource(R.drawable.empty_icon);
        }

        binding.txtWaitingName.setText(mWaitingInfo.getName());
        binding.txtLocDeatail.setText(mWaitingInfo.getLocDetail());
        binding.txtInfo.setText(mWaitingInfo.getInfo());

        binding.btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imgItems.size() > 1) {
                    if (pivot > 0) {
                        pivot--;
                    } else {
                        pivot = imgItems.size() - 1;
                    }
                    setImg();
                }
            }
        });

        binding.btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imgItems.size() > 1) {
                    if (pivot < imgItems.size() - 1) {
                        pivot++;
                    } else {
                        pivot = 0;
                    }
                    setImg();
                }
            }
        });

        binding.btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mWaitingQueue.getWaitingPersonList().size() < mWaitingInfo.getMaxPerson()) {
                    for (UserInfo temp : mWaitingQueue.getWaitingPersonList()) {
                        Log.e("selectUser", String.valueOf(temp.getStudentCode()));
                        if (temp.getStudentCode().equals(DataApplication.currentUser.getStudentCode())) {
                            Toast.makeText(getApplicationContext(), "이미 등록한 웨이팅입니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    waitingRequest();
                } else {
                    Toast.makeText(getApplicationContext(), "최대 인원인 웨이팅입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void setImg() {
        String content = binding.txtImgCnt.getText().toString();
        SpannableString spannableString = new SpannableString(content);

        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#FF6702")), pivot, pivot + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), pivot, pivot + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.txtImgCnt.setText(spannableString);

        Glide.with(getApplicationContext())
                .load(imgItems.get(pivot))
                .into(binding.imageView);

    }

    public void waitingRequest() {
        if (DataApplication.isTest) {
            for (int i = 0; i < ((DataApplication) getApplication()).testWaitingQueueDBList.size(); i++) {
                WaitingQueue temp = ((DataApplication) getApplication()).testWaitingQueueDBList.get(i);
                if (temp.getQueueName().equals(binding.txtWaitingName.getText()) && temp.getTime().equals("NORMAL")) {
                    if (temp.getWaitingPersonList() != null) {
                        int check = temp.addWPerson(((DataApplication) getApplication()).currentUser);
                        switch (check) {
                            case 0:
                                ((DataApplication) getApplication()).testWaitingQueueDBList.set(i, temp);
                                finish();
                                break;
                            case 1:
                                Toast.makeText(getApplicationContext(), "이미 등록한 웨이팅입니다.", Toast.LENGTH_SHORT).show();
                                break;
                            case 2:
                                Toast.makeText(getApplicationContext(), "최대 인원인 웨이팅입니다.", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                    break;
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

            StringRequest request = new StringRequest(Request.Method.POST, DataApplication.serverURL + "/waitinginfo/" + mWaitingInfo.getWaitingId() + "/queue/NORMAL",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String jsonObject) {
                            int statusCode = mStatusCode;
                            switch (statusCode) {
                                case HttpURLConnection.HTTP_OK:
                                    Toast.makeText(getApplicationContext(), "정상 등록되었습니다.", Toast.LENGTH_SHORT).show();
                                    finish();
                                    break;

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

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    if (response != null) {
                        mStatusCode = response.statusCode;
                    }
                    return super.parseNetworkResponse(response);
                }
            };

            request.setShouldCache(false);
            DataApplication.requestQueue.add(request);
        }
    }

    public void imageRequest() {
        imgItems = new ArrayList<>();
        for (int i = 0; i < mWaitingInfo.getUrlList().size(); i++) {

            imgItems.add(mWaitingInfo.getUrlList().get(i));
        }
    }

    public void queueRequest(Long selectWID, String time) {
        if (DataApplication.isTest) {
            for (WaitingQueue temp : ((DataApplication) getApplication()).testWaitingQueueDBList) {
                if (temp.getWId().equals(selectWID) && temp.getTime().equals("NORMAL")) {
                    if (temp.getWaitingPersonList() != null) {
                        binding.txtWaitCnt.setText("현재 " + String.valueOf(temp.getWaitingPersonList().size()) + "명 대기중");
                    } else {
                        binding.txtWaitCnt.setText("현재 대기 인원이 없습니다.");
                    }
                    break;
                }
            }
        } else {
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
                    DataApplication.serverURL + "/waitinginfo/" + selectWID + " /queue?time=" + time, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            try {
                                WaitingQueue data = new WaitingQueue();
                                JSONObject dataObject = jsonObject.getJSONObject("data");
                                data.setQId(dataObject.getLong("id"));

                                JSONObject timeObject = dataObject.getJSONObject("timetable");
                                data.setTime(timeObject.getString("time"));

                                JSONObject waitingObject = timeObject.getJSONObject("waitingInfo");
                                data.setWId(waitingObject.getLong("id"));
                                data.setQueueName(waitingObject.getString("name"));
                                data.setMaxPerson(waitingObject.getInt("maxPerson"));

                                ArrayList<UserInfo> tempUserList = new ArrayList<>();
                                JSONArray userArray = dataObject.getJSONArray("userQueues");
                                for (int j = 0; j < userArray.length(); j++) {
                                    UserInfo tempUser = new UserInfo();
                                    JSONObject userObject = userArray.getJSONObject(j).getJSONObject("user");
                                    tempUser.setStudentCode(userObject.getLong("id"));

                                    tempUserList.add(tempUser);
                                }
                                data.setWaitingPersonList(tempUserList);

                                mWaitingQueue = data;

                                if (mWaitingQueue.getWaitingPersonList() != null) {
                                    binding.txtWaitCnt.setText("현재 " + String.valueOf(mWaitingQueue.getWaitingPersonList().size()) + "명 대기중");
                                } else {
                                    binding.txtWaitCnt.setText("현재 대기 인원이 없습니다.");
                                }

                            } catch (JSONException e) {
                                Log.e("err", e.toString());
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), "조회에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }

            };

            request.setShouldCache(false);
            DataApplication.requestQueue.add(request);
        }
    }
}
