package rom.weldware.ImageFlip;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import rom.weldware.ImageFlip.R;

/**
 * Created by WeldFire on 1/28/14.
 */
public class PrefsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}

