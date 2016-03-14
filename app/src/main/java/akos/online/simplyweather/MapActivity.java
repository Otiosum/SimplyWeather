package akos.online.simplyweather;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private double[] mCoords = {0.0, 0.0};
    private String mLocName = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mCoords = getIntent().getDoubleArrayExtra("EXTRA_COORDS");
        mLocName = getIntent().getStringExtra("EXTRA_LOC_NAME");

        Button btn = (Button) findViewById(R.id.btn_wiki);

        //Opens the browser with a wiki link, if possible
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(createURL(mLocName)));
                    startActivity(intent);
                } catch(ActivityNotFoundException anfe) {
                    Toast.makeText(MapActivity.this, getString(R.string.err_missing_browser), Toast.LENGTH_LONG).show();
                    anfe.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        LatLng marker = new LatLng(mCoords[0], mCoords[1]);
        mMap.addMarker(new MarkerOptions().position(marker).title("Marker in " + mLocName));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(marker));
    }

    private String createURL(String location) {
        try {
            return "https://en.wikipedia.org/wiki/" + URLEncoder.encode(location.split(",")[0], "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            return uee.toString();
        }
    }
}
