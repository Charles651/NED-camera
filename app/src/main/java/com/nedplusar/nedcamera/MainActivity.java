package com.nedplusar.nedcamera;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by NEDPlusAR on 2017/3/9.
 */

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private ImageView back, position;//返回和切换前后置摄像头
    private SurfaceView surface;
    private ImageButton shutter;//快门
    private SurfaceHolder holder;
    private Camera camera;//声明相机
    private String filepath = "";//照片保存路径
    private int cameraPosition = 1;//0代表前置摄像头，1代表后置摄像头

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//没有标题
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//拍照过程屏幕一直处于高亮
        //设置手机屏幕朝向，一共有7种
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //SCREEN_ORIENTATION_BEHIND： 继承Activity堆栈中当前Activity下面的那个Activity的方向
        //SCREEN_ORIENTATION_LANDSCAPE： 横屏(风景照) ，显示时宽度大于高度
        //SCREEN_ORIENTATION_PORTRAIT： 竖屏 (肖像照) ， 显示时高度大于宽度
        //SCREEN_ORIENTATION_SENSOR  由重力感应器来决定屏幕的朝向,它取决于用户如何持有设备,当设备被旋转时方向会随之在横屏与竖屏之间变化
        //SCREEN_ORIENTATION_NOSENSOR： 忽略物理感应器——即显示方向与物理感应器无关，不管用户如何旋转设备显示方向都不会随着改变("unspecified"设置除外)
        //SCREEN_ORIENTATION_UNSPECIFIED： 未指定，此为默认值，由Android系统自己选择适当的方向，选择策略视具体设备的配置情况而定，因此不同的设备会有不同的方向选择
        //SCREEN_ORIENTATION_USER： 用户当前的首选方向

        setContentView(R.layout.main);

        back = (ImageView) findViewById(R.id.camera_back);
        position = (ImageView) findViewById(R.id.camera_position);
        surface = (SurfaceView) findViewById(R.id.camera_surface);
        shutter = (ImageButton) findViewById(R.id.camera_shutter);
        holder = surface.getHolder();//获得句柄
        holder.addCallback(this);//添加回调
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//surfaceview不维护自己的缓冲区，等待屏幕渲染引擎将内容推送到用户面前

        //设置监听
        back.setOnClickListener(listener);
        position.setOnClickListener(listener);
        shutter.setOnClickListener(listener);
    }

    private void setCameraPosition() {
        //切换前后摄像头
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数
        Log.d("Camera", "Camera count: " + cameraCount);

        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if (cameraPosition == 1) {
                //现在是后置，变更为前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    camera.stopPreview();//停掉原来摄像头的预览
                    camera.release();//释放资源
                    camera = null;//取消原来摄像头
                    camera = Camera.open(i);//打开当前选中的摄像头
                    try {
                        camera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    camera.startPreview();//开始预览
                    cameraPosition = 0;
                    break;
                }
            } else {
                //现在是前置， 变更为后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    camera.stopPreview();//停掉原来摄像头的预览
                    camera.release();//释放资源
                    camera = null;//取消原来摄像头
                    camera = Camera.open(i);//打开当前选中的摄像头
                    try {
                        camera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    camera.startPreview();//开始预览
                    cameraPosition = 1;
                    break;
                }
            }
        }
    }

    //响应点击事件
    OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch (v.getId()) {
                case R.id.camera_back:
                    //返回
                    MainActivity.this.finish();
                    break;

                case R.id.camera_position:
                    setCameraPosition();
                    break;
                case R.id.camera_shutter:
                    //快门
                    camera.autoFocus(new AutoFocusCallback() {//自动对焦
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            // TODO Auto-generated method stub
                            if (success) {
                                //设置参数，并拍照
                                Parameters params = camera.getParameters();
                                params.setPictureFormat(PixelFormat.JPEG);//图片格式
                                params.setPreviewSize(800, 480);//图片大小
                                camera.setParameters(params);//将参数设置到我的camera
                                camera.takePicture(null, null, jpeg);//将拍摄到的照片给自定义的对象
                                Log.d("NEDCamera", "Take picture: " + params);
                            }
                        }
                    });
                    break;
            }
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 21 || keyCode == 22) {
            setCameraPosition();
            return true;
        }
        if (keyCode == 130 || keyCode == 0) {
            camera.autoFocus(new AutoFocusCallback() {//自动对焦
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    // TODO Auto-generated method stub
                    if (success) {
                        //设置参数，并拍照
                        Parameters params = camera.getParameters();
                        params.setPictureFormat(PixelFormat.JPEG);//图片格式
                        params.setPreviewSize(800, 480);//图片大小
                        camera.setParameters(params);//将参数设置到我的camera
                        camera.takePicture(null, null, jpeg);//将拍摄到的照片给自定义的对象
                        Log.d("NEDCamera", "Take picture: " + params);
                    }
                }
            });

        }
        return super.onKeyDown(keyCode, event);
    }

/*surfaceHolder他是系统提供的一个用来设置surfaceView的一个对象，而它通过surfaceView.getHolder()这个方法来获得。
     Camera提供一个setPreviewDisplay(SurfaceHolder)的方法来连接*/

    //SurfaceHolder.Callback,这是个holder用来显示surfaceView 数据的接口,他必须实现以下3个方法
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // TODO Auto-generated method stub
        //当surfaceview创建时开启相机
        if (camera == null) {
            camera = Camera.open();
            try {
                camera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
                camera.startPreview();//开始预览
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        camera.stopPreview();
        camera.release();
        camera = null;
        holder = null;
        surface = null;
    }

    PictureCallback jpeg = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
//            File pictureFile = new File("/sdcard/" + System.currentTimeMillis()
//                    + ".jpg");
//            try {
//                FileOutputStream fos = new FileOutputStream(pictureFile);
//                fos.write(data);
//                fos.close();
//            } catch (Exception e) {
//                Log.d("NEDCamera", "保存图片失败");
//            }
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                //自定义文件保存路径  以拍摄时间区分命名
                filepath = "/sdcard/DCIM/Camera/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
                File file = new File(filepath);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);//将图片压缩的流里面
                bos.write(data);
                bos.flush();// 刷新此缓冲区的输出流
                bos.close();// 关闭此输出流并释放与此流有关的所有系统资源
                camera.stopPreview();//关闭预览 处理数据
                camera.startPreview();//数据处理完后继续开始预览
                bitmap.recycle();//回收bitmap空间
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.d("NEDCamera", "保存图片失败");
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }
}
