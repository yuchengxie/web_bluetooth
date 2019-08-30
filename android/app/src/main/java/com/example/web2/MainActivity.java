package com.example.web2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.nationz.sim.sdk.NationzSim;
import com.nationz.sim.sdk.NationzSimCallback;
import com.nationz.sim.util.Logz;

import java.util.ArrayList;
import java.util.HashMap;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL1 = "hzf.bluetooth";
    private static final String CHANNEL2 = "hzf.bluetoothState";
    private static NationzSim mNationzSim;
    private String CMD_PUB_ADDR = "80220200020000";
    private String CMD_PUB_KEY = "8022000000";
    private String CMD_PUB_KEY_HASH = "8022010000";
    private String appSelectID = "00A404000ED196300077130010000000020101";

    private String BLUECONNECTED = "1";
    private String BLUEDISCONNECTED = "0";
    private EventChannel.EventSink _eventSink;
    private String blueState = "";
    private String pubAddr = "";
    private String pubKey = "";
    private String pubHash = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GeneratedPluginRegistrant.registerWith(this);

        initNationzSim();

        new MethodChannel(getFlutterView(), CHANNEL1).setMethodCallHandler(new MethodChannel.MethodCallHandler() {
            @Override
            public void onMethodCall(MethodCall call, MethodChannel.Result result) {
                System.out.println("调用方法:" + call.method);
                System.out.println("传递参数:" + call.arguments == null ? "没有参数" : call.arguments);
                if (call.method.equals("connectBlueTooth")) {
                    ArrayList args = (ArrayList) call.arguments;
                    String bleName = (String) args.get(0);
                    String blePin = (String) args.get(1);
                    System.out.println("bleName:" + bleName);
                    System.out.println("blePin:" + blePin);
                    connectBlueTooth(bleName, blePin);
                } else if (call.method.equals("disConnectBlueTooth")) {
                    disConnectBlueTooth();
                } else if (call.method.equals("transmit")) {
                    ArrayList args = (ArrayList) call.arguments;
                    String cmd = (String) args.get(0);
                    String res = transmit(cmd);
                    if (res != "") {
                        result.success(res);
                    }
                }else if (call.method.equals("hello")){
                    System.out.println(">>>>>>>hello<<<<<<<<<");
                }else {
                    result.notImplemented();
                }
            }
        });

        new EventChannel(getFlutterView(), CHANNEL2).setStreamHandler(new EventChannel.StreamHandler() {

            @Override
            public void onListen(Object o, EventChannel.EventSink eventSink) {
                _eventSink = eventSink;
                sendBlueToothConnectStateEvent();
            }

            @Override
            public void onCancel(Object o) {
                _eventSink = null;
            }

            private Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                }
            };
        });
    }

    private void initNationzSim() {
        Logz.setLogable(true);
        mNationzSim = NationzSim.initialize(getApplication(), new NationzSimCallback() {
            @Override
            public void onConnectionStateChange(int i) {
                if (i == 16) {
                    System.out.println("连接成功");
                    //连接成功后会自动选择应用
                    transmit(appSelectID);
                    blueState = BLUECONNECTED;
                    pubAddr = transmit(CMD_PUB_ADDR);
                    pubKey = transmit(CMD_PUB_KEY);
                    pubHash = transmit(CMD_PUB_KEY_HASH);
                    sendBlueToothConnectStateEvent();
                    //获取
                } else if (i == 18) {
                    System.out.println("断开连接");
                    blueState = BLUEDISCONNECTED;
                    pubAddr = "";
                    pubKey = "";
                    pubHash = "";
                    sendBlueToothConnectStateEvent();
                } else if (i == 27) {
                    System.out.println("连接超时");
                }
            }

            @Override
            public void onMsgWrite(int i) {

            }

            @Override
            public void onMsgBack(byte[] bytes) {
                System.out.println("onMsgBack1:" + bytes);
            }

            @Override
            public void onMsgBack(String s, String s1) {
                System.out.println("onMsgBack2:" + s + ";" + s1);
            }
        });
    }

    private void connectBlueTooth(String bleName, String blePin) {
        System.out.println("正在连接中...");
        if (mNationzSim != null) {
            mNationzSim.connect(bleName, blePin);
        }
    }

    public void disConnectBlueTooth() {
        System.out.println("正在断开连接...");
        if (mNationzSim != null) {
            mNationzSim.close();
        }
    }


    private String transmit(String cmd) {
//        if (mNationzSim == null) return;
        System.out.println("mNationzSim:" + mNationzSim);
        System.out.println("发送:" + cmd);
        byte[] msg = toBytes(cmd);
        byte[] c = mNationzSim.writeSync(msg);
        if (c == null) {
            System.out.println("c=null");
            return "";
        }
        System.out.println("发送返回:" + bytesToHex(c));
        String res = bytesToHex(c);
        if (res.length() > 4 && res.substring(res.length() - 4).equals("9000")) {
            res = res.substring(0, res.length() - 4);
        }
        return res;
    }

    public void sendBlueToothConnectStateEvent() {
        if (_eventSink == null) return;
        HashMap<String, String> dic = new HashMap<String, String>();
        dic.put("state", blueState);
        dic.put("pubAddr", pubAddr);
        dic.put("pubKey", pubKey);
        dic.put("pubHash", pubHash);
        _eventSink.success(dic);
    }

    public String bytesToHex(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) { // 使用String的format方法进行转换
            buf.append(String.format("%02x", new Integer(b & 0xff)));
        }
        return buf.toString();
    }

    /**
     * 将16进制字符串转换为byte[]
     *
     * @param str
     * @return
     */
    public byte[] toBytes(String str) {
        if (str == null || str.trim().equals("")) {
            return new byte[0];
        }

        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }

        return bytes;
    }
}
