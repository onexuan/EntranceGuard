//
// Created by wby on 2018/03/21.
//

/*
 * TO DO : change the P-net and update the generat box
 */

#include <android/log.h>
#include "mtcnn.h"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__)

bool cmpScore(Bbox lsh, Bbox rsh) {
    if (lsh.score < rsh.score)
        return true;
    else
        return false;
}

char* TAG = "MTCNN";
//MTCNN::MTCNN(){}
MTCNN::MTCNN(const string &model_path) {

    std::vector<std::string> param_files = {
            model_path+"/det1.param",
            model_path+"/det2.param",
            model_path+"/det3.param",
            model_path+"/y1.param",
            model_path+"/fage.params",
            model_path+"/fgender.params"
    };

    std::vector<std::string> bin_files = {
            model_path+"/det1.bin",
            model_path+"/det2.bin",
            model_path+"/det3.bin",
            model_path+"/y1.bin",
            model_path+"/fage.bin",
            model_path+"/fgender.bin"
    };

    Pnet.load_param(param_files[0].data());
    Pnet.load_model(bin_files[0].data());
    Rnet.load_param(param_files[1].data());
    Rnet.load_model(bin_files[1].data());
    Onet.load_param(param_files[2].data());
    Onet.load_model(bin_files[2].data());
    Facever.load_param(param_files[3].data());
    Facever.load_model(bin_files[3].data());
    Fage.load_param(param_files[4].data());
    Fage.load_model(bin_files[4].data());
    Fgender.load_param(param_files[5].data());
    Fgender.load_model(bin_files[5].data());
}

MTCNN::MTCNN(const std::vector<std::string> param_files, const std::vector<std::string> bin_files){
    Pnet.load_param(param_files[0].data());
    Pnet.load_model(bin_files[0].data());
    Rnet.load_param(param_files[1].data());
    Rnet.load_model(bin_files[1].data());
    Onet.load_param(param_files[2].data());
    Onet.load_model(bin_files[2].data());
    Facever.load_param(param_files[3].data());
    Facever.load_model(bin_files[3].data());
    Fage.load_param(param_files[4].data());
    Fage.load_model(bin_files[4].data());
    Fgender.load_param(param_files[5].data());
    Fgender.load_model(bin_files[5].data());
}


