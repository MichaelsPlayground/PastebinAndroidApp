package de.androidcrypto.pastebinandroidapp;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicy extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        TextView tvPolicy = (TextView) findViewById(R.id.tvPolicy);
        tvPolicy.setText(Html.fromHtml(getResources().getString(R.string.privacy_policy)));

    }
}
