#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>

// ncnn
#include "net.h"

#include "mtcnn.h"
using namespace std;
#define TAG "MtcnnSo"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__)
static MTCNN *mtcnn;

//sdk是否初始化成功
bool detection_sdk_init_ok = false;

const int resize_ratio = 1;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_deepthink_entranceguard_utils_MTCNN_FaceDetectionModelInit(JNIEnv *env, jobject instance,
                                                        jstring faceDetectionModelPath_) {
    LOGD("JNI开始人脸检测模型初始化");
    //如果已初始化则直接返回
    if (detection_sdk_init_ok) {
        //  LOGD("人脸检测模型已经导入");
        return true;
    }
    jboolean tRet = false;
    if (NULL == faceDetectionModelPath_) {
        //   LOGD("导入的人脸检测的目录为空");
        return tRet;
    }

    //获取MTCNN模型的绝对路径的目录（不是/aaa/bbb.bin这样的路径，是/aaa/)
    const char *faceDetectionModelPath = env->GetStringUTFChars(faceDetectionModelPath_, 0);
    if (NULL == faceDetectionModelPath) {
        return tRet;
    }

    string tFaceModelDir = faceDetectionModelPath;
    string tLastChar = tFaceModelDir.substr(tFaceModelDir.length() - 1, 1);
    //LOGD("init, tFaceModelDir last =%s", tLastChar.c_str());
    //目录补齐/
    if ("\\" == tLastChar) {
        tFaceModelDir = tFaceModelDir.substr(0, tFaceModelDir.length() - 1) + "/";
    } else if (tLastChar != "/") {
        tFaceModelDir += "/";
    }
    LOGD("init, tFaceModelDir=%s", tFaceModelDir.c_str());

    //没判断是否正确导入，懒得改了
    mtcnn = new MTCNN(tFaceModelDir);
    mtcnn->SetMinFace(40);

    env->ReleaseStringUTFChars(faceDetectionModelPath_, faceDetectionModelPath);
    detection_sdk_init_ok = true;
    tRet = true;
    return tRet;
}