MTCNN::~MTCNN(){
    Pnet.clear();
    Rnet.clear();
    Onet.clear();
    Facever.clear();
    Fage.clear();
    Fgender.clear();
}
void MTCNN::SetMinFace(int minSize){
    minsize = minSize;
}
void MTCNN::generateBbox(ncnn::Mat score, ncnn::Mat location, std::vector<Bbox>& boundingBox_, float scale){
    const int stride = 2;
    const int cellsize = 12;
    //score p
    float *p = score.channel(1);//score.data + score.cstep;
    //float *plocal = location.data;
    Bbox bbox;
    float inv_scale = 1.0f/scale;
    for(int row=0;row<score.h;row++){
        for(int col=0;col<score.w;col++){
            if(*p>threshold[0]){
                bbox.score = *p;
                bbox.x1 = round((stride*col+1)*inv_scale);
                bbox.y1 = round((stride*row+1)*inv_scale);
                bbox.x2 = round((stride*col+1+cellsize)*inv_scale);
                bbox.y2 = round((stride*row+1+cellsize)*inv_scale);
                bbox.area = (bbox.x2 - bbox.x1) * (bbox.y2 - bbox.y1);
                const int index = row * score.w + col;
                for(int channel=0;channel<4;channel++){
                    bbox.regreCoord[channel]=location.channel(channel)[index];
                }
                boundingBox_.push_back(bbox);
            }
            p++;
            //plocal++;
        }
    }
}
void MTCNN::nms(std::vector<Bbox> &boundingBox_, const float overlap_threshold, string modelname){
    if(boundingBox_.empty()){
        return;
    }
    sort(boundingBox_.begin(), boundingBox_.end(), cmpScore);
    float IOU = 0;
    float maxX = 0;
    float maxY = 0;
    float minX = 0;
    float minY = 0;
    std::vector<int> vPick;
    int nPick = 0;
    std::multimap<float, int> vScores;
    const int num_boxes = boundingBox_.size();
    vPick.resize(num_boxes);
    for (int i = 0; i < num_boxes; ++i){
        vScores.insert(std::pair<float, int>(boundingBox_[i].score, i));
    }
    while(vScores.size() > 0){
        int last = vScores.rbegin()->second;
        vPick[nPick] = last;
        nPick += 1;
        for (std::multimap<float, int>::iterator it = vScores.begin(); it != vScores.end();){
            int it_idx = it->second;
            maxX = std::max(boundingBox_.at(it_idx).x1, boundingBox_.at(last).x1);
            maxY = std::max(boundingBox_.at(it_idx).y1, boundingBox_.at(last).y1);
            minX = std::min(boundingBox_.at(it_idx).x2, boundingBox_.at(last).x2);
            minY = std::min(boundingBox_.at(it_idx).y2, boundingBox_.at(last).y2);
            //maxX1 and maxY1 reuse
            maxX = ((minX-maxX+1)>0)? (minX-maxX+1) : 0;
            maxY = ((minY-maxY+1)>0)? (minY-maxY+1) : 0;
            //IOU reuse for the area of two bbox
            IOU = maxX * maxY;
            if(!modelname.compare("Union"))
                IOU = IOU/(boundingBox_.at(it_idx).area + boundingBox_.at(last).area - IOU);
            else if(!modelname.compare("Min")){
                IOU = IOU/((boundingBox_.at(it_idx).area < boundingBox_.at(last).area)? boundingBox_.at(it_idx).area : boundingBox_.at(last).area);
            }
            if(IOU > overlap_threshold){
                it = vScores.erase(it);
            }else{
                it++;
            }
        }
    }

    vPick.resize(nPick);
    std::vector<Bbox> tmp_;
    tmp_.resize(nPick);
    for(int i = 0; i < nPick; i++){
        tmp_[i] = boundingBox_[vPick[i]];
    }
    boundingBox_ = tmp_;
}
void MTCNN::refine(vector<Bbox> &vecBbox, const int &height, const int &width, bool square){
    if(vecBbox.empty()){
        cout<<"Bbox is empty!!"<<endl;
        return;
    }
    float bbw=0, bbh=0, maxSide=0;
    float h = 0, w = 0;
    float x1=0, y1=0, x2=0, y2=0;
    for(vector<Bbox>::iterator it=vecBbox.begin(); it!=vecBbox.end();it++){
        bbw = (*it).x2 - (*it).x1 + 1;
        bbh = (*it).y2 - (*it).y1 + 1;
        x1 = (*it).x1 + (*it).regreCoord[0]*bbw;
        y1 = (*it).y1 + (*it).regreCoord[1]*bbh;
        x2 = (*it).x2 + (*it).regreCoord[2]*bbw;
        y2 = (*it).y2 + (*it).regreCoord[3]*bbh;



        if(square){
            w = x2 - x1 + 1;
            h = y2 - y1 + 1;
            maxSide = (h>w)?h:w;
            x1 = x1 + w*0.5 - maxSide*0.5;
            y1 = y1 + h*0.5 - maxSide*0.5;
            (*it).x2 = round(x1 + maxSide - 1);
            (*it).y2 = round(y1 + maxSide - 1);
            (*it).x1 = round(x1);
            (*it).y1 = round(y1);
        }

        //boundary check
        if((*it).x1<0)(*it).x1=0;
        if((*it).y1<0)(*it).y1=0;
        if((*it).x2>width)(*it).x2 = width - 1;
        if((*it).y2>height)(*it).y2 = height - 1;

        it->area = (it->x2 - it->x1)*(it->y2 - it->y1);
    }
}
void MTCNN::PNet(){
    firstBbox_.clear();
    float minl = img_w < img_h? img_w: img_h;
    float m = (float)MIN_DET_SIZE/minsize;
    minl *= m;
    float factor = pre_facetor;
    vector<float> scales_;
    while(minl>MIN_DET_SIZE){
        scales_.push_back(m);
        minl *= factor;
        m = m*factor;
    }
    for (size_t i = 0; i < scales_.size(); i++) {
        int hs = (int)ceil(img_h*scales_[i]);
        int ws = (int)ceil(img_w*scales_[i]);
        ncnn::Mat in;
        resize_bilinear(img, in, ws, hs);
        ncnn::Extractor ex = Pnet.create_extractor();
        ex.set_num_threads(4);
        ex.set_light_mode(true);
        ex.input("data", in);
        ncnn::Mat score_, location_;
        ex.extract("prob1", score_);
        ex.extract("conv4-2", location_);
        std::vector<Bbox> boundingBox_;
        generateBbox(score_, location_, boundingBox_, scales_[i]);
        nms(boundingBox_, nms_threshold[0]);
        firstBbox_.insert(firstBbox_.end(), boundingBox_.begin(), boundingBox_.end());
        boundingBox_.clear();
    }
}
void MTCNN::RNet(){
    secondBbox_.clear();
    int count = 0;
    for(vector<Bbox>::iterator it=firstBbox_.begin(); it!=firstBbox_.end();it++){
        ncnn::Mat tempIm;
        copy_cut_border(img, tempIm, (*it).y1, img_h-(*it).y2, (*it).x1, img_w-(*it).x2);
        ncnn::Mat in;
        resize_bilinear(tempIm, in, 24, 24);
        ncnn::Extractor ex = Rnet.create_extractor();
        ex.set_num_threads(4);
        ex.set_light_mode(true);
        ex.input("data", in);
        ncnn::Mat score, bbox;
        ex.extract("prob1", score);
        ex.extract("conv5-2", bbox);
        if((float)score[1] > threshold[1]){
            for(int channel=0;channel<4;channel++){
                it->regreCoord[channel]=(float)bbox[channel];//*(bbox.data+channel*bbox.cstep);
            }
            it->area = (it->x2 - it->x1)*(it->y2 - it->y1);
            it->score = score.channel(1)[0];//*(score.data+score.cstep);
            secondBbox_.push_back(*it);
        }
    }
}
void MTCNN::ONet(){
    thirdBbox_.clear();
    for(vector<Bbox>::iterator it=secondBbox_.begin(); it!=secondBbox_.end();it++){
        ncnn::Mat tempIm;
        copy_cut_border(img, tempIm, (*it).y1, img_h-(*it).y2, (*it).x1, img_w-(*it).x2);
        ncnn::Mat in;
        resize_bilinear(tempIm, in, 48, 48);
        ncnn::Extractor ex = Onet.create_extractor();
        ex.set_num_threads(4);
        ex.set_light_mode(true);
        ex.input("data", in);
        ncnn::Mat score, bbox, keyPoint;
        ex.extract("prob1", score);
        ex.extract("conv6-2", bbox);
        ex.extract("conv6-3", keyPoint);
        if((float)score[1] > threshold[2]){
            for(int channel = 0; channel < 4; channel++){
                it->regreCoord[channel]=(float)bbox[channel];
            }
            it->area = (it->x2 - it->x1) * (it->y2 - it->y1);
            it->score = score.channel(1)[0];
            for(int num=0;num<5;num++){
                (it->ppoint)[num] = it->x1 + (it->x2 - it->x1) * keyPoint[num];
                (it->ppoint)[num+5] = it->y1 + (it->y2 - it->y1) * keyPoint[num+5];
            }

            thirdBbox_.push_back(*it);
        }
    }
}
void MTCNN::detect(ncnn::Mat& img_, std::vector<Bbox>& finalBbox_){
    img = img_;
    img_w = img.w;
    img_h = img.h;
    img.substract_mean_normalize(mean_vals, norm_vals);

    PNet();
    //the first stage's nms
    if(firstBbox_.size() < 1) return;
    nms(firstBbox_, nms_threshold[0]);
    refine(firstBbox_, img_h, img_w, true);
    printf("firstBbox_.size()=%d\n", firstBbox_.size());


    //second stage
    RNet();
    printf("secondBbox_.size()=%d\n", secondBbox_.size());
    if(secondBbox_.size() < 1) return;
    nms(secondBbox_, nms_threshold[1]);
    refine(secondBbox_, img_h, img_w, true);

    //third stage 
    ONet();
    printf("thirdBbox_.size()=%d\n", thirdBbox_.size());
    if(thirdBbox_.size() < 1) return;
    refine(thirdBbox_, img_h, img_w, true);
    nms(thirdBbox_, nms_threshold[2], "Min");
    finalBbox_ = thirdBbox_;
}

