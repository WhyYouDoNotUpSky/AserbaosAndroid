package com.aserbao.aserbaosandroid.opengl.openGlCamera.simpleCameraOpengl.simpleCamera;
 
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
 
import android.R.integer;
import android.opengl.GLSurfaceView.Renderer;
import android.support.annotation.NonNull;
import android.util.Log;

public class MyRender implements Renderer 
{	
	float[] verteices = new float[]
			{

			};
	int[] colors = new int[]
			{
				65535,0,0,0,
				0,65535,0,0,
				0,0,65535,0
			};
	
	FloatBuffer vBuffer = getVertexBuf(verteices);
	IntBuffer cBuffer = getVertexBuf(colors);
	public void refreshVerteices(float[] v){
//        Log.e("opengl","MyRender:refreshVerteices");
//		Random random = new Random();
//		verteices[0] += (random.nextFloat() - 0.5f)/10;
//		verteices[1] += (random.nextFloat() - 0.5f)/10;
//		verteices[3] += (random.nextFloat() - 0.5f)/10;
//		verteices[4] += (random.nextFloat() - 0.5f)/10;
//		verteices[6] += (random.nextFloat() - 0.5f)/10;
//		verteices[7] += (random.nextFloat() - 0.5f)/10;
		verteices = v;
		vBuffer = getVertexBuf(v);
	}
	@NonNull
	private FloatBuffer getVertexBuf(float[] vertices) {
		FloatBuffer floatBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder
				.nativeOrder()).asFloatBuffer();
		floatBuffer.put(vertices).position(0);
		return floatBuffer;
	}
	@NonNull
	private IntBuffer getVertexBuf(int[] vertices) {
		IntBuffer floatBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder
				.nativeOrder()).asIntBuffer();
		floatBuffer.put(vertices).position(0);
		return floatBuffer;
	}
	@Override
	public void onDrawFrame(GL10 gl) 
	{
		Log.e("opengl","MyRender:onDrawFrame");
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT|GL10.GL_DEPTH_BUFFER_BIT);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(0.0f, 0.0f, -1.0f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuffer);

		gl.glEnable(GL10.GL_BLEND); // 打开混合
		gl.glDisable(GL10.GL_DEPTH_TEST); // 关闭深度测试
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE); // 基于源象素alpha通道值的半透明混合函数
//		gl.glColorPointer(4, GL10.GL_FIXED, 0, cBuffer);
		gl.glColor4f(1f,1f,1f,0.2f);
		gl.glPointSize(10);
		gl.glEnable(GL10.GL_POINT_SMOOTH);
		gl.glDrawArrays(GL10.GL_POINTS, 0, verteices.length/3);
		gl.glFinish();
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
//		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
	}
 
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) 
	{
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		float ratio = (float)width/height;
		gl.glFrustumf(-ratio, ratio, -1, 1, 1, 9);
	}
 
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) 
	{
		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
		gl.glClearColor(0, 0, 0, 0);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
	}
 
}

