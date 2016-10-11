package com.paidaki.xposed.xpogofinder;

import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Method;

import static com.paidaki.xposed.xpogofinder.Util.*;

public class XPoGoFinder implements IXposedHookLoadPackage {

    private static XSharedPreferences preferences;
    private double lat, lng, alt;
    private float acc, speed, bearing;

    private class LatitudeHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            preferences.reload();
            boolean enabled = preferences.getBoolean(ENABLED_KEY, false);

            if (!enabled) {
                return;
            }
            try {
                lat = Double.parseDouble(preferences.getString(LATITUDE_KEY, ""));
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, param.method.getName() + " - NumberFormatException: " + e.getMessage());
            }
            param.setResult(lat);
        }
    }

    private class LongitudeHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            preferences.reload();
            boolean enabled = preferences.getBoolean(ENABLED_KEY, false);

            if (!enabled) {
                return;
            }
            try {
                lng = Double.parseDouble(preferences.getString(LONGITUDE_KEY, ""));
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, param.method.getName() + " - NumberFormatException: " + e.getMessage());
            }
            param.setResult(lng);
        }
    }

    private class AltitudeHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            preferences.reload();
            boolean enabled = preferences.getBoolean(ENABLED_KEY, false);

            if (!enabled) {
                return;
            }
            try {
                alt = Double.parseDouble(preferences.getString(ALTITUDE_KEY, ""));
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, param.method.getName() + " - NumberFormatException: " + e.getMessage());
            }
            param.setResult(alt);
        }
    }

    private class AccuracyHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            preferences.reload();
            boolean enabled = preferences.getBoolean(ENABLED_KEY, false);

            if (!enabled) {
                return;
            }
            try {
                acc = Float.parseFloat(preferences.getString(ACCURACY_KEY, ""));
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, param.method.getName() + " - NumberFormatException: " + e.getMessage());
            }
            param.setResult(acc);
        }
    }

    private class SpeedHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            preferences.reload();
            boolean enabled = preferences.getBoolean(ENABLED_KEY, false);

            if (!enabled) {
                return;
            }
            try {
                speed = Float.parseFloat(preferences.getString(SPEED_KEY, ""));
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, param.method.getName() + " - NumberFormatException: " + e.getMessage());
            }
            param.setResult(speed);
        }
    }

    private class BearingHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            preferences.reload();
            boolean enabled = preferences.getBoolean(ENABLED_KEY, false);

            if (!enabled) {
                return;
            }
            try {
                bearing = Float.parseFloat(preferences.getString(BEARING_KEY, ""));
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, param.method.getName() + " - NumberFormatException: " + e.getMessage());
            }
            param.setResult(bearing);
        }
    }

    private class LocationUpdateHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            try {
                preferences.reload();
                boolean enabled = preferences.getBoolean(ENABLED_KEY, false);

                if (!enabled) {
                    return;
                }

                if (param.args.length > 3 && (param.args[3] instanceof LocationListener)) {

                    LocationListener locationListener = (LocationListener) param.args[3];

                    for (Method method : LocationListener.class.getDeclaredMethods()) {

                        if (method.getName().equals("onLocationChanged")) {
                            try {
                                lat = Double.parseDouble(preferences.getString(LATITUDE_KEY, ""));
                                lng = Double.parseDouble(preferences.getString(LONGITUDE_KEY, ""));
                                alt = Double.parseDouble(preferences.getString(ALTITUDE_KEY, ""));
                                acc = Float.parseFloat(preferences.getString(ACCURACY_KEY, ""));
                                speed = Float.parseFloat(preferences.getString(SPEED_KEY, ""));
                                bearing = Float.parseFloat(preferences.getString(BEARING_KEY, ""));
                            } catch (NumberFormatException e) {
                                Log.e(LOG_TAG, param.method.getName() + " - NumberFormatException: " + e.getMessage());
                            }
                            Location location = new Location(LocationManager.GPS_PROVIDER);
                            location.setTime(System.currentTimeMillis());
                            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                            location.setLatitude(lat);
                            location.setLongitude(lng);
                            location.setAltitude(alt);
                            location.setAccuracy(acc);
                            location.setSpeed(speed);
                            location.setBearing(bearing);

                            method.setAccessible(true);
                            method.invoke(locationListener, location);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, param.method.getName() + " - Exception: " + e.getMessage());
            }
        }
    }

    private class GpsStatusHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            try {
                preferences.reload();
                boolean enabled = preferences.getBoolean(ENABLED_KEY, false);
                GpsStatus gpsStatus = (GpsStatus) param.getResult();

                if (!enabled || gpsStatus == null) {
                    return;
                }

                for (Method method : GpsStatus.class.getDeclaredMethods()) {

                    if (method.getName().equals("setStatus") && method.getParameterTypes().length > 1) {
                        int[] prns = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
                        float[] snrs = new float[]{6.0f, 12.0f, 18.0f, 24.0f, 30.0f, 36.0f, 42.0f, 48.0f, 54.0f, 60.0f, 66.0f, 72.0f};
                        float[] elevations = new float[]{6.0f, 12.0f, 18.0f, 24.0f, 30.0f, 36.0f, 42.0f, 48.0f, 54.0f, 60.0f, 66.0f, 72.0f};
                        float[] azimuths = new float[]{30.0f, 60.0f, 90.0f, 120.0f, 150.0f, 180.0f, 210.0f, 240.0f, 270.0f, 300.0f, 330.0f, 360.0f};
                        int svCount = prns.length;
                        int ephemerisMask = 0xFFF;
                        int almanacMask = 0xFFF;
                        int usedInFixMask = 0xFFF;

                        method.invoke(gpsStatus, svCount, prns, snrs, elevations, azimuths, ephemerisMask, almanacMask, usedInFixMask);
                        param.setResult(gpsStatus);

                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, param.method.getName() + " - Exception: " + e.getMessage());
            }
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam != null &&
                loadPackageParam.isFirstApplication &&
                !loadPackageParam.packageName.equals("com.paidaki.xposed.xpogofinder") &&
                loadPackageParam.appInfo != null) {

            preferences = new XSharedPreferences(XPoGoFinder.class.getPackage().getName(), COMMON_PREF);
            preferences.makeWorldReadable();
            preferences.reload();

            XposedHelpers.findAndHookMethod("android.location.Location", loadPackageParam.classLoader, "getLatitude", new LatitudeHook());
            XposedHelpers.findAndHookMethod("android.location.Location", loadPackageParam.classLoader, "getLongitude", new LongitudeHook());
            XposedHelpers.findAndHookMethod("android.location.Location", loadPackageParam.classLoader, "getAltitude", new AltitudeHook());
            XposedHelpers.findAndHookMethod("android.location.Location", loadPackageParam.classLoader, "getAccuracy", new AccuracyHook());
            XposedHelpers.findAndHookMethod("android.location.Location", loadPackageParam.classLoader, "getBearing", new BearingHook());
            XposedHelpers.findAndHookMethod("android.location.Location", loadPackageParam.classLoader, "getSpeed", new SpeedHook());

            try {
                XposedBridge.hookAllMethods(Class.forName("android.location.LocationManager"), "requestLocationUpdates", new LocationUpdateHook());
            } catch (ClassNotFoundException e) {
                Log.e(LOG_TAG, e.getMessage());
            }

            XposedHelpers.findAndHookMethod("android.location.LocationManager", loadPackageParam.classLoader, "getGpsStatus", GpsStatus.class, new GpsStatusHook());
        }
    }
}