void MTCNN::FaceVer(ncnn::Mat& tempIn, std::vector<float> &out_vec){
//    out_vec.clear();
    ncnn::Mat in, fc_out;

    in = tempIn.clone();
//    ncnn::resize_bilinear(tempIn, in, 112, 112);

    const float mean_vals[3] = {127.5f, 127.5f, 127.5f};
    const float std_vals[3] = {0.0078125f, 0.0078125f, 0.0078125f};
    in.substract_mean_normalize(mean_vals, std_vals);

    ncnn::Extractor ex = Facever.create_extractor();
    ex.set_num_threads(4);
    ex.set_light_mode(true);
    ex.input("data", in);
    ex.extract("pre_fc1", fc_out);
    for(int i = 0; i < fc_out.w; i++)
    {
        out_vec.push_back(fc_out[i]);
    }
}

void MTCNN::FAge(ncnn::Mat& tempIn, std::vector<float> &vector) {
    ncnn::Mat in,out;
    in = tempIn;
    ncnn::Extractor ex = Fage.create_extractor();
    ex.set_num_threads(4);
    ex.set_light_mode(true);
    ex.input("data", in);
    ex.extract("pre_fc1", out);
    for(int i = 0;i<out.w;i++)
    {
        vector.push_back(out[i]);
    }


}

void MTCNN::FGender(ncnn::Mat& tempIn, std::vector<float> &vector) {
    ncnn::Mat in,out;
    in = tempIn;
    ncnn::Extractor ex = Fgender.create_extractor();
    ex.set_num_threads(4);
    ex.set_light_mode(true);
    ex.input("data", in);
    ex.extract("pre_fc1", out);
    for(int i = 0;i<out.w;i++)
    {
        vector.push_back(out[i]);
    }
}
//void MTCNN::detection(const cv::Mat& img, std::vector<cv::Rect>& rectangles){
//    ncnn::Mat ncnn_img = ncnn::Mat::from_pixels(img.data, ncnn::Mat::PIXEL_BGR2RGB, img.cols, img.rows);
//    std::vector<Bbox> finalBbox;
//    detect(ncnn_img, finalBbox);
//    const int num_box = finalBbox.size();
//    rectangles.resize(num_box);
//    for(int i = 0; i < num_box; i++){
//        rectangles[i] = cv::Rect(finalBbox[i].x1, finalBbox[i].y1, finalBbox[i].x2 - finalBbox[i].x1 + 1, finalBbox[i].y2 - finalBbox[i].y1 + 1);
//    }
//}
