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

package com.aioisystems.imaging;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Point;

/**
 * スマートタグのディスプレイ表示用画像を作成する機能を提供します。
 */
public class DisplayPainter {

	private static final int MAX_WIDTH = 200;
	private static final int MAX_HEIGHT = 96;

	private Bitmap mScreen = null;
	private Canvas mCanvas = null;

	public DisplayPainter(){
		mScreen = Bitmap.createBitmap(MAX_WIDTH, MAX_HEIGHT, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mScreen);
		clearDisplay();
	}

	/**
	 * 仮想ディスプレイをクリアする
	 */
	public void clearDisplay(){
		Paint paint = new Paint();
    	paint.setColor(Color.WHITE);
    	paint.setStyle(Paint.Style.FILL);
    	mCanvas.drawRect(0, 0, MAX_WIDTH, MAX_HEIGHT, paint);
	}

	/**
	 * 仮想ディスプレイのプレビュー用画像を取得する
	 * @return プレビュー画像
	 */
	public Bitmap getPreviewImage(){
		return mScreen;
	}

	/**
	 * 仮想ディスプレイにテキストを配置
	 * @param text
	 * @param x
	 * @param y
	 */
	public void putText(
			String text,
			int x,
			int y,
			int size){

		Paint paint = new Paint();
    	paint.setAntiAlias(false);
    	paint.setColor(Color.BLACK);
    	paint.setTextSize(size);
    	FontMetrics metrics = paint.getFontMetrics();
    	float top = y - metrics.top;
    	mCanvas.drawText(text, x, top, paint);
	}

	/**
	 * 直線を仮想ディスプレイに描画する
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param size
	 * @param inverted 反転する場合はtrue
	 * @param dashed 点線を引く場合はtrue、実線はfalse。
	 */
	public void putLine(
			int x1,
			int y1,
			int x2,
			int y2,
			int size,
			boolean invert,
			boolean dashed){

		Paint paint = new Paint();
    	paint.setAntiAlias(false);
    	if(invert){
    		paint.setColor(Color.WHITE);
    	}else{
    		paint.setColor(Color.BLACK);
    	}
    	paint.setStrokeWidth(size);
    	paint.setStyle(Paint.Style.STROKE);
    	paint.setPathEffect(new DashPathEffect(new float[]{2.0f, 2.0f}, 0.0f));
    	mCanvas.drawLine(x1, y1, x2, y2, paint);
	}

	public void putLine(
			int x1,
			int y1,
			int x2,
			int y2,
			int size){
		putLine(x1, y1, x2, y2, size, false, false);
	}

	/**
	 * 矩形を仮想ディスプレイに描画する
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param border
	 */
	public void putRectangle(
			int x,
			int y,
			int width,
			int height,
			int border){

		Paint paint = new Paint();
    	paint.setAntiAlias(false);
    	paint.setColor(Color.BLACK);
    	paint.setStrokeWidth(border);
    	paint.setStyle(Paint.Style.STROKE);

    	mCanvas.drawRect(x, y, x + width - 1, y + height - 1, paint);

	}

	/**
	 * 画像を仮想ディスプレイに配置する
	 * @param bitmap
	 * @param x
	 * @param y
	 * @param dither ディザリングする場合はtrueを指定。
	 */
	public void putImage(Bitmap bitmap, int x, int y, boolean dither ){
		if(bitmap == null)
			return;

		if(dither){
			bitmap = getDitheredImage(bitmap);
		}else{
			bitmap = getThresholdImage(bitmap);
		}

		Canvas canvas = new Canvas(mScreen);
		canvas.drawBitmap(bitmap, x, y, null);
	}



