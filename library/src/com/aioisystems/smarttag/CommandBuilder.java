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

/**
 * スマートタグに送信するコマンド作成を補助する機能を提供します。
 */
public class CommandBuilder {
	private int mMaxBlocks = 8;	//同時送信ブロック数(R/Wによって上限がある)
	private byte mSeq = 0;

	public int getMaxBlocks() {
		return mMaxBlocks;
	}

	public void setMaxBlocks(int maxBlocks) {
		this.mMaxBlocks = maxBlocks;
	}

	/**
	 * Function dataが無い場合のコマンドを作成します。
	 * @param functionNo
	 * @param paramData
	 * @return 作成されたコマンド。
	 */
	public byte[] buildCommand(byte functionNo,
			byte[] paramData){
		ArrayList<byte[]> list = buildCommand(functionNo,
				paramData, null);
		if(list.isEmpty()){
			return null;
		}else{
			return list.get(0);
		}
	}

	/***
	 * スマートタグコマンドを作成します。ヘッダ部分は自動的に作成し、
	 * 同時送信ブロック数を超える場合は複数コマンドに分割します。
	 * @param functionNo
	 * @param paramData
	 * @param functionData
	 * @return 作成されたコマンド。
	 * @throws Exception
	 */
	public ArrayList<byte[]> buildCommand(byte functionNo,
			byte[] paramData,
			byte[] functionData) {

		int dataBlocks;

		if(functionData == null){
			dataBlocks = 0;
		}else{
			dataBlocks = functionData.length / 16;
			if(functionData.length % 16 > 0){
				dataBlocks ++;
			}
		}

		//全ブロック数
		int blocks = dataBlocks + 1;
		//Common.addLogi(String.format("Total data blocks = %d", blocks));

		//フレーム分割数
		int splitCount = getSplitCount(blocks);
		//Common.addLogi(String.format("Total frames = %d", splitCount));

		//分割送信
		ArrayList<byte[]> result = new ArrayList<byte[]>();
		int offset = 0;
		int frameBlocks = 0;
		for(int i = 0; i < splitCount; i++){
			int dataLen;
			if(i == splitCount - 1){
				//最後のフレーム
				frameBlocks = getLastBlockCount(dataBlocks) + 1;
				//Common.addLogi(String.format("last block count = %d", frameBlocks));
				//最後のフレームのデータ長
				if(functionData == null){
					dataLen = 16;
				}else{
					dataLen = functionData.length - offset;
				}
			}else{
				frameBlocks = mMaxBlocks;
				dataLen = (frameBlocks - 1) * 16;
			}

			byte[] cmd = new byte[frameBlocks * 16];
			cmd[0] = functionNo;
			cmd[1] = (byte)splitCount;
			cmd[2] = (byte)(i + 1);
			cmd[3] = (byte)((frameBlocks - 1) * 16);

			if(functionNo == (byte)0xd0){
				cmd[4] = 0;
			}else{
				cmd[4] = getNextSeq();
			}
			cmd[5] = 0;
			cmd[6] = 0;
			cmd[7] = 0;

			//set function parameter data.
			System.arraycopy(paramData, 0, cmd, 8, paramData.length);

			//set function data.
			if(functionData != null){
				System.arraycopy(functionData, offset, cmd, 16, dataLen);
			}
			//Common.addLogi("command: " +  Common.makeHexText(cmd));
			result.add(cmd);
			offset += dataLen;
		}
		return result;
	}

	public void setSeq(byte seq){
		if(seq >= 127)
			seq = 0;
		this.mSeq = seq;

	}

	private int getSplitCount(int totalBlocks)
	{
		int totalBytes = totalBlocks * 16;
		int result = totalBytes / (mMaxBlocks * 16 - 16);
		if(totalBytes % (mMaxBlocks * 16 - 16) > 0)
			result++;
		return result;
	}

	private int getLastBlockCount(int dataBlocks){
		int mod = dataBlocks % (mMaxBlocks - 1);
		return mod;
	}

	private byte getNextSeq(){
		byte result = mSeq;

		mSeq++;
		if(mSeq >= 127)
			mSeq = 0;

		return result;
	}

}
