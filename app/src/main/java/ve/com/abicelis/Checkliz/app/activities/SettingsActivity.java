package ve.com.abicelis.Checkliz.app.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import ve.com.abicelis.Checkliz.R;
import ve.com.abicelis.Checkliz.app.fragments.SettingsFragment;
import ve.com.abicelis.Checkliz.enums.DateFormat;
import ve.com.abicelis.Checkliz.enums.TimeFormat;
import ve.com.abicelis.Checkliz.util.SharedPreferenceUtil;

public class SettingsActivity extends AppCompatActivity {

    //CONSTS
    public static final int SETTINGS_ACTIVITY_REQUEST_CODE = 109;

    //DATA
    private TimeFormat mOldTimeFormat;
    private DateFormat mOldDateFormat;
    public boolean mForceHomeRefresh;

    //UI
    private Toolbar mToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mOldTimeFormat = SharedPreferenceUtil.getTimeFormat(this);
        mOldDateFormat = SharedPreferenceUtil.getDateFormat(this);

        setUpToolbar();

        if (savedInstanceState == null) {
            SettingsFragment fragment = new SettingsFragment();

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.activity_settings_fragment, fragment);
            ft.commit();
        }
    }

    private void setUpToolbar() {

        mToolbar = (Toolbar) findViewById(R.id.activity_settings_toolbar);
        mToolbar.setTitle(getResources().getString(R.string.activity_settings_toolbar_title));
        mToolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.icon_back_material));

        //ubah toolbar jadi actionbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            // jika tekan back/home berfungsi
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(!mOldTimeFormat.equals(SharedPreferenceUtil.getTimeFormat(this)) || !mOldDateFormat.equals(SharedPreferenceUtil.getDateFormat(this)) || mForceHomeRefresh)
            setResult(RESULT_OK);
        else
            setResult(RESULT_CANCELED);
        finish();
    }
}
