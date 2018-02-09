package com.example.eran.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity {

    private final static String PROCESS_LIST = "process_list";
    private final static String SOURCE_LIST = "src_list";
    private final static String DESTINATION_LIST = "dest_list";
    private final static String FONT_LIST = "font_list";

    public SettingsActivity() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new LocationFragment()).commit();

    }

    //inner class
    @SuppressLint("ValidFragment")
    public class LocationFragment extends PreferenceFragment {

        private final static String TAG = "LocationFragment";
        private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();

                if (preference instanceof ListPreference)
                {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                            index >= 0
                                    ? listPreference.getEntries()[index]
                                    : null);

                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }
                return true;
            }
        };

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if(id==R.id.home){
                startActivity(new Intent(this.getActivity(), MainActivity.class));
            }
            return true;
        }

        /**
         * bind the preferences to the menu
         * @param preference - the preferences.
         */
        private void bindPreferenceSummaryToValue(Preference preference)
        {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_set_process_lang);
            bindPreferenceSummaryToValue(findPreference(PROCESS_LIST));
            bindPreferenceSummaryToValue(findPreference(FONT_LIST));
            bindPreferenceSummaryToValue(findPreference(SOURCE_LIST));
            bindPreferenceSummaryToValue(findPreference(DESTINATION_LIST));
        }


    }
}