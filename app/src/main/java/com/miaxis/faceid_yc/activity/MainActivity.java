package com.miaxis.faceid_yc.activity;

import android.app.smdt.SmdtManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.miaxis.faceid_yc.R;
import com.miaxis.faceid_yc.app.FaceId_YC_App;
import com.miaxis.faceid_yc.event.FaceDetectedEvent;
import com.miaxis.faceid_yc.event.FingerPassEvent;
import com.miaxis.faceid_yc.event.IdFingerEvent;
import com.miaxis.faceid_yc.event.InfoEvent;
import com.miaxis.faceid_yc.event.MoveAwayEvent;
import com.miaxis.faceid_yc.event.NewIdEvent;
import com.miaxis.faceid_yc.event.PassEvent;
import com.miaxis.faceid_yc.utils.CommonUtil;
import com.miaxis.faceid_yc.utils.Constants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;
import org.zz.faceapi.MXFaceAPI;
import org.zz.idcard_hid_driver.IdCardDriver;
import org.zz.jni.mxImageLoad;
import org.zz.jni.zzFingerAlg;
import org.zz.mxhidfingerdriver.MXFingerDriver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.cloudwalk.sdk.FaceInfo;

import static com.miaxis.faceid_yc.app.FaceId_YC_App.feature_len;
import static com.miaxis.faceid_yc.utils.Constants.ID_PHOTO_NAME;
import static com.miaxis.faceid_yc.utils.Constants.ID_PHOTO_PATH;
import static com.miaxis.faceid_yc.utils.Constants.MAX_FACE_NUM;

