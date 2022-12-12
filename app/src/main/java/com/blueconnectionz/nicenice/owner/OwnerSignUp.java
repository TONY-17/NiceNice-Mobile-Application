package com.blueconnectionz.nicenice.owner;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Patterns;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.blueconnectionz.nicenice.MainActivity;
import com.blueconnectionz.nicenice.R;
import com.blueconnectionz.nicenice.driver.entry.DocumentUpload;
import com.blueconnectionz.nicenice.driver.entry.LandingPage;
import com.blueconnectionz.nicenice.driver.entry.ProfileUpload;
import com.blueconnectionz.nicenice.network.RetrofitClient;
import com.blueconnectionz.nicenice.network.Role;
import com.blueconnectionz.nicenice.network.model.OwnerRegisterReq;
import com.blueconnectionz.nicenice.utils.Common;
import com.droidbyme.dialoglib.AnimUtils;
import com.droidbyme.dialoglib.DroidDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class OwnerSignUp extends AppCompatActivity implements PickiTCallbacks {

    //Permissions
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;

    //Declare PickiT
    PickiT pickiT;


    public static TextInputEditText emailField;
    public static TextInputEditText phoneNumberField;
    public static TextInputEditText passwordField;

    View loadingView;
    AVLoadingIndicatorView avLoadingIndicatorView;

    MaterialButton signUp;
    MaterialCardView uploadIDCopy;

    TextView selectedDocumentTXT;
    ImageView checkBox;
    MultipartBody.Part document;
    File file = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_page);

        //Initialize PickiT
        pickiT = new PickiT(this, this, this);

        loadingView = findViewById(R.id.loadingView);
        avLoadingIndicatorView = findViewById(R.id.avi);
        selectedDocumentTXT = findViewById(R.id.selectedDocument);
        checkBox = findViewById(R.id.cb1);
        Common.setStatusBarColor(getWindow(), this, Color.WHITE);

        emailField = findViewById(R.id.emailTxT);
        phoneNumberField = findViewById(R.id.phoneNumberTxT);
        passwordField = findViewById(R.id.passwordTxt);
        uploadIDCopy = findViewById(R.id.uploadIdCopy);

        uploadIDCopy.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(
                    OwnerSignUp.this,
                    Manifest.permission
                            .READ_EXTERNAL_STORAGE)
                    != PackageManager
                    .PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        OwnerSignUp.this,
                        new String[]{
                                Manifest.permission
                                        .READ_EXTERNAL_STORAGE},
                        1);
            } else {
                selectPDF();
            }
        });

        signUp = findViewById(R.id.signUpUser);
        signUp.setOnClickListener(view -> {
            try {
                createAccount();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        TextView terms = findViewById(R.id.signInTextView);
        terms.setOnClickListener(view -> runOnUiThread(() -> Common.termsAndConditions(OwnerSignUp.this)));


    }


    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();

                        ClipData clipData = Objects.requireNonNull(data).getClipData();
                        if (clipData != null) {
                            int numberOfFilesSelected = clipData.getItemCount();
                            if (numberOfFilesSelected > 1) {
                                pickiT.getMultiplePaths(clipData);
                                StringBuilder allPaths = new StringBuilder("Multiple Files Selected:" + "\n");
                                for (int i = 0; i < clipData.getItemCount(); i++) {
                                    allPaths.append("\n\n").append(clipData.getItemAt(i).getUri());
                                }
                                System.out.println("FILE CONTENT " + allPaths.toString());
                                //selectedDocumentTXT.setText(allPaths.toString());
                            } else {
                                pickiT.getPath(clipData.getItemAt(0).getUri(), Build.VERSION.SDK_INT);
                                System.out.println("FILE CONTENT " + String.valueOf(clipData.getItemAt(0).getUri()));
                                //selectedDocumentTXT.setText(String.valueOf(clipData.getItemAt(0).getUri()));
                            }
                        } else {
                            pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
                            System.out.println("FILE CONTENT " + data.getData());
                           // selectedDocumentTXT.setText(String.valueOf(data.getData()));
                        }
                        checkBox.setImageResource(R.drawable.ic_baseline_check_circle_24);
                    }
                }

            });

    private void convertToFile(String path) throws Exception {
        file = new File(path);
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("multipart/form-data"));
        document = MultipartBody.Part.createFormData("document", file.getName(), fileBody);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void createAccount() throws JSONException {
        Common.hideKeyboard(this);
        String email = Objects.requireNonNull(emailField.getText()).toString().trim();
        String number = Objects.requireNonNull(phoneNumberField.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordField.getText()).toString().trim();

        if (email.isEmpty()) {
            emailField.setError("Email required");
            emailField.requestFocus();
            return;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Email invalid");
            emailField.requestFocus();
            return;
        }
        if (number.isEmpty()) {
            phoneNumberField.setError("Phone number required");
            phoneNumberField.requestFocus();
            return;

        } else if (!PhoneNumberUtils.isGlobalPhoneNumber(number)) {
            phoneNumberField.setError("Phone number invalid");
            phoneNumberField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordField.setError("Password required");
            passwordField.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordField.setError("Password less than 6 characters");
            passwordField.requestFocus();
            return;
        }


        if (file == null) {
            runOnUiThread(() -> {
                uploadIDCopy.setStrokeWidth(2);
                uploadIDCopy.setStrokeColor(Color.RED);
                Snackbar.make(getCurrentFocus(), "UPLOAD YOUR ID COPY", Snackbar.LENGTH_LONG).show();
            });
        } else {
            System.out.println("FILE NAME " + file.getPath());
            Common.setStatusBarColor(getWindow(), OwnerSignUp.this, getResources().getColor(R.color.background, null));
            loadingView.setVisibility(View.VISIBLE);
            avLoadingIndicatorView.setVisibility(View.VISIBLE);
            signUp.setVisibility(View.GONE);


            JSONObject user = new JSONObject();
            user.put("email", emailField.getText().toString().trim());
            user.put("password", passwordField.getText().toString().trim());
            user.put("role", Role.OWNER);

            JSONObject owner = new JSONObject();
            owner.put("phoneNumber", phoneNumberField.getText().toString().trim());
            owner.put("approved", false);
            owner.put("uniqueDocumentId", "automated");
            owner.put("reported", false);
            owner.put("user", user);

            String userReq = user.toString();
            String ownerReq = owner.toString();

            System.out.println("REQUEST 11 " + userReq);
            System.out.println("REQUEST 11 " + ownerReq);

            Call<ResponseBody> registerOwner = RetrofitClient.getRetrofitClient().getAPI().registerOwner(userReq,
                    ownerReq,
                    document);
            registerOwner.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            try {
                                String data = response.body().string();
                                Snackbar.make(getCurrentFocus(), "Account created", Snackbar.LENGTH_LONG).show();
                                startActivity(new Intent(OwnerSignUp.this, LandingPage.class));
                                finish();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            try {
                                System.out.println("UPLOAD ERR " + response.errorBody().string());
                                Snackbar.make(getCurrentFocus(), "Account already exists", Snackbar.LENGTH_LONG).show();
                                loadingView.setVisibility(View.GONE);
                                avLoadingIndicatorView.setVisibility(View.GONE);
                                Common.setStatusBarColor(getWindow(), OwnerSignUp.this, Color.WHITE);
                                signUp.setVisibility(View.VISIBLE);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    runOnUiThread(() -> {
                        System.out.println("SERVER ERR " + t.getMessage());
                        Snackbar.make(getCurrentFocus(), "Server error", Snackbar.LENGTH_LONG).show();
                        loadingView.setVisibility(View.GONE);
                        avLoadingIndicatorView.setVisibility(View.GONE);
                        signUp.setVisibility(View.VISIBLE);
                        Common.setStatusBarColor(getWindow(), OwnerSignUp.this, Color.WHITE);
                    });
                }
            });
        }


    }

    private void selectPDF() {
        Intent intent
                = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        activityResultLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {
            selectPDF();
        } else {
            Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    ProgressBar mProgressBar;
    TextView percentText;
    private AlertDialog mdialog;
    ProgressDialog progressBar;

    @Override
    public void PickiTonUriReturned() {
        progressBar = new ProgressDialog(this);
        progressBar.setMessage("Waiting to receive file...");
        progressBar.setCancelable(false);
        progressBar.show();
    }

    @Override
    public void PickiTonStartListener() {
        if (progressBar.isShowing()) {
            progressBar.cancel();
        }
        final AlertDialog.Builder mPro = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog));
        @SuppressLint("InflateParams") final View mPView = LayoutInflater.from(this).inflate(R.layout.dailog_layout, null);
        percentText = mPView.findViewById(R.id.percentText);

        percentText.setOnClickListener(view -> {
            pickiT.cancelTask();
            if (mdialog != null && mdialog.isShowing()) {
                mdialog.cancel();
            }
        });

        mProgressBar = mPView.findViewById(R.id.mProgressBar);
        mProgressBar.setMax(100);
        mPro.setView(mPView);
        mdialog = mPro.create();
        mdialog.show();

    }

    @Override
    public void PickiTonProgressUpdate(int progress) {
        String progressPlusPercent = progress + "%";
        percentText.setText(progressPlusPercent);
        mProgressBar.setProgress(progress);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String reason) {
        if (mdialog != null && mdialog.isShowing()) {
            mdialog.cancel();
        }
        //  Chick if it was successful
        if (wasSuccessful) {
            //  Set returned path to TextView
            if (path.contains("/proc/")) {
                // "Sub-directory inside Downloads was selected." + "\n" + " We will be making use of the /proc/ protocol." + "\n" + " You can use this path as you would normally." + "\n\n" + "PickiT path:" + "\n" +
            } else {
                System.out.println("FILE CONTENT 2" + path);
            }
            selectedDocumentTXT.setText(path);
            try {
                convertToFile(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            selectedDocumentTXT.setText(reason);
        }
    }

    @Override
    public void PickiTonMultipleCompleteListener(ArrayList<String> paths, boolean wasSuccessful, String Reason) {
        if (mdialog != null && mdialog.isShowing()) {
            mdialog.cancel();
        }
        StringBuilder allPaths = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            allPaths.append("\n").append(paths.get(i)).append("\n");
        }
    }

    @Override
    public void onBackPressed() {
        pickiT.deleteTemporaryFile(this);
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            pickiT.deleteTemporaryFile(this);
        }
    }
}