package org.apache.cordova.facebook;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.text.TextUtils;

import com.facebook.FacebookSdk;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.FacebookCallback;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.FacebookDialogException;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookServiceException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.share.widget.*;
import com.facebook.share.model.AppInviteContent;

public class ConnectPlugin extends CordovaPlugin {

    private static final int INVALID_ERROR_CODE = -2; //-1 is FacebookRequestError.INVALID_ERROR_CODE
    private static final String PUBLISH_PERMISSION_PREFIX = "publish";
    private static final String MANAGE_PERMISSION_PREFIX = "manage";
    @SuppressWarnings("serial")
    private static final Set<String> OTHER_PUBLISH_PERMISSIONS = new HashSet<String>() {
        {
            add("ads_management");
            add("create_event");
            add("rsvp_event");
        }
    };
    private final String TAG = "ConnectPlugin";

    private AppEventsLogger logger;
    private String applicationId = null;
    private CallbackContext loginContext = null;
    private CallbackContext showDialogContext = null;
    private CallbackContext graphContext = null;
    private CallbackContext grantPermissionContext = null;
    private Bundle paramBundle;
    private String method;
    private String graphPath;
    private String userID;
    private boolean trackingPendingCall = false;
    private CallbackManager callbackManager;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        FacebookSdk.sdkInitialize(cordova.getActivity().getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        logger = AppEventsLogger.newLogger(cordova.getActivity());

        int appResId = cordova.getActivity().getResources().getIdentifier("fb_app_id", "string", cordova.getActivity().getPackageName());
        applicationId = cordova.getActivity().getString(appResId);

        // Set up the activity result callback to this class
        cordova.setActivityResultCallback(this);
        super.initialize(cordova, webView);
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        // Developers can observe how frequently users activate their app by logging an app activation event.
        AppEventsLogger.activateApp(cordova.getActivity());
    }

    // protected void onSaveInstanceState(Bundle outState) {
    //     uiHelper.onSaveInstanceState(outState);
    // }

    // public void onPause() {
    //     uiHelper.onPause();
    // }

    // @Override
    // public void onDestroy() {
    //     super.onDestroy();
    //     uiHelper.onDestroy();
    // }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(TAG, "activity result in plugin: requestCode(" + requestCode + "), resultCode(" + resultCode + ")");

