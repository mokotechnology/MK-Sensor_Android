package com.moko.bxp.s.fragment;

import static com.moko.support.s.entity.SlotAdvType.NO_DATA;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.InputFilter;
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
import com.moko.bxp.s.databinding.FragmentSensorInfoBinding;
import com.moko.bxp.s.utils.ToastUtils;
import com.moko.support.s.MokoSupport;
import com.moko.support.s.OrderTaskAssembler;
import com.moko.support.s.entity.SlotData;
import com.moko.support.s.entity.TxPowerEnum;

import java.util.Objects;

public class SensorInfoFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, ISlotDataAction {
    private static final String TAG = "SensorInfoFragment";
    private final String FILTER_ASCII = "[ -~]*";
    private FragmentSensorInfoBinding mBind;
    private boolean isLowPowerMode;
    private SlotData slotData;
    private int mRssi;
    private int mTxPower;
    private boolean isTriggerAfter;

    public SensorInfoFragment() {
    }

    public static SensorInfoFragment newInstance() {
        return new SensorInfoFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentSensorInfoBinding.inflate(inflater, container, false);
        if (isTriggerAfter) {
            mBind.layoutLowPower.setVisibility(View.GONE);
            mBind.layoutStandDuration.setVisibility(View.GONE);
            mBind.etAdvDuration.setHint("0~65535");
        }
        mBind.sbRssi.setOnSeekBarChangeListener(this);
        mBind.sbTxPower.setOnSeekBarChangeListener(this);
        //限制只输入大写，自动小写转大写
        mBind.etTagId.setTransformationMethod(new A2bigA());
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) return "";
            return null;
        };
        mBind.etDeviceName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20), filter});
        mBind.ivLowPowerMode.setOnClickListener(v -> {
            isLowPowerMode = !isLowPowerMode;
            changeView();
        });
        return mBind.getRoot();
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

    public void setTriggerAfter(boolean isTriggerAfter) {
        this.isTriggerAfter = isTriggerAfter;
        if (isTriggerAfter && null != mBind) {
            mBind.layoutLowPower.setVisibility(View.GONE);
            mBind.layoutStandDuration.setVisibility(View.GONE);
            mBind.etAdvDuration.setHint("0~65535");
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateData(seekBar.getId(), progress);
    }

    @SuppressLint("DefaultLocale")
    public void updateData(int viewId, int progress) {
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
        String advDuration = mBind.etAdvDuration.getText().toString();
        String standbyDuration = mBind.etStandbyDuration.getText().toString();
        if (TextUtils.isEmpty(mBind.etDeviceName.getText())) {
            ToastUtils.showToast(requireContext(), "Data format incorrect!");
            return false;
        }
        if (TextUtils.isEmpty(mBind.etTagId.getText()) || mBind.etTagId.getText().length() % 2 != 0) {
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
        int mAdvDuration;
        int mStandbyDuration = 0;
        if (isLowPowerMode || isTriggerAfter) {
            if (TextUtils.isEmpty(advDuration)) {
                ToastUtils.showToast(requireContext(), "The Adv duration can not be empty.");
                return false;
            }
            int advDurationInt = Integer.parseInt(advDuration);
            if (isTriggerAfter) {
                if (advDurationInt > 65535) {
                    ToastUtils.showToast(requireContext(), "The Adv duration range is 1~65535");
                    return false;
                }
            } else {
                if (advDurationInt < 1 || advDurationInt > 65535) {
                    ToastUtils.showToast(requireContext(), "The Adv duration range is 1~65535");
                    return false;
                }
            }
            mAdvDuration = advDurationInt;
            if (!isTriggerAfter) {
                if (TextUtils.isEmpty(standbyDuration)) {
                    ToastUtils.showToast(requireContext(), "The Standby duration can not be empty.");
                    return false;
                }
                int standbyDurationInt = Integer.parseInt(standbyDuration);
                if (standbyDurationInt > 65535 || standbyDurationInt < 1) {
                    ToastUtils.showToast(requireContext(), "The Standby duration range is 1~65535");
                    return false;
                }
                mStandbyDuration = standbyDurationInt;
            }
        } else {
            mAdvDuration = 10;
        }
        slotData.deviceName = mBind.etDeviceName.getText().toString();
        slotData.tagId = mBind.etTagId.getText().toString();
        slotData.advInterval = advIntervalInt;
        slotData.advDuration = mAdvDuration;
        slotData.standbyDuration = mStandbyDuration;
        slotData.rssi = mRssi;
        slotData.txPower = mTxPower;
        return true;
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
        mBind.etDeviceName.setText(slotData.deviceName);
        mBind.etTagId.setText(slotData.tagId);
    }

    @Override
    public SlotData getSlotData() {
        return slotData;
    }
}