JNIEXPORT jintArray JNICALL
Java_com_deepthink_entranceguard_utils_MTCNN_FaceDetect(JNIEnv *env, jobject instance, jbyteArray imageDate_,
                                                 jint imageWidth, jint imageHeight, jint imageChannel) {
    //  LOGD("JNI开始检测人脸");
    if(!detection_sdk_init_ok){
        LOGD("人脸检测MTCNN模型SDK未初始化，直接返回空");
        return NULL;
    }

    int tImageDateLen = env->GetArrayLength(imageDate_);
    if(imageChannel == tImageDateLen / imageWidth / imageHeight){
        LOGD("数据宽=%d,高=%d,通道=%d",imageWidth,imageHeight,imageChannel);
    }
    else{
        LOGD("数据长宽高通道不匹配，直接返回空");
        return NULL;
    }

    jbyte *imageDate = env->GetByteArrayElements(imageDate_, NULL);
    if (NULL == imageDate){
        LOGD("导入数据为空，直接返回空");
        return NULL;
    }

    if(imageWidth<20||imageHeight<20){
        LOGD("导入数据的宽和高小于20，直接返回空");
        return NULL;
    }

    //TODO 通道需测试
    if(3 == imageChannel || 4 == imageChannel){
        //图像通道数只能是3或4；
    }else{
        LOGD("图像通道数只能是3或4，直接返回空");
        return NULL;
    }

    int32_t minFaceSize=40;
    mtcnn->SetMinFace(minFaceSize);

    unsigned char *faceImageCharDate = (unsigned char*)imageDate;
    ncnn::Mat ncnn_img;
    if(imageChannel==3) {
        ncnn_img = ncnn::Mat::from_pixels(faceImageCharDate, ncnn::Mat::PIXEL_BGR2RGB,
                                          imageWidth, imageHeight);
    }else{
        ncnn_img = ncnn::Mat::from_pixels(faceImageCharDate, ncnn::Mat::PIXEL_RGBA2RGB, imageWidth, imageHeight);
    }

    ncnn::Mat ncnn_img_resize;
    ncnn::resize_bilinear(ncnn_img, ncnn_img_resize, imageWidth / resize_ratio, imageHeight / resize_ratio);

    std::vector<Bbox> finalBbox;
    mtcnn->detect(ncnn_img_resize, finalBbox);

    int32_t num_face = static_cast<int32_t>(finalBbox.size());
    LOGD("检测到的人脸数目：%d\n", num_face);

    int out_size = 1+num_face*15;
    //  LOGD("内部人脸检测完成,开始导出数据");
    int *faceInfo = new int[out_size];
    faceInfo[0] = num_face;
    for(int i=0;i<num_face;i++){
        faceInfo[15*i+1] = finalBbox[i].x1 * resize_ratio;//left
        faceInfo[15*i+2] = finalBbox[i].y1 * resize_ratio;//top
        faceInfo[15*i+3] = min(finalBbox[i].x2 * resize_ratio,imageWidth - 1);//right
        faceInfo[15*i+4] = min(finalBbox[i].y2 * resize_ratio, imageHeight - 1);//bottom
        for (int j = 0; j < 10; j++){
            faceInfo[15*i+5+j] = static_cast<int>(finalBbox[i].ppoint[j]) * resize_ratio;
        }

        int x_1 = static_cast<int>(finalBbox[i].ppoint[0]);
        int y_1 = static_cast<int>(finalBbox[i].ppoint[5]);
        int x_2 = static_cast<int>(finalBbox[i].ppoint[1]);
        int y_2 = static_cast<int>(finalBbox[i].ppoint[6]);
        int x_3 = static_cast<int>(finalBbox[i].ppoint[2]);
        int y_3 = static_cast<int>(finalBbox[i].ppoint[7]);
//        float x_4 = finalBbox[i].ppoint[3];
//        float y_4 = finalBbox[i].ppoint[8];
//        float x_5 = finalBbox[i].ppoint[4];
//        float y_5 = finalBbox[i].ppoint[10];

        float dst_1 = sqrt(static_cast<float>((x_1 - x_3)*(x_1 - x_3)+(y_1 - y_3)*(y_1 - y_3)));
        float dst_2 = sqrt(static_cast<float>((x_2 - x_3)*(x_2 - x_3)+(y_2 - y_3)*(y_2 - y_3)));

        float dst_3 = sqrt(static_cast<float>((x_2 - x_1)*(x_2 - x_1)+(y_2 - y_1)*(y_2 - y_1)));

        float score = fabs(dst_1 - dst_2)*1000 / dst_3;

        LOGD("dst1: %f\n",dst_1);
        LOGD("dst2: %f\n",dst_2);

        LOGD("score: %f\n",score);

        faceInfo[15*i+5+10] = static_cast<int>(score);
        LOGD("faceInfo[15*i+5+10]: %d\n",faceInfo[15*i+5+10]);


    }

    jintArray tFaceInfo = env->NewIntArray(out_size);
    env->SetIntArrayRegion(tFaceInfo,0,out_size,faceInfo);
    //  LOGD("内部人脸检测完成,导出数据成功");
    delete[] faceInfo;
    env->ReleaseByteArrayElements(imageDate_, imageDate, 0);
    return tFaceInfo;
}

JNIEXPORT jintArray JNICALL
Java_com_deepthink_entranceguard_utils_MTCNN_FaceInfo(JNIEnv *env, jobject instance, jbyteArray imageDate_,
                                                  jint imageWidth, jint imageHeight, jint imageChannel) {
    //  LOGD("JNI开始检测人脸");
    if(!detection_sdk_init_ok){
        LOGD("人脸检测MTCNN模型SDK未初始化，直接返回空");
        return NULL;
    }

    int tImageDateLen = env->GetArrayLength(imageDate_);
    if(imageChannel == tImageDateLen / imageWidth / imageHeight){
        LOGD("数据宽=%d,高=%d,通道=%d",imageWidth,imageHeight,imageChannel);
    }
    else{
        LOGD("数据长宽高通道不匹配，直接返回空");
        return NULL;
    }

    jbyte *imageDate = env->GetByteArrayElements(imageDate_, NULL);
    if (NULL == imageDate){
        LOGD("导入数据为空，直接返回空");
        return NULL;
    }

    if(imageWidth<20||imageHeight<20){
        LOGD("导入数据的宽和高小于20，直接返回空");
        return NULL;
    }

    //TODO 通道需测试
    if(3 == imageChannel || 4 == imageChannel){
        //图像通道数只能是3或4；
    }else{
        LOGD("图像通道数只能是3或4，直接返回空");
        return NULL;
    }

    int32_t minFaceSize=40;
    mtcnn->SetMinFace(minFaceSize);

    unsigned char *faceImageCharDate = (unsigned char*)imageDate;
    ncnn::Mat ncnn_img;
    if(imageChannel==3) {
        ncnn_img = ncnn::Mat::from_pixels(faceImageCharDate, ncnn::Mat::PIXEL_BGR2RGB,
                                          imageWidth, imageHeight);
    }else{
        ncnn_img = ncnn::Mat::from_pixels(faceImageCharDate, ncnn::Mat::PIXEL_RGBA2RGB, imageWidth, imageHeight);
    }

    ncnn::Mat ncnn_img_resize;
    ncnn::resize_bilinear(ncnn_img, ncnn_img_resize, 112, 112);

    std::vector<float> ageRet, genderRet;
    mtcnn->FAge(ncnn_img_resize, ageRet);
    mtcnn->FGender(ncnn_img_resize, genderRet);

    int* info = new int[2];
    info[0] = 0;

    float sum = 0, age = 0;
    for (int i = 0; i < ageRet.size(); ++i) {
        sum += exp(ageRet[i]);
    }

    int tmp = 5;
    for (int i = 0; i < ageRet.size(); ++i) {
        age += tmp * (exp(ageRet[i]) / sum);
        tmp += 10;
    }


    info[0] = int(age);
    info[1] = (genderRet[0] > genderRet[1]) ? 0 : 1;

    jintArray tInfo = env->NewIntArray(2);
    env->SetIntArrayRegion(tInfo, 0, 2, info);
    env->ReleaseByteArrayElements(imageDate_, imageDate, 0);
    return tInfo;
}

