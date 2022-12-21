package de.androidcrypto.pastebinandroidapp;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.HashMap;

public class ViewPaste extends AppCompatActivity {

    FloatingActionButton fab, fab1Share, fab2copy, fab3delete;
    Animation fab_open, fab_close, rotate_forward, rotate_backward;
    Boolean isFabOpen = false;
    String result = "", paste_id;
    SharedPreferences sp;
    boolean mine = false, isInFront = true, isrunning = true;
    //InterstitialAd mInterstitialAd;
    ServerPaste spaste;

    WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_paste);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        sp = getSharedPreferences("user", MODE_PRIVATE);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab1Share = (FloatingActionButton) findViewById(R.id.fab1Share);
        fab2copy = (FloatingActionButton) findViewById(R.id.fab2copy);
        fab3delete = (FloatingActionButton) findViewById(R.id.fab3delete);

        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_backward);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFAB();
            }
        });
        fab2copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("paste", result);
                clipboard.setPrimaryClip(clip);
                Snackbar.make(view, "Text copied to clipboard", Snackbar.LENGTH_LONG)
                        .setAction("Done", null).show();
                animateFAB();
            }
        });

        if (getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            paste_id = extras.getString("paste_id");
            if (extras.containsKey("paste_name")) {
                getSupportActionBar().setTitle(extras.getString("paste_name"));
            }
            if (!extras.containsKey("mine")) {
                mine = false;
                spaste = new ServerPaste(0);
                spaste.execute("https://pastebin.com/raw/" + paste_id);
            } else {
                mine = true;
                spaste = new ServerPaste(1);
                spaste.execute(getResources().getString(R.string.api_url) + "api_raw.php");
            }
        } else if (getIntent().getData() != null) {


            Uri data = getIntent().getData();
            String fullPath = data.getEncodedSchemeSpecificPart();

            paste_id = fullPath.substring(fullPath.toLowerCase().indexOf(".com/") + 5);
            spaste = new ServerPaste(0);
            spaste.execute("https://pastebin.com/raw/" + paste_id);

        }

        fab1Share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = "https://pastebin.com/" + paste_id;
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Pastebin url");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
                animateFAB();
            }
        });

        if (mine) {
            fab3delete.setVisibility(View.GONE);
        }

        myWebView = (WebView) findViewById(R.id.myWebView);

        fab3delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ViewPaste.this)
                        .setTitle("Confirm")
                        .setMessage("Are you sure to delete this paste")
                        .setPositiveButton("Cancel", null)
                        .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String url = getResources().getString(R.string.api_url) + "api_post.php";
                                new ServerPaste(2).execute(url);
                            }
                        })
                        .setIcon(R.drawable.ic_delete_black)
                        .show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isrunning) {
            spaste.cancel(true);
        }
        finish();
        super.onBackPressed();
    }

    public void animateFAB() {
        if (isFabOpen) {

            fab.startAnimation(rotate_backward);
            fab1Share.startAnimation(fab_close);
            fab2copy.startAnimation(fab_close);
            if (mine)
                fab3delete.startAnimation(fab_close);

            fab1Share.setClickable(false);
            fab2copy.setClickable(false);
            if (mine)
                fab3delete.setClickable(false);
            isFabOpen = false;

        } else {

            fab.startAnimation(rotate_forward);
            fab1Share.startAnimation(fab_open);
            fab2copy.startAnimation(fab_open);
            if (mine)
                fab3delete.startAnimation(fab_open);

            fab1Share.setClickable(true);
            fab2copy.setClickable(true);
            if (mine)
                fab3delete.setClickable(true);
            isFabOpen = true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        isInFront = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isInFront = false;
    }

    private class ServerPaste extends AsyncTask<String, Void, String> {

        HashMap<String, String> postData;
        Spanned dataReturned;
        boolean status = false;
        int type;
        ProgressDialog pd;
        PastebinRequest request;

        ServerPaste(int type) {
            this.type = type;
        }

        ServerPaste() {
            type = 0;
        }

        public HashMap<String, String> getRawPostData() {
            HashMap<String, String> data = new HashMap<>();
            return data;
        }

        public HashMap<String, String> getUserRawPostData() {
            HashMap<String, String> data = new HashMap<>();
            data.put("api_dev_key", getResources().getString(R.string.api_key));
            data.put("api_user_key", sp.getString("user_key", ""));
            data.put("api_paste_key", paste_id);
            data.put("api_option", "show_paste");
            return data;
        }

        @Override
        protected String doInBackground(String... params) {

            try {
                request = new PastebinRequest(params[0], ViewPaste.this);
                request.setToGet();
                request.postData(postData);
                if (request.resultOk()) {
                    result = request.getResponseAsIs();
                    status = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        public HashMap<String, String> getDeleteData() {
            HashMap<String, String> data = new HashMap<>();
            data.put("api_option", "delete");
            data.put("api_dev_key", getResources().getString(R.string.api_key));
            data.put("api_user_key", sp.getString("user_key", ""));
            data.put("api_paste_key", paste_id);
            return data;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            request.cancel();
            myWebView.destroy();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(ViewPaste.this);
            pd.setIndeterminate(true);
            pd.setTitle("Please wait..");
            pd.setCancelable(false);
            pd.show();
            switch (type) {
                case 0:
                    postData = getRawPostData();
                    pd.setMessage("Loading paste.");
                    break;
                case 1:
                    postData = getUserRawPostData();
                    pd.setMessage("Loading paste.");
                    break;
                case 2:
                    postData = getDeleteData();
                    pd.setMessage("Deleting paste.");
                    break;
                default:            //default hashmap
                    postData = new HashMap<>();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (status) {

                if (type == 2) {
                    Toast.makeText(ViewPaste.this, "Paste deleted", Toast.LENGTH_LONG);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(getResources().getString(R.string.html_top));
                sb.append(result);
                sb.append(getResources().getString(R.string.html_bottom));

                pd.setMessage("Rendering..");
                myWebView.loadDataWithBaseURL("", sb.toString(), "text/html", "UTF-8", "");
                myWebView.getSettings().setJavaScriptEnabled(true);
                myWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                myWebView.setWebViewClient(new WebViewClient() {

                    public void onPageFinished(WebView view, String url) {
                        if (pd.isShowing()) {
                            pd.dismiss();
                        }
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                try {
                                    sleep(30000);
                                    ViewPaste.this.runOnUiThread(new Runnable() {
                                        public void run() {
                                            /*
                                            if (mInterstitialAd.isLoaded() && isInFront) {
                                                mInterstitialAd.show();
                                            }

                                             */
                                        }
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            }
                        }.start();

                    }
                });

                isrunning = false;

            } else {
                Toast.makeText(ViewPaste.this, "Some error occured.", Toast.LENGTH_LONG).show();
                if (pd.isShowing()) {
                    pd.dismiss();
                }
                finish();
            }
        }
    }
}
