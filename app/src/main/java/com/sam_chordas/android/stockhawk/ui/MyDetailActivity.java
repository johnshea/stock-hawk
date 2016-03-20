package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;

public class MyDetailActivity extends Activity {

    private static String LOG_TAG = MyDetailActivity.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();

    String symbol;
    TextView tvStockSymbol;
    LineChartView lineChartView;

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_detail);

        Intent intent = getIntent();
        if ( intent.hasExtra("SYMBOL") ) {
            symbol = intent.getStringExtra("SYMBOL");
            tvStockSymbol = (TextView) findViewById(R.id.stock_symbol_textview);
            tvStockSymbol.setText(symbol);

            DownloadQuoteDetailsTask quoteDetailsTask = new DownloadQuoteDetailsTask();
            quoteDetailsTask.execute(symbol);
        }

        // Query URL for last 1 year of stock data
        // Bucket the data and create averages
        // Plot the data on the chart
        // Display the chart

    }

    private class DownloadQuoteDetailsTask extends AsyncTask<String, Void, float[]> {
        @Override
        protected float[] doInBackground(String... params) {

            String stockInput = params[0];

            StringBuilder urlStringBuilder = new StringBuilder();

            try{
                // Base URL for the Yahoo query
                urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                urlStringBuilder.append(URLEncoder.encode("select Date, Close from yahoo.finance.historicaldata where symbol "
                        + "in (", "UTF-8"));
                urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));

                // Determine going back 1 year
                Calendar startDate = Calendar.getInstance();
                Calendar endDate = Calendar.getInstance();
                startDate.add(Calendar.YEAR, -1);
                endDate.add(Calendar.DAY_OF_WEEK, -1);

                String startMonth = null;
                String endMonth = null;

                startMonth = "00" + (startDate.get(Calendar.MONTH)+1);
                startMonth = startMonth.substring(startMonth.length() - 2);

                endMonth = "00" + (endDate.get(Calendar.MONTH)+1);
                endMonth = endMonth.substring(endMonth.length() - 2);

                String startDay = null;
                String endDay= null;

                startDay = "00" + (startDate.get(Calendar.DAY_OF_MONTH)+1);
                startDay = startDay.substring(startDay.length() - 2);

                endDay = "00" + (endDate.get(Calendar.DAY_OF_MONTH)+1);
                endDay = endDay.substring(endDay.length() - 2);


                String startDateString = startDate.get(Calendar.YEAR)
                        + "-" + startMonth
                        + "-" + startDay;

                String endDateString = endDate.get(Calendar.YEAR)
                        + "-" + endMonth
                        + "-" + endDay;

                Log.d(LOG_TAG, "startDate = " + startDateString);
                Log.d(LOG_TAG, "endDate = " + endDateString);

                urlStringBuilder.append(URLEncoder.encode("and startDate=\"" + startDateString + "\" and endDate=\"" + endDateString + "\"", "UTF-8"));
                urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                        + "org%2Falltableswithkeys&callback=");

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String urlString;
            urlString = urlStringBuilder.toString();

            String getResponse = "";

            try {
                getResponse = fetchData(urlString);
            } catch (IOException e){
                e.printStackTrace();
            }

            Log.d(LOG_TAG, getResponse);

            JSONObject jsonObject = null;
            JSONArray resultsArray = null;

            BuildChart buildChart = new BuildChart();
            float[] results;

            try {
                jsonObject = new JSONObject(getResponse);
                jsonObject = jsonObject.getJSONObject("query");
                resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                if (resultsArray != null && resultsArray.length() != 0){
                    for (int i = 0; i < resultsArray.length(); i++){
                        jsonObject = resultsArray.getJSONObject(i);
                        buildChart.addDataPoint(jsonObject);
                    }
                }

            } catch (JSONException e){
                Log.e(LOG_TAG, "String to JSON failed: " + e);
            }

            results = buildChart.getAverages();

            return results;
        }

        @Override
        protected void onPostExecute(float[] results) {
            super.onPostExecute(results);

            float min = 10000;
            float max = -1000;

            for(int i=0; i < results.length; i++) {
                if (results[i] < min) {
                    min = results[i];
                }
                if (results[i] > max ){
                    max = results[i];
                }
            }
            final String[] mLabels= {"Jan", "Feb", "Mar", "Apr", "May", "June",
                    "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"};
            LineSet dataset = new LineSet(mLabels, results);
            dataset.setColor(Color.parseColor("#758cbb"))
                    .setFill(Color.parseColor("#2d374c"))
                    .setDotsColor(Color.parseColor("#758cbb"))
                    .setThickness(4)
                    .setDashed(new float[]{10f,10f})
                    .beginAt(0);

            lineChartView = (LineChartView) findViewById(R.id.linechart);
            lineChartView.addData(dataset);
            int minimumValue = (int)min;
            int maximumValue = (int)max;

            if (minimumValue % 10 != 0) {
                minimumValue = ((int) (minimumValue/10)) * 10;
            }

            if (maximumValue % 10 != 0) {
                maximumValue =  (((int)(maximumValue/10))+1) * 10;
            }

            lineChartView.setAxisBorderValues(minimumValue, maximumValue, 10);
            lineChartView.show();

        }
    }

    public class BuildChart {

        float[] monthlyTotal;
        int[] monthlyCount;

        public BuildChart() {
            monthlyTotal = new float[12];
            monthlyCount = new int[12];
        }

        public void addDataPoint(JSONObject jsonObject) {
            try {
                String close = jsonObject.getString("Close");
                String closeDate = jsonObject.getString("Date");
                int currentMonth = Integer.valueOf(closeDate.substring(5,7))-1;
                monthlyCount[currentMonth]++;
                monthlyTotal[currentMonth] += Float.valueOf(close);
            } catch (JSONException e){
                Log.e(LOG_TAG, "String to JSON failed: " + e);
            }
        }

        public float[] getAverages() {
            float[] monthlyAverages = new float[12];
            for(int i=0; i < monthlyAverages.length; i++) {
                monthlyAverages[i] = monthlyTotal[i] / monthlyCount[i];
            }

            return monthlyAverages;
        }
    }
}
