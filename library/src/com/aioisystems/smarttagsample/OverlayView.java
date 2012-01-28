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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class OverlayView extends View {
	int mWidth, mHeight;
	private Context mContext;
	public OverlayView(Context context){
		super(context);

		mContext = context;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mWidth = w;
		mHeight = h;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// 背景色を設定
		  canvas.drawColor(Color.TRANSPARENT);

		  // 描画するための線の色を設定
		  Paint paint = new Paint();
		  paint.setStyle(Paint.Style.STROKE);
		  paint.setAntiAlias(false);
		  paint.setStrokeWidth(2);
		  paint.setColor(Color.GREEN);

		  //画面横向きで縦長の長方形を描く

		  //アスペクト比を合わせた四角
		  float rate = (float)SmartTag.SCREEN_HEIGHT / SmartTag.SCREEN_WIDTH;
		  float h = (float)(mHeight * 0.9);
		  float w = (float)(h * rate);
		  float left = mWidth / 2 - w / 2;
		  float top = mHeight / 2 - h / 2;
		  canvas.drawRect(left, top, left + w, top + h, paint);

		  Paint paint2 = new Paint();
		  paint2.setStyle(Paint.Style.FILL);
		  paint2.setARGB(150, 0, 0, 0);

		  //上マスク表示
		  canvas.drawRect(0, 0, mWidth, top, paint2);
		  //左側
		  canvas.drawRect(0, top, left - 1, top + h, paint2);
		  //右側
		  canvas.drawRect(left + w, top, mWidth, top + h, paint2);
		  //下側
		  canvas.drawRect(0, top + h, mWidth, mHeight, paint2);

		  //文字の描画
		  Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		  int textSize = 24;
		  textPaint.setTextSize(textSize);
		  textPaint.setColor(Color.WHITE);
		  canvas.rotate(-90, 0, 0);

		  canvas.drawText(mContext.getString(R.string.msgTapToSnap),
				  top - mHeight,
				  //left + w + textSize + 5,
				  left - textSize - 5,
				  textPaint);

	}


}
