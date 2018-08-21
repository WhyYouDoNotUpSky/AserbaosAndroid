/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aserbao.aserbaosandroid.opengl.openGlCamera.simpleCameraOpengl.simpleOne;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * This class renders the AR background from camera feed. It creates and hosts the texture given to
 * ARCore to be filled with the camera image.
 */
public class BackgroundRenderer {
  private static final String TAG = BackgroundRenderer.class.getSimpleName();

  // Shader names.
  private static final String VERTEX_SHADER_NAME = "Shader/screenquad.vert";
  private static final String FRAGMENT_SHADER_NAME = "Shader/screenquad.frag";

  private static final int COORDS_PER_VERTEX = 3;
  private static final int TEXCOORDS_PER_VERTEX = 2;
  private static final int FLOAT_SIZE = 4;

  private FloatBuffer quadVertices;
  private FloatBuffer quadTexCoord;
  private FloatBuffer quadTexCoordTransformed;

  private int quadProgram;

  private int quadPositionParam;
  private int quadTexCoordParam;
  private int textureId = -1;

  public BackgroundRenderer() {}

  public int getTextureId() {
    return textureId;
  }

  public SurfaceTexture getCameraTexture() {
    return mCameraTexture;
  }

  public void setCameraTexture(SurfaceTexture cameraTexture) {
    mCameraTexture = cameraTexture;
  }

  private SurfaceTexture mCameraTexture;

  public void createOnGlThread(Context context,SurfaceTexture.OnFrameAvailableListener listener) throws IOException {
    // Generate the background texture.
    int[] textures = new int[1];
    GLES20.glGenTextures(1, textures, 0);
    textureId = textures[0];
    int textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    GLES20.glBindTexture(textureTarget, textures[0]);
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

    mCameraTexture = new SurfaceTexture(textures[0]);//以上面OpenGl生成的纹理函数参数创建SurfaceTexture,SurfaceTexture接收的数据将传入该纹理
    mCameraTexture.setOnFrameAvailableListener(listener);//设置SurfaceTexture的回调，通过摄像头预览数据已更新

    int numVertices = 4;
    if (numVertices != QUAD_COORDS.length / COORDS_PER_VERTEX) {
      throw new RuntimeException("Unexpected number of vertices in BackgroundRenderer.");
    }

    ByteBuffer bbVertices = ByteBuffer.allocateDirect(QUAD_COORDS.length * FLOAT_SIZE);
    bbVertices.order(ByteOrder.nativeOrder());
    quadVertices = bbVertices.asFloatBuffer();
    quadVertices.put(QUAD_COORDS);
    quadVertices.position(0);

    ByteBuffer bbTexCoords =
        ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
    bbTexCoords.order(ByteOrder.nativeOrder());
    quadTexCoord = bbTexCoords.asFloatBuffer();
    quadTexCoord.put(QUAD_TEXCOORDS);
    quadTexCoord.position(0);

    ByteBuffer bbTexCoordsTransformed =
        ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
    bbTexCoordsTransformed.order(ByteOrder.nativeOrder());
    quadTexCoordTransformed = bbTexCoordsTransformed.asFloatBuffer();

    int vertexShader =
        ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
    int fragmentShader =
        ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

    quadProgram = GLES20.glCreateProgram();
    GLES20.glAttachShader(quadProgram, vertexShader);
    GLES20.glAttachShader(quadProgram, fragmentShader);
    GLES20.glLinkProgram(quadProgram);
    GLES20.glUseProgram(quadProgram);

    ShaderUtil.checkGLError(TAG, "Program creation");

    quadPositionParam = GLES20.glGetAttribLocation(quadProgram, "a_Position");
    quadTexCoordParam = GLES20.glGetAttribLocation(quadProgram, "a_TexCoord");

    ShaderUtil.checkGLError(TAG, "Program parameters");
  }

  public void draw() {

    // No need to test or write depth, the screen quad has arbitrary depth, and is expected
    // to be drawn first.
    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    GLES20.glDepthMask(false);

    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

    GLES20.glUseProgram(quadProgram);

    // Set the vertex positions.
    GLES20.glVertexAttribPointer(
        quadPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadVertices);

    // Set the texture coordinates.
    GLES20.glVertexAttribPointer(
        quadTexCoordParam,
        TEXCOORDS_PER_VERTEX,
        GLES20.GL_FLOAT,
        false,
        0,
        quadTexCoordTransformed);

    // Enable vertex arrays
    GLES20.glEnableVertexAttribArray(quadPositionParam);
    GLES20.glEnableVertexAttribArray(quadTexCoordParam);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

    // Disable vertex arrays
    GLES20.glDisableVertexAttribArray(quadPositionParam);
    GLES20.glDisableVertexAttribArray(quadTexCoordParam);

    // Restore the depth state for further drawing.
    GLES20.glDepthMask(true);
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);

    ShaderUtil.checkGLError(TAG, "Draw");
  }

  private static final float[] QUAD_COORDS =
      new float[] {
        -1.0f, -1.0f, 0.0f, -1.0f, +1.0f, 0.0f, +1.0f, -1.0f, 0.0f, +1.0f, +1.0f, 0.0f,
      };

  private static final float[] QUAD_TEXCOORDS =
      new float[] {
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        1.0f, 0.0f,
      };
}
