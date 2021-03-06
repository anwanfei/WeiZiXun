package com.anfly.weizixun.ui.activity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anfly.weizixun.R;
import com.anfly.weizixun.adapter.EMMessageAdapter;
import com.anfly.weizixun.common.Constants;
import com.anfly.weizixun.utils.AudioUtil;
import com.anfly.weizixun.utils.SharedPreferencesUtils;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMLocationMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMMessageBody;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chat.EMVoiceMessageBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChatActivity extends AppCompatActivity {

    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.rv)
    RecyclerView rv;
    @BindView(R.id.et_content)
    EditText etContent;
    @BindView(R.id.btN_send)
    Button btNSend;
    @BindView(R.id.btn_record)
    Button btnRecord;
    @BindView(R.id.btn_send_audio)
    Button btnSendAudio;
    private String toName;
    private String curName;
    private ArrayList<EMMessage> list;
    private EMMessageAdapter adapter;
    private EMMessageListener msgListener;
    private String mPath;
    private long mTime;
    private LocationClient mLocationClient;
    private MapView mapView;
    private BaiduMap baiduMap;
    private BDLocation mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);
        initData();
        initView();

        initReceiver();
        initHistory();
    }

    private void initHistory() {
        EMConversation conversation = EMClient.getInstance().chatManager().getConversation(toName);
        if (conversation == null) {
            return;
        }
        //获取此会话的所有消息
        List<EMMessage> messages = conversation.getAllMessages();
        if (messages.size() <= 0) {
            return;
        }
        list.addAll(messages);
        adapter.notifyDataSetChanged();
    }

    private void initReceiver() {
        msgListener = new EMMessageListener() {

            @Override
            public void onMessageReceived(List<EMMessage> messages) {

                list.addAll(messages);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCmdMessageReceived(List<EMMessage> messages) {
                //收到透传消息
            }

            @Override
            public void onMessageRead(List<EMMessage> messages) {
                //收到已读回执
            }

            @Override
            public void onMessageDelivered(List<EMMessage> message) {
                //收到已送达回执
            }

            @Override
            public void onMessageRecalled(List<EMMessage> messages) {
                //消息被撤回
            }

            @Override
            public void onMessageChanged(EMMessage message, Object change) {
                //消息状态变动
            }
        };

        EMClient.getInstance().chatManager().addMessageListener(msgListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
    }

    private void initData() {
        toName = getIntent().getStringExtra(Constants.NAME);
        curName = (String) SharedPreferencesUtils.getParam(this, Constants.NAME, "a");

        tvTitle.setText(curName + "正在和" + toName + "聊天中...");
    }

    private void initView() {
        list = new ArrayList<>();
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EMMessageAdapter(list, this, toName, curName);
        rv.setAdapter(adapter);
        adapter.setOnItemClick(new EMMessageAdapter.OnItemClick() {
            @Override
            public void onItemClick(String localUrl, EMMessageBody emMessageBody) {

                if (emMessageBody instanceof EMVoiceMessageBody) {
                    playAudio(localUrl);
                } else if (emMessageBody instanceof EMLocationMessageBody) {
                    Intent intent = new Intent(ChatActivity.this, MapActivity.class);
                    double latitude = ((EMLocationMessageBody) emMessageBody).getLatitude();
                    double longitude = ((EMLocationMessageBody) emMessageBody).getLongitude();
//                    intent.putExtra()
                    startActivity(intent);
                }


            }
        });

        initLocation();
    }


    private void playAudio(String localUrl) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            boolean playing = mediaPlayer.isPlaying();
            if (playing) {
                mediaPlayer.pause();
            }
            if (TextUtils.isEmpty(localUrl)) {
                return;
            }
            mediaPlayer.setDataSource(localUrl);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClick({R.id.btN_send, R.id.btn_record, R.id.btn_send_audio, R.id.btn_location})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btN_send:
                send();
                break;
            case R.id.btn_record:
                record();
                break;
            case R.id.btn_send_audio:
                sendAudio();
                break;
            case R.id.btn_location:
                sendLocation();
                break;
        }
    }

    private void sendLocation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                double latitude = mLocation.getLatitude();
                double longitude = mLocation.getLongitude();
                //latitude为纬度，longitude为经度，locationAddress为具体位置内容
                EMMessage message = EMMessage.createLocationSendMessage(latitude, longitude, "我的位置", toName);
                //如果是群聊，设置chattype，默认是单聊
                EMClient.getInstance().chatManager().sendMessage(message);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        list.add(message);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    private void initLocation() {
        mapView = new MapView(this);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        //定位初始化
        mLocationClient = new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

        //设置locationClientOption
        mLocationClient.setLocOption(option);

        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }

    public class MyLocationListener extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mapView == null) {
                return;
            }

            mLocation = location;
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            baiduMap.setMyLocationData(locData);
        }
    }

    private void sendAudio() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //voiceUri为语音文件本地资源标志符，length为录音时间(秒)
                EMMessage message = EMMessage.createVoiceSendMessage(mPath, (int) mTime, toName);
                EMClient.getInstance().chatManager().sendMessage(message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        list.add(message);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    private void record() {
        boolean isRecording = AudioUtil.isRecording;
        if (isRecording) {
            btnRecord.setText("开始录音");
            AudioUtil.stopRecord();
        } else {
            btnRecord.setText("停止录音");
            AudioUtil.startRecord(new AudioUtil.ResultCallBack() {
                @Override
                public void onSuccess(String path, long time) {
                    mPath = path;
                    mTime = time;
                }

                @Override
                public void onFail(String msg) {
                    Log.e("TAG", msg);
                }
            });
        }
    }

    private void send() {

        String content = etContent.getText().toString();

        if (content.isEmpty()) {
            Toast.makeText(ChatActivity.this, "请输入消息", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //创建一条文本消息，content为消息文字内容，toChatUsername为对方用户或者群聊的id，后文皆是如此
                EMMessage message = EMMessage.createTxtSendMessage(content, toName);
                //发送消息
                EMClient.getInstance().chatManager().sendMessage(message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        list.add(message);
                        adapter.notifyDataSetChanged();
                        etContent.setText("");
                    }
                });
            }
        }).start();

    }
}
