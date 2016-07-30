package com.marz.snapprefs.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.marz.snapprefs.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ActivateFragment extends Fragment {
    public View view = null;
    public Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activate_layout,
                container, false);
        context = container.getContext();
        final TelephonyManager tm = (TelephonyManager) getActivity().getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getActivity().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();

        Button submitbtn = (Button) view.findViewById(R.id.submit);
        final EditText cID = (EditText) view.findViewById(R.id.confirmationID);
        final TextView textView = (TextView) view.findViewById(R.id.textView);
        final Button buynow = (Button) view.findViewById(R.id.button);
        final Button applygod = (Button) view.findViewById(R.id.god);
        final EditText name = (EditText) view.findViewById(R.id.username);
        final TextView god = (TextView) view.findViewById(R.id.god_desc);
        TextView dID = (TextView) view.findViewById(R.id.deviceID);
        god.setVisibility(View.GONE);
        applygod.setVisibility(View.GONE);
        name.setVisibility(View.GONE);
        dID.setText(deviceId);
        cID.setText(readStringPreference("confirmation_id"));
        final String deviceID = dID.getText().toString();
        final String confirmationID = cID.getText().toString();
        String text = "Your license status is: <font color='#FFCC00'>Deluxe</font>";
        textView.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
        buynow.setVisibility(View.GONE);
        if (!confirmationID.isEmpty()) {
            //new Connection().execute(cID.getText().toString(), deviceID);
        }

        submitbtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                new Connection().execute(cID.getText().toString(), deviceID);
            }
        });
        applygod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (name.getText().toString().trim().length() > 0){
                    MessageDigest md = null;
                    try {
                        md = MessageDigest.getInstance("SHA-256");
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "NoSuchAlgorithm", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        md.update(name.getText().toString().getBytes("UTF-8")); // Change this to "UTF-16" if needed
                    } catch (UnsupportedEncodingException e) {
                        Toast.makeText(context, "Invalid username", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    byte[] digest = md.digest();
                    String hashed = String.format("%064x", new java.math.BigInteger(1, digest));
                    new ConnectionGod().execute(cID.getText().toString(), hashed);
                } else {
                    Toast.makeText(context, "Empty username", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }
    public void postData(final String confirmationID, final String deviceID) {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://snapprefs.com/checkuser.php");
        saveDeviceID(deviceID);
        saveStringPreference("device_id", deviceID);
        saveStringPreference("confirmation_id", confirmationID);

        try {
            // Add your data
            List nameValuePairs = new ArrayList(2);
            nameValuePairs.add(new BasicNameValuePair("confirmationID", confirmationID));
            nameValuePairs.add(new BasicNameValuePair("deviceID", deviceID));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);

            InputStream is = response.getEntity().getContent();
            BufferedInputStream bis = new BufferedInputStream(is);
            final ByteArrayBuffer baf = new ByteArrayBuffer(20);

            int current = 0;

            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }

            /* Convert the Bytes read to a String. */
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // This code will always run on the UI thread, therefore is safe to modify UI elements.
                    String text = new String(baf.toByteArray());
                    String status = null;
                    String error_msg = null;
                    TextView txtvw = (TextView) view.findViewById(R.id.textView);
                    TextView errorTV = (TextView) view.findViewById(R.id.errorTV);
                    Button buynow = (Button) view.findViewById(R.id.button);
                    Button applygod = (Button) view.findViewById(R.id.god);
                    EditText name = (EditText) view.findViewById(R.id.username);
                    TextView god = (TextView) view.findViewById(R.id.god_desc);
                    try {

                        JSONObject obj = new JSONObject(text);
                        status = obj.getString("status");
                        error_msg = obj.getString("error_msg");

                        if (status.equals("0") && error_msg.isEmpty()) {
                            String text2 = "Your license status is: <font color='#FFCC00'>Deluxe</font>";
                            txtvw.setText(Html.fromHtml(text2), TextView.BufferType.SPANNABLE);
                            buynow.setVisibility(View.GONE);
                            errorTV.setText("");
                            saveLicense(deviceID, confirmationID, 2);
                            new AlertDialog.Builder(context)
                                    .setTitle("Apply License")
                                    .setMessage("License verification is done, you have to do a soft reboot. If you want to type in your Redeem ID, click dismiss, otherwise click Soft Reboot. Without it, you will not be able to use your license and Snapprefs properly.")
                                    .setPositiveButton("Soft reboot", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                Process proc = Runtime.getRuntime()
                                                        .exec(new String[]{"su", "-c", "busybox killall system_server"});
                                                proc.waitFor();
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    })
                                    .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .show();
                        }

                    } catch (Throwable t) {
                        Log.e("Snapprefs", "Could not parse malformed JSON: \"" + text + "\"");
                        Log.e("Snapprefs", t.toString());
                        errorTV.setText("Error while reedeming your license, bad response");
                        saveLicense(deviceID, confirmationID, 0);
                    }

                }
            });
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            Toast.makeText(context, "ClientProtocolException" + e.toString(), Toast.LENGTH_SHORT).show();
            saveLicense(deviceID, confirmationID, 0);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            Toast.makeText(context, "IOException" + e.toString(), Toast.LENGTH_SHORT).show();
            saveLicense(deviceID, confirmationID, 0);
        }
    }
    public void postGod(final String confirmationID, final String username) {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://snapprefs.com/god.php");

        try {
            // Add your data
            List nameValuePairs = new ArrayList(2);
            nameValuePairs.add(new BasicNameValuePair("confirmationID", confirmationID));
            nameValuePairs.add(new BasicNameValuePair("username", username));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);

            InputStream is = response.getEntity().getContent();
            BufferedInputStream bis = new BufferedInputStream(is);
            final ByteArrayBuffer baf = new ByteArrayBuffer(20);

            int current = 0;

            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }

            /* Convert the Bytes read to a String. */
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // This code will always run on the UI thread, therefore is safe to modify UI elements.
                    String text = new String(baf.toByteArray());
                    String status = null;
                    String error_msg = null;
                    TextView errorTV = (TextView) view.findViewById(R.id.errorTV);
                    try {

                        JSONObject obj = new JSONObject(text);
                        status = obj.getString("status");
                        error_msg = obj.getString("error_msg");
                        errorTV.setText(error_msg);
                        errorTV.setVisibility(View.VISIBLE);
                    } catch (Throwable t) {
                        Log.e("Snapprefs", "Could not parse malformed JSON: \"" + text + "\"");
                        errorTV.setText("Error while applying for god, bad response");
                        errorTV.setVisibility(View.VISIBLE);
                    }

                }
            });
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            Toast.makeText(context, "ClientProtocolException" + e.toString(), Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            Toast.makeText(context, "IOException" + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLicense(String deviceID, String confirmationID, int i) {
        if (confirmationID != null) {
            SharedPreferences.Editor editor = context.getSharedPreferences("com.marz.snapprefs_preferences", Context.MODE_WORLD_READABLE).edit();
            editor.putString("device_id", deviceID);
            editor.putInt(deviceID, i);
            editor.apply();
        }
    }

    public int readLicense(String deviceID, String confirmationID) {
        int status;
        if (confirmationID != null) {
            SharedPreferences prefs = context.getSharedPreferences("com.marz.snapprefs_preferences", Context.MODE_WORLD_READABLE);
            String dvcid = prefs.getString("device_id", null);
            if (dvcid != null && dvcid.equals(deviceID)) {
                status = 2;
            } else {
                status = 2;
            }
        } else {
            status = 2;
        }
        return status;
    }

    public void saveStringPreference(String key, String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences("com.marz.snapprefs_preferences", Context.MODE_WORLD_READABLE).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void saveDeviceID(String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences("com.marz.snapprefs_preferences", Context.MODE_WORLD_READABLE).edit();
        editor.putString("device_id", value);
        editor.apply();
    }
    public String readStringPreference(String key) {
        SharedPreferences prefs = context.getSharedPreferences("com.marz.snapprefs_preferences", Context.MODE_WORLD_READABLE);
        String returned = prefs.getString(key, null);
        return returned;
    }

    private class Connection extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            postData(params[0], params[1]);
            return null;
        }

    }
    private class ConnectionGod extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            postGod(params[0], params[1]);
            return null;
        }

    }
}
