package baidumapsdk.demo;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.MyLocationOverlay.LocationMode;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.mapapi.map.PopupClickListener;
import com.baidu.mapapi.map.PopupOverlay;
import com.baidu.platform.comapi.basestruct.GeoPoint;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;



/*
 * 要处理overlay点击事件时需要继承ItemizedOverlay
 * 不处理点击事件时可直接生成ItemizedOverlay.
 */
class OverlayTest extends ItemizedOverlay<OverlayItem> {
    
	//用MapView构造ItemizedOverlay
    public OverlayTest(Drawable mark,MapView mapView){
            super(mark,mapView);
    }
    protected boolean onTap(int index) {
        //在此处理item点击事件
        System.out.println("item onTap: "+index);
        return true;
    }
    public boolean onTap(GeoPoint pt, MapView mapView){
                //在此处理MapView的点击事件，当返回 true时
                super.onTap(pt,mapView);
                return false;
    }

}       
/**
 * 此demo用来展示如何结合定位SDK实现定位，并使用MyLocationOverlay绘制定位位置
 * 同时展示如何使用自定义图标绘制并点击时弹出泡泡
 *
 */

public class LocationOverlayDemo extends Activity {
	private enum E_BUTTON_TYPE {
		LOC,
		COMPASS,
		FOLLOW,
	}
	
	private E_BUTTON_TYPE mCurBtnType;

	// 定位相关
	LocationClient mLocClient;
	LocationData locData = null;
	public MyLocationListenner myListener = new MyLocationListenner();
	
	//定位图层
	locationOverlay myLocationOverlay = null;
	//弹出泡泡图层
	private PopupOverlay   pop  = null;//弹出泡泡图层，浏览节点时使用
	private TextView  popupText = null;//泡泡view
	private View viewCache = null;
	
	//地图相关，使用继承MapView的MyLocationMapView目的是重写touch事件实现泡泡处理
	//如果不处理touch事件，则无需继承，直接使用MapView即可
	MyLocationMapView mMapView = null;	// 地图View
	private MapController mMapController = null;

	//UI相关
	OnCheckedChangeListener radioButtonListener = null;
	Button requestLocButton = null;
	boolean isRequest = false;//是否手动触发请求定位
	boolean isFirstLoc = true;//是否首次定位
	
	Button captureButton = null;
	Button uploadButton = null;
	
	
	public void post(String url, List<NameValuePair> nameValuePairs) {
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpContext localContext = new BasicHttpContext();
	    HttpPost httpPost = new HttpPost(url);

	    try {
	        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

	        for(int index=0; index < nameValuePairs.size(); index++) {
	            if(nameValuePairs.get(index).getName().equalsIgnoreCase("image")) {
	                // If the key equals to "image", we use FileBody to transfer the data
	                entity.addPart(nameValuePairs.get(index).getName(), new FileBody(new File (nameValuePairs.get(index).getValue())));
	            } else {
	                // Normal string data
	                entity.addPart(nameValuePairs.get(index).getName(), new StringBody(nameValuePairs.get(index).getValue()));
	            }
	        }

	        httpPost.setEntity(entity);

	        HttpResponse response = httpClient.execute(httpPost, localContext);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}	
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locationoverlay);
        CharSequence titleLable="定位功能";
        setTitle(titleLable);
        
