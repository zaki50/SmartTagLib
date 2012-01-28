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

import com.aioisystems.smarttagsample.OverlayView;
import com.aioisystems.smarttagsample.CamPrevView;
import com.aioisystems.smarttagsample.CamPrevView.GetPictureCallback;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

public class CameraActivity extends Activity {
	private CamPrevView mCameraView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation((ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout l = new LinearLayout(this);
        l.setHorizontalGravity(Gravity.CENTER);

        mCameraView = new CamPrevView(this);

        //撮影後インテントを返してActivityを閉じる
        mCameraView.setOnGetPicureCallback(new GetPictureCallback() {
			public void onBitmapTaken(Bitmap bitmap) {
				Intent intent = new Intent();
		    	intent.putExtra("BITMAP", bitmap);
		    	setResult(RESULT_OK, intent);
		    	finish();
			}
		});

        l.addView(mCameraView);
        setContentView(l);

    	//オーバーレイ画面を表示
        addContentView(new OverlayView(this),
    			new LayoutParams(LayoutParams.FILL_PARENT,
    					LayoutParams.FILL_PARENT));
	}
}
