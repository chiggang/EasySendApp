/**
 * App : EasySend
 * Title : 메인 프로세스
 * Date : 2017.12.12
 * by chiggang
 */

/**
 * Latest Log #1015
 * Latest Error #5009
 */

package kr.co.easysend.easysendapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    /* ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** *
     *
     * Section : 전역 변수
     *
     * ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** */

    // RESTful API 주소를 정의함
    public String _restFulApiUrl = "http://easysend.co.kr:3000";

    // BroadcastReceiver 개체를 생성함
    BroadcastReceiver _broadcastReceiver = new Broadcast();

    private TextView _textMessage;



    /* ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** *
     *
     * Section : 초기화
     *
     * ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** */

    /**
     * 화면 하단의 메뉴 탭 리스너를 정의함
     */
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        // 화면 하단의 메뉴 탭을 클릭함
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    _textMessage.setText(R.string.title_home);
                    return true;

                case R.id.navigation_dashboard:
                    _textMessage.setText(R.string.title_dashboard);
                    return true;

                case R.id.navigation_notifications:
                    _textMessage.setText(R.string.title_notifications);
                    return true;
            }

            return false;
        }
    };

    /**
     * 앱이 생성될 때, 아래의 내용을 처리함
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 앱에서 사용할 폰의 권한을 체크함
        checkPermission();

        // 폰의 장비 토큰키을 RESTful API 서버로 전송함
        setDeviceToken();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _textMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // BroadcastReceiver를 생성하여 동적으로 적용함
        // 임시!
        /*
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        */

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);

        registerReceiver(_broadcastReceiver, intentFilter);
        Log.d("Log", "[Log #1000] MainActivity.java > onCreate() : BroadcastReceiver 동적 등록 완료");

        // 폰의 안드로이드 버전이 9 이상 일 경우, 권한 정책을 별도로 처리함
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    /**
     * 앱이 종료될 때, 아래의 내용을 처리함
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 동적으로 등록된 BroadcastReceiver를 해제함
        unregisterReceiver(_broadcastReceiver);

        Log.d("Log", "[Log #1001] MainActivity.java > onDestory() : BroadcastReceiver가 해제됨");
    }



    /* ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** *
     *
     * Section : 앱 권한
     *
     * ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** */

    /**
     * 앱에서 사용할 폰의 권한을 체크함
     */
    private void checkPermission() {
        // SMS 사용 권한을 체크함
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // 앱 첫 실행 시, 권한에 대한 거부를 하고 난 후 두번째 실행 시 부터 아래의 이벤트가 발생함
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
            }

            // 앱 실행 시, 권한에 대해 허용함(SMS, 전화)
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_SMS,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.CALL_PHONE
                    },
                    1
            );
        } else {
            // 권한이 있는 경우
        }
    }



    /* ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** *
     *
     * Section : RESTful API
     *
     * ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** */

    /**
     * 폰의 장비 토큰키을 RESTful API 서버로 전송함
     */
    private void setDeviceToken() {
        try {
            String a1 = "";
            String a2 = "";
            String a3 = "";

            // 비동기식으로 폰의 장비 토큰키을 RESTful API 서버로 전송함
            new AsyncTaskUploadDeviceToken().execute(this, a1, a2, a3);
        } catch(Exception e) {
            Log.d("Error", "[Error #5001] MainActivity.java > checkDeviceToken() : " + e.toString());
            e.printStackTrace();
            Log.d("Error", "..........................................................................................");
        }
    }

    /**
     * 비동기식으로 폰의 장비 토큰키을 RESTful API 서버로 전송함
     */
    private class AsyncTaskUploadDeviceToken extends AsyncTask<Object, Object, Object> {

        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d("Log", "[Log #1002] MainActivity.java > AsyncTaskUploadDeviceToken > doInBackground() : 전송 시작");

                Context context = (Context) params[0];

                // RESTful API 서버로 데이터를 전송하기 위하여 초기화함
                URL url = new URL(_restFulApiUrl + "/api/set/token");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept","application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                // 이 폰의 전화번호를 불러옴(숫자만)
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String paramMobileNumber = telephonyManager.getLine1Number();
                paramMobileNumber = paramMobileNumber.replace("-", "").replace("+82", "0");

                // 이 폰의 Firebase device token 정보를 불러옴
                String paramFirebaseDeviceToken = FirebaseInstanceId.getInstance().getToken();

                // 데이터를 JSON 형식으로 변환함
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("mobile_number", paramMobileNumber);
                jsonParam.put("firebase_device_token", paramFirebaseDeviceToken);

                // RESTful API 서버로 데이터를 전송함
                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                Log.d("Log", "[Log #1003] MainActivity.java > AsyncTaskUploadDeviceToken > doInBackground() : 전송 완료(ResponseCode : " + String.valueOf(conn.getResponseCode()) + ", gResponseMessage : " + conn.getResponseMessage() + ")");

                // RESTful API 서버로부터 결과값을 받음
                JSONObject responseJSON = null;

                // RESTful API 서버로부터 받은 결과값을 JSON 형식으로 변환함
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    StringBuilder builder = new StringBuilder();

                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line;

                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    } catch (IOException e) {
                        Log.d("Error", "[Error #5002] MainActivity.java > AsyncTaskUploadDeviceToken > doInBackground() > BufferedReader() : " + e.toString());
                        e.printStackTrace();
                        Log.d("Error", "..........................................................................................");
                    }

                    responseJSON = new JSONObject(builder.toString());
                }

                // 전송 결과값에 따라 각각 처리함
                switch (responseJSON.get("status").toString()) {
                    // 전송 완료
                    case "200":
                    case "201":
                        // 임시!
                        /*
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line+"\n");
                        }
                        br.close();
                        Log.i("결과 - DATA" , sb.toString());

                        JSONArray jarray = new JSONArray(sb.toString());
                        */

                        /*
                        for(int i=0; i < jarray.length(); i++){
                            JSONObject jObject = jarray.getJSONObject(i);  // JSONObject 추출
                            String rsFirebaseDeviceToken = jObject.getString("firebase_device_token");
                            Log.i("[메일]", rsFirebaseDeviceToken);
                        }
                        */
                        /*
                        JSONObject jObject = jarray.getJSONObject(0);  // JSONObject 추출
                        String rsFirebaseDeviceToken = jObject.getString("firebase_device_token");
                        Log.i("[토큰]", rsFirebaseDeviceToken);
                        */
                        break;
                }

                // 전송 처리를 종료함
                conn.disconnect();
            } catch(Exception e) {
                Log.d("Error", "[Error #5003] MainActivity.java > AsyncTaskUploadDeviceToken > doInBackground() : " + e.toString());
                e.printStackTrace();
                Log.d("Error", "..........................................................................................");
            }

            return null;
        }

        protected void onPostExecute() {
        }

    }

}
