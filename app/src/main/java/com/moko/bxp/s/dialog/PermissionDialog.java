package com.moko.bxp.s.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.moko.bxp.s.R;
import com.permissionx.guolindev.dialog.RationaleDialogFragment;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author: jun.liu
 * @date: 2022/9/27 18:28
 * @des:
 */
public class PermissionDialog extends RationaleDialogFragment {
    private TextView tvSure;
    private TextView tvCancel;
    @NonNull
    private final List<String> permissionList;
    @NonNull
    private final String content;

    public PermissionDialog(@NonNull List<String> permissionList, @NonNull String content) {
        this.permissionList = permissionList;
        this.content = content;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (null != getDialog() && null != getDialog().getWindow())
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View layoutView = inflater.inflate(R.layout.dialog_permission_d, container, false);
        initViews(layoutView);
        return layoutView;
    }

    private void initViews(@NonNull View layoutView) {
        TextView tvContent = layoutView.findViewById(R.id.tvContent);
        tvSure = layoutView.findViewById(R.id.tvSure);
        tvCancel = layoutView.findViewById(R.id.tvCancel);
        tvContent.setText(content);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initWindow();
    }

    private void initWindow() {
        if (null != getDialog()) {
            Window window = getDialog().getWindow();
            if (null != window) {
                window.getAttributes().width = dip2px(requireContext());
                window.getAttributes().height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.getAttributes().gravity = Gravity.CENTER;
                window.setBackgroundDrawableResource(R.drawable.shape_radius3_solid_ffffff);
            }
        }
    }

    @NonNull
    @Override
    public View getPositiveButton() {
        return tvSure;
    }

    @Nullable
    @Override
    public View getNegativeButton() {
        return tvCancel;
    }

    @NonNull
    @Override
    public List<String> getPermissionsToRequest() {
        return permissionList;
    }

    private int dip2px(Context context) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (260 * scale + 0.5f);
    }
}
