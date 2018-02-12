/**
 * App : EasySend
 * Title : BroadcastReceiver 통신
 * Date : 2017.12.12
 * by chiggang
 */

package kr.co.easysend.easysendapp;

import android.app.PendingIntent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Messenger;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Broadcast extends BroadcastReceiver {

    /* ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** *
     *
     * Section : 전역 변수
     *
     * ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** */

    // RESTful API 주소를 정의함
    public static String _restFulApiUrl = "http://easysend.co.kr:3000";



    /* ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** *
     *
     * Section : 초기화
     *
     * ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** */

    /**
     * 폰에서 발생한 행동 이벤트를 처리함
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // 부팅 완료
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                //Log.d("Log", "[Log #1004] Broadcast.java > onReceive() : 부팅 완료");
            }

            // 화면 켜짐
            if (Intent.ACTION_SCREEN_ON == intent.getAction()) {
                //Log.d("Log", "[Log #1005] Broadcast.java > onReceive() : 화면 켜짐");
            }

            // 화면 꺼짐
            if (Intent.ACTION_SCREEN_OFF == intent.getAction()) {
                //Log.d("Log", "[Log #1006] Broadcast.java > onReceive() : 화면 꺼짐");
            }

            // SMS 수신
            if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
                Log.d("Log", "[Log #1007] Broadcast.java > onReceive() : SMS 수신");

                // 받은 SMS 내용을 분석함
                Bundle bundle = intent.getExtras();
                Object messages[] = (Object[]) bundle.get("pdus");
                SmsMessage smsMessage[] = new SmsMessage[messages.length];

                // PDU 포맷으로 되어 있는 메시지를 복원함
                for (int i = 0; i < messages.length; i++) {
                    smsMessage[i] = SmsMessage.createFromPdu((byte[]) messages[i]);
                }

                // SMS의 송수신을 구분함(S:보냄, R:받음)
                String paramTransmitType = "R";

                // SMS를 받은 일시를 불러옴
                Date smsReceiveDatetime = new Date(smsMessage[0].getTimestampMillis());
                SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String smsReceiveDatetimeText = transFormat.format(smsReceiveDatetime);

                // SMS를 보낸 상대방 전화번호를 불러옴(숫자만)
                String smsSendMobileNumber = smsMessage[0].getOriginatingAddress();
                smsSendMobileNumber = smsSendMobileNumber.replace("-", "").replace("+82", "0");

                // SMS를 받은 이 폰의 전화번호를 불러옴(숫자만)
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String smsReceiveMobileNumber = telephonyManager.getLine1Number();
                smsReceiveMobileNumber = smsReceiveMobileNumber.replace("-", "").replace("+82", "0");

                // SMS의 내용을 불러옴
                String smsMessageContent = smsMessage[0].getMessageBody().toString();

                // 받은 SMS 내용을 비동기식으로 RESTful API 서버로 전송함(보낸 전화번호, 받은 전화번호, 받은 일시, 메시지 내용)
                new AsyncTaskUploadSMS().execute(context, paramTransmitType, smsSendMobileNumber, smsReceiveMobileNumber, smsReceiveDatetimeText, smsMessageContent);

                // 우선순위가 낮은 다른 문자 앱이 SMS를 수신 받지 못하도록 함
                //abortBroadcast();
            }
        } catch(Exception e) {
            Log.d("Error", "[Error #5004] Broadcast.java > onReceive() : " + e.toString());
            e.printStackTrace();
            Log.d("Error", "..........................................................................................");
        }
    }



    /* ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** *
     *
     * Section : RESTful API
     *
     * ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** */

    /**
     * 받은 SMS 내용을 비동기식으로 RESTful API 서버로 전송함
     */
    public static class AsyncTaskUploadSMS extends AsyncTask<Object, Object, Object> {

        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d("Log", "[Log #1008] Broadcast.java > AsyncTaskUploadSMS > doInBackground() : 전송 시작");

                Context context = (Context) params[0];

                // RESTful API 서버로 데이터를 전송하기 위하여 초기화함
                URL url = new URL(_restFulApiUrl + "/api/set/message");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept","application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                // 인자값을 정의함
                String paramSendType = "S";                             // 메시지 전송 구분(M:메신저, S:SMS)
                String paramMessageType = "T";                          // 메시지 형식 구분(T:텍스트, M:MMS)
                String paramTransmitType = (String) params[1];          // 송수신 구분(S:보냄, R:받음)
                String paramSendMobileNumber = (String) params[2];      // 보내는 휴대폰 번호
                String paramReceiveMobileNumber = (String) params[3];   // 받는 휴대폰 번호
                String paramReceiveDatetime = (String) params[4];       // 전송 일시
                String paramMessageContent = (String) params[5];        // 메시지 내용

                // 데이터를 JSON 형식으로 변환함
                JSONObject jsonParam = new JSONObject();

                jsonParam.put("send_type", paramSendType);
                jsonParam.put("message_type", paramMessageType);
                jsonParam.put("transmit_type", paramTransmitType);
                jsonParam.put("send_mobile_number", paramSendMobileNumber);
                jsonParam.put("receive_mobile_number", paramReceiveMobileNumber);
                jsonParam.put("receive_datetime", paramReceiveDatetime);
                jsonParam.put("message_content", paramMessageContent);

                // RESTful API 서버로 데이터를 전송함
                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                Log.d("Log", "[Log #1009] Broadcast.java > AsyncTaskUploadSMS > doInBackground() : 전송 완료");

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
                        Log.d("Error", "[Error #5005] Broadcast.java > AsyncTaskUploadSMS > doInBackground() > BufferedReader() : " + e.toString());
                        e.printStackTrace();
                        Log.d("Error", "..........................................................................................");
                    }

                    responseJSON = new JSONObject(builder.toString());
                }

                // 전송 결과값에 따라 각각 처리함
                switch (responseJSON.get("status").toString()) {
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
                        */

                        //JSONArray jarray = new JSONArray(sb.toString());

                        /*
                        for(int i=0; i < jarray.length(); i++){
                            JSONObject jObject = jarray.getJSONObject(i);  // JSONObject 추출
                            String rsFirebaseDeviceToken = jObject.getString("firebase_device_token");
                            Log.i("[메일]", rsFirebaseDeviceToken);
                        }
                        */

                        // SMS 전송
                        //PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT_ACTION"), 0);
                        //PendingIntent deliveredIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_DELIVERED_ACTION"), 0);
                        //SmsManager mSmsManager = SmsManager.getDefault();
                        //mSmsManager.sendTextMessage(origNumber, null, "당신의 메시지:" + message, sentIntent, deliveredIntent);
                        break;
                }

                // 전송 처리를 종료함
                conn.disconnect();
            } catch(Exception e) {
                Log.d("Error", "[Error #5006] Broadcast.java > AsyncTaskUploadSMS > doInBackground() : " + e.toString());
                e.printStackTrace();
                Log.d("Error", "..........................................................................................");
            }

            return null;
        }

        protected void onPostExecute() {
        }

    }

}
