package com.moko.bxp.s.fragment;

import static com.moko.support.s.entity.SlotAdvType.NO_DATA;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ReplacementTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.moko.bxp.s.ISlotDataAction;
import com.moko.bxp.s.R;
import com.moko.bxp.s.databinding.FragmentUidBinding;
import com.moko.bxp.s.utils.ToastUtils;
import com.moko.support.s.MokoSupport;
import com.moko.support.s.OrderTaskAssembler;
import com.moko.support.s.entity.SlotData;
import com.moko.support.s.entity.TxPowerEnum;

import java.util.Objects;

public class UidFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, ISlotDataAction {
    private static final String TAG = "UidFragment";
    private FragmentUidBinding mBind;
    private boolean isLowPowerMode;
    private SlotData slotData;
    private boolean isTriggerAfter;

    public UidFragment() {
    }

    public static UidFragment newInstance() {
        return new UidFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentUidBinding.inflate(inflater, container, false);
        if (isTriggerAfter) {
            mBind.layoutLowPower.setVisibility(View.GONE);
            mBind.layoutStandDuration.setVisibility(View.GONE);
            mBind.etAdvDuration.setHint("0~65535");
        }
        mBind.sbRssi.setOnSeekBarChangeListener(this);
        mBind.sbTxPower.setOnSeekBarChangeListener(this);
        mBind.etNamespace.setTransformationMethod(new A2bigA());
        mBind.etInstanceId.setTransformationMethod(new A2bigA());
        mBind.ivLowPowerMode.setOnClickListener(v -> {
            isLowPowerMode = !isLowPowerMode;
            changeView();
        });
        return mBind.getRoot();
    }

    public void setTriggerAfter(boolean isTriggerAfter) {
        this.isTriggerAfter = isTriggerAfter;
        if (isTriggerAfter && null != mBind) {
            mBind.layoutLowPower.setVisibility(View.GONE);
            mBind.layoutStandDuration.setVisibility(View.GONE);
            mBind.etAdvDuration.setHint("0~65535");
        }
    }

    private void changeView() {
        mBind.ivLowPowerMode.setImageResource(isLowPowerMode ? R.drawable.ic_checked : R.drawable.ic_unchecked);
        if (isLowPowerMode) {
            mBind.layoutAdvDuration.setVisibility(View.VISIBLE);
            mBind.layoutStandDuration.setVisibility(View.VISIBLE);
        } else {
            mBind.layoutAdvDuration.setVisibility(View.GONE);
            mBind.layoutStandDuration.setVisibility(View.GONE);
        }
    }

    private int mAdvDuration;
    private int mRssi;
    private int mTxPower;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateData(seekBar.getId(), progress);
    }

    @SuppressLint("DefaultLocale")
    private void updateData(int viewId, int progress) {
        if (viewId == R.id.sb_rssi) {
            int rssi = progress - 100;
            mBind.tvRssi.setText(String.format("%ddBm", rssi));
            mRssi = rssi;
        } else if (viewId == R.id.sb_tx_power) {
            TxPowerEnum txPowerEnum = TxPowerEnum.fromOrdinal(progress);
            if (null == txPowerEnum) return;
            int txPower = txPowerEnum.getTxPower();
            mBind.tvTxPower.setText(String.format("%ddBm", txPower));
            mTxPower = txPower;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public boolean isValid() {
        if (TextUtils.isEmpty(mBind.etNamespace.getText()) || TextUtils.isEmpty(mBind.etInstanceId.getText())) {
            ToastUtils.showToast(requireContext(), "Data format incorrect!");
            return false;
        }
        String namespace = mBind.etNamespace.getText().toString();
        String instanceId = mBind.etInstanceId.getText().toString();
        if (namespace.length() != 20 || instanceId.length() != 12) {
            ToastUtils.showToast(requireContext(), "Data format incorrect!");
            return false;
        }
        if (TextUtils.isEmpty(mBind.etAdvInterval.getText())) {
            ToastUtils.showToast(requireContext(), "The Adv interval can not be empty.");
            return false;
        }
        int advIntervalInt = Integer.parseInt(mBind.etAdvInterval.getText().toString());
        if (advIntervalInt < 1 || advIntervalInt > 100) {
            ToastUtils.showToast(requireContext(), "The Adv interval range is 1~100");
            return false;
        }
        int mStandbyDuration = 0;
        if (isLowPowerMode || isTriggerAfter) {
            if (TextUtils.isEmpty(mBind.etAdvDuration.getText())) {
                ToastUtils.showToast(requireContext(), "The Adv duration can not be empty.");
                return false;
            }
            int mAdvDuration = Integer.parseInt(mBind.etAdvDuration.getText().toString());
            if (isTriggerAfter) {
                if (mAdvDuration > 65535) {
                    ToastUtils.showToast(requireContext(), "The Adv duration range is 0~65535");
                    return false;
                }
            } else {
                if (mAdvDuration < 1 || mAdvDuration > 65535) {
                    ToastUtils.showToast(requireContext(), "The Adv duration range is 1~65535");
                    return false;
                }
            }
            if (!isTriggerAfter) {
                if (TextUtils.isEmpty(mBind.etStandbyDuration.getText())) {
                    ToastUtils.showToast(requireContext(), "The Standby duration can not be empty.");
                    return false;
                }
                int standbyDurationInt = Integer.parseInt(mBind.etStandbyDuration.getText().toString());
                if (standbyDurationInt > 65535 || standbyDurationInt < 1) {
                    ToastUtils.showToast(requireContext(), "The Standby duration range is 1~65535");
                    return false;
                }
                mStandbyDuration = standbyDurationInt;
            }
        } else {
            mAdvDuration = 10;
        }
        slotData.advInterval = advIntervalInt;
        slotData.advDuration = mAdvDuration;
        slotData.standbyDuration = mStandbyDuration;
        slotData.rssi = mRssi;
        slotData.txPower = mTxPower;
        slotData.namespace = namespace;
        slotData.instanceId = instanceId;
        return true;
    }

    @Override
    public SlotData getSlotData() {
        return slotData;
    }

    @Override
    public void sendData() {
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setNormalSlotAdvParams(slotData));
    }

    public static class A2bigA extends ReplacementTransformationMethod {
        @Override
        protected char[] getOriginal() {
            return new char[]{'a', 'b', 'c', 'd', 'e', 'f'};
        }

        @Override
        protected char[] getReplacement() {
            return new char[]{'A', 'B', 'C', 'D', 'E', 'F'};
        }
    }

    @Override
    public void setParams(@NonNull SlotData slotData) {
        this.slotData = slotData;
        if (slotData.currentFrameType == NO_DATA) return;
        mBind.etAdvInterval.setText(String.valueOf(slotData.advInterval / 100));
        mBind.etAdvDuration.setText(String.valueOf(slotData.advDuration));
        if (!isTriggerAfter) {
            if (slotData.standbyDuration > 0) {
                mBind.etStandbyDuration.setText(String.valueOf(slotData.standbyDuration));
            }
            isLowPowerMode = slotData.standbyDuration != 0;
            changeView();
        }
        int rssiProgress = slotData.rssi + 100;
        mBind.sbRssi.setProgress(rssiProgress);

        int txPowerProgress = Objects.requireNonNull(TxPowerEnum.fromTxPower(slotData.txPower)).ordinal();
        mBind.sbTxPower.setProgress(txPowerProgress);
        mBind.etNamespace.setText(slotData.namespace);
        mBind.etInstanceId.setText(slotData.instanceId);
    }
}
