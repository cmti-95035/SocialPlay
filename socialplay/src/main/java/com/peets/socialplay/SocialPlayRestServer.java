package com.peets.socialplay;

import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.CreateRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.common.EmptyRecord;
import com.peets.socialplay.server.Account;
import com.peets.socialplay.server.AccountArray;
import com.peets.socialplay.server.ActivationBuilders;
import com.peets.socialplay.server.Identity;
import com.peets.socialplay.server.IdentityType;
import com.peets.socialplay.server.RegistrationBuilders;
import com.peets.socialplay.server.RegistrationCreateBuilder;
import com.peets.socialplay.server.RegistrationDoFindOnlineFriendsBuilder;
import com.peets.socialplay.server.RegistrationDoInviteBuilder;
import com.peets.socialplay.server.RegistrationDoKeepLiveBuilder;
import com.peets.socialplay.server.RegistrationDoRegisterAccountBuilder;
import com.peets.socialplay.server.SocialPlayBuilders;
import com.peets.socialplay.server.SocialPlayContext;
import com.peets.socialplay.server.SocialPlayCreateBuilder;
import com.peets.socialplay.server.SocialPlayDoFindIncomingInvitationBuilder;
import com.peets.socialplay.server.SocialPlayDoFindParticipantJoinedBuilder;
import com.peets.socialplay.server.SocialPlayDoInviteToChatBuilder;
import com.peets.socialplay.server.SocialPlayDoUpdateParticipantJoinedBuilder;

import java.util.Collections;

import android.util.Log;

public class SocialPlayRestServer {
    // Create an HttpClient and wrap it in an abstraction layer
    private static final HttpClientFactory http = new HttpClientFactory();
    private static final Client r2Client = new TransportClientAdapter(
            http.getClient(Collections.<String, String> emptyMap()));
    private static final String BASE_URL = "http://54.183.228.194:8080/SocialPlay-server/";
    // Create a RestClient to talk to localhost:8080
    private static RestClient restClient = new RestClient(r2Client, BASE_URL);
    private static RegistrationBuilders registrationBuilders = new RegistrationBuilders();
    private static SocialPlayBuilders socialPlayBuilders = new SocialPlayBuilders();
    private static ActivationBuilders activationBuilders = new ActivationBuilders();
    private static final String TAG = "SocialPlayRestServer";

    /**
     * ping to server to keep live
     * @param accountId
     * @return
     */
    public static Boolean keepLive(long accountId){
        try {
            RegistrationDoKeepLiveBuilder registrationDoKeepLiveBuilder = registrationBuilders.actionKeepLive();

            ActionRequest<Boolean> keepAliveRequest = registrationDoKeepLiveBuilder.paramAccountId(accountId).build();
            ResponseFuture<Boolean> keepAliveFuture = restClient.sendRequest(keepAliveRequest);
            Response<Boolean> keepAliveResponse = keepAliveFuture.getResponse();

            return keepAliveResponse.getEntity();
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error doing keep live: " + ex.getMessage());
        }

        return false;
    }

    /**
     * invite a friend -- this is different from chat invitation
     * @param invitor
     * @param invitee
     * @return
     */
    public static Boolean invite(long invitor, long invitee)
    {
        try {
            RegistrationDoInviteBuilder registrationDoKeepLiveBuilder = registrationBuilders.actionInvite();

            ActionRequest<Boolean> inviteRequest = registrationDoKeepLiveBuilder.paramInvitor(invitor).paramInvitee(invitee).build();
            ResponseFuture<Boolean> inviteFuture = restClient.sendRequest(inviteRequest);
            Response<Boolean> inviteFutureResponse = inviteFuture.getResponse();

            return inviteFutureResponse.getEntity();
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error doing keep live: " + ex.getMessage());
        }

        return false;
    }

    /**
     * invite a friend to a remote play
     * @param invitor
     * @param invitee
     * @return video chat Room ID
     */
    public static String inviteToChat(long invitor, long invitee)
    {
        try{
            SocialPlayDoInviteToChatBuilder socialPlayDoInviteToChatBuilder = socialPlayBuilders.actionInviteToChat();
            ActionRequest<String> actionRequest = socialPlayDoInviteToChatBuilder.paramInvitor(invitor).paramInvitee(invitee).build();
            ResponseFuture<String> actionFuture = restClient.sendRequest(actionRequest);
            Response<String> actionResponse = actionFuture.getResponse();

            return actionResponse.getEntity();
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error doing inviteToChat: " + ex.getMessage());
        }

        return null;
    }

