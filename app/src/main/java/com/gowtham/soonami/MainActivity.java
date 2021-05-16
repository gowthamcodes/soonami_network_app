package com.gowtham.soonami;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String USGS_REQUEST_URL =
            "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2012-01-01&endtime=2012-12-01&minmagnitude=8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TsunamiAsyncTask task = new TsunamiAsyncTask();
        task.execute();
    }

    private void updateUi(Event earthquake) {
        TextView titleTextView = findViewById(R.id.title);
        titleTextView.setText(earthquake.title);

        TextView dateTextView = findViewById(R.id.date);
        dateTextView.setText(getDateString(earthquake.time));

        TextView tsunamiTextView = findViewById(R.id.tsunami_alert);
        tsunamiTextView.setText(getTsunamiAlertString(earthquake.tsunamiAlert));
    }

    private String getTsunamiAlertString(int tsunamiAlert) {
        switch (tsunamiAlert) {
            case 0:
                return getString(R.string.alert_no);
            case 1:
                return getString(R.string.alert_yes);
            default:
                return getString(R.string.alert_not_available);
        }
    }

    private String getDateString(long timeInMilliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss z");
        return formatter.format(timeInMilliseconds);
    }

    private class TsunamiAsyncTask extends AsyncTask<URL, Void, Event> {

        @Override
        protected Event doInBackground(URL... urls) {

            URL url = createUrl(USGS_REQUEST_URL);
            String response = "";
            try {
                response = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error with making request", e);
            }

            Event earthquake = extractFeatureFromJson(response);
            return earthquake;
        }

        @Override
        protected void onPostExecute(Event earthquake) {
            if (earthquake == null) {
                return;
            }
            updateUi(earthquake);
        }

        private Event extractFeatureFromJson(String earthquakeJSON) {

            if (TextUtils.isEmpty(earthquakeJSON)) {
                return null;
            }

            try {
                JSONObject root = new JSONObject(earthquakeJSON);
                JSONArray features = root.getJSONArray("features");
                if (features.length() > 0) {
                    JSONObject firstFeature = features.getJSONObject(0);
                    JSONObject properties = firstFeature.getJSONObject("properties");

                    String title = properties.getString("title");
                    Long time = properties.getLong("time");
                    int tsunami_alert = properties.getInt("tsunami");

                    return new Event(title, time, tsunami_alert);
                }
            }
            catch (JSONException e) {
                Log.e(LOG_TAG, "Error with parsing response JSON", e);
            }
            return null;
        }

        private String makeHttpRequest(URL url) throws IOException {
            String response = "";
            if (url == null) {
                return response;
            }
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;

            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    response = readFromStream(inputStream);
                } else {
                    Log.e(LOG_TAG, "Error response code " + urlConnection.getResponseCode());
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error with connecting network", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null)  {
                    inputStream.close();
                }
            }

            return response;
        }

        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }
    }

}