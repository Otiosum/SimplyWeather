package akos.online.simplyweather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

//A magical place where connection to the API happens
public class WeatherConnectionSupport {
    //connection strings responsible for normal weather info and forecast info
    private static final String API_WEATHER = "http://api.openweathermap.org/data/2.5/weather?q=";
    private static final String API_FORECAST = "http://api.openweathermap.org/data/2.5/forecast/daily?q=";
    private static final String API_WEATHER_ATTR = "&units=metric";
    private static final String API_FORECAST_ATTR = "&mode=json&units=metric&cnt=7";
    private static final String API_ID = "&appid=fa2f564056680e441d472a1589914804";

    public static String lookUp(String cityName) {
        InputStream inpStream = null;
        String result = "";
        String address = createURL(cityName, false);

        try {
            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.connect();

            inpStream = conn.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inpStream, "UTF-8"), 8);
            final StringBuilder stringBuilder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            result = processResponse(stringBuilder.toString());
        } catch(Exception e) {
            result = e.toString();
        }

        return result;
    }

    public static String lookUpForecast(String cityName) {
        InputStream inStream;
        String result;
        String address = createURL(cityName, true);

        try {
            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.connect();

            inStream = conn.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            final StringBuilder stringBuilder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            result = processForecastResponse(stringBuilder.toString());
        } catch(Exception e) {
            result = e.toString();

            //try again..
            if(result.contains("IO")) {
                return lookUpForecast(cityName);
            }
        }

        return result;
    }

    private static String createURL(final String cityName, boolean isForecast) {
        if(isForecast) {
            try {
                return API_FORECAST + URLEncoder.encode(cityName, "UTF-8") + API_FORECAST_ATTR + API_ID;
            } catch (UnsupportedEncodingException uee) {
                return uee.toString();
            }
        }
        else {
            try {
                return API_WEATHER + URLEncoder.encode(cityName, "UTF-8") + API_WEATHER_ATTR + API_ID;
            } catch (UnsupportedEncodingException uee) {
                return uee.toString();
            }
        }
    }

    private static String processResponse(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);

            //Fetch return code
            if(jsonObject.getString("cod").compareTo("200") != 0) {
                return jsonObject.getString("message");
            }
            //Fetch coordinates (lat and long)
            JSONObject coords = jsonObject.getJSONObject("coord");

            //Fetch weather (condition, description)
            JSONArray weather = jsonObject.getJSONArray("weather");
            JSONObject weatherRow = weather.getJSONObject(0);

            //Fetch main info (temp, pressure, humidity, min temp, max temp, sea level, ground level)
            JSONObject main = jsonObject.getJSONObject("main");

            //Fetch wind info (speed, deg)
            JSONObject wind = jsonObject.getJSONObject("wind");

            //Fetch cloud percent
            JSONObject clouds = jsonObject.getJSONObject("clouds");

            //Fetch sys info (sunrise, sunset)
            JSONObject sys = jsonObject.getJSONObject("sys");

            // Return city name and country,
            // the coords,
            // the current conditions,
            // the temperature (current, max, min),pressure, humidity
            // the wind speed and direction,
            // the cloud percentage,
            // the sunrise and sunset times (unix timestamp)
            return jsonObject.getString("name") + "," + sys.getString("country") + "#"
                    + coords.getString("lon") + "," + coords.getString("lat") + "#"
                    + weatherRow.getString("description") + "#"
                    + main.getString("temp") + "," + main.getString("temp_max") + "," + main.getString("temp_min")
                    + "," + main.getString("pressure") + "," + main.getString("humidity") + "#"
                    + wind.getString("speed") + "#"
                    + clouds.getString("all") + "#"
                    + sys.getString("sunrise") + "," + sys.getString("sunset");
        }
        catch(JSONException jsone) {
            throw new RuntimeException(jsone);
        }
    }

    private static String processForecastResponse(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);

            //Fetch return code
            if(jsonObject.getString("cod").compareTo("200") != 0) {
                return jsonObject.getString("message");
            }

            //Fetch forecast list
            JSONArray list = jsonObject.getJSONArray("list");
            StringBuilder infoString = new StringBuilder();

            for(int i = 0; i < jsonObject.getInt("cnt"); i++) {
                JSONObject row = list.getJSONObject(i);
                infoString.append(row.getString("dt") + ","
                        + row.getJSONObject("temp").getString("day") + ","
                        + row.getJSONObject("temp").getString("night") + ","
                        + row.getJSONArray("weather").getJSONObject(0).getString("description") + ","
                        + row.getString("speed") + "#");
            }

            // Return  all forecast info(time of forecast, temp(day. night), condition, wind speed)
            return infoString.toString();
        }
        catch(JSONException jsone) {
            throw new RuntimeException(jsone);
        }
    }
}
