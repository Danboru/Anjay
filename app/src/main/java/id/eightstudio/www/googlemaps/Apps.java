package id.eightstudio.www.googlemaps;

import android.app.Application;

import android.app.*;
import cat.ereza.customactivityoncrash.*;

//Unuse class
public class Apps extends Application {

    @Override
    public void onCreate() {
        CustomActivityOnCrash.install(this);
        super.onCreate();
    }

}
