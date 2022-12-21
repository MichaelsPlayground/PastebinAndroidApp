package de.androidcrypto.pastebinandroidapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class AddPaste extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 1007;
    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 260;
    public int POST_CHAR_LIMIT = 1600 * 1000;
    LinearLayout llFirstStep;
    RelativeLayout ll3rdStep;
    Button btnProceed2;
    ImageButton btnClose, ibCopyUrl, ibViewPaste, ibDelete, ibShare;
    ImageButton btnProceed;
    EditText etPasteName, etPasteText, etFinalRes;
    Spinner spPastePrivacy, spPasteFormat, spPasteExpiry;
    String name, privacy, pasteText, result, pasteid;
    String[] pasteformat, pasteExpiry;
    int step = 0;
    SharedPreferences sp;
    //AdView adView;
    //AdRequest adRequest;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_paste);

        /*
        adView = (AdView) findViewById(R.id.banner_AdView1);
        adRequest = new AdRequest.Builder()
                //.addTestDevice("4EC7E2B2060506BA2CFD947556E4CBF1")
                .build();
*/
        sp = getSharedPreferences("user", MODE_PRIVATE);


        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(AddPaste.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(AddPaste.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    } else {
                        ActivityCompat.requestPermissions(AddPaste.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                } else {
                    showFileChooser();
                }
            }
        });

        llFirstStep = (LinearLayout) findViewById(R.id.llFirstStep);
        ll3rdStep = (RelativeLayout) findViewById(R.id.ll3rdStep);

        etPasteName = (EditText) findViewById(R.id.etPasteName);
        etPasteText = (EditText) findViewById(R.id.etPastetext);
        etFinalRes = (EditText) findViewById(R.id.etFinalRes);
        spPastePrivacy = (Spinner) findViewById(R.id.spPastePrivacy);
        spPasteFormat = (Spinner) findViewById(R.id.spPasteFormat);
        spPasteExpiry = (Spinner) findViewById(R.id.spPasteFormat);

        btnProceed = findViewById(R.id.btnProceed);
        btnClose = (ImageButton) findViewById(R.id.close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        pasteformat = getResources().getStringArray(R.array.paste_format_value);
        pasteExpiry = getResources().getStringArray(R.array.paste_expire_date_value);
        btnProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSomePost();
            }
        });

        ibCopyUrl = (ImageButton) findViewById(R.id.ibCopyUrl);
        ibViewPaste = (ImageButton) findViewById(R.id.ibViewPaste);
        ibDelete = (ImageButton) findViewById(R.id.ibDelete);
        ibShare = (ImageButton) findViewById(R.id.ibShare);

        if (sp.contains("user_key")) {
            {
                ibDelete.setVisibility(View.VISIBLE);
            }
        }

        ibDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(AddPaste.this)
                        .setTitle("Confirm")
                        .setMessage("Are you sure to delete this paste")
                        .setPositiveButton("Cancel", null)
                        .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String url = getResources().getString(R.string.api_url) + "api_post.php";
                                new ServerPaste(1).execute(url);
                            }
                        })
                        .show();
            }
        });

        ibCopyUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("paste", result);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(AddPaste.this, "Link copied to clipboard", Toast.LENGTH_LONG).show();
            }
        });

        ibShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = result;
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Pastebin url");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            }
        });

        ibViewPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(AddPaste.this, ViewPaste.class);
                i.putExtra("paste_id", pasteid);
                i.putExtra("paste_name", name);
                if (sp.contains("user_key")) {
                    i.putExtra("mine", true);
                }
                startActivity(i);
                finish();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showFileChooser();
                } else {
                    Toast.makeText(AddPaste.this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to paste"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri content_describer = data.getData();
                    //Log.d("Path???", content_describer.getPath());

                    BufferedReader reader = null;
                    try {
                        InputStream in = getContentResolver().openInputStream(content_describer);
                        reader = new BufferedReader(new InputStreamReader(in));
                        String line;
                        StringBuilder builder = new StringBuilder();
                        while ((line = reader.readLine()) != null) {

                            if (builder.length() > POST_CHAR_LIMIT) {

                                new AlertDialog.Builder(AddPaste.this)
                                        .setTitle("Some text removed.")
                                        .setMessage("The file size exceeded the maximum character limit.")
                                        .setPositiveButton("OK", null)
                                        .show();

                                break;
                            }

                            builder.append(line + System.getProperty("line.separator"));
                        }
                        etPasteText.setText(builder.toString());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(AddPaste.this, "Unable to read this file", Toast.LENGTH_SHORT).show();
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void doSomePost() {
        String url = getResources().getString(R.string.api_url) + "api_post.php";
        name = etPasteName.getText().toString();
        pasteText = etPasteText.getText().toString();
        privacy = spPastePrivacy.getSelectedItemPosition() + "";
        //Toast.makeText(AddPaste.this,pasteformat[spPasteFormat.getSelectedItemPosition()]+","+pasteformat.length,Toast.LENGTH_LONG).show();
        if (pasteText.length() < 1) {
            Toast.makeText(AddPaste.this, "Add some paste text", Toast.LENGTH_LONG).show();
            return;
        }
        new ServerPaste().execute(url);
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


    private class ServerPaste extends AsyncTask<String, Void, String> {

        HashMap<String, String> postData;
        String dataReturned;
        boolean status = false, apiStatus = false;
        int type;

        ProgressDialog progressDialog;

        ServerPaste(int type) {
            this.type = type;
        }

        ServerPaste() {
            type = 0;
        }

        public HashMap<String, String> getPasteData() {
            HashMap<String, String> data = new HashMap<>();
            data.put("api_option", "paste");
            data.put("api_dev_key", getResources().getString(R.string.api_key));
            data.put("api_paste_name", name);
            data.put("api_paste_private", privacy);
            try {
                // todo getSelectedItemPosition does not work !!!
                System.out.println("*** itemPos: " + spPasteExpiry.getSelectedItemPosition());
                System.out.println("*** value: " + spPasteExpiry.getSelectedItemPosition());
                data.put("api_paste_expire", pasteExpiry[spPasteExpiry.getSelectedItemPosition()]);
                data.put("api_paste_format", pasteformat[spPasteFormat.getSelectedItemPosition()]);
            } catch (Exception e) {

            }

            if (sp.contains("user_key")) {
                data.put("api_user_key", sp.getString("user_key", ""));
            }
            data.put("api_paste_code", pasteText);

            return data;
        }

        public HashMap<String, String> getDeleteData() {
            HashMap<String, String> data = new HashMap<>();
            data.put("api_option", "delete");
            data.put("api_dev_key", getResources().getString(R.string.api_key));
            data.put("api_user_key", sp.getString("user_key", ""));
            data.put("api_paste_key", pasteid);
            return data;
        }

//        private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
//            StringBuilder result = new StringBuilder();
//            boolean first = true;
//            for (Map.Entry<String, String> entry : params.entrySet()) {
//                if (first)
//                    first = false;
//                else
//                    result.append("&");
//                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
//                result.append("=");
//                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
//            }
//            return result.toString();
//        }

        @Override
        protected String doInBackground(String... params) {

            PastebinRequest request = null;
            try {
                request = new PastebinRequest(params[0], AddPaste.this);
                request.postData(postData);
                if (request.resultOk()) {
                    status = true;

                    if (request.isApiError()) {
                        dataReturned = request.getApiErrors();
                    } else {
                        dataReturned = request.getResponse();
                        apiStatus = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(AddPaste.this);
            progressDialog.setIndeterminate(true);
            progressDialog.setTitle("Please wait.");
            switch (type) {
                case 0:
                    postData = getPasteData();
                    progressDialog.setMessage("Posting your paste.");
                    break;
                case 1:
                    postData = getDeleteData();
                    progressDialog.setMessage("Deleting your paste.");
                    break;
                default:
                    postData = new HashMap<>();
            }
            progressDialog.show();
//            try {
//                //Toast.makeText(AddPaste.this, getPostDataString(postData), Toast.LENGTH_LONG).show();
//                //etPasteText.setText(getPostDataString(postData));
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressDialog.dismiss();
            if (status) {
                if (apiStatus) {
                    if (type == 1) {
                        Toast.makeText(AddPaste.this, "Paste Deleted", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    //adView.loadAd(adRequest);

                    result = dataReturned;
                    pasteid = dataReturned.substring(dataReturned.lastIndexOf("/") + 1, dataReturned.length());

                    fab.setVisibility(View.GONE);
                    llFirstStep.setVisibility(View.GONE);
                    ll3rdStep.setVisibility(View.VISIBLE);

                    etFinalRes.setText(result);
                    etPasteName.setEnabled(false);
                    btnProceed.setVisibility(View.GONE);


                } else {
                    new AlertDialog.Builder(AddPaste.this)
                            .setTitle("Some error occured")
                            .setMessage(dataReturned)
                            .setPositiveButton("Try Again", null)
                            .setIcon(R.drawable.error)
                            .show();
                }

            } else {
                new AlertDialog.Builder(AddPaste.this)
                        .setTitle("Unable to get date")
                        .setMessage("Request had timed out")
                        .setPositiveButton("Try Again", null)
                        .setIcon(R.drawable.error)
                        .show();
            }
        }
    }

}
