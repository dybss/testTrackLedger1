package com.ncusoft.myapplication7.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ncusoft.myapplication7.R;
import com.ncusoft.myapplication7.utils.HttpUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONException;
import org.json.JSONObject;

public class MyFragment extends androidx.fragment.app.Fragment {
    private TextView usernameText;
    private EditText etNewUsername, etNewPassword, etConfirmPassword;
    private Button btnSaveProfile;
    private ImageView avatarImageView;
    private int userId = -1;
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my, container, false);

        usernameText = view.findViewById(R.id.username_text);
        etNewUsername = view.findViewById(R.id.et_new_username);
        etNewPassword = view.findViewById(R.id.et_new_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password); // 新增
        btnSaveProfile = view.findViewById(R.id.btn_save_profile);
        avatarImageView = view.findViewById(R.id.avatar); // 用原有id，兼容布局

        // 优先从SharedPreferences获取userId和用户名
        SharedPreferences sp = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        userId = sp.getInt("userId", -1);
        String username = sp.getString("username", "");
        if (!TextUtils.isEmpty(username)) {
            usernameText.setText(username);
        }

        // 加载本地头像
        loadLocalAvatar();

        btnSaveProfile.setOnClickListener(v -> {
            String newUsername = etNewUsername.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            if (TextUtils.isEmpty(newUsername) && TextUtils.isEmpty(newPassword)) {
                Toast.makeText(getContext(), "请输入新用户名或新密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!TextUtils.isEmpty(newPassword)) {
                if (TextUtils.isEmpty(confirmPassword)) {
                    Toast.makeText(getContext(), "请再次输入确认密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(getContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            updateProfile(newUsername, newPassword);
        });

        // 在 onCreateView 里设置头像点击事件
        avatarImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null && getContext() != null) {
                try (InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        avatarImageView.setImageBitmap(bitmap); // 直接设置原图，交给ImageView的scaleType处理
                        saveAvatarToLocal(bitmap);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "头像设置失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveAvatarToLocal(Bitmap bitmap) {
        if (getContext() == null || bitmap == null || userId == -1) return;
        File file = new File(getContext().getFilesDir(), "avatar_" + userId + ".png");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadLocalAvatar() {
        if (getContext() == null || userId == -1) return;
        File file = new File(getContext().getFilesDir(), "avatar_" + userId + ".png");
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                avatarImageView.setImageBitmap(bitmap);
            }
        } else {
            avatarImageView.setImageResource(android.R.drawable.ic_menu_myplaces); // 用系统自带默认头像
        }
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
                            if (success) {
                                if (!TextUtils.isEmpty(newUsername)) {
                                    usernameText.setText(newUsername);
                                }
                                // 清空输入框
                                etNewUsername.setText("");
                                etNewPassword.setText("");
                                etConfirmPassword.setText("");
                                // 跳转到登录页面重新登录
                                Intent intent = new Intent(getActivity(), com.ncusoft.myapplication7.activity.LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
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