        captureButton = (Button)findViewById(R.id.button_capture);
        uploadButton = (Button)findViewById(R.id.upload_picture);
        
        
        requestLocButton = (Button)findViewById(R.id.button1);
        mCurBtnType = E_BUTTON_TYPE.LOC;
        OnClickListener btnClickListener = new OnClickListener() {
        	public void onClick(View v) {
				if(v==requestLocButton){
        		switch (mCurBtnType) {
				case LOC:
					//手动定位请求
					requestLocClick();
					break;
				case COMPASS:
					myLocationOverlay.setLocationMode(LocationMode.NORMAL);
					requestLocButton.setText("定位");
					mCurBtnType = E_BUTTON_TYPE.LOC;
					break;
				case FOLLOW:
					myLocationOverlay.setLocationMode(LocationMode.COMPASS);
					requestLocButton.setText("罗盘");
					mCurBtnType = E_BUTTON_TYPE.COMPASS;
					break;
					}
        		} //end if v==
        		else if(v == captureButton){
           			captureButton.setText("照相");
        			Intent intent = new Intent();
        			intent.setClass(LocationOverlayDemo.this, AndroidCameraActivity.class);
        			LocationOverlayDemo.this.startActivity(intent);	
					
        			//准备要添加的Overlay
					double mLat1 = Double.parseDouble(GlobalClass.locationx);
					double mLon1 = Double.parseDouble(GlobalClass.locationy);
					double mLat2 = Double.parseDouble(GlobalClass.locationx);
					double mLon2 = Double.parseDouble(GlobalClass.locationy);
					double mLat3 = Double.parseDouble(GlobalClass.locationx);
					double mLon3 = Double.parseDouble(GlobalClass.locationy);
					// 用给定的经纬度构造GeoPoint，单位是微度 (度 * 1E6)
					GeoPoint p1 = new GeoPoint((int) (mLat1 * 1E6), (int) (mLon1 * 1E6));
					GeoPoint p2 = new GeoPoint((int) (mLat2 * 1E6), (int) (mLon2 * 1E6));
					GeoPoint p3 = new GeoPoint((int) (mLat3 * 1E6), (int) (mLon3 * 1E6));
					//准备overlay图像数据，根据实情情况修复
					Drawable mark= getResources().getDrawable(R.drawable.icon_marka);
					//用OverlayItem准备Overlay数据
					OverlayItem item1 = new OverlayItem(p1,"item1","item1");
					//使用setMarker()方法设置overlay图片,如果不设置则使用构建ItemizedOverlay时的默认设置
					OverlayItem item2 = new OverlayItem(p2,"item2","item2");
					item2.setMarker(mark);
					OverlayItem item3 = new OverlayItem(p3,"item3","item3");
					 
					//创建IteminizedOverlay
					OverlayTest itemOverlay = new OverlayTest(mark, mMapView);
					//将IteminizedOverlay添加到MapView中

					mMapView.getOverlays().clear();
					mMapView.getOverlays().add(itemOverlay);
					 
					//现在所有准备工作已准备好，使用以下方法管理overlay.
					//添加overlay, 当批量添加Overlay时使用addItem(List&lt;OverlayItem&gt;)效率更高
					itemOverlay.addItem(item1);
					itemOverlay.addItem(item2);
					itemOverlay.addItem(item3);
					mMapView.refresh();
					//删除overlay .
					//itemOverlay.removeItem(itemOverlay.getItem(0));
					//mMapView.refresh();
					//清除overlay
					// itemOverlay.removeAll();
					// mMapView.refresh();
				}
        		else if (v  == uploadButton)
        		{
        			Intent intent = new Intent();
        			intent.setClass(LocationOverlayDemo.this, MainActivity.class);
        			LocationOverlayDemo.this.startActivity(intent);	
        		}
        	
			}
		};
	    requestLocButton.setOnClickListener(btnClickListener);
	    captureButton.setOnClickListener(btnClickListener);
	    uploadButton.setOnClickListener(btnClickListener);
	    
       // RadioGroup group = (RadioGroup)this.findViewById(R.id.radioGroup);
        //radioButtonListener = new OnCheckedChangeListener() {
			
			//@Override
			/*public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.defaulticon){
					//传入null则，恢复默认图标
					modifyLocationOverlayIcon(null);
				}
				if (checkedId == R.id.customicon){
					//修改为自定义marker
					modifyLocationOverlayIcon(getResources().getDrawable(R.drawable.icon_geo));
				}
			}
		};
		group.setOnCheckedChangeListener(radioButtonListener);*/
        
		//地图初始化
        mMapView = (MyLocationMapView)findViewById(R.id.bmapView);
        mMapController = mMapView.getController();
        mMapView.getController().setZoom(14);
        mMapView.getController().enableClick(true);
        mMapView.setBuiltInZoomControls(true);
      //创建 弹出泡泡图层
        createPaopao();
        
