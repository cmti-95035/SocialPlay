package com.peets.socialplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.peets.socialplay.server.Account;
import com.peets.socialplay.server.AccountArray;
import com.peets.socialplay.server.SocialPlayContext;

import java.io.IOException;

/*
 * the main activity to host the webRTC connectivity
 */
public class TreasureHuntRestActivity extends Activity {
    private static final String TAG = "TreasureHuntRestActivity";
    private Button[] buttons = new Button[3];
    private ImageView imageView = null;

    // state information
    private String chatRoom = null;
    private boolean chatInProgress = false;

    private MediaPlayer mp = null;
    private MediaPlayer mp2 = null;
    private PlayRingtoneTask pTask = null;
    private long duration = 0;
    private CheckExistingConnectionTask checkTask = null;
    private static KeepLiveTask keepLiveTask = null;

    public static String CHATROOM = "chatRoom";

    public static String ACCOUNTID = "accountid";
    public static String FRIENDS = "friends";
    private static Long myAccount = null;
    private Long participantAccount = 1234568L;
    private AccountArray accountArray = null;

    private AccountArray fromString(String friendsStr) {
        if (friendsStr == null)
            return new AccountArray();

        String[] parts = friendsStr.split(";");
        AccountArray accountArray1 = new AccountArray(parts.length);
        for (String part : parts) {
            String[] accountParts = part.split(":");

            Account account = new Account().setAccountId(Long.valueOf(accountParts[0])).setName(accountParts[1]);
            accountArray1.add(account);
        }

        return accountArray1;
    }

    /**
     * Called when the activity is first created. This is where we'll hook up
     * our views in XML layout files to our application.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        disableStrictMode();
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        myAccount = intent.getLongExtra(ACCOUNTID, 1234568L);
        String friendsStr = intent.getStringExtra(FRIENDS);

        if (friendsStr != null)
            accountArray = fromString(friendsStr);

        Log.e(TAG, "On create");
        setContentView(R.layout.playdate);

        // this is the button for a user to connect to a friend
        if (accountArray != null && accountArray.size() > 0) {
            buttons[0] = (Button) findViewById(R.id.button1);
            buttons[0].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "OnClick buttons[0]");

                    buttons[0].setText(R.string.connecting);
                    participantAccount = accountArray.get(0).getAccountId();
                    disableButtons();
                    inviteToPlay();
                }
            });
        }

        if (accountArray != null && accountArray.size() > 1) {
            buttons[1] = (Button) findViewById(R.id.button2);
            buttons[1].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "buttons[1] OnClick");

                    buttons[1].setText(R.string.connecting);
                    participantAccount = accountArray.get(1).getAccountId();
                    disableButtons();
                    inviteToPlay();
                }
            });
        }

        if (accountArray != null && accountArray.size() > 2) {
            buttons[2] = (Button) findViewById(R.id.button3);
            buttons[2].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "buttons[1] OnClick");

                    buttons[2].setText(R.string.connecting);
                    participantAccount = accountArray.get(2).getAccountId();
                    disableButtons();
                    inviteToPlay();
                }
            });
        }

        imageView = (ImageView) findViewById(R.id.imageView1);
        imageView.setImageResource(R.drawable.invite);
        imageView.setVisibility(View.VISIBLE);
    }

    private void disableStrictMode() {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    private void disableButtons() {
        if (accountArray != null) {
            for (int i = 0; i < accountArray.size(); i++)
                buttons[i].setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Called when the activity is coming to the foreground. This is where we
     * will check whether there's an incoming connection.
     */
    @Override
    protected void onStart() {
        chatInProgress = false;
        Log.e(TAG, "onStart");
        super.onStart();

        if (mp2 == null) {
            mp2 = MediaPlayer.create(this, R.raw.ringtone);
        }
        if (mp == null) {
            mp = MediaPlayer.create(this, R.raw.playdate);
        }
        duration = (long) mp.getDuration() + 500;
        mp.start();

        if (accountArray != null && accountArray.size() > 0) {
            buttons[0].setText(accountArray.get(0).getName());
            buttons[0].setEnabled(true);
        }


        if (accountArray != null && accountArray.size() > 1) {
            buttons[1].setText(accountArray.get(1).getName());
            buttons[1].setEnabled(true);
        }


        if (accountArray != null && accountArray.size() > 2) {
            buttons[2].setText(accountArray.get(2).getName());
            buttons[2].setEnabled(true);
        }

        if (keepLiveTask == null) {
            keepLiveTask = new KeepLiveTask();
            keepLiveTask.execute();
        }
    }