        callbackManager.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("login")) {
            Log.d(TAG, "login FB");
            // Get the permissions
            String[] arrayPermissions = new String[args.length()];
            for (int i = 0; i < args.length(); i++) {
                arrayPermissions[i] = args.getString(i);
            }

            List<String> permissions = null;
            if (arrayPermissions.length > 0) {
                permissions = Arrays.asList(arrayPermissions);
            }
            
            loginContext = callbackContext;
            if(permissions == null) {
                PluginResult pr = new PluginResult(PluginResult.Status.ERROR, "No required permissions");
                loginContext.sendPluginResult(pr);
                return true;
            }

            boolean publishPermissions = false;
            boolean readPermissions = false;

            //filter out already granted permissions
            if (checkActiveToken()) {
                AccessToken accessToken = AccessToken.getCurrentAccessToken();
                for(String permission : accessToken.getPermissions()) {
                    permissions.remove(permission);
                }
            }

            if(permissions.size() == 0) {
                PluginResult pr = new PluginResult(PluginResult.Status.OK, getResponse());
                loginContext.sendPluginResult(pr);
                return true;
            }
<<<<<<< HEAD
=======

            // Loop through the permissions to see what
            // is being requested
            for (String permission : permissions) {
                if (isPublishPermission(permission)) {
                    publishPermissions = true;
                } else {
                    readPermissions = true;
                }
                // Break if we have a mixed bag, as this is an error
                if (publishPermissions && readPermissions) {
                    break;
                }
            }
            if (publishPermissions && readPermissions) {
                callbackContext.error("Cannot ask for both read and publish permissions.");
            } else {
                PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
                pr.setKeepCallback(true);
                loginContext.sendPluginResult(pr);
                
                cordova.setActivityResultCallback(this);
                
                LoginManager.getInstance().registerCallback(callbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            PluginResult pr = new PluginResult(PluginResult.Status.OK, getResponse());
                            loginContext.sendPluginResult(pr);
                        }

                        @Override
                        public void onCancel() {
                            PluginResult pr = new PluginResult(PluginResult.Status.ERROR, "User cancelled login");
                            loginContext.sendPluginResult(pr);
                        }

                        @Override
                        public void onError(FacebookException exception) {
                            PluginResult pr = new PluginResult(PluginResult.Status.ERROR, exception.getMessage());
                            loginContext.sendPluginResult(pr);
                        }
                    }
                );

                if (publishPermissions) {
                    LoginManager.getInstance().logInWithPublishPermissions(
                        cordova.getActivity(),
                        Arrays.asList(arrayPermissions)
                    );
                } else {
                    LoginManager.getInstance().logInWithReadPermissions(
                        cordova.getActivity(),
                        Arrays.asList(arrayPermissions)
                    );
                }
            }
            return true;
        } else if (action.equals("logout")) {
            if (checkActiveToken()) {
                LoginManager loginManager = LoginManager.getInstance();
                loginManager.logOut();
                callbackContext.success("User logged out");
            } else {
                callbackContext.error("No valid session found, must call init and login before logout.");
            }
            return true;
        } else if (action.equals("getLoginStatus")) {
            callbackContext.success(getResponse());
            return true;
        } else if (action.equals("getAccessToken")) {
            if (checkActiveToken()) {
                callbackContext.success(AccessToken.getCurrentAccessToken().getToken());
            } else {
                callbackContext.error("No valid session found, must call init and login before logout.");
            }
            return true;
        }
        // else if (action.equals("logEvent")) {
        //     if (args.length() == 0) {
        //         // Not enough parameters
        //         callbackContext.error("Invalid arguments");
        //         return true;
        //     }
        //     String eventName = args.getString(0);
        //     if (args.length() == 1) {
        //         logger.logEvent(eventName);
        //     } else {
        //         // Arguments is greater than 1
        //         JSONObject params = args.getJSONObject(1);
        //         Bundle parameters = new Bundle();

        //         Iterator<?> iterator = params.keys();
        //         while (iterator.hasNext()) {
        //             try {
        //                 // Try get a String
        //                 String key = (String) iterator.next();
        //                 String value = params.getString(key);
        //                 parameters.putString(key, value);
        //             } catch (Exception e) {
        //                 // Maybe it was an int
        //                 Log.w(TAG, "Type in AppEvent parameters was not String for key: " + (String) iterator.next());
        //                 try {
        //                     String key = (String) iterator.next();
        //                     int value = params.getInt(key);
        //                     parameters.putInt(key, value);
        //                 } catch (Exception e2) {
        //                     // Nope
        //                     Log.e(TAG, "Unsupported type in AppEvent parameters for key: " + (String) iterator.next());
        //                 }
        //             }
        //         }
        //         if (args.length() == 2) {
        //             logger.logEvent(eventName, parameters);
        //         }
        //         if (args.length() == 3) {
        //             double value = args.getDouble(2);
        //             logger.logEvent(eventName, value, parameters);
        //         }
        //     }
        //     callbackContext.success();
        //     return true;
        // } else if (action.equals("logPurchase")) {
        //     /*
        //      * While calls to logEvent can be made to register purchase events,
        //      * there is a helper method that explicitly takes a currency indicator.
        //      */
        //     if (args.length() != 2) {
        //         callbackContext.error("Invalid arguments");
        //         return true;
        //     }
        //     int value = args.getInt(0);
        //     String currency = args.getString(1);
        //     logger.logPurchase(BigDecimal.valueOf(value), Currency.getInstance(currency));
        //     callbackContext.success();
        //     return true;
        // }
        else if (action.equals("showDialog")) {
            Bundle collect = new Bundle();
            JSONObject params = null;
            try {
                params = args.getJSONObject(0);
            } catch (JSONException e) {
                params = new JSONObject();
            }
>>>>>>> sdk-4.0

            // Loop through the permissions to see what
            // is being requested
            for (String permission : permissions) {
                if (isPublishPermission(permission)) {
                    publishPermissions = true;
                } else {
                    readPermissions = true;
                }
<<<<<<< HEAD
                // Break if we have a mixed bag, as this is an error
                if (publishPermissions && readPermissions) {
                    break;
                }
            }
            if (publishPermissions && readPermissions) {
                callbackContext.error("Cannot ask for both read and publish permissions.");
            } else {
                PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
                pr.setKeepCallback(true);
                loginContext.sendPluginResult(pr);
                
                cordova.setActivityResultCallback(this);
                
                LoginManager.getInstance().registerCallback(callbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            PluginResult pr = new PluginResult(PluginResult.Status.OK, getResponse());
                            loginContext.sendPluginResult(pr);
                        }

                        @Override
                        public void onCancel() {
                            PluginResult pr = new PluginResult(PluginResult.Status.ERROR, "User cancelled login");
                            loginContext.sendPluginResult(pr);
                        }

                        @Override
                        public void onError(FacebookException exception) {
                            PluginResult pr = new PluginResult(PluginResult.Status.ERROR, exception.getMessage());
                            loginContext.sendPluginResult(pr);
                        }
                    }
                );

                if (publishPermissions) {
                    LoginManager.getInstance().logInWithPublishPermissions(
                        cordova.getActivity(),
                        Arrays.asList(arrayPermissions)
                    );
                } else {
                    LoginManager.getInstance().logInWithReadPermissions(
                        cordova.getActivity(),
                        Arrays.asList(arrayPermissions)
                    );
                }
            }
            return true;
        } else if (action.equals("logout")) {
            if (checkActiveToken()) {
                LoginManager loginManager = LoginManager.getInstance();
                loginManager.logOut();
                callbackContext.success("User logged out");
            } else {
                callbackContext.error("No valid session found, must call init and login before logout.");
            }
            return true;
        } else if (action.equals("getLoginStatus")) {
            callbackContext.success(getResponse());
            return true;
        } else if (action.equals("getAccessToken")) {
            if (checkActiveToken()) {
                callbackContext.success(AccessToken.getCurrentAccessToken().getToken());
            } else {
                callbackContext.error("No valid session found, must call init and login before logout.");
            }
            return true;
        }
        // else if (action.equals("logEvent")) {
        //     if (args.length() == 0) {
        //         // Not enough parameters
        //         callbackContext.error("Invalid arguments");
        //         return true;
        //     }
        //     String eventName = args.getString(0);
        //     if (args.length() == 1) {
        //         logger.logEvent(eventName);
        //     } else {
        //         // Arguments is greater than 1
        //         JSONObject params = args.getJSONObject(1);
        //         Bundle parameters = new Bundle();

        //         Iterator<?> iterator = params.keys();
        //         while (iterator.hasNext()) {
        //             try {
        //                 // Try get a String
        //                 String key = (String) iterator.next();
        //                 String value = params.getString(key);
        //                 parameters.putString(key, value);
        //             } catch (Exception e) {
        //                 // Maybe it was an int
        //                 Log.w(TAG, "Type in AppEvent parameters was not String for key: " + (String) iterator.next());
        //                 try {
        //                     String key = (String) iterator.next();
        //                     int value = params.getInt(key);
        //                     parameters.putInt(key, value);
        //                 } catch (Exception e2) {
        //                     // Nope
        //                     Log.e(TAG, "Unsupported type in AppEvent parameters for key: " + (String) iterator.next());
        //                 }
        //             }
        //         }
        //         if (args.length() == 2) {
        //             logger.logEvent(eventName, parameters);
        //         }
        //         if (args.length() == 3) {
        //             double value = args.getDouble(2);
        //             logger.logEvent(eventName, value, parameters);
        //         }
        //     }
        //     callbackContext.success();
        //     return true;
        // } else if (action.equals("logPurchase")) {
        //     /*
        //      * While calls to logEvent can be made to register purchase events,
        //      * there is a helper method that explicitly takes a currency indicator.
        //      */
        //     if (args.length() != 2) {
        //         callbackContext.error("Invalid arguments");
        //         return true;
        //     }
        //     int value = args.getInt(0);
        //     String currency = args.getString(1);
        //     logger.logPurchase(BigDecimal.valueOf(value), Currency.getInstance(currency));
        //     callbackContext.success();
        //     return true;
        // }
        // else if (action.equals("showDialog")) {
        //     Bundle collect = new Bundle();
        //     JSONObject params = null;
        //     try {
        //         params = args.getJSONObject(0);
        //     } catch (JSONException e) {
        //         params = new JSONObject();
        //     }

        //     final ConnectPlugin me = this;
        //     Iterator<?> iter = params.keys();
        //     while (iter.hasNext()) {
        //         String key = (String) iter.next();
        //         if (key.equals("method")) {
        //             try {
        //                 this.method = params.getString(key);
        //             } catch (JSONException e) {
        //                 Log.w(TAG, "Nonstring method parameter provided to dialog");
        //             }
        //         } else {
        //             try {
        //                 collect.putString(key, params.getString(key));
        //             } catch (JSONException e) {
        //                 // Need to handle JSON parameters
        //                 Log.w(TAG, "Nonstring parameter provided to dialog discarded");
        //             }
        //         }
        //     }
        //     this.paramBundle = new Bundle(collect);
            
        //     //The Share dialog prompts a person to publish an individual story or an Open Graph story to their timeline.
        //     //This does not require Facebook Login or any extended permissions, so it is the easiest way to enable sharing on web.
        //     boolean isShareDialog = this.method.equalsIgnoreCase("share") || this.method.equalsIgnoreCase("share_open_graph");
        //     //If is a Share dialog but FB app is not installed the WebDialog Builder fails. 
        //     //In Android all WebDialogs require a not null Session object.
        //     boolean canPresentShareDialog = isShareDialog && (FacebookDialog.canPresentShareDialog(me.cordova.getActivity(), FacebookDialog.ShareDialogFeature.SHARE_DIALOG));
        //     //Must be an active session when is not a Shared dialog or if the Share dialog cannot be presented.
        //     boolean requiresAnActiveSession = (!isShareDialog) || (!canPresentShareDialog);
        //     if (requiresAnActiveSession) {
        //         if (!checkActiveToken()) {
        //             callbackContext.error("No active session");
        //             return true;
        //         }
        //     }

        //     // Begin by sending a callback pending notice to Cordova
        //     showDialogContext = callbackContext;
        //     PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        //     pr.setKeepCallback(true);
        //     showDialogContext.sendPluginResult(pr);

        //     // Setup callback context
        //     final OnCompleteListener dialogCallback = new OnCompleteListener() {

        //         @Override
        //         public void onComplete(Bundle values, FacebookException exception) {
        //             if (exception != null) {
        //                 handleError(exception, showDialogContext);
        //             } else {
        //                 handleSuccess(values);
        //             }
        //         }
        //     };

        //     if (this.method.equalsIgnoreCase("feed")) {
        //         Runnable runnable = new Runnable() {
        //             public void run() {
        //                 WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(me.cordova.getActivity(), Session.getActiveSession(), paramBundle)).setOnCompleteListener(dialogCallback).build();
        //                 feedDialog.show();
        //             }
        //         };
        //         cordova.getActivity().runOnUiThread(runnable);
        //     } else if (this.method.equalsIgnoreCase("apprequests")) {
        //         Runnable runnable = new Runnable() {
        //             public void run() {
        //                 WebDialog requestsDialog = (new WebDialog.RequestsDialogBuilder(me.cordova.getActivity(), Session.getActiveSession(), paramBundle)).setOnCompleteListener(dialogCallback)
        //                     .build();
        //                 requestsDialog.show();
        //             }
        //         };
        //         cordova.getActivity().runOnUiThread(runnable);
        //     } else if (isShareDialog) {
        //         if (canPresentShareDialog) {
        //             Runnable runnable = new Runnable() {
        //                 public void run() {
        //                     // Publish the post using the Share Dialog
        //                     FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(me.cordova.getActivity())
        //                         .setName(paramBundle.getString("name"))
        //                         .setCaption(paramBundle.getString("caption"))
        //                         .setDescription(paramBundle.getString("description"))
        //                         .setLink(paramBundle.getString("href"))
        //                         .setPicture(paramBundle.getString("picture"))
        //                         .build();
        //                     uiHelper.trackPendingDialogCall(shareDialog.present());
        //                 }
        //             };
        //             this.trackingPendingCall = true;
        //             cordova.getActivity().runOnUiThread(runnable);
        //         } else {
        //             // Fallback. For example, publish the post using the Feed Dialog
        //             Runnable runnable = new Runnable() {
        //                 public void run() {
        //                     WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(me.cordova.getActivity(), Session.getActiveSession(), paramBundle)).setOnCompleteListener(dialogCallback).build();
        //                     feedDialog.show();
        //                 }
        //             };
        //             cordova.getActivity().runOnUiThread(runnable);
        //         }
        //     } else if (this.method.equalsIgnoreCase("send")) {
        //         Runnable runnable = new Runnable() {
        //             public void run() {
        //                 FacebookDialog.MessageDialogBuilder builder = new FacebookDialog.MessageDialogBuilder(me.cordova.getActivity());
        //                 if(paramBundle.containsKey("link"))
        //                     builder.setLink(paramBundle.getString("link"));
        //                 if(paramBundle.containsKey("caption"))
        //                     builder.setCaption(paramBundle.getString("caption"));
        //                 if(paramBundle.containsKey("name"))
        //                     builder.setName(paramBundle.getString("name"));
        //                 if(paramBundle.containsKey("picture"))
        //                     builder.setPicture(paramBundle.getString("picture"));
        //                 if(paramBundle.containsKey("description"))
        //                     builder.setDescription(paramBundle.getString("description"));
        //                 // Check for native FB Messenger application
        //                 if (builder.canPresent()) {
        //                     if("true".equals(paramBundle.getString("peek"))) {
        //                         Log.v(TAG, "Invoking callback due to peek param");
        //                         showDialogContext.success("Messaging available");
        //                     }
        //                     else {
        //                         FacebookDialog dialog = builder.build();
        //                         dialog.present();
        //                     }
        //                 }  else {
        //                     // Not found
        //                     trackingPendingCall = false;
        //                     String errMsg = "Messaging unavailable.";
        //                     Log.e(TAG, errMsg);
        //                     showDialogContext.error(errMsg);
        //                 }
        //             };
        //         };
        //         this.trackingPendingCall = true;
        //         cordova.getActivity().runOnUiThread(runnable);
        //     } else {
        //         callbackContext.error("Unsupported dialog method.");
        //     }
        //    return true;
        // }
