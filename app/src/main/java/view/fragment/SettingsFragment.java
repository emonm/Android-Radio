
package view.fragment;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import org.oucho.radio.R;

public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }


    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // This constructor is intentionally empty, pourquoi ? parce que !
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        // This constructor is intentionally empty, pourquoi ? parce que !
    }

}
