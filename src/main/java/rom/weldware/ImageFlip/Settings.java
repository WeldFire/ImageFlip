package rom.weldware.ImageFlip;

import android.content.Context;
import com.koushikdutta.ion.Ion;

/**
 * Created by WeldFire on 1/28/14.
 */
public class Settings {
    final public int tumblrMaxUrls = 20;
    final public int maxGoogleQueries = 100;
    final public int imagesViewedBetweenAds = 15;

    Context context;
    public SerializableArrayList tumblrUrls;
    public SerializableArrayList dislikeUrls;
    private String search_query;
    private String lastImageURL;
    private boolean vibrateOnCompletion;

    private boolean filterNSFW;
    private boolean deleteAll;
    private boolean exactQueries;
    private int cacheSize;
    private int imagesViewed;

    private static class googleSettings{
        public static boolean queryLimitExceeded;
        public static int lastQueryIndex = 0;
    }

    public Settings(Context inputContext) {
        context = inputContext;
        tumblrUrls = new SerializableArrayList(context, "tumblrUrls");
        dislikeUrls = new SerializableArrayList(context, "dislikeUrls");

        search_query = "null";
        lastImageURL = "";

        deleteAll = false;
        filterNSFW = true;
        exactQueries = true;
        vibrateOnCompletion = false;
        googleSettings.queryLimitExceeded = false;

        cacheSize = 20;
        imagesViewed = 0;

        this.loadSettings();
    }

    public String dislikeLastURL(){
        if(!this.lastImageURL.equals("")){//Make sure the url isn't empty
            if(!dislikeUrls.contains(this.lastImageURL)){//Make sure the url isn't already there
                dislikeUrls.add(this.lastImageURL);
            }
        }
        return this.lastImageURL;
    }


    public void loadSettings(){
        tumblrUrls.load();
        dislikeUrls.load();
    }

    public void saveSettings(){
        tumblrUrls.save();
        dislikeUrls.save();
    }

    public void setLastImageURL(String lastImageURL) {
        this.lastImageURL = lastImageURL;

        if(this.deleteAll){
            this.dislikeLastURL();
        }
    }

    public String getLastImageURL(){
        return this.lastImageURL;
    }

    public boolean imageURLDisliked(String url){
        return dislikeUrls.contains(url);
    }

    public void emptyTumblrUrls(){
        tumblrUrls.clear();
        Ion.getDefault(this.context).cancelAll(this.context);//Stop all connections
    }

    public void setSearch_query(String newQuery){
        if(this.search_query.equals("null")){
            this.search_query = newQuery;
        }else if(!this.search_query.equals(newQuery)){//New query is different and not uninitialized
            this.search_query = newQuery;       //Set the new query
            googleSettings.queryLimitExceeded = false;//Reset google settings
            googleSettings.lastQueryIndex = 0;
            this.emptyTumblrUrls();             //Empty the URL cache
        }
    }

    public String getSearch_query(){
        return this.search_query;
    }

    public void setQueryLimitExceeded(boolean queryLimitExceeded) {
        googleSettings.queryLimitExceeded = queryLimitExceeded;
    }

    public boolean isQueryLimitExceeded(){
        return googleSettings.queryLimitExceeded;
    }

    public void setVibrateOnCompletion(boolean vibrateOnCompletion) {
        this.vibrateOnCompletion = vibrateOnCompletion;
    }

    public boolean vibrateOnCompletion(){
        return this.vibrateOnCompletion;
    }

    public void setFilterNSFW(boolean filterNSFW) {
        this.filterNSFW = filterNSFW;
    }

    public boolean filterNSFW() {
        return filterNSFW;
    }

    public void setDeleteAll(boolean deleteAll) {
        this.deleteAll = deleteAll;
    }

    public boolean deleteAll() {
        return deleteAll;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setExactQueries(boolean exactQueries) {
        this.exactQueries = exactQueries;
    }

    public boolean exactQueries() {
        return exactQueries;
    }

    public void setLastQueryIndex(int index){
        googleSettings.lastQueryIndex = index;
    }

    public int getLastQueryIndex(){
        return googleSettings.lastQueryIndex;
    }

    public int getImagesViewed() {
        return imagesViewed;
    }

    public void setImagesViewed(int imagesViewed) {
        this.imagesViewed = imagesViewed;
    }
}