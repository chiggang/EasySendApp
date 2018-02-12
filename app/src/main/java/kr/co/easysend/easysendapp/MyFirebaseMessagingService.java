/**
 * App : EasySend
 * Title : FirebaseMessagingService 처리
 * Date : 2017.12.12
 * by chiggang
 */

package kr.co.easysend.easysendapp;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    /* ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** *
     *
     * Section : 초기화
     *
     * ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** */

    /**
     * Firebase 알림 메시지를 받음
     *
     * @param remoteMessage
     */
    @SuppressLint("WrongThread")
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            Map<String, String> data = remoteMessage.getData();

            String paramTriggerType = data.get("trigger_type");
            String paramSendType = data.get("send_type");
            String paramMessageType = data.get("messages_type");
            String paramTransmitType = data.get("transmit_type");
            String paramSendMobileNumber = data.get("send_mobile_number");
            String paramReceiveMobileNumber = data.get("receive_mobile_number");
            String paramReceiveDatetime = data.get("receive_datetime");
            String paramMessageContent = data.get("message_content");

            Log.d("Log", "[Log #1010] paramTriggerType: " + paramTriggerType);
            Log.d("Log", "[Log #1010] paramSendType: " + paramSendType);
            Log.d("Log", "[Log #1010] paramMessageType: " + paramMessageType);
            Log.d("Log", "[Log #1010] paramTransmitType: " + paramTransmitType);
            Log.d("Log", "[Log #1010] paramSendMobileNumber: " + paramSendMobileNumber);
            Log.d("Log", "[Log #1010] paramReceiveMobileNumber: " + paramReceiveMobileNumber);
            Log.d("Log", "[Log #1010] paramReceiveDatetime: " + paramReceiveDatetime);
            Log.d("Log", "[Log #1010] paramMessageContent: " + paramMessageContent);

            switch (paramTriggerType) {
                // SMS
                case "S":
                    Log.d("Log", "[Log #1011] S !!!!! paramTriggerType: " + paramTriggerType);

                    // 비동기식으로 받은 Firebase 메시지를 상대방 전화번호로 전송함(보낸 전화번호, 받은 전화번호, 받은 일시, 메시지 내용)
                    new AsyncTaskSendSMS().execute(this, paramSendMobileNumber, paramReceiveMobileNumber, paramReceiveDatetime, paramMessageContent);

                    // 비동기식으로 받은 SMS 내용을 RESTful API 서버로 전송함(송수신 구분, 보낸 전화번호, 받은 전화번호, 받은 일시, 메시지 내용)
                    new Broadcast.AsyncTaskUploadSMS().execute(this, paramTransmitType, paramSendMobileNumber, paramReceiveMobileNumber, paramReceiveDatetime, paramMessageContent);
                    break;

                // Call
                case "C":
                    Log.d("Log", "[Log #1012] C !!!!! paramTriggerType: " + paramTriggerType);

                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("tel:" + paramReceiveMobileNumber));

                    try {
                        Log.d("Log", "[Log #1013] C1 !!!!! paramTriggerType: " + paramTriggerType);
                        ((Context) this).startActivity(intent);
                    } catch(Exception e) {
                        Log.d("Error", "[Error #5007] MyFirebaseMessagingService.java > onMessageReceived() : " + e.toString());
                        e.printStackTrace();
                        Log.d("Error", "..........................................................................................");
                    }
                    break;
            }

            // 비동기식으로 받은 Firebase 메시지를 상대방 전화번호로 전송함(보낸 전화번호, 받은 전화번호, 받은 일시, 메시지 내용)
            //new AsyncTaskSendSMS().execute(this, paramSendMobileNumber, paramReceiveMobileNumber, paramReceiveDatetime, paramMessageContent);
        } catch(Exception e) {
            Log.d("Error", "[Error #5008] MyFirebaseMessagingService.java > onMessageReceived() : " + e.toString());
            e.printStackTrace();
            Log.d("Error", "..........................................................................................");
        }
    }

    /*
    // Push 알림 띄우기
    private void sendNotification(String messageBody) {
        // 화면+앱이 실행되어 화면에 출력되고 있을 때(2)
        Log.d("[로그/5]", "messageBody: " + messageBody);

        // 실제로 상단 알림에 뜨는 내용
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, TAG)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Firebase Push Notification")
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }
    */



    /* ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** *
     *
     * Section : Firebase cloud message
     *
     * ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** */

    /**
     * 비동기식으로 받은 Firebase 메시지를 상대방 전화번호로 전송함
     */
    private class AsyncTaskSendSMS extends AsyncTask<Object, Object, Object> {

        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d("Log", "[Log #1014] MyFirebaseMessagingService.java > AsyncTaskSendSMS > doInBackground() : 전송 시작");

                Context context = (Context) params[0];

                // 인자값을 정의함
                String paramSendMobileNumber = (String) params[1];      // 보내는 휴대폰 번호
                String paramReceiveMobileNumber = (String) params[2];   // 받는 휴대폰 번호
                String paramReceiveDatetime = (String) params[3];       // 전송 일시
                String paramMessageContent = (String) params[4];        // 메시지 내용

                PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT_ACTION"), 0);
                PendingIntent deliveredIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_DELIVERED_ACTION"), 0);

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(paramReceiveMobileNumber, null, paramMessageContent, sentIntent, deliveredIntent);

                // 보내는 SMS 내용을 비동기식으로 RESTful API 서버로 전송함(보낸 전화번호, 받은 전화번호, 받은 일시, 메시지 내용)
                //new AsyncTaskUploadSendSMS().execute(context, paramSendMobileNumber, paramReceiveMobileNumber, paramReceiveDatetime, paramMessageContent);

                Log.d("Log", "[Log #1015] MyFirebaseMessagingService.java > AsyncTaskSendSMS > doInBackground() : 전송 완료");
            } catch(Exception e) {
                Log.d("Error", "[Error #5009] MyFirebaseMessagingService.java > AsyncTaskSendSMS > doInBackground() : " + e.toString());
                e.printStackTrace();
                Log.d("Error", "..........................................................................................");
            }

            return null;
        }

        protected void onPostExecute() {
        }

    }



    /* ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** *
     *
     * Section : RESTful API
     *
     * ********** ********** ********** ********** ********** ********** ********** ********** ********** ********** */

    /**
     * 보내는 SMS 내용을 비동기식으로 RESTful API 서버로 전송함
     */
    /*
    private class AsyncTaskUploadSendSMS extends AsyncTask<Object, Object, Object> {

        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d("Log", "[Log #1020] MyFirebaseMessagingService.java > AsyncTaskUploadSendSMS > doInBackground() : 전송 시작");

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
                String paramTransmitType = "S";                         // 송수신 구분(S:보냄, R:받음)
                String paramSendMobileNumber = (String) params[1];      // 보내는 휴대폰 번호
                String paramReceiveMobileNumber = (String) params[2];   // 받는 휴대폰 번호
                String paramReceiveDatetime = (String) params[3];       // 전송 일시
                String paramMessageContent = (String) params[4];        // 메시지 내용

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

                Log.d("Log", "[Log #1021] MyFirebaseMessagingService.java > AsyncTaskUploadSendSMS > doInBackground() : 전송 완료");

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
                        Log.d("Error", "[Error #5007] MyFirebaseMessagingService.java > AsyncTaskUploadSendSMS > doInBackground() > BufferedReader() : " + e.toString());
                        e.printStackTrace();
                        Log.d("Error", "..........................................................................................");
                    }

                    responseJSON = new JSONObject(builder.toString());
                }

                // 전송 결과값에 따라 각각 처리함
                switch (responseJSON.get("status").toString()) {
                    case "200":
                    case "201":
                        break;
                }

                // 전송 처리를 종료함
                conn.disconnect();
            } catch(Exception e) {
                Log.d("Error", "[Error #5008] MyFirebaseMessagingService.java > AsyncTaskUploadSendSMS > doInBackground() : " + e.toString());
                e.printStackTrace();
                Log.d("Error", "..........................................................................................");
            }

            return null;
        }

        protected void onPostExecute() {
        }

    }
    */

}
