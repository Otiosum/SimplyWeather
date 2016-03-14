package akos.online.simplyweather;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import akos.online.database.DatabaseOpenHelper;

public class MainActivity extends AppCompatActivity {
    private RecyclerView main_rv;
    private LinearLayoutManager main_llm;

    private int mContextPosition;
    private boolean mItemsEmpty;
    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.toolbar_city_list);
        setSupportActionBar(toolbar);

        main_rv = (RecyclerView) findViewById(R.id.rv);
        main_llm = new LinearLayoutManager(this);
        main_rv.setLayoutManager(main_llm);

        FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.addItem);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddCityActivity.class));
            }
        });

        //Only used to keep track of home location
        mPreferences = this.getSharedPreferences(getString(R.string.saved_home_id), Context.MODE_PRIVATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button
        int id = item.getItemId();

        if (id == R.id.action_help) {
            helpDialog();
        }
        else if(id == R.id.action_delete_db) {
            confirmationDialog(getString(R.string.msg_reset_db), getString(R.string.code_reset_db));
        }
        else if(id == R.id.action_home) {
            int res = getHome();

            // Makes sure a home location exists before opening new activity
            if(res != 0) {
                startActivity(new Intent(MainActivity.this, DetailsActivity.class).putExtra("EXTRA_HOME", res));
            }
            else {
                Toast.makeText(this, getString(R.string.msg_no_home_location), Toast.LENGTH_SHORT).show();
            }
        }
        else if(id == R.id.action_refresh) {
            onResume();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Custom click listeners (long and normal) for recycler view items. This is because a recycler view does not have
        // an easily available click listener like the listview.
        ItemClickSupport.addTo(main_rv).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView view, int pos, View v) {
                startActivity(new Intent(MainActivity.this, DetailsActivity.class).putExtra("EXTRA_POSITION", pos + 1));
            }
        });

        ItemClickSupport.addTo(main_rv).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
                if (mItemsEmpty == false) {
                    registerForContextMenu(recyclerView);
                    mContextPosition = position;
                }
                return false;
            }
        });

        // Get items from database
        DatabaseOpenHelper doh = new DatabaseOpenHelper(this);
        SQLiteDatabase db = doh.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM cities", null);

        // Collect all cities from db, and launch an async task to load their weather data
        if (cursor.moveToFirst()) {
            Toast.makeText(this, "Loading cities...", Toast.LENGTH_SHORT).show();

            String[] cities = new String[cursor.getCount()];
            for (int i = 0; i < cursor.getCount(); i++) {
                cities[i] = cursor.getString(0);
                if (!cursor.moveToNext()) break;
            }
            mItemsEmpty = false;
            new GetLocationDataTask().execute(cities);
        }
        //If there are no cities a card is shown with some text that lets the user know.
        // they need to add something to the list
        else {
            mItemsEmpty = true;
            RVAdapter adapter = new RVAdapter(getString(R.string.msg_empty_city_list));
            main_rv.setAdapter(adapter);
        }
        cursor.close();
    }

    //Holds views in a recycler view element
    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView cityName;
        TextView homeToggle;
        TextView condition;
        TextView tempCurrent;
        ImageView conditionIcon;

        ItemViewHolder(View itemView) {
            super(itemView);

            cityName = (TextView) itemView.findViewById(R.id.city_name);
            condition = (TextView) itemView.findViewById(R.id.current_condition);
            tempCurrent = (TextView) itemView.findViewById(R.id.temp_current);
            homeToggle = (TextView) itemView.findViewById(R.id.city_home_toggle);
            conditionIcon = (ImageView) itemView.findViewById(R.id.condition_icon);
        }
    }

    private class RVAdapter extends RecyclerView.Adapter<ItemViewHolder> {
        protected List<String[]> items;
        protected String item;

        RVAdapter(List<String[]> newItems) {
            this.items = newItems;
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
            View v = LayoutInflater.from(vGroup.getContext()).inflate(R.layout.content_main, vGroup, false);
            ItemViewHolder pvh = new ItemViewHolder(v);
            return pvh;
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            if(items == null || items.size() == 0) {
                holder.cityName.setText(item);
            }
            else{
                //Get home location id from preferences
                int homeID = getHome();

                //Connect to db to find home location
                DatabaseOpenHelper doh = new DatabaseOpenHelper(MainActivity.this);
                SQLiteDatabase db = doh.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT name FROM cities WHERE id = " + homeID, null);

                //If a home location is found, show some text next to the city name in the view
                if (cursor.moveToFirst() && cursor.getString(0).split(",")[0].toLowerCase().equals(items.get(position)[0].split(",")[0].toLowerCase())) {
                    holder.cityName.setText(items.get(position)[0]);
                    holder.homeToggle.setText("(home)");
                } else {
                    holder.cityName.setText(items.get(position)[0]);
                    holder.homeToggle.setText("");
                }
                cursor.close();

                holder.condition.setText(items.get(position)[1]);
                holder.tempCurrent.setText(items.get(position)[2] + "°");
                DetailsActivity.chooseIcon(holder.conditionIcon, Double.parseDouble(items.get(position)[2]), 0, items.get(position)[1]);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);
        menu.add(0, v.getId(), 0, R.string.context_make_home);
        menu.add(0, v.getId(), 0, R.string.context_delete_item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle().toString().equals(getString(R.string.context_make_home))) {
            addToHome();
        }
        else if(item.getTitle().toString().equals(getString(R.string.context_delete_item))) {
            //Make sure the user knows what they are doing before proceeding
            confirmationDialog(getString(R.string.msg_delete_city), getString(R.string.code_delete_element));
        }
        else {
            return super.onContextItemSelected(item);
        }
        return true;
    }

    private void addToHome() {
        // Add home location to preferences
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.saved_home_id), String.valueOf(mContextPosition + 1));
        editor.commit();
        onResume();
    }

    private void deleteElement() {
        DatabaseOpenHelper doh = new DatabaseOpenHelper(this);
        SQLiteDatabase dbw = doh.getWritableDatabase();

        dbw.execSQL("DELETE FROM cities WHERE id = " + (mContextPosition + 1));
        onResume();
    }

    private int getHome() {
        // Returns the saved location id if it is not the same as the default
        SharedPreferences sharedPref =this.getPreferences(Context.MODE_PRIVATE);
        String valueDefault = getResources().getString(R.string.saved_home_id_default);
        String valueSaved = sharedPref.getString(getString(R.string.saved_home_id), valueDefault);

        if(Integer.parseInt(valueDefault) != Integer.parseInt(valueSaved)) {
            return Integer.parseInt(valueSaved);
        }
        return Integer.parseInt(valueDefault);
    }

    private class GetLocationDataTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder builder = new StringBuilder();
            for(int i = 0; i < urls.length; i++) {
                builder.append(WeatherConnectionSupport.lookUp(urls[i]) + "_");
            }
            return builder.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            // Some essential error handling
            if(result.contains("Error:")) {
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            }
            else if(result.contains("Exception") || result.length() <= 1) {
                showErrorDialog(result, getString(R.string.err_loading_cities));
            }
            else {
                String[] resFragments = result.split("_");
                List<String[]> items = new ArrayList<>();

                for(int i = 0; i < resFragments.length; i++) {
                    items.add(resFragments[i].split("#"));

                    //Some strange error handling (if looking through cities fails mid way)
                    if(items.get(i).length == 1) {
                        Log.d("ERROR: ", items.get(i)[0]);
                        items.remove(i);
                        Toast.makeText(MainActivity.this, getString(R.string.err_partial_load_fail), Toast.LENGTH_SHORT).show();
                        break;
                    }
                }

                List<String[]> finItems = new ArrayList<>();
                for(int i = 0; i < items.size(); i++) {
                    String[] element = {items.get(i)[0], items.get(i)[2], items.get(i)[3].split(",")[0]};
                    finItems.add(element);
                }

                RVAdapter adapter = new RVAdapter(finItems);
                main_rv.setAdapter(adapter);
            }
        }
    }

    private void confirmationDialog(final String msg, final String code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Different codes, different outcomes
                        if(code.equals(getString(R.string.code_reset_db))) {
                            DatabaseOpenHelper db = new DatabaseOpenHelper(MainActivity.this);
                            db.onUpgrade(db.getReadableDatabase(), 1, 2);

                            //Reset preferences for home location
                            SharedPreferences.Editor edit = mPreferences.edit();
                            edit.putInt(getString(R.string.saved_home_id), Integer.parseInt(getString(R.string.saved_home_id_default)));
                            edit.commit();

                            onResume();
                        }
                        else if(code.equals(getString(R.string.code_delete_element))) {
                            deleteElement();
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void helpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getString(R.string.msg_help))
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showErrorDialog(String e, String msg) {
        Log.d("EXCEPTION_TAG", e.toString());

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onResume();
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