    /**
     * utility to do sleep
     *
     * @param milliseconds
     */
    public static void sleep(long milliseconds) {
        try {
            Log.e(TAG, "will sleep " + milliseconds
                    + "milliseconds");
            Thread.sleep(milliseconds);
        } catch (Exception ex) {
            Log.e(TAG,
                    "sleep encounters exception: " + ex.getMessage());
        }
    }

    /**
     * Async task to constantly ping server to keep live
     */
    private class KeepLiveTask extends
            AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            int count = 0;
            while (count < 1) {
                Boolean returnValue = SocialPlayRestServer.keepLive(myAccount);
                Log.e(TAG, "keep live returns: " + returnValue);

                // keep it constantly running in the background
                sleep(30000);
            }

            return (Void) null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.e(TAG, "KeepLiveTask onPostExecute received: " + result);
        }
    }

    /**
     * Async task to initiate a remote play invitation
     */
    private class InviteToPlayTask extends
            AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String response = null;
            int count = 0;
            while (count < 3) {
                Log.e(TAG, "InviteToPlayTask doInBackground chatInProgress: "
                        + chatInProgress);

                response = SocialPlayRestServer.inviteToChat(myAccount, participantAccount);
                if (response != null)
                    break;      // find a chat room to invite the other party with
                count++;
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            chatRoom = result;
            Log.e(TAG, "InviteToPlayTask onPostExecute received: " + result);
            Log.e(TAG, "will start CheckConnectionEstablishedTask");
            CheckConnectionEstablishedTask checkConnectionEstablishedTask = new CheckConnectionEstablishedTask();
            checkConnectionEstablishedTask.execute();
        }
    }

    /**
     * the Async task to constantly poll whether there's an incoming connection
     */
    private class CheckExistingConnectionTask extends
            AsyncTask<Void, Void, SocialPlayContext> {
        @Override
        protected SocialPlayContext doInBackground(Void... params) {
            SocialPlayContext response = null;
            int count = 0;
            while (count < 1) {
                Log.e(TAG, "CheckExistingConnectionTask doInBackground chatInProgress: "
                        + chatInProgress);

                if (!chatInProgress) {
                    response = SocialPlayRestServer.findIncomingInvitation(myAccount);
                    if (response != null && response.hasChatRoomId())
                        break;      // break infinite loop when seeing an incoming request
                } else {
                    Log.e(TAG, "CheckExistingConnectionTask will break because chatInProgress: "
                            + chatInProgress);
                    break;
                }
                sleep(1000);
            }
            return response;
        }

        @Override
        protected void onPostExecute(SocialPlayContext result) {
            chatRoom = result.getChatRoomId();
            Log.e(TAG, "CheckExistingConnectionTask onPostExecute received: " + result);
            Log.e(TAG, "will start PlayRingtoneTask");
            pTask = new PlayRingtoneTask();
            pTask.execute();
            alertConnection("Your friend invites you to a play date",
                    "Accept?");
        }
    }

    /**
     * the Async task to constantly poll whether an outgoing request is accepted
     */
    private class CheckConnectionEstablishedTask extends
            AsyncTask<Boolean, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Boolean... params) {
            Boolean response = false;
            int count = 0;
            while (count < 10) {
                Log.e(TAG,
                        "CheckConnectionEstablishedTask doInBackground count: "
                                + count);

                response = SocialPlayRestServer.findParticipantJoined(myAccount, myAccount, chatRoom);

                if (response != null && response)
                    return response;    // participant joined

                sleep(1000);
                count++;
            }
            return response;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.e(TAG, "onPostExecute result: "
                    + result);
            if (result) {
                // remote party accepted, now go to the video chat
                proceedToChat();
            } else {
                Log.e(TAG, "onPostExecute show alert and back to start");
                alert("Your friend didn't accept your invite", "Please try again!");
                onStart();
            }
        }
    }

    /**
     * Async task to update participant join status
     */
    private class UpdateParticipantJoinedTask extends
            AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            Boolean response = false;
            int count = 0;

            // will try up to 5 times updating to the server
            while (count < 5) {
                Log.e(TAG,
                        "updateParticipantJoinedTask doInBackground count: "
                                + count);

                response = SocialPlayRestServer.updateParticipantJoined(myAccount, myAccount, chatRoom, true);

                if (response != null && response)
                    return response;    // update successful

                count++;
            }
            return response;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.e(TAG, "onPostExecute result: "
                    + result);
            if (result) {
                // remote party accepted, now go to the video chat
                proceedToChat();
            } else {
                Log.e(TAG, "onPostExecute show alert and back to start");
                alert("Your friend didn't accept your invite", "Please try again!");
                onStart();
            }
        }
    }

    /**
     * user clicks a button so initiate the task to invite a friend to play
     */
    private void inviteToPlay() {
        new InviteToPlayTask().execute();
    }

    /**
     * both parties are ready, proceed to chat
     */
    private void proceedToChat() {
        imageView.setImageResource(R.drawable.treasurehunt);

        Intent intent = new Intent(getApplicationContext(), TreasureHuntImageActivity.class);
        intent.putExtra(CHATROOM, chatRoom);
        if (mp != null) {
            mp.pause();
        }
        if (mp2 != null) {
            mp2.pause();
        }
        startActivity(intent);
    }

    /**
     * the Async task to play ringtone upon an incoming connection
     */
    private class PlayRingtoneTask extends
            AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... param) {
            int count = 0;

            while (count < 10) {
                Log.e(TAG, "PlayRingtoneTask doInBackground count: "
                        + count);

                if (!chatInProgress) {
                    Log.e(TAG, "PlayRingtoneTask doInBackground start playing ringtone");
                    mp2.start();
                } else {
                    mp2.pause();
                    break;
                }
                sleep(duration);
                count++;
            }

            return (Void) null;
        }

    }

    /**
     * Show an alert dialog with message. user can either click OK or cancel no
     * real action is triggered
     *
     * @param header
     * @param message
     */
    private void alert(String header, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(header)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * pops up an alert dialog with 2 messages, one as the header and the other
     * as the message to be shown in the dialog. if user clicks on NO, it enable
     * the connect button on screen if user clicks on YES, it connects user to
     * the incoming request
     *
     * @param header
     * @param message
     */
    private void alertConnection(String header, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(header)
                .setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                Log.e(TAG, "NO clicked, will stop mp2");
                                stopRing();
                                dialog.dismiss();
                            }
                        })
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                Log.e(TAG, "OK clicked, will stop mp2");
                                stopRing();
                                dialog.dismiss();
                                chatInProgress = true;

                                // update the server that it accepted the request
                                UpdateParticipantJoinedTask updateParticipantJoinedTask = new UpdateParticipantJoinedTask();
                                updateParticipantJoinedTask.execute((Void) null);
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * stop playing ringtone
     */
    private void stopRing() {
        if (mp2 != null) {
            mp2.pause();
            chatInProgress = true;
        }

        if (pTask != null) {
            Log.e(TAG, "will cancel the pTask");
            pTask.cancel(true);
        }
    }
}

