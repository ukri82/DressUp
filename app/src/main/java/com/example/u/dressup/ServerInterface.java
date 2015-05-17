package com.example.u.dressup;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by u on 15.05.2015.
 */
public class ServerInterface
{
    public interface ImageLoadedCB
    {
        public void imageLoaded(Bitmap result);
    }
    static public class ImageLoadTask extends AsyncTask<Void, Void, Bitmap>
    {

        private String url;
        private ImageLoadedCB myCB;

        public ImageLoadTask(String url, ImageLoadedCB aCB_in)
        {
            this.url = url;
            this.myCB = aCB_in;
        }

        @Override
        protected Bitmap doInBackground(Void... params)
        {
            try
            {
                URL urlConnection = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlConnection
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result)
        {
            super.onPostExecute(result);
            if(myCB != null)
            {
                myCB.imageLoaded(result);
            }

        }

    }

    static public class DressListGetter extends AsyncTask<Void , Void, String >
    {

        private String myDressJSON;

        private DressListLoadedCB myCB;

        public DressListGetter(DressListLoadedCB aCB_in)
        {
            myCB = aCB_in;
        }

        @Override
        protected String doInBackground(Void... params)
        {
            DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());
            HttpPost httppost = new HttpPost("http://80.240.142.76/mfpush/get_all_dress");
            // Depends on your web service
            httppost.setHeader("Content-type", "application/json");

            InputStream inputStream = null;
            String result = null;
            try {
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();

                inputStream = entity.getContent();
                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line + "\n");
                }
                result = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            myDressJSON = result;
            if(myCB != null)
            {
                myCB.dressListLoaded();
            }

        }

        public ArrayList<String> getAllDressURLs()
        {
            ArrayList<String> aDressList = new ArrayList<String>();
            try
            {
                JSONObject jObject = new JSONObject(myDressJSON);
                JSONArray jArray = jObject.getJSONArray("Dresses");

                for (int i=0; i < jArray.length(); i++)
                {
                    try {
                        String oneObject = jArray.get(i).toString();
                        aDressList.add("http://80.240.142.76/mfpush/static/images/Dress/" + oneObject.toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {

            }

            return aDressList;
        }
    }
}
