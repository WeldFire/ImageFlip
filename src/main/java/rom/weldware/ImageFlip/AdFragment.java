package rom.weldware.ImageFlip;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class AdFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        LinearLayout view = new LinearLayout(context);

        setupAdMobAds(context);
        showAdMobAds(view);

        return view;
    }

    AdView adView;

    private void setupAdMobAds(Context context){
        // Create the adView.
        adView = new AdView(context);
        adView.setAdUnitId("ca-app-pub-2575928438432441/8930461417");

        Configuration config = context.getResources().getConfiguration();
        int screenlayout = config.screenLayout;
        if(screenlayout == Configuration.SCREENLAYOUT_SIZE_XLARGE){
            adView.setAdSize(AdSize.LEADERBOARD);
        }else{
            adView.setAdSize(AdSize.BANNER);
        }
    }

    private void showAdMobAds(LinearLayout layout){

        // Add the adView to it.
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.weight = 1;
        layout.addView(adView, params);

        // Initiate a generic request.
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("CFC5092E57C674B6AFF09CC757205111")
                .build();

        // Load the adView with the ad request.
        adView.loadAd(adRequest);
    }
}
