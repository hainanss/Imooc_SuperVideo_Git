package com.hejunlin.imooc_supervideo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hejunlin.imooc_supervideo.base.BaseActivity;
import com.hejunlin.imooc_supervideo.model.sohu.Video;
import com.hejunlin.imooc_supervideo.utils.DateUtils;
import com.hejunlin.imooc_supervideo.widget.media.IjkVideoView;

import java.util.Formatter;
import java.util.Locale;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class PlayActivity extends BaseActivity {

    private static final String TAG = PlayActivity.class.getSimpleName();
    private static final int CHECK_TIME = 1;
    private static final int CHECK_BATTERY = 2;
    private static final int CHECK_PROGRESS = 3;
    private static final int AUTO_HIDE_TIME = 10000;
    private static final int AFTER_DRAGGLE_HIDE_TIME = 3000;
    private String mUrl;
    private int mStreamType;
    private int mCurrentPosition;
    private Video mVideo;
    private IjkVideoView mVideoView;
    private RelativeLayout mLoadingLayout;
    private TextView mLoadingText;
    private FrameLayout mTopLayout;
    private LinearLayout mBottomLayout;
    private ImageView mBackButton;
    private TextView mVideoNameView;
    private TextView mSysTimeView;
    private ImageView mBigPauseButton;
    private CheckBox mPlayOrPauseButton;
    private TextView mVideoCurrentTime;
    private TextView mVideoTotalTime;
    private TextView mBitStreamView;
    private EventHandler mEventHandler;
    private boolean mIsPanelShowing = false;
    private int mBatteryLevel;
    private ImageView mBatteryView;
    private boolean mIsMove = false;//是否在屏幕上滑动
    private SeekBar mSeekBar;
    private Formatter mFormatter;
    private StringBuilder mFormatterBuilder;
    private boolean mIsDragging;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_play;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBatteryReceiver != null) {
            unregisterReceiver(mBatteryReceiver);
            mBatteryReceiver = null;
        }

    }

    /**
     * 通过广播获取系统电量情况
     */
    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBatteryLevel = intent.getIntExtra("level", 0);
            Log.d(TAG, ">> mBatteryReceiver onReceive mBatteryLevel=" + mBatteryLevel);
        }
    };

    class EventHandler extends Handler {
        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CHECK_TIME:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSysTimeView.setText(DateUtils.getCurrentTime());
                        }
                    });
                    break;
                case CHECK_BATTERY:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setCurrentBattery();
                        }
                    });
                    break;
                case CHECK_PROGRESS:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long duration = mVideoView.getDuration();
                            long nowduration = (mSeekBar.getProgress() * duration)/1000L;
                            mVideoCurrentTime.setText(stringForTime((int)nowduration));
                        }
                    });
                    break;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mIsMove == false) {
                toggleTopAndBottomLayout();
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void initView() {
        mUrl = getIntent().getStringExtra("url");
        mStreamType = getIntent().getIntExtra("type", 0);
        mCurrentPosition = getIntent().getIntExtra("currentPosition", 0);
        mVideo = getIntent().getParcelableExtra("video");
        Log.d(TAG, ">> ulr " + mUrl + ", mStreamType " + mStreamType + ", mCurrentPosition " + mCurrentPosition);
        Log.d(TAG, ">> video " + mVideo);
        mEventHandler = new EventHandler(Looper.myLooper());
        initTopAndBottomView();
        initListener();
        //init player
        mVideoView = bindViewId(R.id.video_view);
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        mLoadingLayout = bindViewId(R.id.rl_loading_layout);
        mLoadingText = bindViewId(R.id.tv_loading_info);
        mLoadingText.setText("正在加载中...");
        mVideoView.setVideoURI(Uri.parse(mUrl));
        mVideoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                mVideoView.start();
            }
        });
        mVideoView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                switch (what) {
                    case IjkMediaPlayer.MEDIA_INFO_BUFFERING_START:
                        mLoadingLayout.setVisibility(View.VISIBLE);
                        break;
                    case IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    case IjkMediaPlayer.MEDIA_INFO_BUFFERING_END:
                        mLoadingLayout.setVisibility(View.GONE);
                        break;
                }
                return false;
            }
        });
        registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        toggleTopAndBottomLayout();
    }

    private void initTopAndBottomView() {
        mTopLayout = bindViewId(R.id.fl_player_top_container);
        mBottomLayout = bindViewId(R.id.ll_player_bottom_layout);
        mBackButton = bindViewId(R.id.iv_player_close);//返回按钮
        mVideoNameView = bindViewId(R.id.tv_player_video_name);//video标题
        mBatteryView = bindViewId(R.id.iv_battery);
        mSysTimeView = bindViewId(R.id.tv_sys_time);//系统时间
        mBigPauseButton = bindViewId(R.id.iv_player_center_pause);//屏幕中央暂停按钮
        mPlayOrPauseButton = bindViewId(R.id.cb_play_pause);//底部播放暂停按钮
        mVideoCurrentTime = bindViewId(R.id.tv_current_video_time);//当前播放进度
        mVideoTotalTime = bindViewId(R.id.tv_total_video_time);//视频总时长
        mBitStreamView = bindViewId(R.id.tv_bitstream);//码流
        mSeekBar = bindViewId(R.id.sb_player_seekbar);
        mSeekBar.setMax(1000);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mFormatterBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatterBuilder,Locale.getDefault());
    }

    private void initListener() {
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mBigPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoView.start();
                updatePlayPauseStatus(true);
            }
        });
        mPlayOrPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePlayPause();
            }
        });
    }

    private void toggleTopAndBottomLayout() {
        if (mIsPanelShowing) {
            hideTopAndBottomLayout();
        } else {
            showTopAndBottomLayout();
            //先显示,没有任何操作,就5s后隐藏
            mEventHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideTopAndBottomLayout();
                }
            }, AUTO_HIDE_TIME);
        }
    }

    private void showTopAndBottomLayout() {
        mIsPanelShowing = true;
        mTopLayout.setVisibility(View.VISIBLE);
        mBottomLayout.setVisibility(View.VISIBLE);
        updateProgress();
        if (mEventHandler != null) {
            mEventHandler.removeMessages(CHECK_TIME);
            Message msg = mEventHandler.obtainMessage(CHECK_TIME);
            mEventHandler.sendMessage(msg);

            mEventHandler.removeMessages(CHECK_BATTERY);
            Message batterymsg = mEventHandler.obtainMessage(CHECK_BATTERY);
            mEventHandler.sendMessage(batterymsg);

            mEventHandler.removeMessages(CHECK_PROGRESS);
            Message progressmsg = mEventHandler.obtainMessage(CHECK_PROGRESS);
            mEventHandler.sendMessage(progressmsg);
        }
        switch (mStreamType) {
            case AlbumDetailActivity.StreamType.SUPER:
                mBitStreamView.setText(getResources().getString(R.string.stream_super));
                break;
            case AlbumDetailActivity.StreamType.NORMAL:
                mBitStreamView.setText(getResources().getString(R.string.stream_normal));
                break;
            case AlbumDetailActivity.StreamType.HIGH:
                mBitStreamView.setText(getResources().getString(R.string.stream_high));
                break;
            default:
                break;
        }
    }

    private void hideTopAndBottomLayout() {
        if (mIsDragging == true) {
            return;
        }
        mIsPanelShowing = false;
        mTopLayout.setVisibility(View.GONE);
        mBottomLayout.setVisibility(View.GONE);
    }

    private void handlePlayPause() {
        //TODO
        if (mVideoView.isPlaying()) {//视频正在播放
            mVideoView.pause();
            updatePlayPauseStatus(false);
        } else {
            mVideoView.start();
            updatePlayPauseStatus(true);
        }
    }

    private void updatePlayPauseStatus(boolean isPlaying) {
        mBigPauseButton.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
        mPlayOrPauseButton.invalidate();
        mPlayOrPauseButton.setChecked(isPlaying);
        mPlayOrPauseButton.refreshDrawableState();
    }


    @Override
    protected void initData() {
        Log.d(TAG, ">> initData mVideo=" + mVideo);
        if (mVideo != null) {
            Log.d(TAG, ">> initData mVideoName" + mVideo.getVideoName());
            mVideoNameView.setText(mVideo.getVideoName());
        }
    }

    private void setCurrentBattery() {
        Log.d(TAG, ">> setCurrentBattery level " + mBatteryLevel);
        if ( 0 < mBatteryLevel && mBatteryLevel <= 10) {
            mBatteryView.setBackgroundResource(R.drawable.ic_battery_10);
        } else if (10 < mBatteryLevel && mBatteryLevel <= 20) {
            mBatteryView.setBackgroundResource(R.drawable.ic_battery_20);
        } else if (20 < mBatteryLevel && mBatteryLevel <= 50) {
            mBatteryView.setBackgroundResource(R.drawable.ic_battery_50);
        } else if (50 < mBatteryLevel && mBatteryLevel <= 80) {
            mBatteryView.setBackgroundResource(R.drawable.ic_battery_80);
        } else if (80 < mBatteryLevel && mBatteryLevel <= 100) {
            mBatteryView.setBackgroundResource(R.drawable.ic_battery_100);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        // seekbar进度发生变化时回调
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }
            long duration = mVideoView.getDuration();//视频时长
            long nowPosition = (duration * progress) / 1000L;
            mVideoCurrentTime.setText(stringForTime((int) nowPosition));
        }

        // seekbar开始拖动时回调
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mIsDragging = true;
        }

        // seekbar拖动完成后回调
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mIsDragging = false;
            int progress = seekBar.getProgress();//最后拖动停止的进度
            long duration = mVideoView.getDuration();//视频时长
            long newPosition = (duration * progress) / 1000L;//当前的进度
            mVideoView.seekTo((int) newPosition);
            mEventHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideTopAndBottomLayout();
                }
            },AFTER_DRAGGLE_HIDE_TIME);
        }
    };

    private void updateProgress() {
        int currentPosition = mVideoView.getCurrentPosition();//当前的视频位置
        int duration = mVideoView.getDuration();//视频时长
        if (mSeekBar != null) {
            if (duration > 0) {
                //转成long型,避免溢出
                long pos = currentPosition * 1000L/ duration;
                mSeekBar.setProgress((int) pos);
            }
            int perent = mVideoView.getBufferPercentage();//已经缓冲的进度
            mSeekBar.setSecondaryProgress(perent);//设置缓冲进度
            mVideoCurrentTime.setText(stringForTime(currentPosition));
            mVideoTotalTime.setText(stringForTime(duration));
        }
    }


    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60; //换成秒
        int minutes = (totalSeconds / 60) % 60;
        int hours = (totalSeconds / 3600);
        mFormatterBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

}