package baidumapsdk.demo;

import java.io.File;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

public class MainActivity extends Activity {
	/** Called when the activity is first created. */
	
	
	@SuppressLint("NewApi")
	public Bitmap getImageThumbnail(String imagePath, int width, int height) {
		Bitmap bitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		// 获取这个图片的宽和高，注意此处的bitmap为null
		bitmap = BitmapFactory.decodeFile(imagePath, options);
		options.inJustDecodeBounds = false; // 设为 false
		// 计算缩放比
		int h = options.outHeight;
		int w = options.outWidth;
		int beWidth = w / width;
		int beHeight = h / height;
		int be = 1;
		if (beWidth < beHeight) {
			be = beWidth;
		} else {
			be = beHeight;
		}
		if (be <= 0) {
			be = 1;
		}
		options.inSampleSize = be;
		// 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
		bitmap = BitmapFactory.decodeFile(imagePath, options);
		// 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
		bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
				ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		return bitmap;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		final ProgressDialog dlg = new ProgressDialog(this);
		dlg.setMessage("上传中...");
		dlg.show();
		new AsyncTask<Void, Void, HttpResponse>() {
			@Override
			protected HttpResponse doInBackground(Void... params) {
				try {
					HttpPost post = new HttpPost(
							"http://10.46.189.51:8578/location.php?&type=put");
					
				/*	String imagePath = Environment.getExternalStorageDirectory()
							.getAbsolutePath()
							+ File.separator +"Pictures"+File.separator+"IMAGE_20131213_094154.jpg";
				*/
					String imagePath =  GlobalClass.lastFileName;
					
					
					File file = new File(imagePath);
					MultipartEntity multipart = new MultipartEntity();
					
					Bitmap bmp = getImageThumbnail(imagePath,100,100);
					
					
					multipart.addPart("user", new StringBody("zhangchao09"));
				    multipart.addPart("locationx", new StringBody(GlobalClass.locationx));
				    multipart.addPart("locationy", new StringBody(GlobalClass.locationy));
				    multipart.addPart("locationz", new StringBody("0"));
				    multipart.addPart("minipic", new StringBody("zhangchao09"));
				    				
					ContentBody cbFile = new FileBody(file, "image/jpg");
					multipart.addPart("pic", cbFile);
					
				/*	ContentBody mmFile = new FileBody(bmp, "image/jpg");
					multipart.addPart("minipic", mmFile);
					*/
					HttpClient client = new DefaultHttpClient();
					post.setEntity(multipart);
					HttpResponse response = client.execute(post);
					return response;
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}

			@Override
			protected void onPostExecute(HttpResponse result) {
				dlg.dismiss();
				if (result != null) {
					try {
						new AlertDialog.Builder(MainActivity.this)
								.setMessage(
										EntityUtils.toString(result.getEntity()))
								.create().show();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					Toast.makeText(MainActivity.this, "异常啊异常。。。", 0).show();
				}
			}
		}.execute();
	}
}