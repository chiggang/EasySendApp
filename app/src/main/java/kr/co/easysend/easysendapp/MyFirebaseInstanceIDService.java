/**
 * App : EasySend
 * Title : FirebaseInstanceIDService 처리
 * Date : 2017.12.12
 * by chiggang
 */

package kr.co.easysend.easysendapp;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String _tag = "MyFirebaseIIDService";

    @Override
    public void onTokenRefresh() {
        Log.d("[로그/0]", "Refreshed token: !!!!!");

        //Getting registration token
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        sendRegistrationToServer(refreshedToken);

        //Displaying token on logcat
        Log.d(_tag, "Refreshed token: " + refreshedToken);
        Log.d("[로그/1]", "Refreshed token: " + refreshedToken);
    }

    private void sendRegistrationToServer(String token) {
        //You can implement this method to store the token on your server
        //Not required for current project
        Log.d("[로그/2]", "token: " + token);
    }

}
