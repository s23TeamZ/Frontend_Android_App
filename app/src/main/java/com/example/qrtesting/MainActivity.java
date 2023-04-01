package com.example.qrtesting;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
                                JSONObject jsonResponse = new JSONObject(response);
                                // format the JSON response as a string
                                String formattedResponse = jsonResponse.toString(4);
                                resp.setText(formattedResponse);
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
}