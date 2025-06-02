package com.ncusoft.myapplication7;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AboutInfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_info);

        TextView tvAppName = findViewById(R.id.tv_app_name);
        TextView tvAppVersion = findViewById(R.id.tv_app_version);

        String appName = getString(R.string.app_name);
        String version = "v1.0";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = "v" + pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        tvAppName.setText(appName);
        tvAppVersion.setText("版本：" + version);
    }
}
