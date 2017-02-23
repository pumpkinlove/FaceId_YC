package com.miaxis.faceid_yc.utils;

import android.os.Environment;

/**
 * Created by xu.nan on 2017/2/15.
 */

public class Constants {

    public static final String[] FOLK = { "汉", "蒙古", "回", "藏", "维吾尔", "苗", "彝", "壮", "布依", "朝鲜",
            "满", "侗", "瑶", "白", "土家", "哈尼", "哈萨克", "傣", "黎", "傈僳", "佤", "畲",
            "高山", "拉祜", "水", "东乡", "纳西", "景颇", "柯尔克孜", "土", "达斡尔", "仫佬", "羌",
            "布朗", "撒拉", "毛南", "仡佬", "锡伯", "阿昌", "普米", "塔吉克", "怒", "乌孜别克",
            "俄罗斯", "鄂温克", "德昂", "保安", "裕固", "京", "塔塔尔", "独龙", "鄂伦春", "赫哲",
            "门巴", "珞巴", "基诺", "", "", "穿青人", "家人", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "", "", "", "", "其他", "外国血统", "",
            "" };

    public static final String ID_PHOTO_PATH = Environment.getExternalStorageDirectory().getPath();
    public static final String ID_PHOTO_NAME = "id_photo.bmp";

    // 人脸检测最大数
    public static final int MAX_FACE_NUM        = 5;
    // 图像、特征临时文件名
    public static final String strImgFile1 = "face_temp_1.jpg";
    public static final String strImgFile2 = "face_temp_2.jpg";
    public static final String strTzFile1  = "face_tz_1.dat";
    public static final String strTzFile2  = "face_tz_2.dat";
    // 界面显示图像大小
    public static final int iImgShowWidth  = 128;
    public static final int iImgShowHeight = 128;
}