	/**
	 * 誤差拡散による白黒画像を取得する
	 * @return
	 */
	private static Bitmap getDitheredImage(Bitmap bitmap){
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		char[] src = getGrayPixels(bitmap);
		char[] dst = new char[width * height];

		//誤差拡散設定(Floyd & steinberg)
		double[][] df = {
			{      -1,       -1, 7.0/16.0},
			{3.0/16.0, 5.0/16.0, 1.0/16.0}
		};
		int dfRows = df.length;
		int dfCols = df[0].length;
		int xRange = (dfCols - 1) / 2;

		boolean d;
		double err;
		int xx, yy;

		//ピクセル処理
		int index = 0;

		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				index = x + y * width;
				char pixel = src[index];

				if(pixel > 127){
					d = true;
					dst[index] = 255;
				}else{
					d = false;
				}

				if(d){
					err = pixel - 255;
				}else{
					err = pixel;
				}

				//誤差を拡散
				for(int iy = 0; iy < dfRows; iy++){
					for(int ix = -xRange; ix <= xRange; ix++){
						xx = x + ix;

						if((xx < 0) | (xx > width - 1)){
							continue;
						}
						yy = y + iy;
						if(yy > height - 1){
							continue;
						}
						if(df[iy][ix + xRange] < 0){
							continue;
						}

						double wk = src[xx + yy * width] +
								err * df[iy][ix + xRange];
						src[xx + yy * width] =
							adjustByte(wk);
					}
				}
			}
		}

		return createBlackWhiteImage(dst, width, height);
	}



	/**
	 * 0-255の範囲に調整する
	 * @param value
	 * @return
	 */
	private static char adjustByte(double value){
		if(value < 0){
			value = 0;
		}else if(value > 255){
			value = 255;
		}
		return (char)value;
	}

	/**
	 * カラー画像からグレイスケールのピクセル配列データを取得する
	 * @param bitmap
	 * @return
	 */
	private static char[] getGrayPixels(Bitmap bitmap){
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		int[] src = new int[width * height];
		bitmap.getPixels(src, 0, width, 0, 0, width, height);
		Point range = getPixelRange(src);
		float adjust = (float)255 / (range.y - range.x);

		//格納先配列
		char[] dst = new char[width * height];

		//ピクセル処理
		int index = 0;
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				index = x + y * width;
				int pixel = src[index];
				//RGBの平均を使う(グレースケール値)
				char tmp = (char)getAverage(Color.red(pixel),
						Color.green(pixel),
						Color.blue(pixel));
				//コントラストを調整
				tmp -= range.x;
				dst[index] = (char)(tmp * adjust);
			}
		}
		return dst;
	}

	/**
	 * 画素データから白黒のBitmapオブジェクトを取得する
	 * @param pixels
	 * @return
	 */
	private static Bitmap createBlackWhiteImage(char[] pixels, int width, int height){
		int[] pixelsInt = new int[width * height];

		int index;
		int value;
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				index = x + y * width;
				value = pixels[index];
				pixelsInt[index] =
					Color.argb(255, value, value, value);
			}
		}
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bmp.setPixels(pixelsInt, 0, width, 0, 0, width, height);
		return bmp;
	}

	/**
	 * しきい値による白黒画像を取得
	 * @param bitmap
	 * @return
	 */
	private static Bitmap getThresholdImage(Bitmap bitmap){
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		char[] src = getGrayPixels(bitmap);
		char[] dst = new char[width * height];

		//ピクセル処理
		int index = 0;
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				index = x + y * width;
				char pixel = src[index];
				if(pixel > 127){
					dst[index] = 255;
				}else{
					dst[index] = 0;
				}
			}
		}

		return createBlackWhiteImage(dst, width, height);
	}

	/**
	 * スマートタグ用の画像データを取得する
	 * @return
	 */
	public byte[] getLocalDisplayImage(){
		int width = MAX_WIDTH;
		int height = MAX_HEIGHT;
		int[] pixels = new int[width * height];
		mScreen.getPixels(pixels, 0, width, 0, 0, width, height);

		//格納先配列を作成
		int cols = width / 8;
		if(width % 8 > 0){
			cols++;
		}
		int size = cols * height;
		byte[] result = new byte[size];

		//ピクセル別処理
		int index = 0;
		byte bitPos = 7;
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				int pixel = pixels[x + y * width];

				if(Color.red(pixel) == 0){
					byte color = 1;
					result[index] |= (color << bitPos);
				}

				bitPos--;
				if(bitPos < 0
						|| x == (width - 1)/*1ラインの最終ピクセル*/){
					index++;
					bitPos = 7;
				}
			}
		}

		return result;
	}

	private static int getAverage(int red, int green, int blue){
		return (int)((float)(red + green + blue) / 3);
	}

	/**
	 * グレースケールにしたときの最大値と最小値を取得する
	 * @param pixels
	 * @return
	 */
	private static Point getPixelRange(int[] pixels){

		int max = 0;
		int min = 255;
		for(int i = 0; i < pixels.length; i++){
			int pixel = pixels[i];
			//RGBの平均を使う(グレースケール値)
			int value = getAverage(Color.red(pixel),
					Color.green(pixel),
					Color.blue(pixel));
			max = Math.max(value, max);
			min = Math.min(value, min);

		}
		return new Point(min, max);
	}
}