JNIEXPORT jfloatArray JNICALL
Java_com_deepthink_entranceguard_utils_MTCNN_FaceFeature(JNIEnv *env, jobject instance, jbyteArray imageDate_,
                                                  jint imageWidth, jint imageHeight, jint imageChannel) {
    //  LOGD("JNI开始检测人脸");
    if(!detection_sdk_init_ok){
        LOGD("人脸检测MTCNN模型SDK未初始化，直接返回空");
        return NULL;
    }

    int tImageDateLen = env->GetArrayLength(imageDate_);
    if(imageChannel == tImageDateLen / imageWidth / imageHeight){
        LOGD("数据宽=%d,高=%d,通道=%d",imageWidth,imageHeight,imageChannel);
    }
    else{
        LOGD("数据长宽高通道不匹配，直接返回空");
        return NULL;
    }

    jbyte *imageDate = env->GetByteArrayElements(imageDate_, NULL);
    if (NULL == imageDate){
        LOGD("导入数据为空，直接返回空");
        return NULL;
    }

    if(imageWidth<20||imageHeight<20){
        LOGD("导入数据的宽和高小于20，直接返回空");
        return NULL;
    }

    //TODO 通道需测试
    if(3 == imageChannel || 4 == imageChannel){
        //图像通道数只能是3或4；
    }else{
        LOGD("图像通道数只能是3或4，直接返回空");
        return NULL;
    }

    unsigned char *faceImageCharDate = (unsigned char*)imageDate;
    ncnn::Mat ncnn_img;
    if(imageChannel==3) {
        ncnn_img = ncnn::Mat::from_pixels(faceImageCharDate, ncnn::Mat::PIXEL_BGR2RGB,
                                          imageWidth, imageHeight);
    }else{
        ncnn_img = ncnn::Mat::from_pixels(faceImageCharDate, ncnn::Mat::PIXEL_RGBA2RGB, imageWidth, imageHeight);
    }

    ncnn::Mat ncnn_img_resize;
    ncnn::resize_bilinear(ncnn_img, ncnn_img_resize, 112, 112);

    std::vector<float> faceRet;
    mtcnn->FaceVer(ncnn_img_resize, faceRet);

    jfloatArray tInfo = env->NewFloatArray(512);
    if (faceRet.empty() == false) {
        env->SetFloatArrayRegion(tInfo, 0, 512, faceRet.data());
    }
    env->ReleaseByteArrayElements(imageDate_, imageDate, 0);
    return tInfo;
}

JNIEXPORT jboolean JNICALL
Java_com_deepthink_entranceguard_utils_MTCNN_FaceDetectionModelUnInit(JNIEnv *env, jobject instance) {
    if(!detection_sdk_init_ok){
        LOGD("人脸检测MTCNN模型已经释放过或者未初始化");
        return true;
    }
    jboolean tDetectionUnInit = false;
    delete mtcnn;


    detection_sdk_init_ok=false;
    tDetectionUnInit = true;
    LOGD("人脸检测初始化锁，重新置零");
    return tDetectionUnInit;

}

}
