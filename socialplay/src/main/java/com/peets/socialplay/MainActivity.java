package com.peets.socialplay;

import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.CallbackManager;

import com.peets.socialplay.R;
import com.peets.socialplay.server.Account;
import com.peets.socialplay.server.AccountArray;
import com.peets.socialplay.server.IdentityType;

import java.io.IOException;

public class MainActivity extends FragmentActivity {

    private static final int SPLASH = 0;
    private static final int FRAGMENT_COUNT = SPLASH +1;

    private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
    private boolean isResumed = false;
    private AccessTokenTracker accessTokenTracker;
    private CallbackManager callbackManager;
    private static String TAG ="MainActivity";
    private Long accountId = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());

        if (savedInstanceState != null) {
        }
        callbackManager = CallbackManager.Factory.create();

        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
                                                       AccessToken currentAccessToken) {
                if (isResumed) {
                    FragmentManager manager = getSupportFragmentManager();
                    int backStackSize = manager.getBackStackEntryCount();
                    for (int i = 0; i < backStackSize; i++) {
                        manager.popBackStack();
                    }
                    if (currentAccessToken != null) {
                        showFragment(SPLASH, false);
                    } else {
                        showFragment(SPLASH, false);
                    }
                }
            }
        };

        setContentView(R.layout.main);

        FragmentManager fm = getSupportFragmentManager();
        SplashFragment splashFragment = (SplashFragment) fm.findFragmentById(R.id.splashFragment);
        fragments[SPLASH] = splashFragment;

        FragmentTransaction transaction = fm.beginTransaction();
        for(int i = 0; i < fragments.length; i++) {
            transaction.hide(fragments[i]);
        }
        transaction.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        isResumed = true;

        // Call the 'activateApp' method to log an app event for use in analytics and advertising
        // reporting.  Do so in the onResume methods of the primary Activities that an app may be
        // launched into.
        AppEventsLogger.activateApp(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        isResumed = false;

        // Call the 'deactivateApp' method to log an app event for use in analytics and advertising
        // reporting.  Do so in the onPause methods of the primary Activities that an app may be
        // launched into.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        AccessToken token = AccessToken.getCurrentAccessToken();
        if (token != null) {

            RegisterTask registerTask = new RegisterTask();

            //TODO: need a way to get user's name
            registerTask.execute(token.getUserId(), "Susan");
        } else {
            // otherwise present the splash screen and ask the user to login,
            showFragment(SPLASH, false);
        }
    }

    private class RegisterTask extends
            AsyncTask<String, Void, Long> {
        @Override
        protected Long doInBackground(String... params) {
            int count = 0;
            while(count < 5) {
                Long returnValue = SocialPlayRestServer.registerAccount(IdentityType.FB, params[0], params[1]);
                if(returnValue != null) {
                    Log.e(TAG, "register task returns: " + returnValue);

                    return returnValue;
                }
                count++;

            }

            return null;
        }

        @Override
        protected void onPostExecute(Long result) {
            Log.e(TAG, "KeepLiveTask onPostExecute received: " + result);
            accountId = result;

            GetOnlineFriendsTask getOnlineFriendsTask = new GetOnlineFriendsTask();
            getOnlineFriendsTask.execute(accountId);
//            // if the user already logged in, proceed to the TreasureHunt screen
//            Intent intent = new Intent(getApplicationContext(), TreasureHuntRestActivity.class);
//
//            intent.putExtra(TreasureHuntRestActivity.ACCOUNTID, result);
//            startActivity(intent);
        }
    }

    private class GetOnlineFriendsTask extends
            AsyncTask<Long, Void, AccountArray> {
        @Override
        protected AccountArray doInBackground(Long... params) {
            int count = 0;
            while(count < 2) {
                Account[] accounts = SocialPlayRestServer.findOnlineFriends(accountId);
                if(accounts != null && accounts.length > 0) {
                    Log.e(TAG, "GetOnlineFriendsTask returns: " + accounts.length);

                    AccountArray accountArray = new AccountArray();
                    for(int i = 0; i<accounts.length; i++)
                        accountArray.add(accounts[i]);
                    return accountArray;
                }
                count++;
            }

            return new AccountArray();
        }

        @Override
        protected void onPostExecute(AccountArray result) {
            Log.e(TAG, "KeepLiveTask onPostExecute received: " + result);

            // if the user already logged in, proceed to the TreasureHunt screen
            Intent intent = new Intent(getApplicationContext(), TreasureHuntRestActivity.class);

            intent.putExtra(TreasureHuntRestActivity.ACCOUNTID, accountId);
            if(result != null && result.size()>0)
            {
                StringBuilder sb = new StringBuilder();
                for(int i = 0 ; i < result.size(); i++)
                {
                    Account account = result.get(i);
                    sb.append(account.getAccountId());
                    sb.append(":");
                    sb.append(account.getName());
                    if(i != result.size() -1)
                        sb.append(";");
                }
                intent.putExtra(TreasureHuntRestActivity.FRIENDS, sb.toString());
            }
            startActivity(intent);
        }
    }
    private void showFragment(int fragmentIndex, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        for (int i = 0; i < fragments.length; i++) {
            if (i == fragmentIndex) {
                transaction.show(fragments[i]);
            } else {
                transaction.hide(fragments[i]);
            }
        }
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}