=======
            }
            //this.paramBundle = new Bundle(collect);
            
        //     //The Share dialog prompts a person to publish an individual story or an Open Graph story to their timeline.
        //     //This does not require Facebook Login or any extended permissions, so it is the easiest way to enable sharing on web.
        //     boolean isShareDialog = this.method.equalsIgnoreCase("share") || this.method.equalsIgnoreCase("share_open_graph");
        //     //If is a Share dialog but FB app is not installed the WebDialog Builder fails. 
        //     //In Android all WebDialogs require a not null Session object.
        //     boolean canPresentShareDialog = isShareDialog && (FacebookDialog.canPresentShareDialog(me.cordova.getActivity(), FacebookDialog.ShareDialogFeature.SHARE_DIALOG));
        //     //Must be an active session when is not a Shared dialog or if the Share dialog cannot be presented.
        //     boolean requiresAnActiveSession = (!isShareDialog) || (!canPresentShareDialog);
        //     if (requiresAnActiveSession) {
        //         if (!checkActiveToken()) {
        //             callbackContext.error("No active session");
        //             return true;
        //         }
        //     }

        //     // Begin by sending a callback pending notice to Cordova
        //     showDialogContext = callbackContext;
        //     PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        //     pr.setKeepCallback(true);
        //     showDialogContext.sendPluginResult(pr);

        //     // Setup callback context
        //     final OnCompleteListener dialogCallback = new OnCompleteListener() {

        //         @Override
        //         public void onComplete(Bundle values, FacebookException exception) {
        //             if (exception != null) {
        //                 handleError(exception, showDialogContext);
        //             } else {
        //                 handleSuccess(values);
        //             }
        //         }
        //     };
            if (this.method.equalsIgnoreCase("appInvite")) {
                if("true".equals(collect.getString("canShow", "false"))) {
                    boolean canShow = AppInviteDialog.canShow();
                    Log.d(TAG, "App invite capabilities: " + canShow);
                    callbackContext.success(Boolean.toString(canShow));
                    return true;
                }

                String appLinkUrl, previewImageUrl;

                appLinkUrl = collect.getString("link", null);
                if(appLinkUrl == null) {
                    callbackContext.error("Cannot show App invite dialog without a link URL");
                    return true;
                }
                previewImageUrl = collect.getString("link", null);

                if (AppInviteDialog.canShow()) {
                    AppInviteContent content = new AppInviteContent.Builder()
                                .setApplinkUrl(appLinkUrl)
                                .setPreviewImageUrl(previewImageUrl)
                                .build();

                    AppInviteDialog.show(cordova.getActivity(), content);
                    callbackContext.success("App invite dialog opened");
                }
                else {
                    callbackContext.error("App invite dialog could not be opened");
                }
            }
        //     if (this.method.equalsIgnoreCase("feed")) {
        //         Runnable runnable = new Runnable() {
        //             public void run() {
        //                 WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(me.cordova.getActivity(), Session.getActiveSession(), paramBundle)).setOnCompleteListener(dialogCallback).build();
        //                 feedDialog.show();
        //             }
        //         };
        //         cordova.getActivity().runOnUiThread(runnable);
        //     } else if (this.method.equalsIgnoreCase("apprequests")) {
        //         Runnable runnable = new Runnable() {
        //             public void run() {
        //                 WebDialog requestsDialog = (new WebDialog.RequestsDialogBuilder(me.cordova.getActivity(), Session.getActiveSession(), paramBundle)).setOnCompleteListener(dialogCallback)
        //                     .build();
        //                 requestsDialog.show();
        //             }
        //         };
        //         cordova.getActivity().runOnUiThread(runnable);
        //     } else if (isShareDialog) {
        //         if (canPresentShareDialog) {
        //             Runnable runnable = new Runnable() {
        //                 public void run() {
        //                     // Publish the post using the Share Dialog
        //                     FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(me.cordova.getActivity())
        //                         .setName(paramBundle.getString("name"))
        //                         .setCaption(paramBundle.getString("caption"))
        //                         .setDescription(paramBundle.getString("description"))
        //                         .setLink(paramBundle.getString("href"))
        //                         .setPicture(paramBundle.getString("picture"))
        //                         .build();
        //                     uiHelper.trackPendingDialogCall(shareDialog.present());
        //                 }
        //             };
        //             this.trackingPendingCall = true;
        //             cordova.getActivity().runOnUiThread(runnable);
        //         } else {
        //             // Fallback. For example, publish the post using the Feed Dialog
        //             Runnable runnable = new Runnable() {
        //                 public void run() {
        //                     WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(me.cordova.getActivity(), Session.getActiveSession(), paramBundle)).setOnCompleteListener(dialogCallback).build();
        //                     feedDialog.show();
        //                 }
        //             };
        //             cordova.getActivity().runOnUiThread(runnable);
        //         }
        //     } else if (this.method.equalsIgnoreCase("send")) {
        //         Runnable runnable = new Runnable() {
        //             public void run() {
        //                 FacebookDialog.MessageDialogBuilder builder = new FacebookDialog.MessageDialogBuilder(me.cordova.getActivity());
        //                 if(paramBundle.containsKey("link"))
        //                     builder.setLink(paramBundle.getString("link"));
        //                 if(paramBundle.containsKey("caption"))
        //                     builder.setCaption(paramBundle.getString("caption"));
        //                 if(paramBundle.containsKey("name"))
        //                     builder.setName(paramBundle.getString("name"));
        //                 if(paramBundle.containsKey("picture"))
        //                     builder.setPicture(paramBundle.getString("picture"));
        //                 if(paramBundle.containsKey("description"))
        //                     builder.setDescription(paramBundle.getString("description"));
        //                 // Check for native FB Messenger application
        //                 if (builder.canPresent()) {
        //                     if("true".equals(paramBundle.getString("peek"))) {
        //                         Log.v(TAG, "Invoking callback due to peek param");
        //                         showDialogContext.success("Messaging available");
        //                     }
        //                     else {
        //                         FacebookDialog dialog = builder.build();
        //                         dialog.present();
        //                     }
        //                 }  else {
        //                     // Not found
        //                     trackingPendingCall = false;
        //                     String errMsg = "Messaging unavailable.";
        //                     Log.e(TAG, errMsg);
        //                     showDialogContext.error(errMsg);
        //                 }
        //             };
        //         };
        //         this.trackingPendingCall = true;
        //         cordova.getActivity().runOnUiThread(runnable);
            else {
                callbackContext.error("Unsupported dialog method.");
            }
            return true;
        }
