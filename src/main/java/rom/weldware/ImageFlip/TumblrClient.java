package rom.weldware.ImageFlip;

import android.net.Uri;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by WeldFire on 2/4/14.
 */
public class TumblrClient {
    Settings settings;

    public interface OnInitUrlFoundListener {
        public void onURLFound();
    }

    /**
     * Register a callback to be invoked when a touch event is sent to this view.
     * @param l the touch listener to attach to this view
     */
    public void setOnInitUrlFoundListener(OnInitUrlFoundListener l) {
        mOnInitUrlFoundListener = l;
    }

    private OnInitUrlFoundListener mOnInitUrlFoundListener;

    public TumblrClient(Settings settings){
        this.settings = settings;
    }

    public void refreshTumblrURLs(final String query){
        if(settings.tumblrUrls.size() < settings.tumblrMaxUrls && !settings.isQueryLimitExceeded()){
            String googleSearchURL = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&userip=" + Utils.getIPAddress(true) + "&q=" + Uri.encode("inurl:tumblr.com -tagged ");
            String queryString = Uri.encode(query);
            googleSearchURL = googleSearchURL + queryString;

            int start = settings.getLastQueryIndex();

            Ion .with(this.settings.context, googleSearchURL + "&start=" + start)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e == null) {

                            int statusCode = result.getAsJsonPrimitive("responseStatus").getAsInt();
                            if (statusCode == 200) {
                                final JsonArray results = result.getAsJsonObject("responseData").getAsJsonArray("results");

                                for (int i = 0; i < results.size(); i++) {
                                    JsonObject line = results.get(i).getAsJsonObject();

                                    String recievedUrl = line.getAsJsonPrimitive("url").getAsString();

                                    String cleanResult = recievedUrl.substring(0, recievedUrl.lastIndexOf(".com") + 4);
                                    if (!settings.tumblrUrls.contains(cleanResult)) {
                                        settings.tumblrUrls.add(cleanResult);
                                        if (settings.tumblrUrls.size() == 1) {
                                            if(mOnInitUrlFoundListener != null) {
                                                mOnInitUrlFoundListener.onURLFound();
                                            }
                                        }
                                    }
                                }

                                if (results.size() < 4) {
                                    settings.setQueryLimitExceeded(true);
                                    settings.setLastQueryIndex(settings.getLastQueryIndex()-4);//revert last response on fail
                                }else{
                                    //Restart the query
                                    //refreshTumblrURLs(query); maybe...
                                }
                            } else {
                                Log.e("SearchResponse", "ResponseCode:" + statusCode);
                                settings.setLastQueryIndex(settings.getLastQueryIndex()-4);//revert last response on fail
                            }
                        } else {
                            e.printStackTrace();
                        }
                    }
                });
            start += 4;
            settings.setLastQueryIndex(start);
            if(start >= settings.maxGoogleQueries){
                settings.setQueryLimitExceeded(true);
            }
        }
    }

    // turn a stream into a string
    private static String readToEnd(InputStream input) throws IOException
    {
        DataInputStream dis = new DataInputStream(input);
        byte[] stuff = new byte[1024];
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        int read = 0;
        while ((read = dis.read(stuff)) != -1)
        {
            buff.write(stuff, 0, read);
        }

        return new String(buff.toByteArray());
    }
}