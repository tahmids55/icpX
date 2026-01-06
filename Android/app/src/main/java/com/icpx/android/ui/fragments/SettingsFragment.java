package com.icpx.android.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.icpx.android.R;
import com.icpx.android.ui.LoginActivity;

/**
 * Fragment for app settings
 */
public class SettingsFragment extends Fragment {

    private View cfHandleLayout;
    private TextView cfHandleText;
    private View changePasswordLayout;
    private View logoutLayout;
    private View clearDataLayout;
    private SwitchCompat requireLoginSwitch;
    private SwitchCompat notificationsSwitch;
    private SwitchCompat autoFetchSwitch;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        prefs = requireActivity().getSharedPreferences("user_prefs", 0);
        
        initViews(view);
        setupListeners();
        loadCfHandle();
        
        return view;
    }

    private void initViews(View view) {
        cfHandleLayout = view.findViewById(R.id.cfHandleLayout);
        cfHandleText = view.findViewById(R.id.cfHandleText);
        changePasswordLayout = view.findViewById(R.id.changePasswordLayout);
        logoutLayout = view.findViewById(R.id.logoutLayout);
        clearDataLayout = view.findViewById(R.id.clearDataLayout);
        requireLoginSwitch = view.findViewById(R.id.requireLoginSwitch);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        autoFetchSwitch = view.findViewById(R.id.autoFetchSwitch);

        // Load saved preference
        boolean requireLogin = prefs.getBoolean("require_login", true);
        requireLoginSwitch.setChecked(requireLogin);
    }

    private void loadCfHandle() {
        String handle = prefs.getString("codeforces_handle", "");
        if (handle.isEmpty()) {
            cfHandleText.setText("Not set - Tap to add");
            cfHandleText.setTextColor(getResources().getColor(R.color.textSecondary));
        } else {
            cfHandleText.setText(handle);
            cfHandleText.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    private void setupListeners() {
        cfHandleLayout.setOnClickListener(v -> showCfHandleDialog());

        requireLoginSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            prefs.edit()
                    .putBoolean("require_login", isChecked)
                    .apply();
            
            Toast.makeText(requireContext(), 
                    isChecked ? "Login required on startup" : "Login disabled - app will skip login screen", 
                    Toast.LENGTH_SHORT).show();
        });

        changePasswordLayout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Change password feature coming soon", Toast.LENGTH_SHORT).show();
        });

        logoutLayout.setOnClickListener(v -> {
            // Clear user session and go to login
            prefs.edit()
                    .clear()
                    .apply();
            
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        clearDataLayout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Cache cleared", Toast.LENGTH_SHORT).show();
        });

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(requireContext(), 
                    "Notifications " + (isChecked ? "enabled" : "disabled"), 
                    Toast.LENGTH_SHORT).show();
        });

        autoFetchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(requireContext(), 
                    "Auto-fetch " + (isChecked ? "enabled" : "disabled"), 
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void showCfHandleDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(android.R.layout.select_dialog_item, null);
        EditText input = new EditText(requireContext());
        input.setHint("Enter Codeforces handle");
        
        String currentHandle = prefs.getString("codeforces_handle", "");
        if (!currentHandle.isEmpty()) {
            input.setText(currentHandle);
            input.setSelection(currentHandle.length());
        }
        
        input.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(requireContext())
                .setTitle("Codeforces Handle")
                .setMessage("Enter your Codeforces handle for automatic problem verification")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String handle = input.getText().toString().trim();
                    if (!handle.isEmpty()) {
                        prefs.edit()
                                .putString("codeforces_handle", handle)
                                .apply();
                        loadCfHandle();
                        Toast.makeText(requireContext(), "Codeforces handle saved: " + handle, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Handle cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Remove", (dialog, which) -> {
                    prefs.edit()
                            .remove("codeforces_handle")
                            .apply();
                    loadCfHandle();
                    Toast.makeText(requireContext(), "Codeforces handle removed", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
