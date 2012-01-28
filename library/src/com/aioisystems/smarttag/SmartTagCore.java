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

package com.aioisystems.smarttag;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.nfc.Tag;
import android.nfc.tech.NfcF;

import com.aioisystems.imaging.DisplayPainter;


/**
 * スマートタグとの低レベルの通信機能を提供します。
 */
public class SmartTagCore {

	public static final int SCREEN_WIDTH = 200;
	public static final int SCREEN_HEIGHT = 96;

	public static final int BATTERY_NORMAL1 = 0;
	public static final int BATTERY_NORMAL2 = 1;
	public static final int BATTERY_LOW1 = 2;
	public static final int BATTERY_LOW2 = 3;
	public static final int BATTERY_UNKOWN = -1;

	public static final byte TAG_STS_UNKNOWN = -1;
	public static final byte TAG_STS_INIT = 0;
	public static final byte TAG_STS_PROCESSED = (byte)0xF0;
	public static final byte TAG_STS_BUSY = (byte)0xF2;

	protected NfcF mFelica = null;
	protected byte[] mIdm = null;
	protected byte[] mSystemCode = null;
	protected int mBattery = BATTERY_UNKOWN;
	protected byte mVersion = 0;
	protected byte mStatus = 0;

	private CommandBuilder mBuilder;

	public SmartTagCore(){
		mBuilder = new CommandBuilder();
		mBuilder.setMaxBlocks(12);
	}

	/**
	 * 最大同時転送ブロック数を取得します。
	 * @return
	 */
	public int getMaxBlocks(){
		return mBuilder.getMaxBlocks();
	}

	/**
	 * 最大同時転送ブロック数を設定します。
	 * @param value
	 */
	public void setMaxBlocks(int value){
		mBuilder.setMaxBlocks(value);
	}

	/**
	 * 最後にステータスをチェックしたときのステータスコードを取得します。
	 * @return
	 */
	public byte getStatus(){
		return mStatus;
	}

	/**
	 * IDmを取得します。
	 */
	public byte[] getIdm(){
		return mIdm;
	}

	/**
	 * システムコードを取得します。
	 */
	public byte[] getSystemCode(){
		return mSystemCode;
	}

	/**
	 * 最後にステータスをチェックしたときのSmart-Tagのバージョンを取得します。
	 */
	public byte getVersion(){
		return mVersion;
	}

	/**
	 * 最後にステータスをチェックしたときの電池状態を取得します。
	 */
	public int getBatteryLevel(){
		return mBattery;
	}

	/**
	 * 通信対象となるタグを設定します。
	 * (タグがタッチされる都度実行してください)
	 * @param idm スキャンしたタグのIDm。
	 * @param tag スキャンしたタグのTagオブジェクト。
	 */
	public void selectTarget(byte[] idm, Tag tag){
		mIdm = idm;
		mFelica = NfcF.get(tag);
		mSystemCode = mFelica.getSystemCode();

	}

	/**
	 * スマートタグに接続します。
	 * @throws Exception
	 */
	public void connect() throws Exception{
		mFelica.connect();
	}

	/**
	 * スマートタグを閉じます。
	 * @throws Exception
	 */
	public void close() throws Exception{
		mFelica.close();
	}

	/**
	 * スマートタグかどうかを取得します。
	 * @return スマートタグである場合はtrue、それ以外はfalse。
	 */
	public boolean isSmartTag(){
		if(mSystemCode == null)
			return false;

		if(mSystemCode.length < 2)
			return false;

		if(mSystemCode[0] != (byte)0xfe
				|| mSystemCode[1] != (byte)0xe1)
			return false;

		return true;
	}

	/**
	 * Smart-tagのステータスをタグから読み取り、更新します。
	 */
	public void checkStatus() throws Exception{
		//mBattery = SmartTag.BATTERY_UNKOWN;
		mVersion = -1;
		mStatus = TAG_STS_UNKNOWN;

		byte[] paramData = new byte[]{
				0, 0, 0, 0,
				0, 0, 0, 0
		};

		writeCommand((byte)0xd0, paramData);

		//read status
		byte[] blockData = readBlocks(2);
		if(blockData != null){
			mStatus = blockData[3];
			mBattery = blockData[5];	//電池状態
			mVersion = blockData[15];
			mBuilder.setSeq((byte)(blockData[4] + 1));
		}
	}

