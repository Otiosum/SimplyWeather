package akos.online.simplyweather;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import akos.online.database.DatabaseOpenHelper;


public class DetailsActivity extends AppCompatActivity {
    private TextView name;
    private TextView temp;
    private ImageView img;

    private RecyclerView forecast_rv;
    private LinearLayoutManager forecast_llm;

    private int mPosition;
    private double[] mCoords = {0.0, 0.0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_details);

        name = (TextView) findViewById(R.id.det_city_txt);
        name.setText("");
        temp = (TextView) findViewById(R.id.det_temp_txt);
        img = (ImageView) findViewById(R.id.det_cond_icon);

        forecast_rv = (RecyclerView) findViewById(R.id.forecast_rv);
        forecast_llm = new LinearLayoutManager(this);
        forecast_rv.setLayoutManager(forecast_llm);

        // Make sure activity starts at the top
        forecast_rv.setFocusable(false);
        ScrollView sv = (ScrollView) findViewById(R.id.det_scroller);
        sv.requestFocus();

        mPosition = getIntent().getIntExtra("EXTRA_POSITION", 1);

        ImageButton imgBtn = (ImageButton) findViewById(R.id.det_top_img_btn);
        imgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DetailsActivity.this, MapActivity.class).putExtra("EXTRA_COORDS", mCoords).putExtra("EXTRA_LOC_NAME", name.getText().toString().trim()));
            }
        });

        //When user wants to see home location, get ID
        int homeRes = getIntent().getIntExtra("EXTRA_HOME", 0);
        if(homeRes != 0) {
            mPosition = homeRes;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        DatabaseOpenHelper doh = new DatabaseOpenHelper(this);
        SQLiteDatabase db = doh.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM cities WHERE id = " + String.valueOf(mPosition), null);

        if(c.moveToFirst()) {
            name.setText(c.getString(0));
            Log.d("NAME: ", name.getText().toString());
        }

        //DB can be unreliable, error handling/checking here
        if(name.getText().toString().equals("")) {
            finish();
            Toast.makeText(this, "Failed to load this city's information", Toast.LENGTH_SHORT).show();
        }

        // Get current weather information and forecast information
        String s = name.getText().toString().split(",")[0];
        Log.d("NAME: ", s);
        new GetLocationDataTask().execute(name.getText().toString().split(",")[0]);
        new GetLocationForecastTask().execute(name.getText().toString().split(",")[0]);
    }

    private class GetLocationDataTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return WeatherConnectionSupport.lookUp(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            String[] resFragments = result.split("#");

            if(resFragments[0].contains("Exception") || resFragments.length < 2) {
                showErrorDialog(resFragments[0], getString(R.string.err_loading_weather));
            }
            else {
                mCoords[1] = Double.parseDouble(resFragments[1].split(",")[0]);
                mCoords[0] = Double.parseDouble(resFragments[1].split(",")[1]);

                TextView desc = (TextView) findViewById(R.id.det_cond_desc);
                TextView tempCurrent = (TextView) findViewById(R.id.det_temp_txt);
                TextView tempDay = (TextView) findViewById(R.id.det_temp_day);
                TextView tempNight = (TextView) findViewById(R.id.det_temp_night);
                TextView clouds = (TextView) findViewById(R.id.det_clouds_val);
                TextView pressure = (TextView) findViewById(R.id.det_pressure_val);
                TextView humid = (TextView) findViewById(R.id.det_humidity_val);
                TextView windSpeed = (TextView) findViewById(R.id.det_wind_speed_val);

                desc.setText(resFragments[2]);
                tempCurrent.setText(resFragments[3].split(",")[0] + "°");
                tempDay.setText(resFragments[3].split(",")[1] + "°");
                tempNight.setText(resFragments[3].split(",")[2] + "°");
                clouds.setText(resFragments[5] + "%");
                pressure.setText(resFragments[3].split(",")[4] + " hPa");
                humid.setText(resFragments[3].split(",")[4] + "%");
                windSpeed.setText(resFragments[4] + " m/s");

                chooseIcon(img, Double.parseDouble(resFragments[3].split(",")[0]), Integer.parseInt(resFragments[5]), resFragments[2] );
            }
        }
    }

    private class GetLocationForecastTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return WeatherConnectionSupport.lookUpForecast(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            String[] resFragments = result.split("#");

            RVAdapter adapter = new RVAdapter(resFragments);
            forecast_rv.setAdapter(adapter);
        }
    }

    //Holds views that will be printed in the recycler view (These will be repeated just like in a listview)
    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView date;
        TextView condition;
        TextView temp;
        TextView windSpeed;
        ImageView conditionIcon;

        ItemViewHolder(View itemView) {
            super(itemView);

            date = (TextView) itemView.findViewById(R.id.forecast_date);
            condition = (TextView) itemView.findViewById(R.id.forecast_cond);
            temp = (TextView) itemView.findViewById(R.id.forecast_temp);
            windSpeed = (TextView) itemView.findViewById(R.id.forecast_wind_speed);
            conditionIcon = (ImageView) itemView.findViewById(R.id.forecast_img);
        }
    }

    //Stores list of items that will be printed (similar to listview)
    private class RVAdapter extends RecyclerView.Adapter<ItemViewHolder> {
        protected List<String[]> items;
        protected String item;

        RVAdapter(String[] newItems) {
            items = new ArrayList<>();
            for(int i = 0; i < newItems.length; i++) {
                items.add(newItems[i].split(","));
            }
        }

        RVAdapter(String singleItem) { this.item = singleItem; }

        @Override
        public int getItemCount() {
            if(items != null) {
                return items.size();
            }
            return 1;
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup vGroup, int viewType) {
            View v = LayoutInflater.from(vGroup.getContext()).inflate(R.layout.rv_forecast_element, vGroup, false);
            ItemViewHolder pvh = new ItemViewHolder(v);
            return pvh;
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            if(items == null || items.size() == 0) {
                holder.date.setText(item);
            }
            //This is here to handle an IO exception that randomly occurs...
            else if(items.get(position)[0].contains("Exception")) {
                showErrorDialog(items.get(position)[0], getString(R.string.err_loading_forecast));
            }
            else {
                Date dt = new Date(Long.parseLong(items.get(position)[0]) * 1000L);
                SimpleDateFormat sdf = new SimpleDateFormat("EEE (d MMM)");

                holder.date.setText(sdf.format(dt).toString());
                holder.temp.setText(items.get(position)[1] + "° / " + items.get(position)[2] + "°");
                holder.condition.setText(items.get(position)[3]);
                holder.windSpeed.setText(items.get(position)[4] + " m/s");

                double avg = (Double.parseDouble(items.get(position)[1]) - Double.parseDouble(items.get(position)[2])) / 2;
                chooseIcon(holder.conditionIcon, avg, 0, items.get(position)[3]);
            }
        }
    }

    //Simple logic that decides what icon to show,depending on some weather factors
    public static void chooseIcon(ImageView img, double temp, int clouds, String description) {
        if(temp > 0) {
            if(description.contains("rain")) {
                img.setImageResource(R.drawable.ic_showers);
            }
            else {
                if(clouds > 30) {
                    if(clouds > 60) {
                        img.setImageResource(R.drawable.ic_completely_cloudy);
                    }
                }
                else if (clouds < 5){
                    img.setImageResource(R.drawable.ic_fair);
                }
                else {
                    img.setImageResource(R.drawable.ic_cloudy);
                }
            }
        }
        else {
            if(description.contains("rain")) {
                img.setImageResource(R.drawable.ic_showers);
            }
            else if(description.contains("snow")) {
                img.setImageResource(R.drawable.ic_snow);
            }
            else {
                if(clouds > 30) {
                    if (clouds > 60) {
                        img.setImageResource(R.drawable.ic_completely_cloudy);
                    }
                }
                else if(clouds < 5){
                    img.setImageResource(R.drawable.ic_fair);
                }
                else {
                    img.setImageResource(R.drawable.ic_cloudy);
                }
            }
        }
        img.setColorFilter(R.color.colorWeatherIcon, PorterDuff.Mode.MULTIPLY);
    }

    private void showErrorDialog(String e, String msg) {
        Log.d("EXCEPTION_TAG", e.toString());

        AlertDialog.Builder builder = new AlertDialog.Builder(DetailsActivity.this);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(DetailsActivity.this, MainActivity.class));
                    }
                })
                .setNegativeButton("QUIT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
