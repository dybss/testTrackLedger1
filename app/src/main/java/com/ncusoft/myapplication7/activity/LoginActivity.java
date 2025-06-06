package com.ncusoft.myapplication7.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ncusoft.myapplication7.R;
import com.ncusoft.myapplication7.utils.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);

        // 设置App名称和版本信息
        TextView tvAppInfo = findViewById(R.id.tv_app_info);
        String versionName = "";
        try {
            versionName = getPackageManager()
                .getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            versionName = "";
        }
        String appInfo = getString(R.string.app_name) + " v" + versionName;
        tvAppInfo.setText(appInfo);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonBody = new JSONObject();
                            jsonBody.put("username", username);
                            jsonBody.put("password", password);

                            String response = HttpUtils.sendPostRequest("/users/login", jsonBody.toString());
                            if (response != null) {
                                JSONObject jsonResponse = new JSONObject(response);
                                boolean success = jsonResponse.getBoolean("success");
                                String message = jsonResponse.getString("message");

                                if (success) {
                                    int userId = jsonResponse.getInt("userId"); // 获取 user_id
                                    // 新增：获取用户名
                                    String loginUsername = jsonResponse.has("username") ? jsonResponse.getString("username") : username;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                                            onLoginSuccess(userId, loginUsername);
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(LoginActivity.this, "登录失败，请重试", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "登录失败，请重试", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 无需处理
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 文本改变时的逻辑
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 文本改变后执行验证
                if (s.length() < 6) {
                    etPassword.setError("密码长度至少6位");
                } else {
                    etPassword.setError(null);
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    // 修改onLoginSuccess，保存用户名
    private void onLoginSuccess(int userId, String username) {
        SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
        sp.edit().putInt("userId", userId)
                .putString("username", username)
                .apply();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}