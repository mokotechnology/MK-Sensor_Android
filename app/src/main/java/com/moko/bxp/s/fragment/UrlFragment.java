package com.moko.bxp.s.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.s.ISlotDataAction;
import com.moko.bxp.s.R;
import com.moko.bxp.s.databinding.FragmentUrlBinding;
import com.moko.bxp.s.dialog.UrlSchemeDialog;
import com.moko.bxp.s.entity.SlotData;
import com.moko.bxp.s.entity.TriggerStep1Bean;
import com.moko.bxp.s.utils.ToastUtils;
import com.moko.support.s.MokoSupport;
import com.moko.support.s.OrderTaskAssembler;
import com.moko.support.s.entity.SlotFrameTypeEnum;
import com.moko.support.s.entity.TxPowerEnum;
import com.moko.support.s.entity.TxPowerEnumC112;
import com.moko.support.s.entity.UrlExpansionEnum;
import com.moko.support.s.entity.UrlSchemeEnum;

import java.util.ArrayList;
import java.util.Objects;

public class UrlFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, ISlotDataAction {
    private static final String TAG = "UrlFragment";
    private final String FILTER_ASCII = "[!-~]*";
    private FragmentUrlBinding mBind;
    private boolean isLowPowerMode;
    private SlotData slotData;

    public UrlFragment() {
    }

    public static UrlFragment newInstance() {
        return new UrlFragment();
    }

