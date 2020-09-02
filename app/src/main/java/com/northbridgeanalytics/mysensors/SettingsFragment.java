package com.northbridgeanalytics.mysensors;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import android.view.View;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class SettingsFragment extends PreferenceFragmentCompat {

    boolean useEsriJASON;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        addPreferencesFromResource(R.xml.preferences);

        // TODO: Talvez isso pode ser usado para disparar a validação do serviço REST.
//        SwitchPreferenceCompat esriJASON = (SwitchPreferenceCompat) findPreference("preference_filename_json");
//        esriJASON.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object o) {
//
//                useEsriJASON = (boolean) o;
//
//                Log.i("Preference", "Esri is " + useEsriJASON);
//
//                return true;
//            }
//        });

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setBackgroundColor(Color.WHITE);


    }



}