@ContentView(R.layout.activity_main)
public class MainActivity extends BaseActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    @ViewInject(R.id.iv_id_photo)       private ImageView iv_id_photo;
    @ViewInject(R.id.iv_camera_photo)   private ImageView iv_camera_photo;

    @ViewInject(R.id.tv_display_info)   private TextView tv_display_info;
    @ViewInject(R.id.tv_id_name)        private TextView tv_id_name;
    @ViewInject(R.id.tv_id_gender)      private TextView tv_id_gender;
    @ViewInject(R.id.tv_id_race)        private TextView tv_id_race;
    @ViewInject(R.id.tv_id_birthday)    private TextView tv_id_birthday;
    @ViewInject(R.id.tv_id_address)     private TextView tv_id_address;
    @ViewInject(R.id.tv_id_cardno)      private TextView tv_id_cardno;

    @ViewInject(R.id.sv_camera)         private SurfaceView sv_camera;
    @ViewInject(R.id.sv_rect)           private SurfaceView sv_rect;


    @ViewInject(R.id.tv_finger1)        private TextView tv_finger1;
    @ViewInject(R.id.tv_finger2)        private TextView tv_finger2;

    @ViewInject(R.id.fl_main)           private FrameLayout fl_main;

    private int svWidth  = 640;
    private int svHeight = 480;

    private static final int PRE_WIDTH          = 160;
    private static final int PRE_HEIGHT         = 120;

    private float zoomRate = (float) svWidth / (float) PRE_WIDTH;

    private static final float PASS_SCORE       = 0.71f;        // 比对通过阈值

    private static final int PHOTO_SIZE         = 38862;        // 解码后身份证图片长度
    private static final int mFingerDataSize    = 512;          // 指纹数据长度
    private static final int mFingerDataB64Size = 684;          // 指纹数据Base64编码后的长度

    private static final int LEVEL              = 2;            // 指纹比对级别
    private static final int TIME_OUT           = 15 * 1000;    // 等待按手指的超时时间，单位：ms
    private static final int IMAGE_X_BIG        = 256;          // 指纹图像宽高 大小
    private static final int IMAGE_Y_BIG        = 360;
    private static final int IMAGE_SIZE_BIG     = IMAGE_X_BIG * IMAGE_Y_BIG;
    private static final int TZ_SIZE            = 684;          // 指纹特征长度  BASE64

    private EventBus bus;
    private boolean detectFlag = true;          // 判断是否继续检测人脸
    private boolean isMatchWorking;             // 判断人脸比对线程是否运行
    private boolean verifyFlag;                 // 判断是否继续比对人脸
    private boolean fingerFlag;                 // 判断是否循环采集指纹

    private IdCardDriver idCardDriver;          // 二代证
    private zzFingerAlg alg;                    // 指纹比对
    private mxImageLoad dtload;                 // 加载图像
    private MXFingerDriver fingerDriver;        // 指纹采集
    private MXFaceAPI mxAPI;                    // 人脸算法 云从
    private SmdtManager smdtManager;            // 视美泰

    private byte[] cardId;                      // 二代证id
    private byte[] idFaceFeature;               // 二代证照片 人脸特征
    private byte[] firstFeature;                // 视频 第一个 人脸特征

    private Camera mCamera;
    private SurfaceHolder mHolder;
    private SurfaceHolder rectHolder;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();//        用来进行特征提取的线程池
    private Thread waitThread;

    private Bitmap bmpIdCard;                   // 二代证照片

    private byte[] finger1;                     // 二代证指纹特征1 2
    private byte[] finger2;
    private byte[] vBuffer;                     // 采集到的指纹特征
    private long start;
    private long end;
    private long startFromId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//去掉信息栏

        x.view().inject(this);

        initData();
        initView();
        startReadId();
        startFinger();

    }

    private void initData() {
        mxAPI = FaceId_YC_App.mxAPI;
        smdtManager = SmdtManager.create(this);
        bus = EventBus.getDefault();
        bus.register(this);
        idCardDriver = new IdCardDriver(this);
        fingerDriver = new MXFingerDriver(this);
        alg = new zzFingerAlg();
        dtload = new mxImageLoad();
        mHolder = sv_camera.getHolder();//获得句柄
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        sv_rect.setZOrderOnTop(true);
        rectHolder = sv_rect.getHolder();
        rectHolder.setFormat(PixelFormat.TRANSLUCENT);
    }

    private void initView() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bus.unregister(this);
    }

    /* 开始循环读身份证id */
    private void startReadId() {
        Thread idThread = new Thread(new ReadIdRunnable());
        idThread.start();
    }

    /* 开始循环采集指纹 */
    private void startFinger() {
        Thread fingerThread = new Thread(new FingerRunnable());
        fingerThread.start();
    }

    /* 开始等待线程 10s */
    private void startWait() {
        if (waitThread != null) {
            waitThread.interrupt();
            waitThread = null;
        }
        waitThread = new Thread(new WaitRunnable());
        waitThread.start();
    }

    /* 停止等待线程 10s */
    private void stopWait() {
        if (waitThread != null) {
            waitThread.interrupt();
            waitThread = null;
        }
    }

    private void openCamera() {
        try {
            mCamera = Camera.open();
            Camera.Parameters p = mCamera.getParameters();
            p.setPreviewSize(PRE_WIDTH, PRE_HEIGHT);
            mCamera.setParameters(p);
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            displayInfo(-1, "打开摄像头失败");
        }
    }

    private void closeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /* 显示提示信息 */
    private void displayInfo(int code, String info) {
        Log.e("-------", info);
        bus.post(new InfoEvent(code, info));
    }

    /* 同步方法提取人脸特征 */
    private synchronized byte[] extractFreature(byte[] dataAlign, int iWidth, int iHeight, int iChannel) {
        byte[] pFeatureBuf = new byte[feature_len];
        Date d1 = new Date();
        int re = mxAPI.mxFeatureExtract(dataAlign, iWidth, iHeight, iChannel, pFeatureBuf);
        Date d2 = new Date();
        Log.e("_____", "___提取特征耗时_" + (d2.getTime() - d1.getTime()));
        if (re == 0) {
            return pFeatureBuf;
        } else {
            return null;
        }
    }

    /** SurfaceView 预览回调 */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!detectFlag) {
            return;
        }
        Canvas canvas = rectHolder.lockCanvas(null);
        if (canvas == null)
            return;
        int[] pFaceNum  = new int[1];
        pFaceNum[0] = MAX_FACE_NUM;
        FaceInfo[] pFaceBuffer = new FaceInfo[MAX_FACE_NUM];
        for (int i = 0; i < MAX_FACE_NUM; i++) {
            pFaceBuffer[i] = new FaceInfo();
        }
