package rom.weldware.ImageFlip;

import android.app.Activity;
import android.os.Bundle;

import com.google.analytics.tracking.android.EasyTracker;

import rom.weldware.ImageFlip.R;

public class SetPreferenceActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.xml.settings);

        getFragmentManager().beginTransaction()
                .replace(R.id.preferences_fragment, new PrefsFragment()).commit();

    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }
}
