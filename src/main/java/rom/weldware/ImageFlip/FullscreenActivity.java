package rom.weldware.ImageFlip;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.jirbo.adcolony.AdColony;
import com.jirbo.adcolony.AdColonyVideoAd;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.jsoup.HttpStatusException;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rom.weldware.ImageFlip.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends ActionBarActivity implements SettingsCompleteListener {
    /********************CHROME CAST VARS**************************/
    private static final String TAG = FullscreenActivity.class.getSimpleName();

    private static final int REQUEST_CODE = 1;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private Cast.Listener mCastListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private ConnectionFailedListener mConnectionFailedListener;
    private HelloWorldChannel mHelloWorldChannel;
    private boolean mApplicationStarted;
    private boolean mWaitingForReconnect;
    private String mSessionId;
    /********************CHROME CAST VARS**************************/


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = false;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 5000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    public static GoogleAnalyticsManager analyticsManager;

    class GoogleAnalyticsManager{
        /*
        * Google Analytics configuration values.
        */
        // Placeholder property ID.
        private final String GA_PROPERTY_ID = "UA-48763618-1";

        // Dispatch period in seconds.
        private final int GA_DISPATCH_PERIOD = 30;

        // Prevent hits from being sent to reports, i.e. during testing.
        private final boolean GA_IS_DRY_RUN = false;

        // GA Logger verbosity.
        private final Logger.LogLevel GA_LOG_VERBOSITY = Logger.LogLevel.INFO;

        // Key used to store a user's tracking preferences in SharedPreferences.
        private final String TRACKING_PREF_KEY = "trackingPreference";

        private GoogleAnalytics mGa;
        private Tracker mTracker;

        GoogleAnalyticsManager(Context context){
            mGa = GoogleAnalytics.getInstance(context);
            mTracker = mGa.getTracker(GA_PROPERTY_ID);

            // Set dispatch period.
            GAServiceManager.getInstance().setLocalDispatchPeriod(GA_DISPATCH_PERIOD);

            // Set dryRun flag.
            mGa.setDryRun(GA_IS_DRY_RUN);

            // Set Logger verbosity.
            mGa.getLogger().setLogLevel(GA_LOG_VERBOSITY);

            // Set the opt out flag when user updates a tracking preference.
            SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            userPrefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener () {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals(TRACKING_PREF_KEY)) {
                        GoogleAnalytics.getInstance(getApplicationContext()).setAppOptOut(sharedPreferences.getBoolean(key, false));
                    }
                }
            });
        }

        /*
       * Returns the Google Analytics tracker.
       */
        public Tracker getGaTracker() {
            return mTracker;
        }

        /*
         * Returns the Google Analytics instance.
         */
        public GoogleAnalytics getGaInstance() {
            return mGa;
        }
    }

    private AdView adView;
    private InterstitialAd interstitial;

    private void setupAdMobAds(){
        // Create the interstitial.
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId("ca-app-pub-2575928438432441/7453728213");

        // Create ad request.
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("B24C52E0983905D3884EA97F6515EA70")
                .build();

        // Begin loading your interstitial.
        interstitial.loadAd(adRequest);
        // Create the adView.
        adView = new AdView(this);
        adView.setAdUnitId("ca-app-pub-2575928438432441/7453728213");
        adView.setAdSize(AdSize.BANNER);
    }
    // Invoke displayInterstitial() when you are ready to display an interstitial.
    public void displayInterstitial() {
        /*if (interstitial.isLoaded()) {
            interstitial.show();
            setupAdMobAds();
        }*/
    }


    private void showAdMobAds(){
        if(adView == null){
            setupAdMobAds();
        }
        // Lookup your LinearLayout assuming it's been given
        // the attribute android:id="@+id/mainLayout".
        LinearLayout layout = (LinearLayout)findViewById(R.xml.preferences);

        // Add the adView to it.
        layout.addView(adView);

        // Initiate a generic request.
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("CFC5092E57C674B6AFF09CC757205111")
                .build();

        // Load the adView with the ad request.
        adView.loadAd(adRequest);
    }

    final static String APP_ID  = "appa00ce658d769473ea6";
    final static String ZONE_ID = "vzfad0ab1d093146d687";
    private void startAdColonyVideoAd(){
        AdColony.configure( this, "version:1.0,store:google", APP_ID, ZONE_ID );
        // version - arbitrary application version
        // store   - google or amazon

        AdColonyVideoAd ad = new AdColonyVideoAd( ZONE_ID );
        ad.show();
    }

    enum TranslationDirection{
        Up,
        Down,
        Left,
        Right
    }

    Settings settings;
    ImageStore imageStore;
    TouchImageView imageContent;
    TumblrClient tumblrClient;
    ProgressDialog delayWheel;
    ProgressDialog startupWheel;
    IONDownloader ionDownloader;

    private void setup(){
        // Initialize a tracker using a Google Analytics property ID.
        GoogleAnalytics.getInstance(FullscreenActivity.this).getTracker("UA-48763618-1");

        settings = new Settings(FullscreenActivity.this);
        tumblrClient = new TumblrClient(settings);
        imageStore = new ImageStore();
        imageStore.loadSettings(FullscreenActivity.this);

        //Setup the delayWheel dialog
        delayWheel = new ProgressDialog(FullscreenActivity.this);
        delayWheel.setMessage("Fetching new image");
        delayWheel.setTitle("Please Wait...");
        delayWheel.setCancelable(true);
        delayWheel.setIndeterminate(true);


        //Setup the startupWheel dialog
        startupWheel = new ProgressDialog(FullscreenActivity.this);
        startupWheel.setMessage("Starting up");
        startupWheel.setTitle("Please Wait...");
        startupWheel.setCancelable(false);
        startupWheel.setIndeterminate(true);

        final View backgroundView = findViewById(R.id.background);

        imageContent = (TouchImageView) findViewById(R.id.fullscreen_content);
        ionDownloader = new IONDownloader(FullscreenActivity.this, imageContent, settings);


        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, imageContent, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {

                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                        } else {
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        imageContent.setOnSwipeListener(new TouchImageView.OnSwipeListener() {
            @Override
            public void onSwipeRight() {
                setupImageViewTranslation(TranslationDirection.Right);

                startNewImageGrab();
            }

            @Override
            public void onSwipeLeft() {
                setupImageViewTranslation(TranslationDirection.Left);

                startNewImageGrab();
            }

            @Override
            public void onSwipeUp() {
                String lastURL = settings.dislikeLastURL();
                imageStore.removeImage(lastURL);

                setupImageViewTranslation(TranslationDirection.Up);

                startNewImageGrab();
            }

            @Override
            public void onSwipeDown() {
                setupImageViewTranslation(TranslationDirection.Down);

                saveLastImage(settings.getLastImageURL());

                startNewImageGrab();
            }
        });

        imageContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent();
                intent.setClass(FullscreenActivity.this, SetPreferenceActivity.class);
                startActivityForResult(intent, 0);
                //openOptionsMenu();
                return false;
            }
        });

        backgroundView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent();
                intent.setClass(FullscreenActivity.this, SetPreferenceActivity.class);
                startActivityForResult(intent, 0);
                //openOptionsMenu();
                return false;
            }
        });

        //setupAdMobAds();

        analyticsManager = new GoogleAnalyticsManager(FullscreenActivity.this);

        mSystemUiHider.show();
    }

    RemoteSettings rs;
    FrameLayout contentBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //requestWindowFeature(Window.FEATURE_NO_TITLE);//Turn off the title bar!
        contentBase = new FrameLayout(FullscreenActivity.this);
        setContentView(contentBase);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));

        View main = LayoutInflater.from(this).inflate(R.layout.activity_fullscreen, null);
        contentBase.addView(main);

        // Configure Cast device discovery
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(
                        CastMediaControlIntent.categoryForCast(getResources()
                                .getString(R.string.app_id))).build();
        mMediaRouterCallback = new MyMediaRouterCallback();
        setup();

        startupWheel.show();

        rs = new RemoteSettings(FullscreenActivity.this);
        rs.addSettingsCompleteEventListener(this);
        rs.checkSettings();


        analyticsManager.getGaTracker().set(Fields.SCREEN_NAME, "Main Screen");
    }

    @Override
    public void updateComplete(UpdateSettingsComplete ene) {//Gets called when remote settings are finished loading
        //addAdMobAds();

        loadPref();

        startupWheel.dismiss();

        for(int i = 0; i < rs.alertMessageList.size(); i++){//display all new alerts
            rs.alertMessageList.get(i).displayMessage(FullscreenActivity.this);
        }

        if(settings.tumblrUrls.size() > 0){
            startNewImageGrab();
        }
    }

    public void displayStartupDialog(){

        final View startup = LayoutInflater.from(this).inflate(R.layout.startup, null);
        contentBase.addView(startup);

        startup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FullscreenActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("startedbefore_preference", true);
                editor.commit();
                contentBase.removeView(startup);
            }
        });
        /*
        new AlertDialog.Builder(FullscreenActivity.this)
                .setTitle("ImageFlip")
                .setMessage("Hello and welcome to ImageFlip!\nThe controls are as follows:\nSwiping left or right will supply you with a random image based on your query set in settings.\n\nSwiping up will do the same as left or right, however will 'discard' the image and you will never see it again.\n\nSwiping down will download the current image to your SD card and supply you with a new image.\n\nLong pressing anywhere on the screen will bring up the menu.\n\nThat's about it, have fun!")
                .setPositiveButton("Lets go!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FullscreenActivity.this);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("startedbefore_preference", true);
                        editor.commit();
                    }
                })
                .show();*/
    }

    public void setImagesViewed(int imagesViewed){
        settings.setImagesViewed(imagesViewed);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FullscreenActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("imagesViewed_preference", imagesViewed);
        editor.commit();
    }

    public void setupImageViewTranslation(TranslationDirection direction){
        TranslateAnimation animation;
        if(direction == TranslationDirection.Up){
            animation = new TranslateAnimation(0,0,0,-10000);
        }else if(direction == TranslationDirection.Down){
            animation = new TranslateAnimation(0,0,0,10000);
        }else if(direction == TranslationDirection.Left){
            animation = new TranslateAnimation(0,-10000,0,0);
        }else{
            animation = new TranslateAnimation(0,10000,0,0);
        }

        animation.setDuration(5000);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageContent.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        imageContent.startAnimation(animation);
    }

    public void saveLastImage(String url) {
        File directory = new File(Environment.getExternalStorageDirectory() + "/ImageFlip");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        DownloadManager mgr =
                (DownloadManager) FullscreenActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);

        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(
                downloadUri);

        request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI
                        | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(true).setTitle("Image Flip")
                .setDescription("Requested image")
                .setDestinationInExternalPublicDir("/ImageFlip", "downloaded.jpg");

        mgr.enqueue(request);

    }

    public void startNewImageGrab() {
        delayWheel.show();
        ionDownloader.beginImageDownload();
    }

    public void startCacheRebuild(){
        ionDownloader.beginCacheLoad();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
                .getActionProvider(mediaRouteMenuItem);
        // Set the MediaRouteActionProvider selector for device discovery.
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent();
            intent.setClass(FullscreenActivity.this, SetPreferenceActivity.class);
            startActivityForResult(intent, 0);

            return true;
        }else if(id == R.id.action_help) {
            displayStartupDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        loadPref();
    }

    private void loadPref(){
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean startedbefore_preference = mySharedPreferences.getBoolean("startedbefore_preference", false);
        String query_preference = mySharedPreferences.getString("query_preference", "cute cats");
        boolean vibrateOnComplete_preference = mySharedPreferences.getBoolean("vibrateOnComplete_preference", false);
        boolean filterNSFW_preference = mySharedPreferences.getBoolean("filterNSFW_preference", false);
        boolean deleteAll_preference = mySharedPreferences.getBoolean("deleteAll_preference", false);
        boolean exactQueries_preference = mySharedPreferences.getBoolean("exactQueries_preference", true);
        int imagesViewed_preference = mySharedPreferences.getInt("imagesViewed_preference", 0);


        String cacheSize_preference = mySharedPreferences.getString("cacheSize_preference", "10");
        int intCacheSize_preference = 20;
        try{ intCacheSize_preference = Integer.parseInt(cacheSize_preference); } catch (Exception e){e.printStackTrace();}
        intCacheSize_preference = (intCacheSize_preference > -1) ? intCacheSize_preference : 0;

        boolean queryEquals = exactQueries_preference
                ? settings.getSearch_query().equals("\""+query_preference+"\"")
                : settings.getSearch_query().equals(query_preference);

        boolean nsfwEquals = (settings.filterNSFW() == filterNSFW_preference);

        if(!queryEquals || !nsfwEquals){
            String newQuery = "";
            if(exactQueries_preference){
                newQuery = "\"" + query_preference + "\"";
            }else{
                newQuery = query_preference;
            }
            Toast.makeText(FullscreenActivity.this, newQuery + " set as the query", Toast.LENGTH_LONG).show();
            imageStore.clearImages();
            settings.setSearch_query(newQuery);

            tumblrClient.setOnInitUrlFoundListener(new TumblrClient.OnInitUrlFoundListener() {
                @Override
                public void onURLFound() {
                    startNewImageGrab();
                }
            });
            tumblrClient.refreshTumblrURLs(newQuery);
        }

        settings.setVibrateOnCompletion(vibrateOnComplete_preference);
        settings.setFilterNSFW(filterNSFW_preference);
        settings.setDeleteAll(deleteAll_preference);
        settings.setCacheSize(intCacheSize_preference);
        settings.setExactQueries(exactQueries_preference);
        settings.setImagesViewed(imagesViewed_preference);

        if(!startedbefore_preference){
            displayStartupDialog();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Send a screen view when the Activity is displayed to the user.
        analyticsManager.getGaTracker().send(MapBuilder.createAppView().build());
    }

    @Override
    public void onStop() {
        super.onStop();
        //EasyTracker.getInstance(this).activityStop(this);  // Add this method.//TODO is this ok to remove?
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onPause() {
        //adView.pause();
        //AdColony.pause();
        settings.saveSettings();
        imageStore.saveSettings(FullscreenActivity.this);
        if (isFinishing()) {
            // End media router discovery
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status!= ConnectionResult.SUCCESS){
            GooglePlayServicesUtil.getErrorDialog(status,this,0);
        }
        //AdColony.resume( this );
        //adView.resume();
        // Start media router discovery
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onDestroy() {
        //adView.destroy();
        teardown();
        super.onDestroy();
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            //mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
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

    private class RefreshTumblrCache extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            tumblrClient.refreshTumblrURLs(params[0]);
            while(settings.tumblrUrls.size() < 2) {//Delay until we have some urls
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return "";
        }
    }

    class IONDownloader{
        Pattern imageURLPattern = Pattern.compile("https?:[^(\"|')]*_(500\\.jpe?g|400\\.gif)");
        Random rand = new Random();
        ImageView imageView;
        Settings settings;
        Context context;
        int imageURLAttempts = 0;

        class WebpageStatusHelper{
            String selectedUrl;
            public WebpageStatusHelper(String selectedURL){
                this.selectedUrl = selectedURL;
            }
            public void setBlogAsNSFW(){
                settings.tumblrUrls.remove(this.selectedUrl);
                //settings.setQueryLimitExceeded(true);//TODO not for blocking the update >.< can use the start index instead
            }
            public void setBlogAs404NotFound(){
                settings.tumblrUrls.remove(this.selectedUrl);
                //settings.setQueryLimitExceeded(true);//TODO not for blocking the update >.< can use the start index instead
            }
        }



        public IONDownloader(Context context, ImageView imageView, Settings settings){
            this.context = context;
            this.imageView = imageView;
            this.settings = settings;
        }

        public void beginImageDownload(){
            if(imageStore.size() > 0){
                ImageStore.ImageUnit newImage = imageStore.get();
                settings.setLastImageURL(newImage.imageURL);
                beginMainImageLoad("file://"+newImage.imageFilepath);
            }else{
                tumblrClient.refreshTumblrURLs(this.settings.getSearch_query());

                if(settings.tumblrUrls.size() > 0) {
                    fetchImageURL(true);//Fetch a new image url and set it to the main image view
                }else{
                    Toast.makeText(context, "No pictures found right now, please try again later or a different query", Toast.LENGTH_LONG).show();
                    finishUIDelay();
                }
            }
        }

        public void finishUIDelay(){
            delayWheel.dismiss();

            imageContent.setAnimation(null);
            imageContent.setVisibility(View.VISIBLE);

            if (settings.vibrateOnCompletion()) {
                long[] vibratePattern = {0L, 100L, 100L, 100L, 100L, 100L};
                Vibrator systemVibrator = (Vibrator) FullscreenActivity.this.getSystemService(FullscreenActivity.this.VIBRATOR_SERVICE);
                systemVibrator.vibrate(vibratePattern, -1);
            }
        }

        public void beginCacheLoad(){
            if(settings.tumblrUrls.size() > 0){
                if(imageStore.size() < settings.getCacheSize()){//Are we finished loading yet?
                    fetchImageURL(false);
                }
            }
        }

        private void fetchImageURL(final boolean mainDownload){
            //Randomly pick a url from our list
            String selectedURL = settings.tumblrUrls.get(rand.nextInt(settings.tumblrUrls.size()));
            final WebpageStatusHelper wStatusHelper = new WebpageStatusHelper(selectedURL);
            selectedURL = selectedURL + "/random";

            Ion .with(this.context, selectedURL)
                    .asString()
                    .setCallback(new FutureCallback<String>() {
                        @Override
                        public void onCompleted(Exception e, String result) {
                            if(e == null) {
                                ArrayList<String> acceptableImages = new ArrayList<String>();
                                Matcher matcher = imageURLPattern.matcher(result);

                                while (matcher.find()) {
                                    if (!acceptableImages.contains(matcher.group())) {
                                        acceptableImages.add(matcher.group());
                                    }
                                }

                                if(acceptableImages.size() == 0){
                                    if(result.contains("status_code = '404'")){
                                        wStatusHelper.setBlogAs404NotFound();
                                    }
                                }

                                String returnedImageURL = "";
                                while(acceptableImages.size() > 0 && returnedImageURL.isEmpty()){//do we have any acceptable images?
                                    int randIndex = rand.nextInt(acceptableImages.size());
                                    returnedImageURL = acceptableImages.get(randIndex);

                                    if(settings.imageURLDisliked(returnedImageURL)){
                                        acceptableImages.remove(randIndex);//Remove the disliked image
                                        returnedImageURL = "";
                                    }else{
                                        if(settings.filterNSFW() && (result != null && result.contains("blog_is_nsfw = 'Yes'"))){
                                            acceptableImages.remove(randIndex);
                                            returnedImageURL = "";
                                            wStatusHelper.setBlogAsNSFW();
                                        }
                                    }
                                }

                                if(mainDownload) {
                                    settings.setLastImageURL(returnedImageURL);
                                    beginMainImageLoad(returnedImageURL);
                                }else{//Cache time
                                    beginCacheImageLoad(returnedImageURL);
                                }
                            }else{
                                if(e instanceof HttpStatusException) {
                                    HttpStatusException httpError = (HttpStatusException) e;
                                    if (httpError.getStatusCode() == 404) {//Webpage not found?
                                        wStatusHelper.setBlogAs404NotFound();
                                    }else{
                                        Toast.makeText(context, "A network error has occurred(1)", Toast.LENGTH_LONG).show();
                                        e.printStackTrace();
                                        finishUIDelay();
                                    }
                                }else{
                                    Toast.makeText(context, "A network error has occurred(2)", Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                    finishUIDelay();
                                }
                            }
                        }
                    });
        }

        private void beginMainImageLoad(final String imagePath){
            if(!imagePath.isEmpty()){
                imageURLAttempts = 0;
                Ion.with(this.imageView)
                        .animateGif(true)
                        .load(imagePath)
                        .setCallback(new FutureCallback<ImageView>() {
                            @Override
                            public void onCompleted(Exception e, ImageView result) {
                                if(e == null){
                                    if(imagePath.startsWith("file:")){
                                        String deviceImagePath = imagePath.substring(7);
                                        File loadedFile = new File(deviceImagePath);
                                        loadedFile.delete();
                                    }

                                    startCacheRebuild();

                                    sendMessage(settings.getLastImageURL());

                                    delayWheel.dismiss();

                                    imageContent.setAnimation(null);
                                    imageContent.setVisibility(View.VISIBLE);

                                    if(settings.vibrateOnCompletion()){
                                        long[] vibratePattern = {0L,100L,100L,100L,100L,100L};
                                        Vibrator systemVibrator = (Vibrator) FullscreenActivity.this.getSystemService(FullscreenActivity.this.VIBRATOR_SERVICE);
                                        systemVibrator.vibrate(vibratePattern, -1);
                                    }

                                    if(settings.getImagesViewed() >= settings.imagesViewedBetweenAds){
                                        //displayInterstitial();//TODO disabled like this for now
                                        setImagesViewed(1);
                                    }else{
                                        setImagesViewed(settings.getImagesViewed()+1);
                                    }
                                }else{
                                    Toast.makeText(context, "A network error has occurred(3)", Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                    finishUIDelay();
                                }
                            }
                        });
            }else{//URL we received was empty
                imageURLAttempts++;
                //if(imageURLAttempts > 5){//stop it}//TODO Do we want to do this????
                beginImageDownload();//Restart process of finding an image
            }
        }

        private void beginCacheImageLoad(final String imagePath){
            if(!imagePath.isEmpty() && !imagePath.contains("%")) {//Uhh lets not even deal with percent signs...
                String safeName = imagePath.substring(imagePath.lastIndexOf('/') + 1);
                Ion .with(this.context, imagePath)
                        .write(new File(context.getFilesDir(), safeName))
                        .setCallback(new FutureCallback<File>() {
                            @Override
                            public void onCompleted(Exception e, File result) {
                                if (e == null) {
                                    imageStore.addImage(null, imagePath, result.getPath());
                                    beginCacheLoad(); //Restart function
                                } else {
                                    e.printStackTrace();
                                    Log.e("beginCacheImageLoad", "ERROR OCCURRED WITH IMAGE PATH:" + imagePath);
                                }
                            }
                        });
            }else{
                beginCacheLoad();//We don't like empty!!
            }
        }
    }

    /*************************************CHROME CAST METHODS******************/
    /**
     * Callback for MediaRouter events
     */
    private class MyMediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(TAG, "onRouteSelected");
            // Handle the user route selection.
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());

            launchReceiver();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(TAG, "onRouteUnselected: info=" + info);
            teardown();
            mSelectedDevice = null;
        }
    }

    /**
     * Start the receiver app
     */
    private void launchReceiver() {
        try {
            mCastListener = new Cast.Listener() {

                @Override
                public void onApplicationDisconnected(int errorCode) {
                    Log.d(TAG, "application has stopped");
                    teardown();
                }

            };
            // Connect to Google Play services
            mConnectionCallbacks = new ConnectionCallbacks();
            mConnectionFailedListener = new ConnectionFailedListener();
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(mSelectedDevice, mCastListener);
            mApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();

            mApiClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "Failed launchReceiver", e);
        }
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected");

            if (mApiClient == null) {
                // We got disconnected while this runnable was pending
                // execution.
                return;
            }

            try {
                if (mWaitingForReconnect) {
                    mWaitingForReconnect = false;

                    // Check if the receiver app is still running
                    if ((connectionHint != null)
                            && connectionHint
                            .getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                        Log.d(TAG, "App  is no longer running");
                        teardown();
                    } else {
                        // Re-create the custom message channel
                        try {
                            Cast.CastApi.setMessageReceivedCallbacks(
                                    mApiClient,
                                    mHelloWorldChannel.getNamespace(),
                                    mHelloWorldChannel);
                        } catch (IOException e) {
                            Log.e(TAG, "Exception while creating channel", e);
                        }
                    }
                } else {
                    // Launch the receiver app
                    Cast.CastApi
                            .launchApplication(mApiClient,
                                    getString(R.string.app_id), false)
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(
                                                ApplicationConnectionResult result) {
                                            Status status = result.getStatus();
                                            Log.d(TAG,
                                                    "ApplicationConnectionResultCallback.onResult: statusCode"
                                                            + status.getStatusCode());
                                            if (status.isSuccess()) {
                                                ApplicationMetadata applicationMetadata = result
                                                        .getApplicationMetadata();
                                                mSessionId = result
                                                        .getSessionId();
                                                String applicationStatus = result
                                                        .getApplicationStatus();
                                                boolean wasLaunched = result
                                                        .getWasLaunched();
                                                Log.d(TAG,
                                                        "application name: "
                                                                + applicationMetadata
                                                                .getName()
                                                                + ", status: "
                                                                + applicationStatus
                                                                + ", sessionId: "
                                                                + mSessionId
                                                                + ", wasLaunched: "
                                                                + wasLaunched);
                                                mApplicationStarted = true;

                                                // Create the custom message
                                                // channel
                                                mHelloWorldChannel = new HelloWorldChannel();
                                                try {
                                                    Cast.CastApi
                                                            .setMessageReceivedCallbacks(
                                                                    mApiClient,
                                                                    mHelloWorldChannel
                                                                            .getNamespace(),
                                                                    mHelloWorldChannel);
                                                } catch (IOException e) {
                                                    Log.e(TAG,
                                                            "Exception while creating channel",
                                                            e);
                                                }

                                                // set the initial instructions
                                                // on the receiver
                                                sendMessage(settings.getLastImageURL());//Send the url we are looking at
                                            } else {
                                                Log.e(TAG,
                                                        "application could not launch");
                                                teardown();
                                            }
                                        }
                                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch application", e);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended");
            mWaitingForReconnect = true;
        }
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.e(TAG, "onConnectionFailed ");

            teardown();
        }
    }

    /**
     * Tear down the connection to the receiver
     */
    private void teardown() {
        Log.d(TAG, "teardown");
        if (mApiClient != null) {
            if (mApplicationStarted) {
                if (mApiClient.isConnected()) {
                    try {
                        Cast.CastApi.stopApplication(mApiClient, mSessionId);
                        if (mHelloWorldChannel != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(
                                    mApiClient,
                                    mHelloWorldChannel.getNamespace());
                            mHelloWorldChannel = null;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception while removing channel", e);
                    }
                    mApiClient.disconnect();
                }
                mApplicationStarted = false;
            }
            mApiClient = null;
        }
        mSelectedDevice = null;
        mWaitingForReconnect = false;
        mSessionId = null;
    }

    /**
     * Send a text message to the receiver
     *
     * @param message
     */
    private void sendMessage(String message) {
        if (mApiClient != null && mHelloWorldChannel != null) {
            try {
                Cast.CastApi.sendMessage(mApiClient,
                        mHelloWorldChannel.getNamespace(), message)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status result) {
                                if (!result.isSuccess()) {
                                    Log.e(TAG, "Sending message failed");
                                }
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "Exception while sending message", e);
            }
        } else {
            //Toast.makeText(FullscreenActivity.this, message, Toast.LENGTH_SHORT).show();//Displays the current image url being cast
        }
    }

    /**
     * Custom message channel
     */
    class HelloWorldChannel implements MessageReceivedCallback {

        /**
         * @return custom namespace
         */
        public String getNamespace() {
            return getString(R.string.namespace);
        }

        /*
         * Receive message from the receiver app
         */
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace,
                                      String message) {
            Log.d(TAG, "onMessageReceived: " + message);
        }

    }
    /**********************************************CHROME CAST METHODS*************/
}


