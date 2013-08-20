package mei.tcd.smta;

import java.util.Locale;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    public static class SettingsFragment  extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        Preference smaTextPreference;
        Preference lpfTextPreference;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            // Carregar as preferencias do recurso XML
            addPreferencesFromResource(R.xml.preferences);
            smaTextPreference = findPreference("thresholdSMA");
            lpfTextPreference = findPreference("thresholdLPF");

            // Verifico qual o tipo de filtro de acelerometro para mostrar o dialogo dos coeficientes.
            final SharedPreferences preferences = getPreferenceManager().getSharedPreferences() ;
            SharedPreferences.Editor editor = preferences.edit();
            if(preferences.getString("filtroRuido", "lpf").contains("lpf"))
            {
                smaTextPreference.setEnabled(false);
                lpfTextPreference.setEnabled(true);
            }
            else
            {
                lpfTextPreference.setEnabled(false);
                smaTextPreference.setEnabled(true);
            }
            // Preferencias valores de escala e bias
            if(preferences.contains("k_X"))
            {
                editor.putString("k_Xeditor",String.valueOf(preferences.getFloat("k_X", 1.0f)));

            }
            if(preferences.contains("k_Y"))
            {
                editor.putString("k_Yeditor",String.valueOf(preferences.getFloat("k_Y", 1.0f)));

            }
            if(preferences.contains("k_Z"))
            {
                editor.putString("k_Zeditor",String.valueOf(preferences.getFloat("k_Z", 1.0f)));

            }
            if(preferences.contains("b_X"))
            {
                editor.putString("b_Xeditor",String.valueOf(preferences.getFloat("b_X", 1.0f)));

            }
            if(preferences.contains("b_Y"))
            {
                editor.putString("b_Yeditor",String.valueOf(preferences.getFloat("b_Y", 1.0f)));

            }
            if(preferences.contains("b_Z"))
            {
                editor.putString("b_Zeditor",String.valueOf(preferences.getFloat("b_Z", 1.0f)));

            }
            editor.commit();
            //Toast.makeText(getActivity(), "It Worked!!!"+preferences.getString("filtroRuido", "0"), 2000).show();
        }
        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }
        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (key.equals("filtroRuido")) {
                if(sharedPreferences.getString("filtroRuido", "lpf").contains("lpf"))
                {

                    smaTextPreference.setEnabled(false);
                    lpfTextPreference.setEnabled(true);
                }
                else
                {
                    lpfTextPreference.setEnabled(false);
                    smaTextPreference.setEnabled(true);
                }
                // Um preference fragment n?o tem objecto context. Logo chamamos o getActivity, mas tem de estar ligaad a uma activity

               // Toast.makeText(getActivity(), "It Worked!!!"+sharedPreferences.getString(key,"lpf"), 2000).show();

               /* if (*//* check if livewallpaper_testpattern equals to blue *//*) {
                    findPreference("ListPreferenceKey_A").setEnabled(true);
                    findPreference("ListPreferenceKey_B").setEnabled(false);
                    findPreference("ListPreferenceKey_C").setEnabled(false);
                } else if (*//* check if livewallpaper_testpattern equals to red *//*) {
                    // enable B, disable A & C
                } else if (*//* check id livewallpaper_testpattern equals to white *//*) {
                    // enable C, disable A & B
                }*/
            }
            if(key.equals("k_Xeditor"))
            {
                editor.putFloat("k_X", Float.parseFloat(sharedPreferences.getString("k_Xeditor", "1.0f")));
                editor.commit();
            }
            if(key.equals("k_Yeditor"))
            {
                editor.putFloat("k_Y", Float.parseFloat(sharedPreferences.getString("k_Yeditor", "1.0f")));
                editor.commit();
            }
            if(key.equals("k_Zeditor"))
            {
                editor.putFloat("k_Z", Float.parseFloat(sharedPreferences.getString("k_Zeditor", "1.0f")));
                editor.commit();
            }
            if(key.equals("b_Xeditor"))
            {
                editor.putFloat("b_X", Float.parseFloat(sharedPreferences.getString("b_Xeditor", "1.0f")));
                editor.commit();
            }
            if(key.equals("b_Yeditor"))
            {
                editor.putFloat("b_Y", Float.parseFloat(sharedPreferences.getString("b_Yeditor", "1.0f")));
                editor.commit();
            }
            if(key.equals("b_Zeditor"))
            {
                editor.putFloat("b_Z", Float.parseFloat(sharedPreferences.getString("b_Zeditor", "1.0f")));
                editor.commit();
            }
        }
    }

}
