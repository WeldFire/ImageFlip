package rom.weldware.ImageFlip;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

/**
 * Created by WeldFire on 2/27/14.
 */

class UpdateSettingsComplete extends EventObject {
    public UpdateSettingsComplete(Object source){
        super(source);
    }
}

interface SettingsCompleteListener {
    void updateComplete(UpdateSettingsComplete ene);
}

class alertMessage {
    public String alertID;
    public String btnText;
    public String message;
    public String title;

    public void displayMessage(final Context context){
        this.message = this.message.replace("\\n", "\n");//Replace \n with actual new lines

        String versionName = "Unknown Version!";
        try {
            versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        this.message = this.message.replace("{version}", versionName);//Replace with the current version name

        new AlertDialog.Builder(context)
                .setTitle(this.title)
                .setMessage(this.message)
                .setPositiveButton(btnText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("alert_" + alertID, true);
                        editor.commit();
                    }
                })
                .show();
    }
}

public class RemoteSettings {
    String remoteUrl = "https://dl.dropboxusercontent.com/s/fuqz4njwrsemwy0/SETTINGS";
    public static String prefID = "remotesettings_last_checked";
    private HashMap<String, String> remoteSettings = new HashMap<String, String>();
    private Context context;
    public ArrayList<alertMessage> alertMessageList;


    RemoteSettings(Context context){
        this.context = context;
        this.alertMessageList = new ArrayList<alertMessage>();
    }

    private String getCurrentDate(){
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
        String formattedDate = format.format(date);
        return formattedDate;
    }

    public void checkSettings(){
        if(shouldUpdate()) {
            //new DownloadSettingsTask().execute(this.remoteUrl);//Old way
            Ion .with(this.context, this.remoteUrl)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        if(e == null){
                            Scanner scanner = new Scanner(result);
                            while (scanner.hasNextLine()) {
                                String line = scanner.nextLine();
                                // str is one line of text; readLine() strips the newline character(s)
                                if(!line.isEmpty() && !line.contains("//")){//Ignore white space and any comment line

                                    if(line.startsWith("alert-")){//Is this an alert box?
                                        String content[] = line.split("'");
                                        alertMessage nextMessage = new alertMessage();
                                        nextMessage.alertID = content[0];
                                        nextMessage.title = content[1];
                                        nextMessage.message = content[2];

                                        if(content.length >= 4){
                                            nextMessage.btnText = content[3];
                                        }else{
                                            nextMessage.btnText = "OK";
                                        }

                                        alertMessageList.add(nextMessage);
                                    }else{//Or is this a setting?
                                        String content[] = line.split(" = ");
                                        if (content.length == 2) {
                                            remoteSettings.put(content[0].trim(), content[1].trim());
                                        }
                                    }
                                }
                            }
                            scanner.close();
                            updateSettings();
                            OnSettingsComplete();
                        }else{
                            e.printStackTrace();
                        }
                    }
                });
        }else{
            OnSettingsComplete();
        }
    }

    public boolean shouldUpdate(){
        boolean shouldUpdate = true;//TODO change this back to false for release
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        SimpleDateFormat  format = new SimpleDateFormat("MM-dd-yyyy");
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();



        String lastChecked = prefs.getString(prefID, "01-01-1990");
        try {
            Date todayNoTime = format.parse(format.format(today));
            Date lastDate = format.parse(lastChecked);

            if(lastDate.before(todayNoTime)){
                shouldUpdate = true;
            }
        } catch (ParseException e) {
            shouldUpdate = true;
            e.printStackTrace();
        }
        return shouldUpdate;
    }

    private void updateSettings(){
        setCheckedPreference();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);

        int listSize = this.alertMessageList.size();//This variable might change so we need to save it before the loop
        for (int i = listSize-1; i >= 0; i--){//For each message we received
            if(prefs.contains("alert_" + alertMessageList.get(i).alertID)){//have we displayed it before?
                alertMessageList.remove(i);//If so remove it..
            }
        }

        SharedPreferences.Editor prefsEditor = prefs.edit();
        Map<String,?> allPrefs = prefs.getAll();

        for (Map.Entry<String, String> entry : this.remoteSettings.entrySet()) {//For each remote setting name
            if(prefs.contains(entry.getKey())){//prefs.contains(key) //Check if the preference exists
                Object value = allPrefs.get(entry.getKey());//Get the pref type

                //parse the remote data to the correct type
                if(value instanceof Float){
                    Float fValue = Float.parseFloat(entry.getValue());//Parse object
                    prefsEditor.putFloat(entry.getKey(), fValue);//set the pref
                }else if(value instanceof Long){
                    Long lValue = Long.parseLong(entry.getValue());//Parse object
                    prefsEditor.putLong(entry.getKey(), lValue);//set the pref
                }else if(value instanceof Integer){
                    int iValue = Integer.parseInt(entry.getValue());//Parse object
                    prefsEditor.putInt(entry.getKey(), iValue);//set the pref
                }else if(value instanceof Boolean){
                    Boolean bValue = Boolean.parseBoolean(entry.getValue());//Parse object
                    prefsEditor.putBoolean(entry.getKey(), bValue);//set the pref
                }else if(value instanceof String){
                    String sValue = entry.getValue();//Parse object
                    prefsEditor.putString(entry.getKey(), sValue);//set the pref
                }
            }
        }

        prefsEditor.commit();//Commit changes
    }

    private void setCheckedPreference(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(RemoteSettings.prefID, this.getCurrentDate());
        editor.commit();
    }

    private class DownloadSettingsTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            String returned = "";
            try {
                // Create a URL for the desired page
                URL url = new URL(params[0]);

                // Read all the text returned by the server
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String str;
                while ((str = in.readLine()) != null) {
                    // str is one line of text; readLine() strips the newline character(s)
                    if(!str.isEmpty() && !str.contains("//")){//Ignore white space and any comment line

                        if(str.startsWith("alert-")){//Is this an alert box?
                            String content[] = str.split("'");
                            alertMessage nextMessage = new alertMessage();
                            nextMessage.alertID = content[0];
                            nextMessage.title = content[1];
                            nextMessage.message = content[2];

                            if(content.length >= 4){
                                nextMessage.btnText = content[3];
                            }else{
                                nextMessage.btnText = "OK";
                            }

                            alertMessageList.add(nextMessage);
                        }else{//Or is this a setting?
                            String content[] = str.split(" = ");
                            if (content.length == 2) {
                                remoteSettings.put(content[0].trim(), content[1].trim());
                            }
                        }
                    }
                }
                in.close();
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            }
            return returned;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateSettings();
            OnSettingsComplete();
        }
    }

    Vector subscribers = new Vector();

    private void OnSettingsComplete(){
        for(int i=0, size = subscribers.size(); i < size; i++)
            ((SettingsCompleteListener)subscribers.get(i)).updateComplete(new UpdateSettingsComplete(this));
    }


    public void addSettingsCompleteEventListener(SettingsCompleteListener ensl){
        subscribers.add(ensl);
    }

    public void removeSettingsCompleteEventListener(SettingsCompleteListener ensl){
        subscribers.remove(ensl);
    }
}