//        Date d1 = new Date();
        int re = mxAPI.mxDetectFaceYUV(data, PRE_WIDTH, PRE_HEIGHT, pFaceNum, pFaceBuffer);
//        Log.e("检测人脸___", "人脸数：" + pFaceNum[0] + "___耗时" + (new Date().getTime() - d1.getTime()));
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (re == 0 && pFaceNum[0] > 0) {
            drawFaceRect(pFaceBuffer, canvas, pFaceNum[0]);
            if (verifyFlag) {
                if (!isMatchWorking) {
                    isMatchWorking = true;
                    MatchRunnable matchRunnable = new MatchRunnable(data.clone(), pFaceNum, pFaceBuffer);
                    executorService.submit(matchRunnable);
                }
            }
        }
        if (canvas != null)
            rectHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        closeCamera();
    }

    /** EventBus */
    /* 处理 提示信息 事件 */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInfoEvent(InfoEvent e) {
        tv_display_info.setText(e.getInfo());
        switch (e.getCode()) {
            case 0:
                tv_display_info.setTextColor(getResources().getColor(R.color.dark));
                tv_display_info.setBackground(getResources().getDrawable(R.drawable.dark_stroke_bg));
                break;
            case 1:
                tv_display_info.setTextColor(getResources().getColor(R.color.green_dark));
                tv_display_info.setBackground(getResources().getDrawable(R.drawable.green_stroke_bg));
                break;
            case -1:
                tv_display_info.setTextColor(getResources().getColor(R.color.red));
                tv_display_info.setBackground(getResources().getDrawable(R.drawable.red_stroke_bg));
                break;
        }
    }

    /* 处理 新身份证 事件 */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewIdEvent(NewIdEvent e) {
        if (null == e) {
            return;
        }
        tv_id_name.     setText(e.getName());
        tv_id_gender.   setText(e.getGender());
        tv_id_race.     setText(e.getRace());
        tv_id_birthday. setText(e.getBirthday());
        tv_id_address.  setText(e.getAddress());
        tv_id_cardno.   setText(e.getIdNum());
        iv_id_photo.    setImageBitmap(e.getBitmap());
        iv_camera_photo.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.default_photo));
        startWait();
        startFromId = new Date().getTime();
    }

    /* 处理 人脸通过 事件 */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPassEvent(PassEvent e) {
        stopWait();
        fingerDriver.mxCancelGetImage();
        fingerFlag = false;
        if (null == e.getpCameraData()) {
            verifyFlag = true;
            return;
        }
        byte[] cameraData = e.getpCameraData();
        FaceInfo passFace = e.getFaceInfo();
        if (null == passFace) {
            verifyFlag = true;
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        int imageFormat= parameters.getPreviewFormat();
        YuvImage yuvImg = new YuvImage(cameraData,imageFormat, PRE_WIDTH, PRE_HEIGHT, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImg.compressToJpeg(new Rect(0, 0, PRE_WIDTH, PRE_HEIGHT), 50, out);

        byte[] bytes = out.toByteArray();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        Bitmap rectBitmap = Bitmap.createBitmap(bitmap, PRE_WIDTH - passFace.x -  passFace.width, passFace.y, passFace.width, passFace.height);//截取
        iv_camera_photo.setImageBitmap(rectBitmap);
        end = new Date().getTime();
        Log.e("_________","总耗时__________" +(end - start));
        Log.e("_________","读完身份证开始计时， 耗时__________" +(end - startFromId));
    }

    /* 处理 读到身份证中指纹 事件 */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onIdFingerEvent(IdFingerEvent e) {
        if (e.isFlag()) {
            finger1 = e.getFinger0();
            finger2 = e.getFinger1();
            tv_finger1.setTextColor(getResources().getColor(R.color.dark));
            tv_finger1.setBackground(getResources().getDrawable(R.drawable.dark_stroke_bg));
            tv_finger1.setText(e.getFingerPosition1());
            tv_finger2.setTextColor(getResources().getColor(R.color.dark));
            tv_finger2.setBackground(getResources().getDrawable(R.drawable.dark_stroke_bg));
            tv_finger2.setText(e.getFingerPosition2());
        } else {
            finger1 = null;
            finger2 = null;
            tv_finger1.setTextColor(getResources().getColor(R.color.dark));
            tv_finger1.setBackground(getResources().getDrawable(R.drawable.dark_stroke_bg));
            tv_finger1.setText("指纹一（未注册）");
            tv_finger2.setTextColor(getResources().getColor(R.color.dark));
            tv_finger2.setBackground(getResources().getDrawable(R.drawable.dark_stroke_bg));
            tv_finger2.setText("指纹二（未注册）");
        }
    }

    /* 处理 指纹通过 事件 */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFingerPassEvent(FingerPassEvent e) {
        stopWait();
        switch (e.getIndex()) {
            case 1 :
                tv_finger1.setTextColor(getResources().getColor(R.color.green_dark));
                tv_finger1.setBackground(getResources().getDrawable(R.drawable.green_stroke_bg));
                break;
            case 2 :
                tv_finger2.setTextColor(getResources().getColor(R.color.green_dark));
                tv_finger2.setBackground(getResources().getDrawable(R.drawable.green_stroke_bg));
                break;
        }
        fingerFlag = false;
        verifyFlag = false;
    }

    /* 处理 拿开身份证 事件 */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMoveAwayEvent(MoveAwayEvent e) {
        fingerDriver.mxCancelGetImage();
        tv_display_info.setText("请放身份证");
        tv_display_info.setTextColor(getResources().getColor(R.color.dark));
        tv_display_info.setBackground(getResources().getDrawable(R.drawable.dark_stroke_bg));
        tv_finger1.setText("指纹一");
        tv_finger2.setText("指纹二");
        tv_id_name.setText("");
        tv_id_cardno.setText("");
        tv_id_gender.setText("");
        tv_id_address.setText("");
        tv_id_birthday.setText("");
        tv_id_race.setText("");
        iv_camera_photo.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.default_photo));
        iv_id_photo.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.default_photo));
        cardId = null;
        verifyFlag = false;
        fingerFlag = false;
    }

    /** 线程 */
    /* 循环读身份证id线程 */
    private class ReadIdRunnable implements Runnable {
        @Override
        public void run() {
            byte[] bCardId;
            int re;
            while (true) {
                try {
                    Thread.sleep(50);
                    bCardId = new byte[64];
                    Date d1 = new Date();
                    re = idCardDriver.mxReadCardId(bCardId);
                    Date d2 = new Date();
                    if (re == 0) {
                        smdtManager.smdtSetExtrnalGpioValue(3, true);           //开灯
                        if (!Arrays.equals(bCardId, cardId)) {
                            Log.e("___身份证_", "_读id耗时___" + (d2.getTime() - d1.getTime()));
                            start = d1.getTime();
                            cardId = bCardId;
                            readCardInfo();
                        }
                    } else if (re == 134) {
                        cardId = null;
                        smdtManager.smdtSetExtrnalGpioValue(3, false);          //关灯
                        bus.post(new MoveAwayEvent());
                    }
                } catch (Exception e) {
                    bus.post(new MoveAwayEvent());
                }
            }
        }
    }

    /* 线程 循环采集指纹并比对 */
    private class FingerRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                if (fingerFlag) {
                    byte[] bImgBuf = new byte[IMAGE_SIZE_BIG];
                    int ret = fingerDriver.mxAutoGetImage(bImgBuf, IMAGE_X_BIG, IMAGE_Y_BIG, TIME_OUT, 0);
                    if (0 != ret) {
                        if (-3 == ret) {                    //time out
//                            displayInfo(0, "请按手指...");
                        } else if (-2 == ret) {                 //cancel
//                            displayInfo(0, "指纹图像采集 取消");
                        } else {
                            displayInfo(-1, "指纹图像采集 失败 " + ret);
                        }
                        continue;
                    } else {
                        displayInfo(0, "指纹图像采集成功");
                        vBuffer = new byte[TZ_SIZE];
                        ret = alg.mxGetTzBase64(bImgBuf, vBuffer);
                        if (ret == 1) {
                            displayInfo(0, "提取特征成功");
//                            String fingerZw = "UXdFU0RnRmpTdi8vLy8vLy8vLy8vLy8vL3h3QWVRQUFBQUFBQUFBQUFJNFdudnpOSjVMOG5qaWwv\n" +
//                                    "RjFHb2Z6T2JrZitQMnV5L0JGeUNQeCtkNno4NW4rUS9ITjlZUDdPZzZIOHlJZFAvbkdPcy96emxa\n" +
//                                    "RDhaSjVuL3E2Zm1meTRyVTcrVGI2Ni9KSE9xL3h1NHJYODhQbzMvcG9Sb2YxRUd4ajlUQ0xYL1pz\n" +
//                                    "eHR2MExPL0g5MmtrcC8xRlA3djBBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFB\n" +
//                                    "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFB\n" +
//                                    "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFB\n" +
//                                    "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFB\n" +
//                                    "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFB\n" +
//                                    "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFB\n" +
//                                    "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFB\n" +
//                                    "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFB\n" +
//                                    "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBTEk9\n";
//                            vBuffer = Base64.decode(fingerZw, 1);
                            ret = alg.mxFingerMatchBase64(finger1, vBuffer, LEVEL);
                            if (ret == 0) {
                                displayInfo(1, "指纹一比对通过！");
                                bus.post(new FingerPassEvent(1));
                            } else {
                                ret = alg.mxFingerMatchBase64(finger2, vBuffer, LEVEL);
                                if (ret == 0) {
                                    displayInfo(1, "指纹二比对通过！");
                                    bus.post(new FingerPassEvent(2));
                                } else {
                                    displayInfo(-1, "指纹比对失败！");
                                }
                            }
                        } else {
                            displayInfo(-1, "指纹提取特征失败 " + ret);
                        }
                    }
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /* 线程 从视频流中提取特征并比对 */
    private class MatchRunnable implements Runnable {
        private byte[] pCameraData = null;
        private int[] pFaceNum = null;
        private FaceInfo[]  pFaceBuffer = null;

        public MatchRunnable(byte[] pCameraData, int[] pFaceNum, FaceInfo[] pFaceBuffer) {
            this.pCameraData = pCameraData;
            this.pFaceNum = pFaceNum;
            this.pFaceBuffer = pFaceBuffer;
        }

        @Override
        public void run() {
            int re;
            float[] fScore = new float[1];
            for (int i=0; i<pFaceNum[0]; i++) {
                if (!verifyFlag) {
                    break;
                }
                if (pFaceBuffer[i].width == 0) {
                    continue;
                }
                detectFlag = false;
                byte[] pFeatureBuf = extractFreature(pFaceBuffer[i].alignedData, pFaceBuffer[i].alignedW, pFaceBuffer[i].alignedH, pFaceBuffer[i].nChannels);
                if (pFeatureBuf == null) {
                    displayInfo(-1, "提取相机人脸特征失败");
                    continue;
                }
                Date d1 = new Date();
                re = mxAPI.mxFeatureMatch(idFaceFeature, pFeatureBuf, fScore);
                Date d2 = new Date();
                Log.e("_____摄像头_", "_比对耗时__" + (d2.getTime() - d1.getTime()));
                if (re == 0 && fScore[0] > PASS_SCORE ) {
                    displayInfo(1, "验证通过 " + fScore[0]);
                    verifyFlag = false;
                    bus.post(new PassEvent(pFaceBuffer[i], pCameraData));
                    break;
                } else {
                    displayInfo(-1, "比对失败_ " + re + " _" + fScore[0]);
                }
            }
            isMatchWorking = false;
            detectFlag = true;
        }
    }

    /* 线程 等待10s 验证失败 提示 */
    private class WaitRunnable implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(10000);
                displayInfo(-1, "比对失败, 请重新放置身份证");
                verifyFlag = false;
//                bus.post(new MoveAwayEvent());
            } catch (InterruptedException e) {
            }
        }
    }

    /** 读二代证开始 */
    /* 读身份证信息 */
    private void readCardInfo() {
        byte[] bCardFullInfo = new byte[256 + 1024 + 1024];
        Date d1 = new Date();
        int re = idCardDriver.mxReadCardFullInfo(bCardFullInfo);
        Date d2 = new Date();
        Log.e("___身份证_", "_读全部信息（含指纹）耗时___" + (d2.getTime() - d1.getTime()));
        if (re != 0) {
            fingerFlag = false;
            if (re == -14) {
                byte[] bCardInfo = new byte[256 + 1024];
                Date d3 = new Date();
                int nRet = idCardDriver.mxReadCardInfo(bCardInfo);
                Date d4 = new Date();
                Log.e("___身份证_", "_读全部信息（不含指纹）耗时___" + (d4.getTime() - d3.getTime()));
                if (nRet != 0) {
                    if (nRet == -100) {
                        displayInfo(-1, "无设备");
                    }
                    else {
                        displayInfo(-1, "请放置身份证");
                    }
                } else {
                    analysisCardInfo(bCardInfo);
                    bus.post(new IdFingerEvent(false, null, null));
                }
            } else if (re == -100) {
                displayInfo(-1, "无设备");
            } else {
                displayInfo(-1, "读身份证失败");
            }
        } else {
            analysisCardInfo(bCardFullInfo);
            byte[] bFingerData1 = new byte[mFingerDataSize];
            byte[] bFingerData2 = new byte[mFingerDataSize];
            byte[] bFingerData1_B64 = new byte[mFingerDataB64Size];
            byte[] bFingerData2_B64 = new byte[mFingerDataB64Size];
            for (int i = 0; i < bFingerData1.length; i++) {
                bFingerData1[i] = bCardFullInfo[256 + 1024 + i];
            }
            for (int i = 0; i < bFingerData1.length; i++) {
                bFingerData2[i] = bCardFullInfo[256 + 1024 + 512 + i];
            }
            idCardDriver.Base64Encode(bFingerData1, mFingerDataSize, bFingerData1_B64, mFingerDataB64Size);
            idCardDriver.Base64Encode(bFingerData2, mFingerDataSize, bFingerData2_B64, mFingerDataB64Size);
            IdFingerEvent e = new IdFingerEvent(true, bFingerData1_B64,  bFingerData2_B64);
            e.setFingerPosition1(getFingerPosition(bFingerData1[5]));
            e.setFingerPosition2(getFingerPosition(bFingerData2[5]));
            bus.post(e);
            fingerFlag = true;
        }
    }

    /* 解析身份证信息 */
    private void analysisCardInfo(byte[] bCardInfo) {
        try {
            Date d1 = new Date();
            NewIdEvent infoEvent = new NewIdEvent();
            byte[] id_Name = new byte[30]; // 姓名
            byte[] id_Sex = new byte[2]; // 性别 1为男 其他为女
            byte[] id_Rev = new byte[4]; // 民族
            byte[] id_Born = new byte[16]; // 出生日期
            byte[] id_Home = new byte[70]; // 住址
            byte[] id_Code = new byte[36]; // 身份证号
            byte[] _RegOrg = new byte[30]; // 签发机关
            byte[] id_ValidPeriodStart = new byte[16]; // 有效日期 起始日期16byte 截止日期16byte
            byte[] id_ValidPeriodEnd = new byte[16];
            byte[] id_NewAddr = new byte[36]; // 预留区域
            byte[] id_pImage = new byte[1024]; // 图片区域
            int iLen = 0;
            for (int i = 0; i < id_Name.length; i++) {
                id_Name[i] = bCardInfo[i + iLen];
            }
            iLen = iLen + id_Name.length;
            infoEvent.setName(CommonUtil.unicode2String(id_Name));

            for (int i = 0; i < id_Sex.length; i++) {
                id_Sex[i] = bCardInfo[iLen + i];
            }
            iLen = iLen + id_Sex.length;

            if (id_Sex[0] == '1') {
                infoEvent.setGender("男");
            } else {
                infoEvent.setGender("女");
            }

            for (int i = 0; i < id_Rev.length; i++) {
                id_Rev[i] = bCardInfo[iLen + i];
            }
            iLen = iLen + id_Rev.length;
            int iRev = Integer.parseInt(CommonUtil.unicode2String(id_Rev));
            infoEvent.setRace(Constants.FOLK[iRev - 1]);

            for (int i = 0; i < id_Born.length; i++) {
                id_Born[i] = bCardInfo[iLen + i];
            }
            iLen = iLen + id_Born.length;
            infoEvent.setBirthday(CommonUtil.unicode2String(id_Born));

            for (int i = 0; i < id_Home.length; i++) {
                id_Home[i] = bCardInfo[iLen + i];
            }
            iLen = iLen + id_Home.length;
            infoEvent.setAddress(CommonUtil.unicode2String(id_Home));

            for (int i = 0; i < id_Code.length; i++) {
                id_Code[i] = bCardInfo[iLen + i];
            }
            iLen = iLen + id_Code.length;
            infoEvent.setIdNum(CommonUtil.unicode2String(id_Code));

            for (int i = 0; i < _RegOrg.length; i++) {
                _RegOrg[i] = bCardInfo[iLen + i];
            }
            iLen = iLen + _RegOrg.length;
            infoEvent.setRegOrg(CommonUtil.unicode2String(_RegOrg));

            for (int i = 0; i < id_ValidPeriodStart.length; i++) {
                id_ValidPeriodStart[i] = bCardInfo[iLen + i];
            }
            iLen = iLen + id_ValidPeriodStart.length;
            for (int i = 0; i < id_ValidPeriodEnd.length; i++) {
                id_ValidPeriodEnd[i] = bCardInfo[iLen + i];
            }
            iLen = iLen + id_ValidPeriodEnd.length;
            infoEvent.setValidTime(CommonUtil.unicode2String(id_ValidPeriodStart) + "-" + CommonUtil.unicode2String(id_ValidPeriodEnd));

            for (int i = 0; i < id_NewAddr.length; i++) {
                id_NewAddr[i] = bCardInfo[iLen + i];
            }
            iLen = iLen + id_NewAddr.length;
            /** 显示照片*/
            for (int i = 0; i < id_pImage.length; i++) {
                id_pImage[i] = bCardInfo[i + iLen];
            }
            iLen = iLen + id_pImage.length;
            byte[] bmp = new byte[PHOTO_SIZE];
            int re = idCardDriver.Wlt2Bmp(id_pImage, bmp);
            if (re == 0) {
                bmpIdCard = BitmapFactory.decodeByteArray(bmp, 0, bmp.length);
                CommonUtil.saveBitmap(bmpIdCard);           //落地
                infoEvent.setBitmap(bmpIdCard);
            }
            Date d2 = new Date();
            Log.e("___身份证_", "_解析耗时___" + (d2.getTime() - d1.getTime()));
            bus.post(infoEvent);
            detectFlag = false;
            getIdPhotoFeature();
        } catch (Exception e) {
            displayInfo(-1, "身份证信息解析错误");
        }
    }

    /* 获取指位信息 */
    private String getFingerPosition(int f) {
        switch (f) {
            case 11:
                return "右手拇指";
            case 12:
                return "右手食指";
            case 13:
                return "右手中指";
            case 14:
                return "右手环指";
            case 15:
                return "右手小指";
            case 16:
                return "左手拇指";
            case 17:
                return "左手食指";
            case 18:
                return "左手中指";
            case 19:
                return "左手环指";
            case 20:
                return "左手小指";
            case 97:
                return "右手不确定指位";
            case 98:
                return "左手不确定指位";
            case 99:
                return "其他不确定指位";
            default:
                return "";
        }
    }

    /* 提取身份证照片特征 */
    private void getIdPhotoFeature() {
        if (feature_len <= 0) {
            displayInfo(-1, "SDK初始化失败");
            return;
        }
        /** 加载图像 */
        int re = -1;
        int[] oX = new int[1];
        int[] oY = new int[1];
        // 获取图像大小
        File f = new File(ID_PHOTO_PATH + File.separator + ID_PHOTO_NAME);
        Date d1 = new Date();
        re = dtload.LoadFaceImage(ID_PHOTO_PATH + File.separator + ID_PHOTO_NAME, null, null, oX, oY);
        if (re != 1) {
            displayInfo(-1, "获取身份证本地图片失败");
            return;
        }
        byte[] pGrayBuff = new byte[oX[0] * oY[0]];
        byte[] pRGBBuff = new byte[oX[0] * oY[0] * 3];
        re = dtload.LoadFaceImage(ID_PHOTO_PATH + File.separator + ID_PHOTO_NAME, pRGBBuff, pGrayBuff, oX, oY);
        if (re != 1) {
            displayInfo(-1, "加载图片失败");
            return;
        }
        Date d2 = new Date();
        Log.e("_____身份证_", "_加载图像耗时__" + (d2.getTime() - d1.getTime()));
        /** 检测人脸 */
        int[] pFaceNum = new int[1];
        pFaceNum[0] = 1;                //身份证照片只可能检测到一张人脸
        FaceInfo[] pFaceBuffer = new FaceInfo[1];
        pFaceBuffer[0] = new FaceInfo();
        int iX = oX[0];
        int iY = oY[0];
        re = mxAPI.mxDetectFace(pGrayBuff, iX, iY, pFaceNum, pFaceBuffer);
        Date d3 = new Date();
        Log.e("_____身份证_", "_检测人脸耗时__" + (d3.getTime() - d2.getTime()));
        if (re != 0) {
            displayInfo(-1, "身份证照片未检测到人脸");
            return;
        }
        /** 提取特征 */
        idFaceFeature = extractFreature(pFaceBuffer[0].alignedData,
                pFaceBuffer[0].alignedW, pFaceBuffer[0].alignedH,
                pFaceBuffer[0].nChannels);
        if (re != 0 || idFaceFeature == null) {
            displayInfo(-1, "提取身份证人脸特征失败");
            return;
        }
        verifyFlag = true;
        detectFlag = true;
    }

    /** 按两次返回退出程序 */
    private long mExitTime;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出程序",Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /** 画人脸框 */
    private void drawFaceRect(FaceInfo[] faceInfos, Canvas canvas, int len) {
        float[] startArrayX = new float[len];
        float[] startArrayY = new float[len];
        float[] stopArrayX = new float[len];
        float[] stopArrayY = new float[len];
        for (int i=0; i<len; i++) {
            startArrayX[i] = (svWidth - faceInfos[i].x * zoomRate);
            startArrayY[i] = (faceInfos[i].y * zoomRate);
            stopArrayX[i] = (svWidth - faceInfos[i].x * zoomRate - faceInfos[i].width * zoomRate);
            stopArrayY[i] = (faceInfos[i].y * zoomRate + faceInfos[i].height * zoomRate);
        }
        canvasDrawLine(canvas, len, startArrayX, startArrayY, stopArrayX, stopArrayY);
    }

    /** 画线 */
    private void canvasDrawLine(Canvas canvas,int iNum,float[] startArrayX, float[] startArrayY,
                                float[] stopArrayX, float[] stopArrayY) {
        try {
            int iLen  = 50;
            Paint mPaint = new Paint();
            mPaint.setColor(getResources().getColor(R.color.gold));
            mPaint.setStrokeWidth(4);// 设置画笔粗细
            mPaint.setTextSize(50.0f);

            float startX, startY, stopX, stopY;
            for (int i = 0; i < iNum; i++) {
                startX = startArrayX[i];
                startY = startArrayY[i];
                stopX = stopArrayX[i];
                stopY = stopArrayY[i];
                canvas.drawLine(startX, startY, startX - iLen, startY, mPaint);
                canvas.drawLine(stopX + iLen, startY, stopX, startY, mPaint);

                canvas.drawLine(startX, startY, startX, startY + iLen, mPaint);
                canvas.drawLine(startX, stopY - iLen, startX, stopY, mPaint);

                canvas.drawLine(stopX, stopY, stopX, stopY - iLen, mPaint);
                canvas.drawLine(stopX, startY + iLen, stopX, startY, mPaint);

                canvas.drawLine(stopX, stopY, stopX + iLen, stopY, mPaint);
                canvas.drawLine(startX - iLen, stopY, startX, stopY, mPaint);
            }
        }
        catch(Exception e){}
    }
}