        //定位初始化
        mLocClient = new LocationClient( this );
        locData = new LocationData();
        mLocClient.registerLocationListener( myListener );
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);//打开gps
        option.setCoorType("bd09ll");     //设置坐标类型
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();
       
        //定位图层初始化
		myLocationOverlay = new locationOverlay(mMapView);
		//设置定位数据
	    myLocationOverlay.setData(locData);
	    //添加定位图层
		mMapView.getOverlays().add(myLocationOverlay);
		myLocationOverlay.enableCompass();
		//修改定位数据后刷新图层生效
		mMapView.refresh();
		
    }
    /**
     * 手动触发一次定位请求
     */
    public void requestLocClick(){
    	isRequest = true;
        mLocClient.requestLocation();
        Toast.makeText(LocationOverlayDemo.this, "正在定位……", Toast.LENGTH_SHORT).show();
    }
    /**
     * 修改位置图标
     * @param marker
     */
    public void modifyLocationOverlayIcon(Drawable marker){
    	//当传入marker为null时，使用默认图标绘制
    	myLocationOverlay.setMarker(marker);
    	//修改图层，需要刷新MapView生效
    	mMapView.refresh();
    }
    /**
	 * 创建弹出泡泡图层
	 */
	public void createPaopao(){
		viewCache = getLayoutInflater().inflate(R.layout.custom_text_view, null);
        popupText =(TextView) viewCache.findViewById(R.id.textcache);
        //泡泡点击响应回调
        PopupClickListener popListener = new PopupClickListener(){
			@Override
			public void onClickedPopup(int index) {
				Log.v("click", "clickapoapo");
			}
        };
        pop = new PopupOverlay(mMapView,popListener);
        MyLocationMapView.pop = pop;
	}
	/**
     * 定位SDK监听函数
     */
    public class MyLocationListenner implements BDLocationListener {
    	

    	
    
    	
    	
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null)
                return ;
            
            locData.latitude = location.getLatitude();
            locData.longitude = location.getLongitude();
            GlobalClass.locationx = Double.toString(locData.latitude);
            GlobalClass.locationy = Double.toString(locData.longitude);
            
            
            
            //如果不显示定位精度圈，将accuracy赋值为0即可
            locData.accuracy = location.getRadius();
            // 此处可以设置 locData的方向信息, 如果定位 SDK 未返回方向信息，用户可以自己实现罗盘功能添加方向信息。
            locData.direction = location.getDerect();
            //更新定位数据
            myLocationOverlay.setData(locData);
            //更新图层数据执行刷新后生效
            mMapView.refresh();
            //是手动触发请求或首次定位时，移动到定位点
            if (isRequest || isFirstLoc){
            	//移动地图到定位点
            	Log.d("LocationOverlay", "receive location, animate to it");
                mMapController.animateTo(new GeoPoint((int)(locData.latitude* 1e6), (int)(locData.longitude *  1e6)));
                isRequest = false;
                myLocationOverlay.setLocationMode(LocationMode.FOLLOWING);
				requestLocButton.setText("跟随");
                mCurBtnType = E_BUTTON_TYPE.FOLLOW;
            }
            //首次定位完成
            isFirstLoc = false;
        }
        
        public void onReceivePoi(BDLocation poiLocation) {
            if (poiLocation == null){
                return ;
            }
        }
    }
    
    //继承MyLocationOverlay重写dispatchTap实现点击处理
  	public class locationOverlay extends MyLocationOverlay{

  		public locationOverlay(MapView mapView) {
  			super(mapView);
  			// TODO Auto-generated constructor stub
  		}
  		@Override
  		protected boolean dispatchTap() {
  			// TODO Auto-generated method stub
  			//处理点击事件,弹出泡泡
  			popupText.setBackgroundResource(R.drawable.popup);
			popupText.setText("我的位置");
			pop.showPopup(BMapUtil.getBitmapFromView(popupText),
					new GeoPoint((int)(locData.latitude*1e6), (int)(locData.longitude*1e6)),
					8);
  			return true;
  		}
  		
  	}

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }
    
    @Override
    protected void onDestroy() {
    	//退出时销毁定位
        if (mLocClient != null)
            mLocClient.stop();
        mMapView.destroy();
        super.onDestroy();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	mMapView.onSaveInstanceState(outState);
    	
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	mMapView.onRestoreInstanceState(savedInstanceState);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

}
/**
 * 继承MapView重写onTouchEvent实现泡泡处理操作
 * @author hejin
 *
 */
class MyLocationMapView extends MapView{
	static PopupOverlay   pop  = null;//弹出泡泡图层，点击图标使用
	public MyLocationMapView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	public MyLocationMapView(Context context, AttributeSet attrs){
		super(context,attrs);
	}
	public MyLocationMapView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}
	@Override
    public boolean onTouchEvent(MotionEvent event){
		if (!super.onTouchEvent(event)){
			//消隐泡泡
			if (pop != null && event.getAction() == MotionEvent.ACTION_UP)
				pop.hidePop();
		}
		return true;
	}
}

