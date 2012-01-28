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

import java.util.Timer;
import java.util.TimerTask;

import com.aioisystems.imaging.DisplayPainter;
import com.aioisystems.smarttagsample.CameraActivity;
import com.aioisystems.smarttagsample.Common;
import com.aioisystems.smarttagsample.EditActivity;
import com.aioisystems.smarttagsample.MainActivity;
import com.aioisystems.smarttagsample.SmartTag;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {
	private NfcAdapter mAdapter = null;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;

	private SmartTagTask mTagTask;

	private static final SmartTag mSmartTag = new SmartTag();
	private volatile boolean mIsBusy = false;

	public static final int SHOW_EDIT = 1;
	public static final int SHOW_CAMERA_PREVIEW = 2;
	public static final int SHOW_URL_EDIT = 3;

	private Handler mHandler = new Handler();

	private TextView mErrorView;
	private TextView mIdView;
	private TextView mUserTextView;
	private LinearLayout mCameraItemLayout;
	private ImageView mCameraPreview;
	private LinearLayout mSaveImageLayout;
	private Spinner mSaveNoList;
	private Spinner mLayoutList;
	private LinearLayout mSelectImageLayout;
	private int mLayoutNo = 1;
	private LinearLayout mWriteDataLayout;
	private TextView mUrlView;
	private TextView mCaution1;

	private boolean mIsEdited = false;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mErrorView = (TextView)findViewById(R.id.errorView);
        mIdView = (TextView)findViewById(R.id.idmView);
        mIdView.setText("ID: ----");

        mSmartTag.setFunctionNo(SmartTag.FN_SHOW_STATUS);

        //文字表示
        mUserTextView = (TextView)findViewById(R.id.userTextView);
        mUserTextView.setText(getString(R.string.msgTouchToEdit));
        mUserTextView.setVisibility(View.GONE);

        mUserTextView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showEditActivity(v);
			}
		});

        //URL保存
        mWriteDataLayout = (LinearLayout)findViewById(R.id.writeDataLayout);
        mWriteDataLayout.setVisibility(View.GONE);
        mUrlView = (TextView)findViewById(R.id.urlTextView);
        mUrlView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showUrlEditActivity(v);

			}
		});
        mCaution1 = (TextView)findViewById(R.id.caution1);
        mCaution1.setVisibility(View.GONE);

        //カメラ関連設定
        mCameraPreview = (ImageView)findViewById(R.id.cameraPreview);
    	mCameraItemLayout = (LinearLayout)findViewById(R.id.cameraItemLayout);
    	mCameraItemLayout.setVisibility(View.GONE);

    	//カメラボタンイベント設定
    	ImageButton button = (ImageButton)findViewById(R.id.cameraButton);
    	button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
				startActivityForResult(intent, SHOW_CAMERA_PREVIEW);
			}
		});

    	//画像登録関連
    	mSaveImageLayout = (LinearLayout)findViewById(R.id.saveImageLayout);
    	mSaveImageLayout.setVisibility(View.GONE);
    	mSaveNoList = (Spinner)findViewById(R.id.saveNoList);

    	//レイアウト変更用番号リスト作成
    	String[] items = new String[]{"1", "2", "3", "4", "5", "6",
    			"7", "8", "9", "10", "11", "12",};
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
    	mSaveNoList.setAdapter(adapter);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	mSaveNoList.setPrompt(getString(R.string.layoutSelectionPrompt));

    	//画像指定登録関連
    	mSelectImageLayout = (LinearLayout)findViewById(R.id.selectImageLayout);
    	mSelectImageLayout.setVisibility(View.GONE);
    	mLayoutList = (Spinner)findViewById(R.id.layoutNoList);
    	adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
    	mLayoutList.setAdapter(adapter);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	mLayoutList.setPrompt(getString(R.string.layoutSelectionPrompt));

    	//リスト選択後の処理を実装
    	mLayoutList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				String item = (String)mLayoutList.getSelectedItem();
            	mLayoutNo = Integer.parseInt(item);
    			mSmartTag.setLayoutNo(mLayoutNo);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});

        //ラジオボタン変更イベント設定
    	RadioGroup group = (RadioGroup)findViewById(R.id.radioGroup1);
    	group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				onRadioChanged(checkedId);
			}
		});

    	//NFC機能初期設定
        try{
        	mAdapter = NfcAdapter.getDefaultAdapter(this);
        	if(mAdapter == null){
	        	mErrorView.setText(getString(R.string.msg_nfcDisabled));
	        }else{
		        mPendingIntent = PendingIntent.getActivity(this, 0,
		        		new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		        try{
		        	filter.addDataType("*/*");
		        }catch(Exception e){
		        	mErrorView.setText(e.getMessage());
		        	return;
		        }
		        mFilters = new IntentFilter[]{
		        		filter,
		        	};
		        mTechLists = new String[][]{new String[] { NfcF.class.getName() }};
	        }
        }catch(Exception e){
        	Common.addLoge(e.toString());
        }

    }

    @Override
    protected void onResume() {
    	super.onResume();

    	Common.addLogi("start adapter");
    	if(mAdapter != null){
    		mAdapter.enableForegroundDispatch(
    				this, mPendingIntent, mFilters, mTechLists);
    		if(!mIsBusy){
	    		mErrorView.setText(
	    				String.format("%s",
	    						getString(R.string.scanTagPrompt)));
    		}
    	}
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	if(this.isFinishing()){
    		Common.addLogi("stop adapter");
        	try{
        		if(mAdapter != null)
        			mAdapter.disableForegroundDispatch(this);
        	}catch(Exception e){
        		Common.addLoge(e.getMessage());
        	}
    	}
    }

    @Override
	protected void onNewIntent(Intent intent){
		if(mIsBusy){
			return;
		}

		//IDmを表示
    	Tag tag = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    	byte[] idm = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
    	mIdView.setText("ID: " + Common.makeHexText(idm));

		mSmartTag.selectTarget(idm, tag);

		if(!mSmartTag.isSmartTag()){
			mErrorView.setText(getString(R.string.scanTagPrompt));
			return;
		}

		//非同期処理クラス初期化
        mTagTask = new SmartTagTask();

		setParameter();
		mIsBusy = true;
		mTagTask.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode == SHOW_EDIT){
    		//表示文字編集画面からの戻り
    		if(data != null){
	    		//表示文字編集画面から
	    		String text = data.getCharSequenceExtra("TEXT").toString();
	    		if(text.equals("")){
	    			mIsEdited = false;
	    			text = getString(R.string.msgTouchToEdit);
	    			mSmartTag.setDrawText(" ");
	    		}else{
	    			mIsEdited = true;
	    			mSmartTag.setDrawText(text);
	    		}
	    		mUserTextView.setText(text);
    		}

    	}else if(requestCode == SHOW_CAMERA_PREVIEW){
    		//カメラプレビュー画面からの戻り
    		if(resultCode == RESULT_OK){
    			Bitmap bitmap = (Bitmap)data.getParcelableExtra("BITMAP");

				DisplayPainter painter = new DisplayPainter();
				painter.putImage(bitmap, 0, 0, true);
				bitmap.recycle();

				bitmap = painter.getPreviewImage();
				mSmartTag.setCameraImage(bitmap);

				mCameraPreview.setVisibility(View.VISIBLE);
				mCameraPreview.setImageBitmap(bitmap);
			}
    	}else if(requestCode == SHOW_URL_EDIT){
    		//URL編集
    		if(data != null){
	    		//表示文字編集画面から
	    		String text = data.getCharSequenceExtra("TEXT").toString().trim();
	    		if(text.equals("")){
	    			text = "http://";
	    			mSmartTag.setWriteText("");
	    		}else{
	    			mSmartTag.setWriteText(text);
	    		}
	    		mUrlView.setText(text);
    		}
    	}
    }

    /**
     * ラジオボタン選択時の処理を行う。
     * @param checkedId
     */
    private void onRadioChanged(int checkedId){

    	mUserTextView.setVisibility(View.GONE);
    	mCameraItemLayout.setVisibility(View.GONE);
    	mSaveImageLayout.setVisibility(View.GONE);
    	mSelectImageLayout.setVisibility(View.GONE);
    	mWriteDataLayout.setVisibility(View.GONE);
    	mCaution1.setVisibility(View.GONE);

    	if(checkedId == R.id.radioShowStatus){
    		mSmartTag.setFunctionNo(SmartTag.FN_SHOW_STATUS);
    	}else if(checkedId == R.id.radioShowUserImage){
			mSelectImageLayout.setVisibility(View.VISIBLE);
			mSmartTag.setFunctionNo(SmartTag.FN_CHANGE_LAYOUT);
		}else if(checkedId == R.id.radioShowCameraImage){
			mCameraItemLayout.setVisibility(View.VISIBLE);
			mSmartTag.setFunctionNo(SmartTag.FN_DRAW_CAMERA_IMAGE);
		}else if(checkedId == R.id.radioDrawText){
			mUserTextView.setVisibility(View.VISIBLE);
			mSmartTag.setFunctionNo(SmartTag.FN_DRAW_TEXT);
		}else if(checkedId == R.id.radioSaveImage){
    		mSaveImageLayout.setVisibility(View.VISIBLE);
    		mSmartTag.setFunctionNo(SmartTag.FN_SAVE_LAYOUT);
		}else if(checkedId == R.id.radioWriteData){
			mWriteDataLayout.setVisibility(View.VISIBLE);
			mSmartTag.setFunctionNo(SmartTag.FN_WRITE_DATA);
		}else if(checkedId == R.id.radioReadData){
			mCaution1.setVisibility(View.VISIBLE);
			mSmartTag.setFunctionNo(SmartTag.FN_READ_DATA);
		}
	}

    /**
     * 機能番号とパラメータを設定する
     */
    private void setParameter()
    {
    	switch(mSmartTag.getFunctionNo()){
    	case SmartTag.FN_SAVE_LAYOUT:
    		//登録番号を取得
			String item = (String)mSaveNoList.getSelectedItem();
        	mSmartTag.setSaveNo(Integer.parseInt(item));
    		break;
    	case SmartTag.FN_WRITE_DATA:
    		mSmartTag.setWriteText(mUrlView.getText().toString());
    		break;
    	}
    }

    /**
     * テキスト編集画面を表示
     * @param v
     */
    private void showEditActivity(View v){
    	Intent intent = new Intent(this, EditActivity.class);
    	String text = "";
    	if(mIsEdited){
    		text = mUserTextView.getText().toString();
    	}
    	intent.putExtra("TEXT", text);
    	intent.putExtra("REQ_CODE", SHOW_EDIT);
    	startActivityForResult(intent, SHOW_EDIT);
    }

    /**
     * URL編集画面を表示
     * @param v
     */
    private void showUrlEditActivity(View v){
    	Intent intent = new Intent(this, EditActivity.class);
    	String text =  mUrlView.getText().toString();

    	intent.putExtra("TEXT", text);
    	intent.putExtra("REQ_CODE", SHOW_URL_EDIT);
    	startActivityForResult(intent, SHOW_URL_EDIT);
    }

    /**
     * 次のタグスキャンメッセージを時間差で表示する
     */
    private void showNextScanMessage(){
    	Timer timer1 = new Timer();
		timer1.schedule(new TimerTask() {
			public void run() {
				mHandler.post(new Runnable() {
					public void run() {
						if(!mIsBusy){
							mErrorView.setText(getString(R.string.scanTagPrompt));
						}
					}
				});
			}
		}, 1500);
    }

    /**
     * URLを開く
     * @param url
     */
    private void openUrl(String url){
    	try{
	    	if(!url.startsWith("http://")){
	    		return;
	    	}
	    	Intent intent =
	    			new Intent(Intent.ACTION_VIEW, Uri.parse(url));
	    	startActivity(intent);
    	}catch(Exception err){
    		Common.addLoge(err.toString());
    	}

    }

    /**
     * スマートタグとの通信を非同期で実行します。
     */
    private class SmartTagTask extends AsyncTask<Void, Void, Void> {

    	@Override
    	protected void onPreExecute() {
    		mErrorView.setText(
    				String.format("%s",
    					getString(R.string.msgProcessing)));
    	}

    	@Override
    	protected Void doInBackground(Void... params) {

    		mSmartTag.startSession();
    		return null;
    	}

    	@Override
    	protected void onProgressUpdate(Void... values) {
    		super.onProgressUpdate(values);
    	}

    	@Override
    	protected void onPostExecute(Void result) {

    		Exception error = mSmartTag.getLastError();
    		if(error != null){
    			mErrorView.setText(
	    				String.format("%s",
	    					getString(R.string.msgIoError)));
    		}else{

	    		int function = mSmartTag.getFunctionNo();
				if(function == SmartTag.FN_CHANGE_LAYOUT){
					//次の番号に切り替える
					mLayoutNo++;
	    			if(mLayoutNo > 12){
	    				mLayoutNo = 1;
	    			}
	    			mLayoutList.setSelection(mLayoutNo - 1);
				}else if(function == SmartTag.FN_READ_DATA){
					//URLを開く
					openUrl(mSmartTag.getReadText());
					//showAlert(mSmartTag.getReadText());
				}

	    		mErrorView.setText(
	    				String.format("%s",
	    					getString(R.string.msgDone)));


	    		showNextScanMessage();
    		}
    		mIsBusy = false;

    	}
    }


}