	/**
	 * スマートタグが次の処理を実行できるようになるまで待機します。
	 * @throws Exception
	 */
	public void waitForIdle() throws Exception{
		for(int i = 0; i < 5; i++){

			try{
				checkStatus();
			}catch(Exception e){
				DebugUtil.addLoge(e.toString() + " in waitForIdle()");
			}
			if(mStatus != TAG_STS_BUSY
					&& mStatus != TAG_STS_UNKNOWN){
				return;
			}
			try{
				Thread.sleep(500);
			}catch(Exception ignore){

			}
		}
		throw new Exception("Tag is busy.");
	}

	/**
	 * 最後に表示した画像をスマートタグに登録する
	 * @param layoutNo 保存番号
	 * @throws Exception
	 */
	public void saveLayout(int layoutNo) throws Exception{
		byte[] paramData = new byte[]{
				(byte)layoutNo, 0, 0, 0,
				0, 0, 0, 0
		};
		writeCommand((byte)0xb2, paramData);
	}

	/**
	 * 指定したレイアウトに切り替えます。
	 * @param layoutNo 切り替えるレイアウト番号。
	 */
	public void selectLayout(int layoutNo) throws Exception{
		byte[] paramData = new byte[]{
				1, 1, (byte)0xFF, (byte)0xFF,
				25, 3, (byte)layoutNo, 1
		};

		writeCommand((byte)0xa0, paramData);
	}

	/**
	 * タグのユーザーデータを読み出す
	 * (最大同時書き込みブロック数以上のデータには非対応)
	 * @param blocks ユーザーデータを読み出すのに必要なブロック数。(ヘッダー分は含まない)
	 * @return 読み出したユーザーデータ。
	 * @throws Exception
	 */
	public byte[] readUserData(int blocks) throws Exception{

		int len = blocks * 16;
		byte hByte = (byte)(len >> 8);
		byte lByte = (byte)(len & 0x00FF);

		//Send request for read.
		byte[] paramData = new byte[]{
				0, 0, hByte, lByte,
				0, 0, 0, 0
		};
		writeCommand((byte)0xc0, paramData);

		//Check status.
		waitForIdle();

		if(mStatus != TAG_STS_PROCESSED){
			DebugUtil.addLoge("Tag error.");
			return null;
		}

		//Read data
		byte[] data = readBlocks(blocks + 1);

		byte[] userData = new byte[blocks * 16];
		System.arraycopy(data, 16, userData, 0, userData.length);

		return userData;
	}

	/**
	 * 200x96サイズの画像をディスプレイに表示します。
	 * @param bitmap 表示するBitmapオブジェクト。
	 * @throws Exception
	 */
	public void showImage(Bitmap bitmap) throws Exception {
		if(bitmap == null)
			return;
		//表示画像データを作成
		DisplayPainter display = new DisplayPainter();
		display.putImage(bitmap, 0, 0, false);

		byte[] imageData = display.getLocalDisplayImage();

		showImage(imageData);
	}

	/**
	 * 200x96サイズの画像をディスプレイに表示します。
	 * @param imageData スマートタグ形式の画像データ
	 * @throws Exception
	 */
	public void showImage(byte[] imageData) throws Exception {
		byte[] paramData = new byte[]{
				1, 1, 0, 0,
				25, 0, 0, 3
		};
		writeCommand((byte)0xa0, paramData, imageData);
	}

	/**
	 * ユーザーデータを書き込む
	 */
	public void writeUserData(byte[] data) throws Exception{
		int dataBlocks = data.length / 16;
		if(data.length % 16 > 0)
			dataBlocks ++;
		int len = dataBlocks * 16;
		byte hByte = (byte)(len >> 8);
		byte lByte = (byte)(len & 0x00FF);

		byte[] paramData = new byte[]{
			0, 0, hByte, lByte,
			0, 0, 0, 0
		};

		writeCommand((byte)0xb0, paramData, data);

	}

