package com.aserbao.aserbaosandroid.opengl.openGlCamera.simpleCameraOpengl.simpleCamera;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.aserbao.aserbaosandroid.R;

import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 使用SurfaceView预览Camera数据
 */
public class CameraSurfaceViewShowActivity extends AppCompatActivity implements SurfaceHolder
        .Callback {
    @BindView(R.id.mSurface)
    SurfaceView mSurfaceView;

    public SurfaceHolder mHolder;
    @BindView(R.id.gl_surface_view)
    GLSurfaceView mGlSurfaceView;
    private Camera mCamera;
    private Camera.Parameters mParameters;
    private MyRender render;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_camera);
        ButterKnife.bind(this);
        initGlSurfaceView();
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void initGlSurfaceView() {
        mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGlSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
//        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        render = new MyRender();
        mGlSurfaceView.setRenderer(render);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // Open the Camera in preview mode
            mCamera = Camera.open(0);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    try {
                        Camera.Size size = camera.getParameters().getPreviewSize();
                        processImage(data, size.width, size.height);

                        camera.addCallbackBuffer(data);
                    } catch (RuntimeException e) {
                        // The camera has probably just been released, ignore.
                    }


                }

                private void processImage(byte[] data, int width, int height) {
                    Mat mat = new Mat(((int) (height * 1.5)), width, CvType.CV_8UC1);//初始化一个矩阵,没数据
                    //从(0,0)开始放数据,直到data放完或者矩阵被填满
                    // (若是多通道,则把当前位置的通道全部填满，才继续下一个位置，data长度必须整除通道数).
                    mat.put(0, 0, data);
                    // 转灰度图
                    Mat grayMat1 = new Mat();
                    Imgproc.cvtColor(mat, grayMat1, Imgproc.COLOR_YUV420sp2GRAY);

                    // 二值化处理
                    Mat thresholdMat1 = new Mat();
                    Imgproc.threshold(grayMat1, thresholdMat1, 50, 255, Imgproc.THRESH_BINARY);

                    /* 获取matches */
                    MatOfKeyPoint matOfKeyPoint1 = new MatOfKeyPoint();
                    FastFeatureDetector featureDetector = FastFeatureDetector.create(
                            FastFeatureDetector.THRESHOLD,
                            true,
                            FastFeatureDetector.TYPE_9_16);
                    featureDetector.detect(thresholdMat1, matOfKeyPoint1);
                    KeyPoint[] keyPoints = matOfKeyPoint1.toArray();
                    Log.e("onPreviewFrame", "KeyPoint.length:" + keyPoints.length);
                    int length = keyPoints.length;
                    float[] v = new float[length * 3];
                    if (length > 0) {
                        for (int i = 0; i < keyPoints.length; i++) {
                            KeyPoint keyPoint = keyPoints[i];
                            float x = (float) ((keyPoint.pt.x - (1080 / 2)) / (1080 / 2)) * 1080
                                    / 1920;
                            float y = (float) ((1920 / 2) - keyPoint.pt.y) / (1920 / 2);
                            v[i * 3] = (float) rotateX(x,y,11) -0.43f;
                            v[i * 3 + 1] = (float) rotateY(x,y,11) +0.44f;

                        }
                    }
                    /*float[] v1 = {
                            0, 0, 0,
                            0.5f , 0, 0,
                            0.25f , 0, 0,
                            0.25f , 0.25f, 0,
                            0.5f , 0.5f, 0,
                            0,0.5f , 0,
                            0, 0.25f , 0
                    };
                    for (int i = 0; i < v1.length/3; i++) {
                        float x = v1[i * 3];
                        float y = v1[i * 3 +1];
                        v1[i * 3] = (float) rotateX(x,y,180);
                        v1[i * 3+1] = (float) rotateY(x,y,180);
                    }*/
                    render.refreshVerteices(v);
//                    mGlSurfaceView.requestRender();
                }
            });
            mCamera.startPreview();
        } catch (IOException e) {
        }
    }

    private static double rotateX(double x1, double y1, double alpha) {

        return x1 * Math.cos(alpha) - y1 * Math.sin(alpha);
    }

    private static double rotateY(double x1, double y1, double alpha) {

        return x1 * Math.sin(alpha) + y1 * Math.cos(alpha);
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("opengl", "surfaceChanged");
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    mParameters = mCamera.getParameters();
                    mParameters.setPictureFormat(PixelFormat.JPEG); //图片输出格式
//                    mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//预览持续发光
                    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    //持续对焦模式
                    mCamera.setParameters(mParameters);
                    mCamera.startPreview();
                    mCamera.cancelAutoFocus();
                }
            }
        });
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @OnClick(R.id.btn_change)
    public void onViewClicked() {
//        PropertyValuesHolder valuesHolder2 = PropertyValuesHolder.ofFloat("rotationX", 0.0f,
// 360.0f, 0.0F);
        PropertyValuesHolder valuesHolder = PropertyValuesHolder.ofFloat("rotationY", 0.0f,
                360.0f, 0.0F);
        PropertyValuesHolder valuesHolder1 = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.5f,
                1.0f);
        PropertyValuesHolder valuesHolder3 = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.5f,
                1.0f);
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(mSurfaceView,
                valuesHolder, valuesHolder1, valuesHolder3);
        objectAnimator.setDuration(5000).start();
    }
}
