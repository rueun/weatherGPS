package com.example.weatherapi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.annotations.SerializedName
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    var mLocationManager: LocationManager? = null
    //var mLocationListener: LocationListener? = null
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val PERMISSIONS_REQUEST_CODE = 100

    companion object{
        var BaseUrl = "http://api.openweathermap.org/"
        var AppId = "29ceebd0914454fbb0684b748f59eade"
        var lat : Double?= null
        var lon : Double?= null
        var city : String? = null

//결과값
//splitArray[0] = “abc” , splitArray[1] = “de” , splitArray[2] = “fg”



    }

    private fun getLocation(){
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        var userLocation: Location = getLatLng()
        if(userLocation != null){
            lat = userLocation.latitude // 위도 값
            lon = userLocation.longitude // 경도 값
            Log.d("CheckCurrentLocation", "현재 내 위치 값 -> 위도 : ${lat}, 경도 : ${lon}")
            var mGeoCoder =  Geocoder(applicationContext, Locale.KOREAN)
            var mResultList: List<Address>? = null
            try{
                mResultList = mGeoCoder.getFromLocation(
                        lat!!, lon!!, 1
                )
            }catch(e: IOException){
                e.printStackTrace()
            }
            if(mResultList != null){
                // 내 주소 가져오기
                    city = mResultList[0].getAddressLine(0)

//결과값
//splitArray[0] = “abc” , splitArray[1] = “de” , splitArray[2] = “fg”


                Log.d("내 주소 ", mResultList[0].getAddressLine(0))
            }
        }
    }

    /**
     * getLatLng() 함수: ACCESS_COARSE_LOCATION 권한과 ACCESS_FINE_LOCATION권한이 허용되어 있을 경우 좌표를 구하고,
     * 허용되있지 않을 경우  권한을 요청한 뒤, 다시 getLatLng함수를 호출하도록 하는 코드
     */
    private fun getLatLng(): Location{
        var currentLatLng: Location? = null
        var hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        var hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)

        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED){
            val locatioNProvider = LocationManager.GPS_PROVIDER
            currentLatLng = mLocationManager?.getLastKnownLocation(locatioNProvider)
        }else{
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])){
                Toast.makeText(this, "앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            }else{
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            }
            currentLatLng = getLatLng()
        }
        return currentLatLng!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        getLocation()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_city.setOnClickListener {
            getLocation()
            Log.d("지금","어디니"+city)
        }



        //Create Retrofit Builder
        val retrofit = Retrofit.Builder()
            .baseUrl(BaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherService::class.java)
        val call = service.getCurrentWeatherData(lat.toString(), lon.toString(), AppId)
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.d("MainActivity", "result :" + t.message)
            }

            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.code() == 200) {
                    val weatherResponse = response.body()
                    Log.d("Hello", "hi")
                    Log.d("MainActivity", "result: " + weatherResponse.toString())
                    var cTemp = weatherResponse!!.main!!.temp - 273.15  //켈빈을 섭씨로 변환
                    var minTemp = weatherResponse!!.main!!.temp_min - 273.15
                    var maxTemp = weatherResponse!!.main!!.temp_max - 273.15

                    var cutting = city?.split(' ') // 공백을 기준으로 리스트 생성해서 필요한 주소값만 출력하기
                    Log.d("잘리냐","어케생겨먹었니"+cutting?.subList(2,3))
                    val intcTemp = cTemp.roundToInt()
                    var intMinTemp = minTemp.roundToInt()
                    val intMaxTemp = maxTemp.roundToInt()
                    var weatherIMG = weatherResponse!!.weather!!.get(0).icon.toString()

                    when(weatherIMG){ // 날씨에 맞는 아이콘 출력
                        "01d"->img_weather.setImageResource(R.drawable.sun)
                        "01n" -> img_weather.setImageResource(R.drawable.sun_night)
                        "02d" -> img_weather.setImageResource(R.drawable.sun_c)
                        "02n" -> img_weather.setImageResource(R.drawable.suncloud_night)
                        "03n","03d","04d","04n" -> img_weather.setImageResource(R.drawable.cloud_many)
                        "09d","09n","10d","10n" -> img_weather.setImageResource(R.drawable.rain)
                        "11d","11n" -> img_weather.setImageResource(R.drawable.thunder)
                        "13d","13n" -> img_weather.setImageResource(R.drawable.snow)
                        "50n","50d" -> img_weather.setImageResource(R.drawable.mist)
                    }
                    btn_city.text = cutting?.subList(2,3).toString().replace("["," ").replace("]"," ") // []가 같이 출력되어서 []를 공백으로 치환
                    Log.d("[]왜나와","함보자"+btn_city.text)
                    tv_MinMaxTemp.text = intMinTemp.toString() + "\u00B0" + "/" + intMaxTemp.toString() + "\u00B0"
                    tv_cTemp.text = "\n" + intcTemp.toString() + "\u00B0" + "\n"
                }
            }

        })
    }
}



interface WeatherService{

    @GET("data/2.5/weather")
    fun getCurrentWeatherData(
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("appid") appid: String) :
            Call<WeatherResponse>
}

class WeatherResponse(){
    @SerializedName("weather") var weather = ArrayList<Weather>()
    @SerializedName("main") var main: Main? = null
    @SerializedName("wind") var wind : Wind? = null
    @SerializedName("sys") var sys: Sys? = null
}

class Weather {
    @SerializedName("id") var id: Int = 0
    @SerializedName("main") var main : String? = null
    @SerializedName("description") var description: String? = null
    @SerializedName("icon") var icon : String? = null
}

class Main {
    @SerializedName("temp")
    var temp: Float = 0.toFloat()
    @SerializedName("humidity")
    var humidity: Float = 0.toFloat()
    @SerializedName("pressure")
    var pressure: Float = 0.toFloat()
    @SerializedName("temp_min")
    var temp_min: Float = 0.toFloat()
    @SerializedName("temp_max")
    var temp_max: Float = 0.toFloat()

}

class Wind {
    @SerializedName("speed")
    var speed: Float = 0.toFloat()
    @SerializedName("deg")
    var deg: Float = 0.toFloat()
}

class Sys {
    @SerializedName("country")
    var country: String? = null
    @SerializedName("sunrise")
    var sunrise: Long = 0
    @SerializedName("sunset")
    var sunset: Long = 0
}
