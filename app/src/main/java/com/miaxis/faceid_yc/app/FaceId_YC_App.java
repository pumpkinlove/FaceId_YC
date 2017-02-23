package com.miaxis.faceid_yc.app;

import android.app.Application;
import android.app.smdt.SmdtManager;
import android.widget.Toast;

import com.miaxis.faceid_yc.event.ToastEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.x;
import org.zz.faceapi.MXFaceAPI;

/**
 * Created by xu.nan on 2017/2/14.
 */

public class FaceId_YC_App extends Application {

    public static MXFaceAPI mxAPI;
    public static int feature_len = 0;

    private EventBus bus;
    private Thread initAlgThread;
    private SmdtManager smdtManager;

    @Override
    public void onCreate()  {
        x.Ext.init(this);
        super.onCreate();

        initData();
        init();
        smdtManager = new SmdtManager(this);
        smdtManager.smdtSetExtrnalGpioValue(2, true);   //摄像头上电
    }

    private void initData() {
        mxAPI = new MXFaceAPI();
        bus = EventBus.getDefault();
        bus.register(this);
    }

    private void init() {
        if (initAlgThread != null) {
            initAlgThread.interrupt();
            initAlgThread = null;
        }
        initAlgThread = new Thread(new InitAlgRunnable());
        initAlgThread.start();
    }

    private class InitAlgRunnable implements Runnable {
        @Override
        public void run() {
            initAlg();
        }
    }

    private void initAlg() {
        int re = mxAPI.mxInitAlg(null);
        if (re == 0) {
            feature_len = mxAPI.mxGetFeatureSize();
        } else {
            feature_len = -1;
            bus.post(new ToastEvent("初始化SDK失败 " + re));
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        smdtManager.smdtSetExtrnalGpioValue(2, false);   //摄像头上电
        bus.unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onToastEvent(ToastEvent e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    }

}
