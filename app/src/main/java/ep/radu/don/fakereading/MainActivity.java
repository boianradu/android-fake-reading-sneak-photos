package ep.radu.don.fakereading;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.OnClick;
import util.IabHelper;
import util.IabResult;
import util.Inventory;
import util.Purchase;


public class MainActivity extends Activity implements SurfaceHolder.Callback {
    static final String ITEM_SKU = "com.ep.dev.photofake";
    LinearLayout mainLinearLayout;
    public boolean booleanPremium = false;
    Button btnBuy;
    Button btnPrev;
    Camera camera;
    public final static String IS_PREMIUM_USER = "is_premium_user";
    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;
    public static final String LogTAG = "PFR";
    public static final int REQUEST_CODE_WRITE_SDCARD = 5;
    public static final int REQUEST_CODE_CAMERA = 23;

    private static final String TAG = "ep.radu.don";
    IabHelper mHelper;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    public boolean WRITE_PERMISSION;
    private static final String FOLDER_OUTPUT_NAME = "Fake_reading_photos";
    PictureCallback rawCallback;
    ShutterCallback shutterCallback;
    PictureCallback jpegCallback;
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result,
                                          Purchase purchase) {
            Log.e(LogTAG, " Purchase finished " + result + " " + booleanPremium);
            if (result.isFailure()) {
                // Handle error
                //consumeItem();
                //booleanPremium = true;
                //setUserStatus(true);
                return;
            } else if (purchase.getSku().equals(ITEM_SKU)) {
                //consumeItem();
                btnBuy.setEnabled(false);
                booleanPremium = true;
                setUserStatus(true);
                btnBuy.setVisibility(View.GONE);
                btnPrev.setVisibility(View.VISIBLE);
            }

        }
    };
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =
            new IabHelper.OnConsumeFinishedListener() {
                public void onConsumeFinished(Purchase purchase,
                                              IabResult result) {
                    Log.e(LogTAG, " consume finished");

                    if (result.isSuccess()) {
                        //ToDo here on purchase
                        btnBuy.setEnabled(false);
                        booleanPremium = true;
                        setUserStatus(true);
                        btnBuy.setVisibility(View.GONE);
                        btnPrev.setVisibility(View.VISIBLE);
                    } else {
                        // handle error
                    }
                }
            };
    IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener
            = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result,
                                             Inventory inventory) {
            Log.e(LogTAG, " Query inventory finished");


            if (result.isFailure()) {
                // Handle failure
            } else {
                try {
                    mHelper.consumeAsync(inventory.getPurchase(ITEM_SKU),
                            mConsumeFinishedListener);
                } catch (Exception e) {
                    Log.e(LogTAG, "cant start async process consume: " + e);
                }
            }
        }
    };


    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public  void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.e(LogTAG, "Permission CA: " + "ITS  NOT OK");
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_SDCARD);
        } else {
            Log.e(LogTAG, "Permission CA: " + "ITS OK");
            verifyCameraPermissions(activity);
        }
    }

    private void getUserStatus() {
        if (mPref.getBoolean(IS_PREMIUM_USER, false)) {
            booleanPremium = true;
        }
    }

    public void verifyCameraPermissions(Activity activity) {

        Log.e(LogTAG, "AICI");
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.e(LogTAG, "ASking for camera permision.");
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CODE_CAMERA);
        } else {
            Log.e(LogTAG, "Camera permission granted. Starting process...");
            startProcess();
        }
    }

    private void checkPurchased() {
        getUserStatus();
        if (booleanPremium) {
            btnBuy.setEnabled(false);
            btnBuy.setVisibility(View.GONE);
            btnPrev.setVisibility(View.VISIBLE);
            mainLinearLayout.setBackgroundResource(R.drawable.bakcground_reading_paid);
            Log.e(LogTAG, " purchased yay");
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //consumeItem();
        WRITE_PERMISSION = CheckStoragePermission();
        try {
            verifyStoragePermissions(MainActivity.this);
        } catch (Exception e){
            Log.e(LogTAG,"DAFUQ: " + e);
        }
    }

    public void startProcess() {
        setContentView(R.layout.activity_main);
        mPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        mEditor = mPref.edit();
        mainLinearLayout = findViewById(R.id.layout_background);
        btnBuy = findViewById(R.id.btn_buy_full_version);
        btnPrev = findViewById(R.id.btn_prev);

        ButterKnife.bind(this);
        //setUserStatus(false);
        checkPurchased();


        surfaceView = findViewById(R.id.surface_view);
        surfaceHolder = surfaceView.getHolder();
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        surfaceHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        jpegCallback = new PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outStream;
                try {
                    File fileOut = getOutputFile();
                    if (fileOut != null) {
                        Log.e(LogTAG, "FILE:" + fileOut.getAbsolutePath());
                        fileOut.createNewFile();
                        String fileName = fileOut.getAbsolutePath();
                        outStream = new FileOutputStream(fileOut);
                        outStream.write(data);
                        outStream.close();
                        boolean resultWorldReadable = fileOut.setReadable(true, false);
                        Toast.makeText(getApplicationContext(), fileName, Toast.LENGTH_SHORT).show();
                        Log.e(LogTAG, "onPictureTaken: " + fileName + ", wrote bytes: " + data.length);
                    } else {
                        Toast.makeText(getApplicationContext(), "Something went wrong! Sorry", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception se) {
                    Log.e(LogTAG, "oncreate()1 " + se);
                }
                refreshCamera();
            }
        };
    }

    public void consumeItem() {
        try {
            Log.e(LogTAG, "itemconsumed");
            mHelper.queryInventoryAsync(mReceivedInventoryListener);
        } catch (Exception e) {
            Log.e(LogTAG, "can't consume item: " + e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        String base64EncodedPublicKey =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxCvaa9o9Ih+/avcnupBWgiwd7uuYw8pLhFuh" +
                "+CuXWDv/2Yc3wm+izmc6ts2mfu57/x25n4B1AcJLFYLgGzjEeTCcCj//losSiF5cjHjQCyCW" +
                "7O1DfZlXcdom/A94gpybZhXNmrAQHGBDYs5+uGgPMFkVEwwerb7aJ3RtsDIUhY5W0iK6ahoSZ" +
                "nN6E10PokhL/KS7bTAgC6Ii4Ft1Yr1gkKAUUvqs1tbTWsCevF/0FE2HMKNzNOthsdgSm3ZWT+l2" +
                "v/dKfBaLuPWxakjNcMEQHvOQUQsPl46S5m6kY3crqwYAKyHI1W0wF2NZPfVxfykryEPgPhCaxRQ" +
                "jXQ1hU548PQIDAQAB";

        mHelper = new IabHelper(this, base64EncodedPublicKey);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.d(TAG, "In-app Billing failed:" + result);
                } else {
                    Log.d(TAG, "In-app Billing is set up OK");
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (!mHelper.handleActivityResult(requestCode,
                resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @OnClick({R.id.btn_next, R.id.btn_buy_full_version, R.id.btn_prev, R.id.btn_settings})
    public void onClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alert;
        switch (view.getId()) {
            case R.id.btn_next:
                try {
                    System.out.println("DOING STUFF");
                    captureImage(view);
                } catch (Exception e) {
                    System.out.println("Exception: " + e);

                }
                break;
            case R.id.btn_buy_full_version:
                try {
                    Log.e(LogTAG, "PURCHING");
                    mHelper.launchPurchaseFlow(this, ITEM_SKU, 10001,
                            mPurchaseFinishedListener, "mypurchasetoken");
                } catch (Exception e) {
                    Log.e(LogTAG, "buy click not working: " + e);
                }
                break;
            case R.id.btn_prev:
                builder.setMessage("It's just a fake button mate, calm down.")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do things
                            }
                        });
                alert = builder.create();
                alert.show();
                break;
            case R.id.btn_settings:
                builder.setMessage("The sneaky photos are taken at low resolution. " +
                        "For full resolution you can buy the full version of the app.\n\n" +
                        "That can bring me some beer, be good :D.")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do things
                            }
                        });
                alert = builder.create();
                alert.show();
                break;
            default:
                break;
        }
    }

    public void captureImage(View v) throws IOException {
        //take the picture
        //camera.takePicture(null, null, jpegCallback);
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean CheckStoragePermission() {
        int permissionCheckRead = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheckRead != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_SDCARD);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_SDCARD);
            }
            return false;
        } else
            return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        Log.e(LogTAG, grantResults.length + " " + requestCode);
        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        Log.e(LogTAG, "Camera permission granted. Starting process...");
                        startProcess();
                        // permission was granted, yay! Do the
                        // contacts-related task you need to do.
                    } else {
                        Log.e(LogTAG, "Camera permision denied...");
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage("Ok... things are not great right now. We need the camera permission for this to work. :)")
                                .setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finishAndRemoveTask ();
                                        Toast.makeText(getApplicationContext(), "so... try again. Reload the app.", Toast.LENGTH_LONG).show();
                                        //do things
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                    }
                }
                return;

            case REQUEST_CODE_WRITE_SDCARD:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission has been granted. Start camera preview Activity.
                    Log.e(LogTAG, "write sdcardYo");
                } else {
                    Log.e(LogTAG, "don't write on sdcard Yo");
                    // Permission request was denied.
                }

                verifyCameraPermissions(MainActivity.this);
        }
    }

    // get paths to writing partitions on phone
    public File getOutputFile() {
        final String internalPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        String externalPath = "";
        if (!TextUtils.isEmpty(internalPath)) {
            String[] externalCards = internalPath.split(":");
            for (int i = 0; i < externalCards.length; i++) {
                externalPath = externalCards[i];
            }
        }
        File outDir;
        if (isExternalStorageAvailable() && !isExternalStorageReadOnly() && WRITE_PERMISSION) { //return external memory
            outDir = new File(externalPath + File.separator + FOLDER_OUTPUT_NAME);
        } else { //return internal memory
            outDir = new File(internalPath + File.separator + FOLDER_OUTPUT_NAME);
        }
        if (!outDir.exists()) {
            try {
                outDir.mkdir();
                boolean resWorldReadable = outDir.setReadable(true, false);
                return (new File(outDir.getAbsoluteFile() + File.separator +
                        System.currentTimeMillis() + ".jpg"));
            } catch (Exception e) {
                Log.e(LogTAG, "Couldn't create outDir: " + e);
                return (null);
            }
        }
        return (new File(outDir.getAbsoluteFile() + File.separator +
                System.currentTimeMillis() + ".jpg"));
    }

    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            Log.e(LogTAG, "refreshCamera() " + e);
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            if (camera == null)
                camera = Camera.open();
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            Log.e(LogTAG, "refreshCamera()1 " + e);
        }
    }

    private void setUserStatus(boolean status) {
        mEditor.putBoolean(IS_PREMIUM_USER, status);
        mEditor.apply();
        checkPurchased();
        if (status) {
            setCameraParams();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera
            camera = Camera.open();
            camera.setDisplayOrientation(90);
        } catch (RuntimeException e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
        setCameraParams();
    }

    private void setCameraParams() {
        if (camera != null) {
            camera.stopPreview();
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break; //Natural orientation
                case Surface.ROTATION_90:
                    degrees = 90;
                    break; //Landscape left
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;//Upside down
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;//Landscape right
            }
            Camera.Parameters param;
            param = camera.getParameters();
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
            int rotate = (info.orientation - degrees + 360) % 360;

//STEP #2: Set the 'rotation' parameter
            Camera.Size bestSize = null;
            Camera.Size previewSize = null;
            List<Camera.Size> sizeListPreview = camera.getParameters().getSupportedPreviewSizes();
            List<Camera.Size> sizeListPictures = camera.getParameters().getSupportedPictureSizes();
            if (booleanPremium) {
                previewSize = sizeListPreview.get(0);
                bestSize = sizeListPictures.get(0);
                int index;
                for (index = 0; index < sizeListPreview.size(); index++) {
                    if (sizeListPreview.get(index).width * sizeListPreview.get(index).height < previewSize.width * previewSize.height) {
                        previewSize = sizeListPreview.get(index);
                        Log.e(LogTAG, "SIZE p:" + previewSize.width + " " + previewSize.height + " " + index);
                        break;
                    }
                }
                for (index = 0; index < sizeListPictures.size(); index++) {
                    if (sizeListPictures.get(index).width * sizeListPictures.get(index).height < bestSize.width * bestSize.height) {
                        bestSize = sizeListPictures.get(index);
                        Log.e(LogTAG, "SIZE h:" + bestSize.width + " " + bestSize.height + " " + index);
                        break;
                    }
                }
            } else {
                int index;
                for (index = 0; index < sizeListPreview.size(); index++) {
                    if (sizeListPreview.get(index).width * sizeListPreview.get(index).height < 640 * 481) {
                        previewSize = sizeListPreview.get(index);
                        Log.e(LogTAG, "SIZE p:" + previewSize.width + " " + previewSize.height + " " + index);
                        break;
                    }
                }
                for (index = 0; index < sizeListPictures.size(); index++) {
                    if (sizeListPictures.get(index).width * sizeListPictures.get(index).height < 640 * 481) {
                        bestSize = sizeListPictures.get(index);
                        Log.e(LogTAG, "SIZE h:" + bestSize.width + " " + bestSize.height + " " + index);
                        break;
                    }
                }
            }

            List<Integer> supportedPreviewFormats = param.getSupportedPreviewFormats();
            Iterator<Integer> supportedPreviewFormatsIterator = supportedPreviewFormats.iterator();
            while (supportedPreviewFormatsIterator.hasNext()) {
                Integer previewFormat = supportedPreviewFormatsIterator.next();
                if (previewFormat == ImageFormat.JPEG) {
                    param.setPreviewFormat(previewFormat);
                }
            }
            param.setRotation(rotate);

            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            if (previewSize != null) {
                param.setPreviewSize(previewSize.width, previewSize.height);
            }
            if (bestSize != null) {
                param.setPictureSize(bestSize.width, bestSize.height);
            }

            camera.setParameters(param);
            try {
                // The Surface has been created, now tell the camera where to draw
                // the preview.
                if (camera == null)
                    camera = Camera.open();
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
            } catch (Exception e) {
                // check for exceptions
                Log.e(LogTAG, "CAAMERA()1 " + e);
            }
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // stop preview and release camera
        camera.stopPreview();
        camera.release();
        camera = null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mHelper != null) mHelper.dispose();
            mHelper = null;
        } catch (Exception e) {
            Log.e(LogTAG, "Exception destroy: " + e);
        }
    }
}