    public void setSlotData(SlotData slotData) {
        this.slotData = slotData;
        if (slotData.isC112 && null != mBind) {
            mBind.sbTxPower.setMax(5);
            mBind.tvTxPowerTips.setText("(-20, -16, -12, -8, -4, 0)");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentUrlBinding.inflate(inflater, container, false);
        mBind.sbRssi.setOnSeekBarChangeListener(this);
        mBind.sbTxPower.setOnSeekBarChangeListener(this);
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        setDefault();
        mBind.ivLowPowerMode.setOnClickListener(v -> {
            isLowPowerMode = !isLowPowerMode;
            changeView();
        });
        if (slotData.isC112) {
            mBind.sbTxPower.setMax(5);
            mBind.tvTxPowerTips.setText("(-20, -16, -12, -8, -4, 0)");
        }
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

    @SuppressLint("DefaultLocale")
    private void setDefault() {
        if (slotData.frameTypeEnum == SlotFrameTypeEnum.NO_DATA) {
            mBind.etAdvInterval.setText("10");
            mBind.etAdvDuration.setText("10");
            mBind.etStandbyDuration.setText("0");
            mBind.sbRssi.setProgress(100);
            mBind.sbTxPower.setProgress(5);
        } else {
            isLowPowerMode = slotData.standbyDuration != 0;
            mBind.etAdvInterval.setText(String.valueOf(slotData.advInterval/100));
            mBind.etAdvDuration.setText(String.valueOf(slotData.advDuration));
            if (isLowPowerMode) {
                mBind.etStandbyDuration.setText(String.valueOf(slotData.standbyDuration));
            }
            changeView();
            if (slotData.frameTypeEnum == SlotFrameTypeEnum.TLM) {
                mBind.sbRssi.setProgress(100);
                mRssi = 0;
                mBind.tvRssi.setText(String.format("%ddBm", mRssi));
            } else {
                int advTxPowerProgress = slotData.rssi_0m + 100;
                mBind.sbRssi.setProgress(advTxPowerProgress);
                mRssi = slotData.rssi_0m;
                mBind.tvRssi.setText(String.format("%ddBm", mRssi));
            }
            int txPowerProgress;
            if (slotData.isC112) {
                txPowerProgress = Objects.requireNonNull(TxPowerEnumC112.fromTxPower(slotData.txPower)).ordinal();
            } else {
                txPowerProgress = Objects.requireNonNull(TxPowerEnum.fromTxPower(slotData.txPower)).ordinal();
            }
            mBind.sbTxPower.setProgress(txPowerProgress);
            mTxPower = slotData.txPower;
            mBind.tvTxPower.setText(String.format("%ddBm", mTxPower));
        }
        mUrlScheme = UrlSchemeEnum.HTTP_WWW.getUrlType();
        mBind.tvUrlScheme.setText(UrlSchemeEnum.HTTP_WWW.getUrlDesc());
        if (slotData.frameTypeEnum == SlotFrameTypeEnum.URL) {
            mUrlScheme = slotData.urlSchemeEnum.getUrlType();
            mBind.tvUrlScheme.setText(slotData.urlSchemeEnum.getUrlDesc());
            String url = slotData.urlContentHex;
            String urlExpansionStr = url.substring(url.length() - 2);
            int urlExpansionType = Integer.parseInt(urlExpansionStr, 16);
            UrlExpansionEnum urlEnum = UrlExpansionEnum.fromUrlExpanType(urlExpansionType);
            if (urlEnum == null) {
                mBind.etUrl.setText(MokoUtils.hex2String(url));
            } else {
                mBind.etUrl.setText(MokoUtils.hex2String(url.substring(0, url.length() - 2)) + urlEnum.getUrlExpanDesc());
            }
        }
    }

    private int mAdvInterval;
    private int mAdvDuration;
    private int mStandbyDuration;
    private int mRssi;
    private int mTxPower;
    private int mUrlScheme;
    private String mUrlContent;
    private final TriggerStep1Bean triggerStep1Bean = new TriggerStep1Bean();

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

    public TriggerStep1Bean getTriggerStep1Bean(){
        return triggerStep1Bean;
    }

    @Override
    public boolean isValid() {
        String urlContent = mBind.etUrl.getText().toString();
        String advInterval = mBind.etAdvInterval.getText().toString();
        String advDuration = mBind.etAdvDuration.getText().toString();
        String standbyDuration = mBind.etStandbyDuration.getText().toString();
        if (TextUtils.isEmpty(urlContent)) {
            ToastUtils.showToast(requireContext(), "Data format incorrect!");
            return false;
        }
        int advIntervalInt = Integer.parseInt(advInterval);
        if (advIntervalInt < 1 || advIntervalInt > 100) {
            ToastUtils.showToast(requireContext(), "The Adv interval range is 1~100");
            return false;
        }
        if (isLowPowerMode) {
            if (TextUtils.isEmpty(advDuration)) {
                ToastUtils.showToast(requireContext(), "The Adv duration can not be empty.");
                return false;
            }
            int advDurationInt = Integer.parseInt(advDuration);
            if (advDurationInt < 1 || advDurationInt > 65535) {
                ToastUtils.showToast(requireContext(), "The Adv duration range is 1~65535");
                return false;
            }
            mAdvDuration = advDurationInt;
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
        } else {
            mAdvDuration = 10;
            mStandbyDuration = 0;
        }
        if (urlContent.indexOf(".") >= 0) {
            String urlExpansion = urlContent.substring(urlContent.lastIndexOf("."));
            UrlExpansionEnum urlExpansionEnum = UrlExpansionEnum.fromUrlExpanDesc(urlExpansion);
            if (urlExpansionEnum == null) {
                // url中有点，但不符合eddystone结尾格式，内容长度不能超过17个字符
                if (urlContent.length() < 2 || urlContent.length() > 17) {
                    ToastUtils.showToast(requireContext(), "Data format incorrect!");
                    return false;
                }
            } else {
                String content = urlContent.substring(0, urlContent.lastIndexOf("."));
                if (content.length() < 1 || content.length() > 16) {
                    ToastUtils.showToast(requireContext(), "Data format incorrect!");
                    return false;
                }
            }
        } else {
            // url中没有有点，内容长度不能超过17个字符
            if (urlContent.length() < 2 || urlContent.length() > 17) {
                ToastUtils.showToast(requireContext(), "Data format incorrect!");
                return false;
            }
        }
        triggerStep1Bean.url = mUrlContent = urlContent;
        triggerStep1Bean.advInterval = mAdvInterval = advIntervalInt;
        triggerStep1Bean.urlScheme = mUrlScheme;
        triggerStep1Bean.advDuration = mAdvDuration;
        triggerStep1Bean.standByDuration = mStandbyDuration;
        triggerStep1Bean.rssi  = mRssi;
        triggerStep1Bean.txPower = mTxPower;
        triggerStep1Bean.isLowPowerMode = isLowPowerMode;
        return true;
    }

    @Override
    public void sendData() {
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setSlotAdvParamsBefore(slotData.slotEnum.ordinal(),
                mAdvInterval, mAdvDuration, mStandbyDuration, mRssi, mTxPower));
        orderTasks.add(OrderTaskAssembler.setSlotParamsURL(slotData.slotEnum.ordinal(),
                mUrlScheme, mUrlContent));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Override
    public void resetParams(SlotFrameTypeEnum currentFrameTypeEnum) {
        if (slotData.frameTypeEnum == currentFrameTypeEnum) {
            mBind.etAdvInterval.setText(String.valueOf(slotData.advInterval));
            mBind.etAdvDuration.setText(String.valueOf(slotData.advDuration));
            mBind.etStandbyDuration.setText(String.valueOf(slotData.standbyDuration));
            isLowPowerMode = slotData.standbyDuration != 0;
            changeView();

            int rssiProgress = slotData.rssi_0m + 100;
            mBind.sbRssi.setProgress(rssiProgress);

            int txPowerProgress;
            if (slotData.isC112) {
                txPowerProgress = Objects.requireNonNull(TxPowerEnumC112.fromTxPower(slotData.txPower)).ordinal();
            } else {
                txPowerProgress = Objects.requireNonNull(TxPowerEnum.fromTxPower(slotData.txPower)).ordinal();
            }
            mBind.sbTxPower.setProgress(txPowerProgress);

            mUrlScheme = slotData.urlSchemeEnum.getUrlType();
            mBind.tvUrlScheme.setText(slotData.urlSchemeEnum.getUrlDesc());
            String url = slotData.urlContentHex;
            String urlExpansionStr = url.substring(url.length() - 2);
            int urlExpansionType = Integer.parseInt(urlExpansionStr, 16);
            UrlExpansionEnum urlEnum = UrlExpansionEnum.fromUrlExpanType(urlExpansionType);
            if (urlEnum == null) {
                mBind.etUrl.setText(MokoUtils.hex2String(url));
            } else {
                mBind.etUrl.setText(MokoUtils.hex2String(url.substring(0, url.length() - 2)) + urlEnum.getUrlExpanDesc());
            }
            mBind.etUrl.setSelection(mBind.etUrl.getText().toString().length());
        } else {
            mBind.etAdvInterval.setText("10");
            mBind.etAdvDuration.setText("10");
            mBind.etStandbyDuration.setText("");
            mBind.sbRssi.setProgress(100);
            mBind.sbTxPower.setProgress(5);
            mBind.etUrl.setText("");
            isLowPowerMode = false;
            changeView();
        }
    }

    public void selectUrlScheme() {
        UrlSchemeDialog dialog = new UrlSchemeDialog(mBind.tvUrlScheme.getText().toString());
        dialog.setUrlSchemeClickListener(urlType -> {
            UrlSchemeEnum urlSchemeEnum = UrlSchemeEnum.fromUrlType(Integer.parseInt(urlType));
            if (null == urlSchemeEnum) return;
            mBind.tvUrlScheme.setText(urlSchemeEnum.getUrlDesc());
            mUrlScheme = Integer.parseInt(urlType);
        });
        dialog.show(getChildFragmentManager());
    }
}