    /**
     * check whether the invited participant joined the chat
     * @param invitor
     * @param invitee
     * @param roomId
     * @return
     */
    public static Boolean findParticipantJoined(long invitor, long invitee, String roomId)
    {
        try{
            SocialPlayDoFindParticipantJoinedBuilder socialPlayDoFindParticipantJoinedBuilder = socialPlayBuilders.actionFindParticipantJoined();
            ActionRequest<Boolean> actionRequest = socialPlayDoFindParticipantJoinedBuilder.paramInvitor(invitor).paramInvitee(invitee).paramRoomId(roomId).build();
            ResponseFuture<Boolean> actionFuture = restClient.sendRequest(actionRequest);
            Response<Boolean> actionResponse = actionFuture.getResponse();

            return actionResponse.getEntity();
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error doing findParticipantJoined: " + ex.getMessage());
        }

        return false;
    }

    /**
     * find out whether there's any incoming invitation
     * @param accountId
     * @return
     */
    public static SocialPlayContext findIncomingInvitation(long accountId)
    {
        try{
            SocialPlayDoFindIncomingInvitationBuilder socialPlayDoFindIncomingInvitationBuilder = socialPlayBuilders.actionFindIncomingInvitation();
            ActionRequest<SocialPlayContext> actionRequest = socialPlayDoFindIncomingInvitationBuilder.paramInvitee(accountId).build();
            ResponseFuture<SocialPlayContext> actionFuture = restClient.sendRequest(actionRequest);
            Response<SocialPlayContext> actionResponse = actionFuture.getResponse();

            return actionResponse.getEntity();
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error doing findIncomingInvitation: " + ex.getMessage());
        }

        return null;
    }

    /**
     * update the join status
     * @param invitor
     * @param invitee
     * @param roomId
     * @param joined
     * @return
     */
    public static Boolean updateParticipantJoined(long invitor, long invitee, String roomId, Boolean joined)
    {
        try{
            SocialPlayDoUpdateParticipantJoinedBuilder socialPlayDoUpdateParticipantJoinedBuilder = socialPlayBuilders.actionUpdateParticipantJoined();
            ActionRequest<Boolean> actionRequest = socialPlayDoUpdateParticipantJoinedBuilder.paramInvitor(invitor).paramInvitee(invitee).paramRoomId(roomId).paramJoined(joined).build();
            ResponseFuture<Boolean> actionFuture = restClient.sendRequest(actionRequest);
            Response<Boolean> actionResponse = actionFuture.getResponse();

            return actionResponse.getEntity();
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error doing updateParticipantJoined: " + ex.getMessage());
        }

        return false;
    }

    /**
     * register a new user
     * @param identityType
     * @param identityStr
     * @param userName
     * @return
     */
    public static Long registerAccount(IdentityType identityType, String identityStr, String userName)
    {
        try
        {
            RegistrationDoRegisterAccountBuilder registrationDoRegisterAccountBuilder = registrationBuilders.actionRegisterAccount();
            Account account = new Account();
            account.setIdentity(new Identity().setIdentityStr(identityStr).setIdentityType(identityType)).setName(userName);

            ActionRequest<Account> actionRequest = registrationDoRegisterAccountBuilder.paramEntity(account).build();
            ResponseFuture<Account> accountResponseFuture = restClient.sendRequest(actionRequest);
            Response<Account> accountResponse = accountResponseFuture.getResponse();

            return accountResponse.getEntity().getAccountId();
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error doing registerAccount: " + ex.getMessage());
        }

        return null;
    }

    /**
     * find out a list of online friends
     * @param accountId
     * @return
     */
    public static Account[] findOnlineFriends(Long accountId)
    {
        try
        {
            RegistrationDoFindOnlineFriendsBuilder registrationDoFindOnlineFriendsBuilder = registrationBuilders.actionFindOnlineFriends();

            ActionRequest<AccountArray> actionRequest = registrationDoFindOnlineFriendsBuilder.paramAccountId(accountId).build();
            ResponseFuture<AccountArray> accountResponseFuture = restClient.sendRequest(actionRequest);
            Response<AccountArray> accountResponse = accountResponseFuture.getResponse();

            return accountResponse.getEntity().toArray(new Account[0]);
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error doing registerAccount: " + ex.getMessage());
        }

        return null;
    }
}