>>>>>>> sdk-4.0
        // else if (action.equals("graphApi")) {
        //     graphContext = callbackContext;
        //     PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        //     pr.setKeepCallback(true);
        //     graphContext.sendPluginResult(pr);

        //     graphPath = args.getString(0);

        //     JSONArray arr = args.getJSONArray(1);

        //     final List<String> permissionsList = new ArrayList<String>();
        //     for (int i = 0; i < arr.length(); i++) {
        //         permissionsList.add(arr.getString(i));
        //     }

        //     boolean publishPermissions = false;
        //     boolean readPermissions = false;
        //     if (permissionsList.size() > 0) {
        //         for (String permission : permissionsList) {
        //             if (isPublishPermission(permission)) {
        //                 publishPermissions = true;
        //             } else {
        //                 readPermissions = true;
        //             }
        //             // Break if we have a mixed bag, as this is an error
        //             if (publishPermissions && readPermissions) {
        //                 break;
        //             }
        //         }
        //         if (publishPermissions && readPermissions) {
        //             graphContext.error("Cannot ask for both read and publish permissions.");
        //         } else {
        //             Session session = Session.getActiveSession();
        //             if (session.getPermissions().containsAll(permissionsList)) {
        //                 makeGraphCall();
        //             } else {
        //                 // Set up the new permissions request
        //                 Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(cordova.getActivity(), permissionsList);
        //                 // Set up the activity result callback to this class
        //                 cordova.setActivityResultCallback(this);
        //                 // Check for write permissions, the default is read (empty)
        //                 if (publishPermissions) {
        //                     // Request new publish permissions
        //                     session.requestNewPublishPermissions(newPermissionsRequest);
        //                 } else {
        //                     // Request new read permissions
        //                     session.requestNewReadPermissions(newPermissionsRequest);
        //                 }
        //             }
        //         }
        //     } else {
        //         makeGraphCall();
        //     }
        //     return true;
        // }
        // else if (action.equals("grantPermissions")) {
        //     grantPermissionContext = callbackContext;
        //     PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        //     pr.setKeepCallback(true);
        //     grantPermissionContext.sendPluginResult(pr);

        //     JSONArray arr = args.getJSONArray(0);
        //     final List<String> permissionsList = new ArrayList<String>();
        //     for (int i = 0; i < arr.length(); i++) {
        //         permissionsList.add(arr.getString(i));
        //     }

        //     boolean publishPermissions = false;
        //     boolean readPermissions = false;
        //     if (permissionsList.size() > 0) {
        //         for (String permission : permissionsList) {
        //             if (isPublishPermission(permission)) {
        //                 publishPermissions = true;
        //             } else {
        //                 readPermissions = true;
        //             }
        //             // Break if we have a mixed bag, as this is an error
        //             if (publishPermissions && readPermissions) {
        //                 break;
        //             }
        //         }
        //         if (publishPermissions && readPermissions) {
        //             Log.d(TAG, "Cannot ask for both read and publish permissions");
        //             grantPermissionContext.error("Cannot ask for both read and publish permissions.");
        //         } else {
        //             Session session = Session.getActiveSession();
        //             if (session.getPermissions().containsAll(permissionsList)) {
        //                 Log.d(TAG, "All permissions already granted");
        //                 grantPermissionContext.success(session.getAccessToken());
        //             } else {
        //                 // Set up the new permissions request
        //                 Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(cordova.getActivity(), permissionsList);
                        
        //                 // Set up the activity result callback to this class
        //                 cordova.setActivityResultCallback(this);
        //                 // Check for write permissions, the default is read (empty)
        //                 if (publishPermissions) {
        //                     // Request new publish permissions
        //                     session.requestNewPublishPermissions(newPermissionsRequest);
        //                 } else {
        //                     // Request new read permissions
        //                     session.requestNewReadPermissions(newPermissionsRequest);
        //                 }
        //             }
        //         }
        //     } else {
        //         Session session = Session.getActiveSession();
        //         grantPermissionContext.success(session.getAccessToken());
        //     }

        //     return true;
        // }
        return false;
    }

    private boolean checkActiveToken() {
        AccessToken token = AccessToken.getCurrentAccessToken();
        if (token != null && !token.isExpired()) {
            return true;
        } else {
            return false;
        }
    }

    private void handleError(Exception exception, CallbackContext context) {
        String errMsg = "Facebook error: " + exception.getMessage();
        int errorCode = INVALID_ERROR_CODE;
        // User clicked "x"
        if (exception instanceof FacebookOperationCanceledException) {
            errMsg = "User cancelled dialog";
            errorCode = 4201;
        } else if (exception instanceof FacebookDialogException) {
            // Dialog error
            errMsg = "Dialog error: " + exception.getMessage();
        }

        Log.e(TAG, exception.toString());
        context.error(getErrorResponse(exception, errMsg, errorCode));
    }

    private void handleSuccess(Bundle values) {
        // Handle a successful dialog:
        // Send the URL parameters back, for a requests dialog, the "request" parameter
        // will include the resulting request id. For a feed dialog, the "post_id"
        // parameter will include the resulting post id.
        // Note: If the user clicks on the Cancel button, the parameter will be empty
        if (values.size() > 0) {
            JSONObject response = new JSONObject();
            try {
                Set<String> keys = values.keySet();
                for (String key : keys) {
                    //check if key is array
                    int beginArrayCharIndex = key.indexOf("[");
                    if (beginArrayCharIndex >= 0) {
                        String normalizedKey = key.substring(0, beginArrayCharIndex);
                        JSONArray result;
                        if (response.has(normalizedKey)) {
                            result = (JSONArray) response.get(normalizedKey);
                        } else {
                            result = new JSONArray();
                            response.put(normalizedKey, result);
                        }
                        result.put(result.length(), values.get(key));
                    } else {
                        response.put(key, values.get(key));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            showDialogContext.success(response);
        } else {
            Log.e(TAG, "User cancelled dialog");
            showDialogContext.error("User cancelled dialog");
        }
    }

    // private void getUserInfo(final GraphRequest.GraphJSONObjectCallback graphUserCb) {
    //     if (cordova != null) {
    //         GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), graphUserCb).executeAsync();
    //     }
    // }

    // private void makeGraphCall() {
    //     GraphRequest.Callback graphCallback = new GraphRequest.Callback() {

    //         @Override
    //         public void onCompleted(Response response) {
    //             if (graphContext != null) {
    //                 if (response.getError() != null) {
    //                     graphContext.error(getFacebookRequestErrorResponse(response.getError()));
    //                 } else {
    //                     JSONObject graphObject = response.getGraphObject();
    //                     graphContext.success(graphObject.getInnerJSONObject());
    //                 }
    //                 graphPath = null;
    //                 graphContext = null;
    //             }
    //         }
    //     };

    //     //If you're using the paging URLs they will be URLEncoded, let's decode them.
    //     try {
    //         graphPath = URLDecoder.decode(graphPath, "UTF-8");
    //     } catch (UnsupportedEncodingException e) {
    //         e.printStackTrace();
    //     }

    //     String[] urlParts = graphPath.split("\\?");
    //     String graphAction = urlParts[0];
    //     GraphRequest graphRequest = GraphRequest.newGraphPathRequest(null, graphAction, graphCallback);
    //     Bundle params = graphRequest.getParameters();

    //     if (urlParts.length > 1) {
    //         String[] queries = urlParts[1].split("&");

    //         for (String query : queries) {
    //             int splitPoint = query.indexOf("=");
    //             if (splitPoint > 0) {
    //                 String key = query.substring(0, splitPoint);
    //                 String value = query.substring(splitPoint + 1, query.length());
    //                 params.putString(key, value);
    //             }
    //         }
    //     }
    //     params.putString("access_token", session.getAccessToken());

    //     graphRequest.setParameters(params);
    //     graphRequest.executeAsync();
    // }

    /*
     * Handles session state changes
     */
    // private void onSessionStateChange(SessionState state, Exception exception) {
    //     Log.d(TAG, "onSessionStateChange:" + state.toString());
    //     if (exception != null && exception instanceof FacebookOperationCanceledException) {
    //         // only handle FacebookOperationCanceledException to support
    //         // SDK recovery behavior triggered by getUserInfo
    //         Log.e(TAG, "exception:" + exception.toString());
    //         handleError(exception, loginContext);
    //     } else {
    //         final Session session = Session.getActiveSession();
    //         // Check if the session is open
    //         if (state.isOpened()) {
    //             if (loginContext != null) {
    //                 // Get user info
    //                 getUserInfo(session, new GraphRequest.GraphJSONObjectCallback() {
    //                     @Override
    //                     public void onCompleted(JSONObject user, Response response) {
    //                         // Create a new result with response data
    //                         if (loginContext != null) {
    //                             if (response.getError() != null) {
    //                                 loginContext.error(getFacebookRequestErrorResponse(response.getError()));
    //                             } else {
    //                                 JSONObject graphObject = response.getGraphObject();
    //                                 Log.d(TAG, "returning login object " + graphObject.getInnerJSONObject().toString());
    //                                 userID = user.getId();
    //                                 loginContext.success(getResponse());
    //                                 loginContext = null;
    //                             }
    //                         } else {
    //                             // Just update the userID in case we force quit the application before
    //                             userID = user.getId();
    //                         }
    //                     }
    //                 });
    //             } else if (graphContext != null) {
    //                 // Make the graph call
    //                 makeGraphCall();
    //             } else if (grantPermissionContext != null) {
    //                 grantPermissionContext.success(session.getAccessToken());
    //             }
    //         } else if (state == SessionState.CLOSED_LOGIN_FAILED && loginContext != null){
    //             handleError(new FacebookAuthorizationException("Session was closed and was not closed normally"), loginContext);
    //         }
    //     }
    // }

    /*
     * Checks for publish permissions
     */
    private boolean isPublishPermission(String permission) {
        return permission != null && (permission.startsWith(PUBLISH_PERMISSION_PREFIX) || permission.startsWith(MANAGE_PERMISSION_PREFIX) || OTHER_PUBLISH_PERMISSIONS.contains(permission));
    }

    /**
     * Create a Facebook Response object that matches the one for the Javascript SDK
     * @return JSONObject - the response object
     */
    public JSONObject getResponse() {
        try {
            JSONObject response = new JSONObject();
            if (checkActiveToken()) {
                AccessToken accessToken = AccessToken.getCurrentAccessToken();
                Date today = new Date();
                long expiresTimeInterval = (accessToken.getExpires().getTime() - today.getTime()) / 1000L;
                long expiresIn = (expiresTimeInterval > 0) ? expiresTimeInterval : 0;
                
                response.put("status", "connected");
                JSONObject authResponse = new JSONObject();
                authResponse.put("accessToken", accessToken.getToken());
                authResponse.put("expiresIn", expiresIn);
                authResponse.put("userID", accessToken.getUserId());
                authResponse.put("declinedPermissions", accessToken.getDeclinedPermissions());
                authResponse.put("permissions", accessToken.getPermissions());
                response.put("authResponse", authResponse);
            } else {
                response.put("status", "unknown");
            }
            return response;
        }
        catch(JSONException e) {
            Log.e(TAG, "Got JSONException when building response", e);
            return new JSONObject();
        }
    }

    public JSONObject getFacebookRequestErrorResponse(FacebookRequestError error) {
        try {
            JSONObject response = new JSONObject();

            response.put("errorCode", error.getErrorCode());
            response.put("errorType", error.getErrorType());
            response.put("errorMessage", error.getErrorMessage());
            response.put("errorUserMessage", error.getErrorUserMessage());

            return response;
        }
        catch(JSONException e) {
            Log.e(TAG, "Got JSONException when building response", e);
            return new JSONObject();
        }
    }

    public JSONObject getErrorResponse(Exception error, String message, int errorCode) {

        if (error instanceof FacebookServiceException) {
            return getFacebookRequestErrorResponse(((FacebookServiceException) error).getRequestError());
        }

        String response = "{";

        if (error instanceof FacebookDialogException) {
            errorCode = ((FacebookDialogException) error).getErrorCode();
        }

        if (errorCode != INVALID_ERROR_CODE) {
            response += "\"errorCode\": \"" + errorCode + "\",";
        }

        if (message == null) {
            message = error.getMessage();
        }

        response += "\"errorMessage\": \"" + message + "\"}";

        try {
            return new JSONObject(response);
        } catch (JSONException e) {

            e.printStackTrace();
        }
        return new JSONObject();
    }
}