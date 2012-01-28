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

import com.aioisystems.imaging.DisplayPainter;
import com.aioisystems.smarttag.SmartTagCore;
import com.aioisystems.smarttagsample.Common;


import android.graphics.Bitmap;
import android.nfc.Tag;

/**
 * スマートタグとの通信機能を提供します。
 * アプリ固有の処理を実装しています。
 */
public class SmartTag extends SmartTagCore {

	public static final int FN_SHOW_STATUS = 0;
	public static final int FN_CHANGE_LAYOUT = 1;
	public static final int FN_READ_DATA = 2;
	public static final int FN_WRITE_DATA = 3;
	public static final int FN_DRAW_TEXT = 5;
	public static final int FN_DRAW_CAMERA_IMAGE = 7;
	public static final int FN_SAVE_LAYOUT = 8;

	private String mDrawText = "";
	private int mSaveNumber = 0;
	private int mLayoutNo = 0;
	private int mFunctionNo = FN_SHOW_STATUS;
	private Bitmap mCameraImage = null;
	private Exception mLastError = null;
	private String mWriteText = "";
	private String mReadText = "";

	public SmartTag(){
		setMaxBlocks(8);
	}

	public String getReadText(){
		return mReadText;
	}
	public void setWriteText(String text){
		mWriteText = text;
	}

	public Exception getLastError(){
		return mLastError;
	}

	public void setCameraImage(Bitmap bitmap){
		mCameraImage = bitmap;
	}

	public int getFunctionNo() {
		return mFunctionNo;
	}

	public void setFunctionNo(int functionNo) {
		this.mFunctionNo = functionNo;
	}

	public void setLayoutNo(int number){
		mLayoutNo = number;
	}
	/**
	 * レイアウト登録番号を設定します。
	 * @param number
	 */
	public void setSaveNo(int number){
		mSaveNumber = number;
	}

	public void setDrawText(String drawText) {
		this.mDrawText = drawText;
	}

	/**
	 * 通信対象となるタグを設定します。
	 */
	@Override
	public void selectTarget(byte[] idm, Tag tag){
		super.selectTarget(idm, tag);
		mLastError = null;
	}

	/**
	 * タグがスキャンされたときの一連の処理を行う。
	 */
	public void startSession(){
		mLastError = null;

		try{
			connect();

			//実行可能になるまで待機
			waitForIdle();

			//選択した操作を実行
			executeFunction();

			try{
				close();
			}catch(Exception e){
				//クローズは失敗してもエラー表示しない
				Common.addLoge(e.toString());
			}

		}catch(Exception e){
			mLastError = e;
			Common.addLoge(e.toString());
			return;
		}
	}

	/**
     * 選択した機能をSmart-tagに対して実行する
     */
    private void executeFunction() throws Exception{

    	switch(mFunctionNo){

    		case SmartTag.FN_SHOW_STATUS:
    			showStatus();
    			break;
    		case SmartTag.FN_DRAW_CAMERA_IMAGE:
	    		showImage(mCameraImage);
	    		break;

	    	case SmartTag.FN_SAVE_LAYOUT:
	    		saveLayout(mSaveNumber);
	    		break;

	    	case SmartTag.FN_CHANGE_LAYOUT:
	        	selectLayout(mLayoutNo);
	    		break;

	    	case SmartTag.FN_DRAW_TEXT:
	    		drawText();
	    		break;

	    	case SmartTag.FN_WRITE_DATA:
	    		saveUrl();
	    		break;

	    	case SmartTag.FN_READ_DATA:
	    		readUrl();
	    		break;
    	}
    }

    /**
     * スマートタグの状態をディスプレイに表示する
     */
    private void showStatus() throws Exception{
    	//表示画像データを作成
    	DisplayPainter display = new DisplayPainter();
    	int y = 0;
    	display.putText("SMART-TAG", 0, 0, 24);
    	display.putText("[ST1020]", 0, y+=24, 12);
    	display.putText("Battery: " + getBatteryStateText(mBattery), 0, y+=24, 12);
    	display.putText(String.format("Firmware ver.: %02X", mVersion), 0, y+=12, 12);
    	display.putText("ID: " + Common.makeHexText(mIdm), 0, y+=12, 12);

    	byte[] imageData = display.getLocalDisplayImage();

		showImage(imageData);
    }

	/**
	 * 文字列をディスプレイに表示する
	 */
	private void drawText() throws Exception {

		//表示画像データを作成
		DisplayPainter display = new DisplayPainter();
		String[] texts = mDrawText.split("\n");
		for(int i = 0; i < texts.length; i++){
			display.putText(texts[i], 0, i * 22 + 1, 20);
			if(i == 3){
				break;
			}
		}

		byte[] imageData = display.getLocalDisplayImage();

		showImage(imageData);
	}

	/**
	 * URLを保存する
	 * @throws Exception
	 */
	private void saveUrl() throws Exception{
		if(mWriteText == ""){
			return;
		}
		String wk = mWriteText + "\n";
		byte[] dataBytes = wk.getBytes("ASCII");
		writeUserData(dataBytes);
	}

	/**
	 * URLを読み出す
	 * @throws Exception
	 */
	private void readUrl() throws Exception{
		byte[] buffer = readUserData(7);
		mReadText = getUserText(buffer);
	}

	/**
	 * タグから読み出したデータをテキストに変換する
	 */
	private String getUserText(byte[] dataBytes){
		int index = -1;
		for(int i = 0; i < dataBytes.length; i++){
			if(dataBytes[i] == '\n'){
				index = i - 1;
				break;
			}
		}
		if(index == -1)
			return "";

		byte[] tmp = new byte[index + 1];
		System.arraycopy(dataBytes, 0, tmp, 0, tmp.length);

		String text = "";
		try{
			text = new String(tmp, "ASCII");
		}catch(Exception e){
			Common.addLoge(e.toString());
			return "";
		}
		return text;
	}

	private static String getBatteryStateText(int state){
		String msg = "---";

		switch(state){
		case SmartTag.BATTERY_NORMAL1:
			msg = "Fine";
			break;
		case SmartTag.BATTERY_NORMAL2:
			msg = "Normal";
			break;
		case SmartTag.BATTERY_LOW1:
			msg = "Low";
			break;
		case SmartTag.BATTERY_LOW2:
			msg = "Empty";
			break;
		}
		return msg;
	}
}
