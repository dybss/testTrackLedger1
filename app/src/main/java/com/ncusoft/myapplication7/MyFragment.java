package com.ncusoft.myapplication7;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class MyFragment extends Fragment {
    private TextView usernameText;
    private EditText etNewUsername, etNewPassword;
    private Button btnSaveProfile, btnEditProfile;
    private int userId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my, container, false);

        usernameText = view.findViewById(R.id.username_text);
        etNewUsername = view.findViewById(R.id.et_new_username);
        etNewPassword = view.findViewById(R.id.et_new_password);
        btnSaveProfile = view.findViewById(R.id.btn_save_profile);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        // 优先从SharedPreferences获取userId
        SharedPreferences sp = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        userId = sp.getInt("userId", -1);

        // 可选：初始化用户名显示
        // usernameText.setText(...);

        btnSaveProfile.setOnClickListener(v -> {
            String newUsername = etNewUsername.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            if (TextUtils.isEmpty(newUsername) && TextUtils.isEmpty(newPassword)) {
                Toast.makeText(getContext(), "请输入新用户名或新密码", Toast.LENGTH_SHORT).show();
                return;
            }
            updateProfile(newUsername, newPassword);
        });

        btnEditProfile.setOnClickListener(v -> {
            etNewUsername.setVisibility(View.VISIBLE);
            etNewPassword.setVisibility(View.VISIBLE);
            btnSaveProfile.setVisibility(View.VISIBLE);
        });

        return view;
    }

    private void updateProfile(String newUsername, String newPassword) {
        if (userId == -1) {
            Toast.makeText(getContext(), "用户信息异常", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject jsonBody = new JSONObject();
        try {
            if (!TextUtils.isEmpty(newUsername)) {
                jsonBody.put("username", newUsername);
            }
            if (!TextUtils.isEmpty(newPassword)) {
                jsonBody.put("password", newPassword);
            }
        } catch (JSONException e) {
            Toast.makeText(getContext(), "数据异常", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                String api = "/users/" + userId;
                String response = HttpUtils.sendPutRequest(api, jsonBody.toString());
                if (response != null) {
                    JSONObject jsonResponse = new JSONObject(response);
                    boolean success = jsonResponse.optBoolean("success", false);
                    String message = jsonResponse.optString("message", "未知错误");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            if (success && !TextUtils.isEmpty(newUsername)) {
                                usernameText.setText(newUsername);
                            }
                        });
                    }
                } else {
                    showToast("修改失败，请重试");
                }
            } catch (IOException | JSONException e) {
                showToast("网络或数据异常");
            }
        }).start();
    }

    private void showToast(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
        }
    }
}
