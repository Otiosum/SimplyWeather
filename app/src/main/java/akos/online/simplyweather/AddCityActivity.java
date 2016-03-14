package akos.online.simplyweather;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import akos.online.database.DatabaseOpenHelper;

public class AddCityActivity extends AppCompatActivity {
    private EditText nameEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_city);

        nameEdit = (EditText) findViewById(R.id.editTextName);
        Button addButton = (Button) findViewById(R.id.addButton);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cityName = nameEdit.getText().toString().trim();

                // Do not allow empty searches
                if(cityName.isEmpty()) {
                    Toast.makeText(AddCityActivity.this, getString(R.string.err_field_empty), Toast.LENGTH_SHORT).show();
                }
                else {
                    // Launch async task
                    new GetLocationDataTask().execute(nameEdit.getText().toString().trim());
                }
            }
        });
    }

    private class GetLocationDataTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return WeatherConnectionSupport.lookUp(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.contains("Error:")) {
                Toast.makeText(AddCityActivity.this, result, Toast.LENGTH_SHORT).show();
            }
            else {
                String[] resFragments = result.split("#");

                // Check if name searched for matches name in the result
                if(resFragments[0].split(",")[0].toLowerCase().compareTo(nameEdit.getText().toString().toLowerCase().trim()) == 0) {
                    add(resFragments);
                }
                else {
                    Toast.makeText(AddCityActivity.this, getString(R.string.err_city_not_found), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void add(String[] res) {
        DatabaseOpenHelper doh = new DatabaseOpenHelper(this);
        SQLiteDatabase db = doh.getWritableDatabase();

        //Do not allow duplicates in db
        if(isElementPresent(doh, res[0])) {
            Toast.makeText(AddCityActivity.this, getString(R.string.err_city_already_present), Toast.LENGTH_SHORT).show();
            return;
        }

        // Store city and country into db
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", res[0].split(",")[0] + ", " + res[0].split(",")[1]);

        db.insert("cities", null, contentValues);
        finish();
    }

    // Check if element already exists in teh database
    private boolean isElementPresent(DatabaseOpenHelper doh, String element) {
        SQLiteDatabase db = doh.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM cities", null);

        if(c.moveToFirst()) {

            for(int i = 0; i < c.getColumnCount(); i++) {
                String name = c.getString(0).split(",")[0];

                if(name.toLowerCase().compareTo(element.split(",")[0].toLowerCase()) == 0) {
                    return true;
                }
            }
        }
        c.close();

        return false;
    }
}