	/**
	 * Felicaコマンドの書式で書き込み用パケットを作成
	 */
	protected byte[] createPacketForWrite(int blocks, byte[] blockData){
		int len = 14 + 2 * blocks + 16 * blocks;
		byte[] packet = new byte[len];
		int pos = 0;
		packet[0] = (byte)len;
		packet[1] = (byte)0x08;
		pos = 2;
		//IDm
		System.arraycopy(mIdm, 0, packet, pos, 8);
		pos += 8;
		//service count
		packet[pos] = 0x01;
		pos++;
		//service code
		packet[pos] = 0x09;
		packet[pos + 1] = 0x00;
		pos+=2;
		//block count
		packet[pos] = (byte)blocks;
		pos++;
		//block list(dummy)
		for(int i = 0; i < blocks; i++)
		{
			packet[pos] = (byte)0x80;
			packet[pos + 1] = (byte)0x00;
			pos += 2;
		}
		//block data
		System.arraycopy(blockData, 0, packet, pos, blockData.length);
		return packet;
	}

	/**
	 * ReadWithoutEncription用のパケットを作成する
	 */
	protected byte[] createPacketForRead(int blocks){
		int len = 14 + 2 * blocks;
		byte[] packet = new byte[len];
		int pos = 0;
		packet[0] = (byte)len;
		packet[1] = (byte)0x06;
		pos = 2;
		//IDm
		System.arraycopy(mIdm, 0, packet, pos, 8);
		pos += 8;
		//service count
		packet[pos] = 0x01;
		pos++;
		//service code
		packet[pos] = 0x09;
		packet[pos + 1] = 0x00;
		pos+=2;
		//block count
		packet[pos] = (byte)blocks;
		pos++;
		//block list(dummy)
		for(int i = 0; i < blocks; i++)
		{
			packet[pos] = (byte)0x80;
			packet[pos + 1] = (byte)0x00;
			pos += 2;
		}

		return packet;
	}

	/**
	 * felicaのレスポンスパケットからブロックデータを取り出す
	 */
	protected byte[] getBlockData(byte[] response)
	{
		if(response.length < 13)	//先頭にバイト数
			return null;

		int blockCount = response[12];
		byte[] blockData = new byte[blockCount * 16];
		if(response.length < 13 + blockData.length)
			return null;

		System.arraycopy(response, 13, blockData, 0, blockData.length);

		return blockData;
	}

	/**
	 * 先頭から指定ブロック数だけ読み込みを行う（ReadWithoutEncription)
	 * @param blocks コマンドのヘッダを含むブロック数
	 */
	protected byte[] readBlocks(int blocks) throws Exception{
		//Read data
		byte[] packet = createPacketForRead(blocks);
		DebugUtil.addLogi("RWE(Low): " +  DebugUtil.makeHexText(packet));

		byte[] res = mFelica.transceive(packet);
		DebugUtil.addLogi("RES(Low): " +  DebugUtil.makeHexText(res));

		byte[] data = getBlockData(res);
		DebugUtil.addLogi("BlockData: " + DebugUtil.makeHexText(data));

		return data;
	}

	/**
	 * コマンドを書き込む(WWE)
	 * @param functionNo
	 * @param paramData
	 * @param functionData
	 * @throws Exception
	 */
	private void writeCommand(byte functionNo,
			byte[] paramData,
			byte[] functionData) throws Exception {

		ArrayList<byte[]> list = mBuilder.buildCommand(functionNo, paramData, functionData);
		for(byte[] cmd: list){
			int blocks = cmd.length / 16;
			if((cmd.length % 16) != 0){
				blocks ++;
			}
			DebugUtil.addLogi("WWE: " +  DebugUtil.makeHexText(cmd));
			DebugUtil.addLogi("blocks: " +  blocks);
			byte[] packet = createPacketForWrite(blocks, cmd);
			byte[] response = mFelica.transceive(packet);
			DebugUtil.addLogi("RES(Low): " +  DebugUtil.makeHexText(response));
		}
	}

	/**
	 * コマンドを書き込む(WWE)
	 * @param functionNo
	 * @param paramData
	 * @return
	 * @throws Exception
	 */
	public byte[] writeCommand(
			byte functionNo,
			byte[] paramData) throws Exception {

		byte[] cmd = mBuilder.buildCommand(functionNo, paramData);
		DebugUtil.addLogi("WWE: " +  DebugUtil.makeHexText(cmd));

		byte[] packet = createPacketForWrite(1, cmd);

		byte[] response = mFelica.transceive(packet);
		DebugUtil.addLogi("RES(Low): " +  DebugUtil.makeHexText(response));
		return response;
	}

}
