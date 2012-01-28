/*
 * Copyright (c) 2012, AIOI・SYSTEMS CO., LTD.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package com.aioisystems.smarttagsample;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;
import android.view.ViewGroup.LayoutParams;

public class CamPrevView extends SurfaceView
	implements Callback{

	public CamPrevView(Context context) {
		super(context);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mIsTaking = false;
	}

	private Camera mCamera;

	private GetPictureCallback mCallback;
	private boolean mIsTaking = false;

	public void setOnGetPicureCallback(GetPictureCallback callback){
		mCallback = callback;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Common.addLogi("surfaceChanged");

		Camera.Parameters params = mCamera.getParameters();

//		List<Size> prevSizes = params.getSupportedPreviewSizes();
//		for(Size s : prevSizes){
//			Common.addLogi(String.format("supported prev: %d x %d", s.width, s.height));
//		}
		Size prevSize = params.getPreviewSize();
		Common.addLogi(String.format("preview size: %d x %d", prevSize.width, prevSize.height));

		//レイアウトの大きさをプレビューサイズの比率にあわせる
		LayoutParams lParams = getLayoutParams();
		int ch = prevSize.height;
		int cw = prevSize.width;
		if(ch / cw > height / width){
			lParams.width = width;
			lParams.height = width * ch / cw;
		}else{
			lParams.width = height * cw / ch;
			lParams.height = height;
		}
		setLayoutParams(lParams);

		//一番小さい画像サイズを取得する
		List<Size> picSizes = params.getSupportedPictureSizes();
		int min = 10000;
		for(Size s : picSizes){
			//Common.addLogi(String.format("supported pict: %d x %d", s.width, s.height));
			if(s.width > SmartTag.SCREEN_WIDTH
					&& s.height > SmartTag.SCREEN_HEIGHT){
				min = Math.min(min, s.width);
			}
		}
		Size selectedSize = null;
		for(Size s : picSizes){
			if(s.width == min){
				selectedSize = s;
				break;
			}
		}
		if(selectedSize != null){
			params.setPictureSize(selectedSize.width, selectedSize.height);
			Common.addLogi(String.format("selected size: %d x %d", selectedSize.width, selectedSize.height));
		}

		mCamera.setParameters(params);
		mCamera.startPreview();

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			mCamera = Camera.open();
			mCamera.setPreviewDisplay(holder);
		} catch(IOException e) {
			Log.e("App", e.toString());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.stopPreview();
		mCamera.release();

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(!mIsTaking){
			mIsTaking = true;
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				mCamera.autoFocus(mAutoFocusListener);
			}
		}
		return true;
	}

	android.os.Handler handler = new android.os.Handler();

	Camera.AutoFocusCallback mAutoFocusListener =
			new AutoFocusCallback() {

				/**
				 * フォーカス完了時の処理
				 */
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					Common.addLogi("onAutoFocus");

					//ウェイトを入れる
					Timer timer1 = new Timer();
					timer1.schedule(new TimerTask() {
						public void run() {
							handler.post(new Runnable() {
								public void run() {
									mCamera.takePicture(null, null, mPictureListener);
								}
							});
						}
					}, 500);

					//mCamera.takePicture(null,null, mPictureListener);
				}
			};


	private Camera.PictureCallback mPictureListener =
			new PictureCallback() {
				/**
				 * 撮影後の処理を行う
				 */
				@Override
				public void onPictureTaken(byte[] data, Camera camera) {
					Common.addLogi("onPictureTaken");

					Bitmap tmp = BitmapFactory.decodeByteArray(data, 0, data.length, null);

					//アスペクト比を合わせた四角
					float h = (float)(tmp.getHeight() * 0.9);
					float w = (float)(h * 0.48);
					float left = tmp.getWidth() / 2 - w / 2;
					float top = tmp.getHeight() / 2 - h / 2;

					//切り抜き
					Bitmap trimed = Bitmap.createBitmap(tmp, (int)left, (int)top, (int)w, (int)h);
					tmp.recycle();

					//縮小と回転
					int trimW = trimed.getWidth();
					int trimH = trimed.getHeight();

					float sx = (float)SmartTag.SCREEN_HEIGHT / trimW;
					float sy = (float)SmartTag.SCREEN_WIDTH / trimH;

					Matrix matrix = new Matrix();
					matrix.postScale(sx, sy);
					matrix.postRotate(90);
					Bitmap scaled = Bitmap.createBitmap(trimed, 0, 0, trimW, trimH, matrix, true);

					mCallback.onBitmapTaken(scaled);

					mIsTaking = false;
					//mCamera.startPreview();
				}
			};

	public interface GetPictureCallback {
		public void onBitmapTaken(Bitmap bitmap);
	}

}
