package com.example.qrtesting;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.dhaval2404.imagepicker.ImagePicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_CAM = 100;
    private static final int REQ_GAL = 101;
    Button camera;
    Button gallery;
    ImageView editPhoto;
    Button upload;
    EditText resp;
    TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        LinearLayout layout = findViewById(R.id.layout);

        camera = findViewById(R.id.camera);
        gallery = findViewById(R.id.gallery);
        editPhoto = findViewById(R.id.editPhoto);
        upload = findViewById(R.id.upload);
        resp = findViewById(R.id.response);
        title = findViewById(R.id.title);

        camera.setOnClickListener(view -> {
            ImagePicker.Companion.with(MainActivity.this).cameraOnly().crop().galleryMimeTypes(new String[]{"image/png", "image/jpg", "image/jpeg"}).start(REQ_CAM);
        });

        gallery.setOnClickListener(view -> {
            ImagePicker.Companion.with(MainActivity.this).galleryOnly().crop().galleryMimeTypes(new String[]{"image/png", "image/jpg", "image/jpeg"}).start(REQ_GAL);
        });

        upload.setOnClickListener(view -> {
            Drawable drawable = editPhoto.getDrawable();
            if (drawable == null || !(drawable instanceof BitmapDrawable)) {
                Toast.makeText(MainActivity.this, "Please select an image", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap image = ((BitmapDrawable) drawable).getBitmap();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Please wait, image is processing");
            progressDialog.setCancelable(false);
            progressDialog.show();

            String url = "http://10.0.0.93:8055/upload_text";

            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            progressDialog.dismiss();
                            title.setText("Output:");
                            try {
                                layout.removeAllViews();
                                JSONObject json = new JSONObject(response);
                                JSONArray jsonArray = json.getJSONArray("opt");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    String qrType = jsonObject.getString("QR Type");
                                    String data = jsonObject.getString("data");
                                    double score = jsonObject.getDouble("score");
                                    String url = jsonObject.getString("URL");
                                    View qrView = createQRView(qrType, data, score, url);
                                    layout.addView(qrView);
                                }
                            } catch (JSONException e) {
                                Toast.makeText(MainActivity.this, "Error parsing JSON response", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("text", Base64.encodeToString(byteArray, Base64.DEFAULT));
                    return params;
                }
            };
            postRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(postRequest);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQ_CAM) {
                editPhoto.setImageURI(data.getData());
            } else if (requestCode == REQ_GAL) {
                editPhoto.setImageURI(data.getData());
            }
        }
    }

    private View createQRView(String qrType, String data, double score, String url) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.response_row, null);

        TextView qrTypeTextView = view.findViewById(R.id.user_name);
        TextView dataTextView = view.findViewById(R.id.user_name1);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView scoreTextView = view.findViewById(R.id.tx);
        Button urlButton = view.findViewById(R.id.urlbutton);

        qrTypeTextView.setText(qrType);
        dataTextView.setText(data);
        scoreTextView.setText((int) score + "%");

        if (!qrType.equals("URL")) {
            urlButton.setVisibility(View.GONE);
        }
        if (score == 0) {
            scoreTextView.setTextColor(Color.RED);
            urlButton.setVisibility(View.GONE);
        }
        if (score > 0 && score <= 25) {
            progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
            urlButton.setVisibility(View.GONE);
        } else if (score > 25 && score <= 50) {
            progressBar.getProgressDrawable().setColorFilter(Color.parseColor("#FFA500"), PorterDuff.Mode.SRC_IN);
        } else if (score > 50 && score <= 75) {
            progressBar.getProgressDrawable().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_IN);
        } else {
            progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
        }

        progressBar.setProgress((int) score);
        urlButton.setOnClickListener(view1 -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });
        return view;
    }
}