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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

public class MainActivity extends Activity {
	/** Called when the activity is first created. */
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
								
					multipart.addPart("user", new StringBody("zhangchao09"));
				    multipart.addPart("locationx", new StringBody(GlobalClass.locationx));
				    multipart.addPart("locationy", new StringBody(GlobalClass.locationy));
				    multipart.addPart("locationz", new StringBody("0"));
				    multipart.addPart("minipic", new StringBody("zhangchao09"));
				    				
					ContentBody cbFile = new FileBody(file, "image/jpg");
					multipart.addPart("pic", cbFile);
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