package id.eightstudio.www.googlemaps;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.location.places.Place;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import com.google.android.gms.location.*;

import id.eightstudio.www.googlemaps.Helper.DirectionDrawHelper;
import id.eightstudio.www.googlemaps.Helper.PlaceAutoCompleteHelper;
import id.eightstudio.www.googlemaps.Helper.Utils;
import id.eightstudio.www.googlemaps.Model.Jarak;
import id.eightstudio.www.googlemaps.Model.VMargin;
import id.eightstudio.www.googlemaps.Utils.GPSRequest;
import id.eightstudio.www.googlemaps.Widget.MyMarker;
import id.eightstudio.www.googlemaps.Widget.TariffView;
import id.eightstudio.www.googlemaps.Widget.ToastProgress;

public class MapsActivity extends AppCompatActivity implements
        DirectionDrawHelper.OnNavigateReadyListener,
        View.OnClickListener, OnMapReadyCallback,
        PlaceAutoCompleteHelper.onSuggestResultListener,
        PlaceAutoCompleteHelper.onTextFocusListener {

    private final int REQUEST_CHECK_SETTINGS = 122;
    private final int REQUEST_LOCATION = 125;
    //LatLng start_place, end_place;
    private EditText fok;
    private MyMarker centerMarker;
    private View searchArea, mapFrame, TariffVroot;

    private List<Marker> drivers = new ArrayList<>();
    private AutoCompleteTextView addr_from, addr_to;
    private TariffView tariff;
    private ViewPropertyAnimator searchAreaAnimate;
    private VMargin vmargin;
    private GoogleMap gmaps;
    private PlaceAutoCompleteHelper pickerHelper;
    private Geocoder geoCoder;
    private Marker firstMarker;
    private ToastProgress tprog;

    private boolean movetomylocation = false;
    private boolean mMapIsTouched;
    private boolean haszoom = false;
    private boolean sbOnceMove = true;

    //Set default location
    private LatLng home = new LatLng(-6.813738, 110.847039);

    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mapFrame = findViewById(R.id.map_frame);
        tariff = findViewById(R.id.tariff);
        TariffVroot = findViewById(R.id.tariffviewLinearLayout2);
        centerMarker = findViewById(R.id.mainactivity_makercenter);
        addr_from = findViewById(R.id.booking_form_origin);
        addr_to = findViewById(R.id.booking_form_dest);
        fok = findViewById(R.id.fok);
        findViewById(R.id.clearfrom).setOnClickListener(this);
        findViewById(R.id.clearto).setOnClickListener(this);
        searchArea = findViewById(R.id.searcharea);

        //TODO : Set Origin dan Destination
        //Menentukan lat long dari lokasi antar dan lokasi tujuan
        addr_from.setTag(new LatLng(0, 0));
        addr_to.setTag(new LatLng(0, 0));

        searchAreaAnimate = searchArea.animate();

        initilizeMap();

        geoCoder = new Geocoder(getBaseContext(), Locale.getDefault());
        pickerHelper = new PlaceAutoCompleteHelper(addr_from, addr_to);

        pickerHelper.install(this);
        pickerHelper.setOnSuggestResultListener(this);
        pickerHelper.setOnFocusListener(this);

        //TODO : Center marker action
        centerMarker.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View p1) {

                onMarkerClick();
            }
        });


        /**
         *
         */
        KeyboardVisibilityEvent.setEventListener(
                this,
                new KeyboardVisibilityEventListener() {
                    @Override
                    public void onVisibilityChanged(boolean isOpen) {
                        if (!isOpen && isNavigationReady() && !mMapIsTouched) {
                            setNavigationFocus();
                        }
                    }
                });
        setSearchbarMargin();
        tprog = new ToastProgress(MapsActivity.this);
        //waitIndicator.getIndeterminateDrawable().setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.MULTIPLY);
    }

    /**
     * ViewSetup
     */
    private void setSearchbarMargin() {

        int sbheight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height",
                "dimen", "android");

        if (resourceId > 0) {
            sbheight = getResources().getDimensionPixelSize(resourceId);
        }

        int dp10 = (int) Utils.dp2px(this, 10);
        final int mtop = sbheight + (int) Utils.dp2px(this, 10);

        RelativeLayout.LayoutParams salp = (RelativeLayout.LayoutParams) searchArea.getLayoutParams();
        salp.setMargins(dp10, mtop, dp10, dp10);
        searchArea.setLayoutParams(salp);

        //Runable
        searchArea.post(new Runnable() {
            @Override
            public void run() {
                TariffVroot.post(new Runnable() {
                    @Override
                    public void run() {
                        vmargin = new VMargin(mtop + searchArea.getHeight(), TariffVroot.getHeight());
                    }
                });
            }
        });
    }

    /**
     * Menginisialisasi MapFragment
     */
    private void initilizeMap() {

        MapFragment mapf = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapf.getMapAsync(this);
        tariff.hide(false);

    }

    /**
     * Cek permisi aplikasi
     */
    private void checkPerms() {

        //Jika permisi granted
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {

            //Request gps
            //GPSrequest();
            new GPSRequest().GPSrequest(this, pickerHelper);
            gmaps.setMyLocationEnabled(true);
            gmaps.getMyLocation();
        }
    }

    /**
     * Runtime permission on result
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    //GPSrequest();
                    new GPSRequest().GPSrequest(this, pickerHelper);
                } else {

                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }

                    gmaps.setMyLocationEnabled(true);
                    gmaps.getMyLocation();
                }
            } else {
                Toast.makeText(this, "Akses lokasi tidak di izinkan", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void onMarkerClick() {

        // CenterMarker di klik
        LatLng cur = gmaps.getCameraPosition().target;
        if (addr_from.isFocused()) {
            setAddrValue(addr_from, cur);
        } else if (addr_to.isFocused()) {
            setAddrValue(addr_to, cur);
        }
    }

    /**
     * Clear pencarian
     *
     * @param p1
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onClick(View p1) {
        if (p1.getId() == R.id.clearfrom) {
            addr_from.setText("", false);
            addr_from.requestFocus();
        } else if (p1.getId() == R.id.clearto) {
            addr_to.setText("", false);
            addr_to.requestFocus();
        }
    }

    /**
     * Di trigger saat pencarian dipilih
     *
     * @param place
     * @param act
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onSuggestResult(final Place place, final AutoCompleteTextView act) {
        final LatLng placelatlng = place.getLatLng();
        if (!isNavigationReady() && (addr_from.getText().length() < 1 || addr_to.getText().length() < 1))
            gmaps.animateCamera(CameraUpdateFactory.newLatLng(place.getLatLng()), 1000, new GoogleMap.CancelableCallback() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                @Override
                public void onFinish() {
                    setAddrValue(act == addr_from ? addr_from : addr_to, placelatlng);
                }

                @Override
                public void onCancel() {
                    setAddrValue(act == addr_from ? addr_from : addr_to, placelatlng);
                }
            });
        else setAddrValue(act == addr_from ? addr_from : addr_to, placelatlng);
    }

    /**
     * Saat pencarian fokus
     *
     * @param act
     */
    @Override
    public void onFocus(final AutoCompleteTextView act) {
        LatLng actpos = (LatLng) act.getTag();

        // Atur centerMarker tampil saat rute telah siap, saat sebelumnya sembunyi
        if (isNavigationReady() && centerMarker.getVisibility() == View.GONE && (addr_from.isFocused() || addr_to.isFocused())) {
            centerMarker.setVisibility(View.VISIBLE);
        }

        //
        if (act == addr_from && drivers.size() > 0 && !drivers.get(0).isVisible()) {
            for (Marker m : drivers) {
                m.setVisible(true);
            }
        }

        // Saat EditText fokus, sorot ke arah alamat EditText tersebut
        if (actpos.latitude != 0) {
            gmaps.animateCamera(CameraUpdateFactory.newLatLng(actpos), 1500, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    // Animasi sorot selesai
                    // Jika haszoom bernilai false, zoom 0.5
                    if (!haszoom) gmaps.animateCamera(CameraUpdateFactory.zoomBy(0.5f), 800, null);
                    haszoom = true;
                }

                @Override
                public void onCancel() {
                    // TODO: Implement this method
                }
            });
        }

        // Atur label pada centerMarker
        if (act == addr_from) {
            centerMarker.setText("RAJANE ESKRIM");
            centerMarker.setColorRes(R.color.colorAddrStart, R.color.colorAddrStartPressed, 0);
        } else if (act == addr_to) {
            centerMarker.setText("LOKASI TUJUAN");
            centerMarker.setColorRes(R.color.colorAddrEnd, R.color.colorAddrEndPressed, 0);
        }
    }

    /**
     *
     * Saat rute siap
     * Di trigger saat request mencari arah rute/navigasi menggunakan DirectionDrawHelper
     *
     * @param path
     * @param j
     */
    @Override
    public void onNavigationReady(Polyline path, Jarak j) {

        // Sembunyikan centerMarker, hilangkan fokus pada searchview dan tampilkan tarif
        centerMarker.setVisibility(View.GONE);
        fok.setFocusable(true);
        fok.requestFocus();
        tariff.show();//Menampilkan view tarif

        // Dapatkan jarak dari titik A ke B dari google maps
        // ini akurat karena menghitung berdasarkan rute jalan yang ditempuh

        double jarak = 0;
        DecimalFormat df = null;

        try {
            jarak = j.distanceInKm;
            df = new DecimalFormat("#.#");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        //df.setRoundingMode(RoundingMode.CEILING);
        haszoom = false;

        // Hapus driver, hapus marker pertama
        for (Marker m : drivers) {
            m.setVisible(false);
        }

        //Hapus marker pertama
        if (firstMarker != null) firstMarker.remove();

        // Set label jarak serta tarif
        tariff.setTarifByJarak(jarak); //Melakukan perhitungan tarif
        tariff.setJarak(String.format("Jarak (%s Km)", df.format(jarak)));
    }

    /**
     * Ketika Gagal menemukan rute
     */
    @Override
    public void onNavigationFailed() {
        Toast.makeText(this, "Gagal menemukan rute. Cek koneksi Internet kamu", Toast.LENGTH_SHORT).show();
    }

    /**
     * Ketika Gagal membuat koneksi ke layanan Google
     *
     * @param result
     */
    @Override
    public void onSuggestConnectionFailed(ConnectionResult result) {
        Toast.makeText(this, "Tidak bisa terhubung ke layanan Google.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Ketika Gagal memuat suggestion
     *
     * @param status
     */
    @Override
    public void onSuggestFail(Status status) {
        Toast.makeText(this, "Tidak ditemukan, Cek koneksi Internet kamu", Toast.LENGTH_SHORT).show();
    }

    /**
     * Saat maps siap
     *
     * @param map
     */
    @Override
    public void onMapReady(final GoogleMap map) {

        gmaps = map;
        checkPerms();
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setPadding(20, 0, 20, 10);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 16f));
        // camera move listener
        map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {

                // Atur centerMarker samar saat maps digerakan
                centerMarker.setAlpha(0.5f);

                // Hanya saat maps digerakan menggunakan tangan (buka programatically)
                if (mMapIsTouched) {
                    // one shot action
						/*if (sbOnceMove) { // remove hideable searchBar
							// animasi sembunyi pada searchbar
							searchareaAnimate.setStartDelay(0);
							searchareaAnimate.setDuration(500);
							searchareaAnimate.translationY(-searchbarHeight);
							searchareaAnimate.start();
							UIUtil.hideKeyboard(MainActivity.this);
						}*/
                    sbOnceMove = false;
                }
            }
        });

        /**
         *
         * Camera idle (diam) listener
         * di trigger saat pertama maps dijalankan dan saat selesai dari move (menggeser maps)
         */
        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {

                // Atur centerMarker ke solid (bukan samar lagi)
                centerMarker.setAlpha(1.0f);

                // Animasi tampil pada searchbar
                // searchareaAnimate.setStartDelay(1500).setDuration(800).translationY(0).start();
                sbOnceMove = true;
            }
        });

        // Saat menekan tombol GPS (My location)
        //Mencari lokasi device melalui GPS
        tariff.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public boolean onMyLocationButtonClick() {

                // Cek apakah GPS aktif
                // Jika tidak maka jalankan GPSrequest()
                final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    //GPSrequest();
                    new GPSRequest().GPSrequest(MapsActivity.this, pickerHelper);

                } else {
                    // Jika GPS aktif, tampilkan waiting indikator dan cari lokasi pengguna
                    if (tprog.isShowing()) {
                        tprog.cancel();
                        setTitle("" + System.currentTimeMillis());
                        return false;
                    }

                    movetomylocation = true;
                    gmaps.getMyLocation();
                    tprog.show("Waiting for location...", Gravity.CENTER, (1000 * 60) * 2);
                    tprog.setOnTimedOutListener(new ToastProgress.onTimedOutListener() {

                        @Override
                        public void onTimedOut() {
                            Toast.makeText(MapsActivity.this, "Timed Out", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return false;
            }
        });

        // Di trigger saat lokasi pengguna terdeteksi atau terjadi perubahan lokasi
        map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location loc) {

                if (movetomylocation) {
                    tprog.cancel();
                    gmaps.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 15f), 1500, null);
                }
                movetomylocation = false;
            }
        });
    }

    /**
     *
     * Fungsi fokus pada navigasi
     * Di trigger saat keyboard hilang dari searchbar
     * (biasanya saat menekan back) dan saat tekan back pada mode fokus navigasi
     */
    private void setNavigationFocus() {

        centerMarker.setVisibility(View.GONE);
        LatLng[] na = getNaviagatePoint();
        Utils.requestCenterCamera(this, gmaps, na[0], na[1], vmargin, null);
        fok.setFocusable(true);
        fok.requestFocus();
        haszoom = false;
        for (Marker m : drivers) {
            m.setVisible(false);
        }
    }

    /**
     * Fungsi untuk mencari tahu poin A dan B (dari dan lokasi tujuan)
     *
     * @return
     */
    private LatLng[] getNaviagatePoint() {
        LatLng addr_fromPos = (LatLng) addr_from.getTag();
        LatLng addr_toPos = (LatLng) addr_to.getTag();
        return new LatLng[]{addr_fromPos, addr_toPos};
    }

    /**
     * Fungsi mengecek apakah rute siap (point A dan B sudah ditentukan)
     *
     * @return
     */
    private boolean isNavigationReady() {
        LatLng[] na = getNaviagatePoint();
        return (na[0].latitude != 0 && na[1].latitude != 0);
    }

    /**
     *
     * Fungsi atur value di searchbar
     * Di trigger saat pengguna mengklik hasil pencarian (place suggestion) atau menekan centerMarker
     *
     * @param acc
     * @param loc
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void setAddrValue(AutoCompleteTextView acc, final LatLng loc) {

        // Dapatkan nama tempat dari koordinat point
        try {
            List<Address> result = geoCoder.getFromLocation(loc.latitude, loc.longitude, 1);
            if (result.size() > 0) {
                Address addr = result.get(0);
                String ll = String.format("%s, %s, %s, %s %s", addr.getAddressLine(0),
                        addr.getAddressLine(1), addr.getAddressLine(2), addr.getAddressLine(3), addr.getAddressLine(4));
                acc.setTag(loc);
                acc.setText(ll, false);
                // beralih fokus antar Lokasi Jemput dan Tujuan
                if (acc == addr_from && !isNavigationReady()) addr_to.requestFocus();
                else if (acc == addr_to && !isNavigationReady()) addr_from.requestFocus();
            }
        } catch (Exception e) {
            Toast.makeText(MapsActivity.this, "Gagal menemukan lokasi, cek koneksi internet kamu.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dapatkan lokasi A dan B
        LatLng[] na = getNaviagatePoint();
        android.util.Log.d("lok", "start:" + na[0] + "----end:" + na[1]);

        // Saat rute siap (point A dan B sudah ditentukan)
        if (isNavigationReady()) {

            // Cegah jarak yang terlalu jauh (batas 27Km)
            // ini tidak akurat karena mengabaikan rute jalan yang ada
            // tapi ini bekerja offline
            if (Utils.distance(na[0], na[1]) > 27) {
                //acc.setText("", false);
                acc.setTag(new LatLng(0, 0));
                Toast.makeText(this, "Jarak terlalu jauh, maksimal 27 Km", Toast.LENGTH_SHORT).show();
                return;
            }

            // Jika path (jalur navigasi) sudah dibuat
            if (DirectionDrawHelper.anim != null) {
                // Hapus path serta marker A B
                DirectionDrawHelper.anim.clearPolyline();
                DirectionDrawHelper.add_startMarker.remove();
                DirectionDrawHelper.add_endMarker.remove();
            }

            UIUtil.hideKeyboard(this);
            // Cari arah navigasi menggunakan DirectionDrawHelper dan membuat path (jalur navigasi) nya
            DirectionDrawHelper pos = new DirectionDrawHelper(this, gmaps, na[0], na[1], vmargin);
            pos.setOnNavigateReadyListener(this);
            pos.start();

            // Saat navigasi belum siap (baru menentukan 1 point, A/B)
        } else {

            // Jika belum di zoom, zoom kamera ke point pertama
            if (!haszoom || gmaps.getCameraPosition().zoom <= 16f)
                gmaps.animateCamera(CameraUpdateFactory.zoomBy(0.8f), 1000, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        // Geser kamera supaya tidak menutup/timpa marker pertama
                        gmaps.animateCamera(CameraUpdateFactory.scrollBy(Utils.dp2px(MapsActivity.this, 50), Utils.dp2px(MapsActivity.this, 50)));
                    }

                    @Override
                    public void onCancel() {
                        // TODO: Implement this method
                    }
                });
            if (firstMarker != null) {
                firstMarker.remove();
                firstMarker = null;
            }
            // Buat marker di point pertama
            if (firstMarker == null)
                firstMarker = gmaps.addMarker(new MarkerOptions().position(loc).icon(BitmapDescriptorFactory.fromResource(acc == addr_from ? R.drawable.ic_start_marker : R.drawable.ic_end_marker)));

            haszoom = true;
        }

        // Jika data dari addr_from
        if (acc == addr_from) {
            // Hapus dan hilangkan semua drivers
            for (Marker m : drivers) m.remove();
            drivers.clear();

            // Mebuat driver (palsu) yang tersedia diradius 1KM
            // --for testing purpose only--
            //for (int i = 0; i < (acc == addr_from ? 7 : 0); i++) {
            //    LatLng rnd = Utils.getRandLocation(loc, 1000);
            //    drivers.add(gmaps.addMarker(new MarkerOptions().position(rnd).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bajaj_driver))));
            //}

        }
    }

    /**
     * Override dispatchTouchEvent
     * Untuk menentukan apakah view sedang di sentuh atau tidak
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mMapIsTouched) {
                    mMapIsTouched = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                mMapIsTouched = false;
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Saat tombol back ditekan
     */
    @Override
    public void onBackPressed() {

        // jika rute siap dan centerMarker nampil
        if (isNavigationReady() && centerMarker.getVisibility() == View.VISIBLE) {
            // fokus ke navigasi
            setNavigationFocus();
            return;

            // jika hanya rute siap
        } else if (isNavigationReady()) {
            // tampilkan notif untuk mencancel booking (navigasi)
            AlertDialog.Builder d = new AlertDialog.Builder(this);
            d.setTitle("Konfirmasi");
            d.setMessage("Batalkan Pesanan ?");

            //Membatalkan booking
            d.setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                @Override
                public void onClick(DialogInterface p1, int p2) {

                    gmaps.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 16f));

                    // Clear, kembalikan ke awal
                    addr_from.setTag(new LatLng(0, 0));
                    addr_to.setTag(new LatLng(0, 0));
                    addr_from.setText("", false);
                    addr_to.setText("", false);

                    drivers.clear();

                    tariff.hide(true);
                    DirectionDrawHelper.clearNavigate();
                    centerMarker.setVisibility(View.VISIBLE);
                    addr_from.requestFocus();
                    haszoom = false;
                    //waitIndicator.setVisibility(View.INVISIBLE);
                    Toast.makeText(MapsActivity.this, "Pesanan Dibatalkan", Toast.LENGTH_SHORT).show();
                }
            });

            d.setNegativeButton("Tidak", null);
            d.show();

        } else {

            haszoom = false;
            super.onBackPressed();
        }
    }

    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:

                switch (resultCode) {
                    case RESULT_OK:
                        if (ActivityCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        gmaps.setMyLocationEnabled(true);
                        gmaps.getMyLocation();
                        break;

                    case RESULT_CANCELED:
                        //GPSrequest();
                        new GPSRequest().GPSrequest(this, pickerHelper);

                        break;
                }
                break;
        }
    }


    /**
     *
     * Option menu
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
