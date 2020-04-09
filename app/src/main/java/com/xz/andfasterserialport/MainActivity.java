package com.xz.andfasterserialport;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tp.xmaihh.serialport.SerialHelper;
import tp.xmaihh.serialport.bean.ComBean;
import tp.xmaihh.serialport.stick.AbsStickPackageHelper;
import tp.xmaihh.serialport.utils.ByteUtil;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    @BindView(R.id.rg_type)
    RadioGroup mRgType;
    @BindView(R.id.et_read_content)
    EditText mEtReadContent;
    @BindView(R.id.et_send_content)
    EditText mEtSendContent;

    private SerialHelper serialHelper;
    private boolean isHexType = false;
    private String text = "";

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            ComBean comBean = (ComBean) msg.obj;
            String time = comBean.sRecTime;
            String rxText;
            rxText = new String(comBean.bRec);
            if (isHexType) {
                //转成十六进制数据
                rxText = ByteUtil.ByteArrToHex(comBean.bRec);
            }
            text += "Rx-> " + time + ": " + rxText + "\r" + "\n";
            mEtReadContent.setText(text);
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mRgType.setOnCheckedChangeListener(this);

        initSerialConfig();
    }

    private void initSerialConfig() {
        //初始化SerialHelper对象，设定串口名称和波特率
        serialHelper = new SerialHelper(Const.SPORT_NAME, Const.BAUD_RATE) {
            @Override
            protected void onDataReceived(ComBean paramComBean) {
                Message message = mHandler.obtainMessage();
                message.obj = paramComBean;
                mHandler.sendMessage(message);
            }
        };

        /*
         * 默认的BaseStickPackageHelper将接收的数据扩展成64位，一般用不到这么多位
         * 我这里重新设定一个自适应数据位数的
         */

        serialHelper.setStickPackageHelper(new AbsStickPackageHelper() {
            @Override
            public byte[] execute(InputStream is) {
                try {
                    int available = is.available();
                    if (available > 0) {
                        byte[] buffer = new byte[available];
                        int size = is.read(buffer);
                        if (size > 0) {
                            return buffer;
                        }
                    } else {
                        SystemClock.sleep(50);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    @OnClick({R.id.bt_open, R.id.bt_close, R.id.bt_send, R.id.bt_clear_content})
    public void onButtonClicked(View view){
        switch (view.getId()) {
            case R.id.bt_open:
                if (serialHelper.isOpen()) {
                    Toast.makeText(this, Const.SPORT_NAME + "串口已经打开", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    serialHelper.open();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(this, Const.SPORT_NAME + "串口打开成功", Toast.LENGTH_SHORT).show();
                break;

            case R.id.bt_close:
                if (serialHelper.isOpen()) {
                    serialHelper.close();
                    Toast.makeText(this, Const.SPORT_NAME + "串口已经关闭", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.bt_clear_content:
                text = "";
                mEtReadContent.setText(text);
                break;

            case R.id.bt_send:
                if (!serialHelper.isOpen()) {
                    Toast.makeText(this, Const.SPORT_NAME + "串口没打开 发送失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                String sendContent = mEtSendContent.getText().toString();
                if (isHexType) {
                    serialHelper.sendHex(sendContent);
                } else {
                    serialHelper.sendTxt(sendContent);
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_txt:
                isHexType = false;
                mEtSendContent.setText(Const.TXT_TYPE_SEND);
                break;

            case R.id.rb_hex:
                isHexType = true;
                mEtSendContent.setText(Const.HEX_TYPE_SEND);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serialHelper.close();
        serialHelper = null;
    }
}
