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

import android.util.Log;


public class Common{
	private static final boolean DEBUG_FLG = false;
	private static final String TAG = "AppLog";

	/**
  	 * デバッグ用の情報ログを出力する
  	 */
  	public static void addLogi(String message){
  		if (DEBUG_FLG){
  			Log.i(TAG, message);
  		}
  	}

  	/**
     * デバッグ用のエラーログを出力する
     */
  	public static void addLoge(String message){
  		if (DEBUG_FLG){
  			Log.e(TAG, message);
  		}
  	}

  	/**
  	 * バイト配列を16進文字列に変換する
  	 */
	public static String makeHexText(byte[] data){

		StringBuffer sb = new StringBuffer();
    	for(int i = 0; i < data.length; i++){
    		if(i != 0){
    			sb.append("-");
    		}
    		sb.append(String.format("%02X", data[i]));
    	}
    	return sb.toString();
    }

  	/**
  	 * バイト配列を16進文字列に変換する(デバッグ用)
  	 */
	public static String makeHexTextD(byte[] data){
    	if(DEBUG_FLG){
    		return makeHexText(data);
    	}
    	return "";
    }

}
