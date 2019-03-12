package com.dji.videostreamdecodingsample.activities;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

import com.dji.videostreamdecodingsample.R;
import com.dji.videostreamdecodingsample.env.BorderedText;
import com.dji.videostreamdecodingsample.env.ImageUtils;
import com.dji.videostreamdecodingsample.env.Logger;
import com.dji.videostreamdecodingsample.main.Constants;
import com.dji.videostreamdecodingsample.main.DJIApplication;
import com.dji.videostreamdecodingsample.media.DJIVideoStreamDecoder;
import com.dji.videostreamdecodingsample.media.NativeHelper;
import com.dji.videostreamdecodingsample.models.PeriodicalStateData;
import com.dji.videostreamdecodingsample.recognition.Classifier;
import com.dji.videostreamdecodingsample.recognition.TensorFlowMultiBoxDetector;
import com.dji.videostreamdecodingsample.recognition.TensorFlowObjectDetectionAPIModel;
import com.dji.videostreamdecodingsample.recognition.TensorFlowYoloDetector;
import com.dji.videostreamdecodingsample.services.Server;
import com.dji.videostreamdecodingsample.tracking.MultiBoxTracker;
import com.dji.videostreamdecodingsample.utils.ModuleVerificationUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.VisionControlState;
import dji.common.flightcontroller.VisionDetectionState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.product.Model;
import dji.common.remotecontroller.ChargeRemaining;
import dji.common.remotecontroller.HardwareState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.Simulator;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.afinal.core.AsyncTask;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends AppCompatActivity implements DJICodecManager.YuvDataCallback,View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {
    /**Delay Sending each frame **/
    public long incomingTimeMs;
    public long outputTimeMS;
    public long timesTampNeeded;
    /**Socket connection**/
    private Socket socket;
    /**Data for period times**/
    Handler handlerPeriodTimeData = new Handler();
    Runnable  runnable;
    /**Connection Callback**/
    private RemoteController remoteController;
    private FlightController flightController;
    private FlightAssistant intelligentFlightAssistant;
    private FlightControllerKey isSimulatorActived;
    private BaseProduct baseProduct;
    private PeriodicalStateData periodicalStateData;
    private ImageView imViewA;
    /**Views for interface**/
    public TextView infoip;
    public TextView myAwesomeTextView;
    private TextView titleTv;
    private TextureView videostreamPreviewTtView;
    private SurfaceView videostreamPreviewSf;
    private SurfaceHolder videostreamPreviewSh;
    private Button screenShot;
    private StringBuilder stringBuilder;
    /**Image and Video**/
    private Activity activity=this;
    private static final String TAG = MainActivity.class.getSimpleName();
    private SurfaceHolder.Callback surfaceCallback;
    private Handler handler = new Handler();


    //private Mat tmp;
    private enum DemoType { USE_TEXTURE_VIEW, USE_SURFACE_VIEW, USE_SURFACE_VIEW_DEMO_DECODER}
    private static DemoType demoType = DemoType.USE_TEXTURE_VIEW;
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    private Camera mCamera;
    private DJICodecManager mCodecManager;
    private int videoViewWidth;
    private int videoViewHeight;
    public int count;
    private Bitmap imageA;
    public ByteArrayOutputStream mFrames;
    /**FORMAT COORDINATES AND DISTANCE**/
    DecimalFormatSymbols separator = new DecimalFormatSymbols();
    DecimalFormat formatLongitude;
    DecimalFormat formatLatitude;
    DecimalFormat formatHight;
    DecimalFormat formatDistance;
    /**Adding Detector Activity Variables
     * */
    // Configuration values for the prepackaged multibox model.
    private static final int MB_INPUT_SIZE = 224;
    private static final int MB_IMAGE_MEAN = 128;
    private static final float MB_IMAGE_STD = 128;
    private static final String MB_INPUT_NAME = "ResizeBilinear";
    private static final String MB_OUTPUT_LOCATIONS_NAME = "output_locations/Reshape";
    private static final String MB_OUTPUT_SCORES_NAME = "output_scores/Reshape";
    private static final String MB_MODEL_FILE = "file:///android_asset/multibox_model.pb";
    private static final String MB_LOCATION_FILE ="file:///android_asset/multibox_location_priors.txt";
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE ="file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    private static final String YOLO_MODEL_FILE = "file:///android_asset/tiny-yolo-voc-graph.pb";
    private static final int YOLO_INPUT_SIZE = 416;
    private static final String YOLO_INPUT_NAME = "input";
    private static final String YOLO_OUTPUT_NAMES = "output";
    private static final int YOLO_BLOCK_SIZE = 32;

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.  Optionally use legacy Multibox (trained using an older version of the API)
    // or YOLO.
    private enum DetectorMode {
        TF_OD_API, MULTIBOX, YOLO;
    }
    private static final MainActivity.DetectorMode MODE = DetectorMode.YOLO;

    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    private static final float MINIMUM_CONFIDENCE_MULTIBOX = 0.1f;
    private static final float MINIMUM_CONFIDENCE_YOLO = 0.25f;

    private static final boolean MAINTAIN_ASPECT = MODE == DetectorMode.TF_OD_API;

    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;


    private Classifier detector;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private boolean computingDetection = false;


    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private byte[] luminanceCopy;

    private BorderedText borderedText;
    /**
     * Adding CameraActivity Variables
     * **/
    private static final Logger LOGGER = new Logger();
    private boolean debug = false;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    /**
     * Waypoints mission
     * **/
    private GoogleMap gMap;
    private boolean isAdd = false;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;
    private float altitude = 10.0f;
    private float mSpeed = 10.0f;
    private List<Waypoint> waypointList = new ArrayList<>();
    public static WaypointMission.Builder waypointMissionBuilder;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;

    /**Settings**/
    Random ran = new Random();
    final int value = ran.nextInt(16) + 30;
    @Override
    protected void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();
        initSurfaceOrTextureView();
        notifyStatusChange();
        separator.setDecimalSeparator('.');
        formatHight = new DecimalFormat("00.0", separator);
        formatDistance = new DecimalFormat("00.00", separator);
        formatLatitude = new DecimalFormat("00.000000", separator);
        formatLongitude = new DecimalFormat("00.000000", separator);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment =(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        DJIApplication app = (DJIApplication) getApplication();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        socket = app.getSocket();
        socket.on("joystickPossitionChanged",joystickPossitionChanged);
        socket.on("takeOffChanged",takeOffChanged);
        socket.on("landingChanged",landingChanged);
        socket.on("returnToHomeChanged",returnToHomeChanged);
        socket.on("homeChanged",homeChanged);
        socket.on("addWaypointsChanged",addWaypointsChanged);
        socket.on("clearWaypointsChanged",clearWaypointsChanged);
        socket.on("newAltitudeWaypointsChanged",newAltitudeWaypointsChanged);
        socket.on("newSpeedWaypointsChanged",newSpeedWaypointsChanged);
        socket.on("uploadWaypointsMissionChanged",uploadWaypointsMissionChanged);
        socket.on("startWaypointsMissionChanged",startWaypointsMissionChanged);
        socket.on("endWaypointsMissionChanged",endWaypointsMissionChanged);
        socket.on("actionAfterMissionChanged",actionAfterMissionChanged);
        socket.on("headingChanged",headingChanged);
        socket.on("smartRTHChanged",smartRTHChanged);
        socket.on("lowBatteryWarningThresholdChanged",lowBatteryWarningThresholdChanged);
        socket.on("seriousLowBatteryWarningThresholdChanged",seriousLowBatteryWarningThresholdChanged);
        socket.on("returnToHomeDesicionChanged",returnToHomeDesicionChanged);
        socket.connect();
        periodicalStateData = new PeriodicalStateData();
        periodicalStateData.setFirstReading(true);
        if (DJIApplication.isAircraftConnected()) {
            baseProduct=DJIApplication.getProductInstance();
            baseProduct.getBattery().setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState batteryState) {
                    if(batteryState.getChargeRemainingInPercent()!=periodicalStateData.getAircraftBattery()) {
                        JSONObject jsonBattery = new JSONObject();
                        try {
                            jsonBattery.put("batteryLevel", batteryState.getChargeRemainingInPercent());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        JSONObject jsonBatteryState = new JSONObject();
                        try {
                            jsonBatteryState.put("voltage", batteryState.getVoltage());
                            jsonBatteryState.put("temperature", (int)batteryState.getTemperature());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        socket.emit("newBatteryLevel", jsonBattery);
                        socket.emit("newBatteryState", jsonBatteryState);
                    }
                    periodicalStateData.setAircraftBattery(batteryState.getChargeRemainingInPercent());
                }
            });

        }
        else {
            System.out.println("is Aircraft Disconnected");
        }
        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
            flightController =((Aircraft) DJIApplication.getProductInstance()).getFlightController();
            if(periodicalStateData.isFirtsSetting()){
                flightController.setSmartReturnToHomeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if(djiError!=null)
                        sendError("Error Smart Return to Home :"+ djiError.toString());
                        else{
                            JSONObject smartRTH = new JSONObject();
                            try {
                                smartRTH.put("value", "true");
                                periodicalStateData.setSmartRTHstate(true);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            socket.emit("newSmartRTH", smartRTH);
                        }
                    }
                });
                periodicalStateData.setFirtsSetting(false);
                flightController.getSmartReturnToHomeEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        JSONObject smartRTH = new JSONObject();
                        try {
                            smartRTH.put("value", aBoolean);
                            periodicalStateData.setSmartRTHstate(aBoolean);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        socket.emit("newSmartRTH", smartRTH);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        if(djiError!=null)
                            sendError("Smart Return to Home :"+ djiError.toString());
                    }
                });
            }
            if(flightController.isFlightAssistantSupported()){
                intelligentFlightAssistant=flightController.getFlightAssistant();
                if (intelligentFlightAssistant != null) {
                    intelligentFlightAssistant.setVisionDetectionStateUpdatedCallback(new VisionDetectionState.Callback() {
                        @Override
                        public void onUpdate(@NonNull VisionDetectionState visionDetectionState) {
                            //Sensors
                            if(periodicalStateData.isSensorBeingUsedFlightAssistant()!=visionDetectionState.isSensorBeingUsed()) {
                                System.out.println("Flight Controller sensors:" + visionDetectionState.isSensorBeingUsed());
                                JSONObject jsonSensorBeingUsed = new JSONObject();
                                try {
                                    jsonSensorBeingUsed.put("sensorBeingUsed", visionDetectionState.isSensorBeingUsed());

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                socket.emit("newFlightAssistantState", jsonSensorBeingUsed);
                            }
                            periodicalStateData.setSensorBeingUsedFlightAssistant(visionDetectionState.isSensorBeingUsed());
                            //Obstacules
                            visionDetectionState.getObstacleDistanceInMeters();
                        }
                    });
                    intelligentFlightAssistant.setVisionControlStateUpdatedcallback(new VisionControlState.Callback() {
                        @Override
                        public void onUpdate(VisionControlState visionControlState) {
                        }
                    });
                }
            } else {
                System.out.println("onAttachedToWindow FC NOT Available");
            }
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                    //Satelite Counts
                    if(periodicalStateData.isSmartRTHstate()&&!flightControllerState.isGoingHome()&&flightControllerState.isFlying()){
                        JSONObject jsonAirlink = new JSONObject();
                        try {
                            jsonAirlink.put("value", "true");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(flightControllerState.isLowerThanSeriousBatteryWarningThreshold()&&periodicalStateData.isReturnToHomeCanceled())
                        {
                            socket.emit("newReturnToHomeQuestion", jsonAirlink);
                        }
                        else if(flightControllerState.isLowerThanBatteryWarningThreshold()&&periodicalStateData.isUrgentReturnToHomeCanceled()){
                            socket.emit("newReturnToHomeQuestion", jsonAirlink);
                        }
                    }
                    if(periodicalStateData.getFlightControllerGPSSatelliteCount()!=flightControllerState.getSatelliteCount()) {
                        System.out.println("Flight Controller GPS:" + flightControllerState.getSatelliteCount());
                        JSONObject jsonAirlink = new JSONObject();
                        try {
                            jsonAirlink.put("gpsSignalStatus", flightControllerState.getSatelliteCount());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        socket.emit("newGPSSignalStatus", jsonAirlink);
                    }
                    periodicalStateData.setFlightControllerGPSSatelliteCount(flightControllerState.getSatelliteCount());
                    //Battery Required to RTH
                    if(periodicalStateData.getAircraftBatteryPercentageNeededToGoHome()!=flightControllerState.getGoHomeAssessment().getBatteryPercentageNeededToGoHome()){
                        System.out.println("Flight Controller Battery Needed to RTH:" + flightControllerState.getSatelliteCount());
                        JSONObject jsonBattery = new JSONObject();
                        try {
                            jsonBattery.put("batteryNeededRTH", flightControllerState.getGoHomeAssessment().getBatteryPercentageNeededToGoHome());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        socket.emit("newBatteryANeededRTH", jsonBattery);
                    }
                    periodicalStateData.setAircraftBatteryPercentageNeededToGoHome(flightControllerState.getGoHomeAssessment().getBatteryPercentageNeededToGoHome());
                    //Flight Time
                    if(flightControllerState.isFlying()&&periodicalStateData.getFlightTime()!=flightControllerState.getFlightTimeInSeconds()&&flightControllerState.getFlightTimeInSeconds()%10==0){
                        JSONObject flightTime = new JSONObject();
                        try {
                            flightTime.put("flightTime", secToTime(flightControllerState.getFlightTimeInSeconds()/10));
                            System.out.println();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        socket.emit("newFlightTime", flightTime);
                    }
                    periodicalStateData.setHomeLatitude(flightControllerState.getAircraftLocation().getLatitude());
                    periodicalStateData.setHomeLongitude(flightControllerState.getAircraftLocation().getLongitude());
                    if(flightControllerState.isFlying()&&periodicalStateData.isSameAircraftLocation(flightControllerState.getAircraftLocation().getLatitude(),flightControllerState.getAircraftLocation().getLongitude())){
                        JSONObject coordinates = new JSONObject();
                        try {
                            coordinates.put("latitude",formatLatitude.format(flightControllerState.getAircraftLocation().getLatitude()));
                            coordinates.put("longitude",formatLongitude.format(flightControllerState.getAircraftLocation().getLongitude()));
                            coordinates.put("hight", formatHight.format(flightControllerState.getAircraftLocation().getAltitude()));
                            coordinates.put("distance",
                                    formatDistance.format(periodicalStateData.distanciaCoord(
                                            flightControllerState.getAircraftLocation().getLatitude(),
                                            flightControllerState.getAircraftLocation().getLongitude(),
                                            flightControllerState.getHomeLocation().getLatitude(),
                                            flightControllerState.getHomeLocation().getLongitude())
                                    ));
                            System.out.println();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        periodicalStateData.setAircraftLatitude((flightControllerState.getAircraftLocation().getLatitude()));
                        periodicalStateData.setAircraftLongitude((flightControllerState.getAircraftLocation().getLongitude()));
                        socket.emit("newCoordinates", coordinates);
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateDroneLocation();
                            }
                        });
                    }
                }
            });
        }
        if (ModuleVerificationUtil.isRemoteControllerAvailable()) {
            remoteController =((Aircraft) DJIApplication.getProductInstance()).getRemoteController();
            remoteController.setChargeRemainingCallback(new ChargeRemaining.Callback() {
                @Override
                public void onUpdate(@NonNull ChargeRemaining chargeRemaining) {
                    if(periodicalStateData.getRemoteControllerBattery()!=chargeRemaining.getRemainingChargeInPercent()) {
                        System.out.println("Remote Controller Battery:" + chargeRemaining.getRemainingChargeInPercent());
                        JSONObject jsonRCStatus = new JSONObject();
                        try {
                            jsonRCStatus.put("batteryLevel", chargeRemaining.getRemainingChargeInPercent());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        socket.emit("newRCConnectionStatus", jsonRCStatus);
                    }
                    periodicalStateData.setRemoteControllerBattery(chargeRemaining.getRemainingChargeInPercent());
                }
            });
            remoteController.setHardwareStateCallback(new HardwareState.HardwareStateCallback() {
                @Override
                public void onUpdate(@NonNull HardwareState hardwareState) {
                    if(periodicalStateData.getRemoteControllerSwitchMode()!=hardwareState.getFlightModeSwitch().value()){
                        System.out.println("Remote Controller Switch Mode:" +hardwareState.getFlightModeSwitch().value());
                        JSONObject jsonFlightSwitch = new JSONObject();
                        try {
                            jsonFlightSwitch.put("flightMode",hardwareState.getFlightModeSwitch().value());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        socket.emit("newFlightModeSwitch",jsonFlightSwitch);
                    }
                    periodicalStateData.setRemoteControllerSwitchMode(hardwareState.getFlightModeSwitch().value());
                }
            });

        }
        else {
            System.out.println("is Remote Controller Disconnected");
        }
        //Sending the first data collected of the sensors
        if(socket.connected()&&periodicalStateData.isFirstReading()){
            JSONObject jsonFlightSwitch = new JSONObject();
            JSONObject jsonRCStatus = new JSONObject();
            JSONObject jsonAirlink = new JSONObject();
            JSONObject jsonBattery = new JSONObject();
            JSONObject jsonSensorBeingUsed = new JSONObject();
            JSONObject jsonBatteryNRTH = new JSONObject();
            JSONObject flightTime = new JSONObject();
            try {
                jsonFlightSwitch.put("flightMode",periodicalStateData.getRemoteControllerSwitchMode());
                jsonRCStatus.put("batteryLevel", periodicalStateData.getRemoteControllerBattery());
                jsonAirlink.put("gpsSignalStatus", periodicalStateData.getFlightControllerGPSSatelliteCount());
                jsonBattery.put("batteryLevel", periodicalStateData.getAircraftBattery());
                jsonSensorBeingUsed.put("sensorBeingUsed", periodicalStateData.isSensorBeingUsedFlightAssistant());
                jsonBatteryNRTH.put("batteryNeededRTH", periodicalStateData.getAircraftBatteryPercentageNeededToGoHome());
                flightTime.put("flightTime", secToTime(periodicalStateData.getFlightTime()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.emit("newFlightModeSwitch",jsonFlightSwitch);
            socket.emit("newRCConnectionStatus", jsonRCStatus);
            socket.emit("newGPSSignalStatus", jsonAirlink);
            socket.emit("newBatteryLevel", jsonBattery);
            socket.emit("sensorBeingUsed", jsonSensorBeingUsed);
            socket.emit("newBatteryNeededRTH", jsonBatteryNRTH);
            socket.emit("newFlightTime", flightTime);
        }
        periodicalStateData.setFirstReading(false);
        initAllKeys();
        initUi();

        Thread cThread = new Thread(new Server(this,handler));
        cThread.start();
    }
    private void initSurfaceOrTextureView(){
        switch (demoType) {
            case USE_SURFACE_VIEW:
            case USE_SURFACE_VIEW_DEMO_DECODER:
                initPreviewerSurfaceView();
                break;
            default:
                initPreviewerTextureView();
                break;
        }
    }
    private void initUi() {
        screenShot = (Button) findViewById(R.id.activity_main_screen_shot);
        screenShot.setSelected(false);
        infoip = (TextView) findViewById(R.id.infoip);
        titleTv = (TextView) findViewById(R.id.title_tv);
        imViewA = (ImageView) findViewById(R.id.imageViewA);
        videostreamPreviewTtView = (TextureView) findViewById(R.id.livestream_preview_ttv);
        videostreamPreviewSf = (SurfaceView) findViewById(R.id.livestream_preview_sf);
        myAwesomeTextView= (TextView)findViewById(R.id.event);
        updateUIVisibility();
    }
    /**
     * Init a fake texture view to for the codec manager, so that the video raw data can be received
     * by the camera
     */
    private void initPreviewerTextureView() {
        videostreamPreviewTtView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "real onSurfaceTextureAvailable");
                videoViewWidth = width;
                videoViewHeight = height;
                System.out.println("real onSurfaceTextureAvailable: width " + videoViewWidth + " height " + videoViewHeight);
                if (mCodecManager == null) {
                    mCodecManager = new DJICodecManager(getApplicationContext(), surface, width, height);
                }
                //imageA = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                videoViewWidth = width;
                videoViewHeight = height;
                Log.d(TAG, "real onSurfaceTextureAvailable2: width " + videoViewWidth + " height " + videoViewHeight);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mCodecManager != null) {
                    mCodecManager.cleanSurface();
                }
                if(imageA!=null)
                    imageA.recycle();
                imageA = null;
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    /**
     * Init a surface view for the DJIVideoStreamDecoder
     */
    private void initPreviewerSurfaceView() {
        videostreamPreviewSh = videostreamPreviewSf.getHolder();
        surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d("SURFACE", "real onSurfaceTextureAvailable");
                videoViewWidth = videostreamPreviewSf.getWidth();
                videoViewHeight = videostreamPreviewSf.getHeight();
                Log.d(TAG, "real onSurfaceTextureAvailable3: width " + videoViewWidth + " height " + videoViewHeight);
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        if (mCodecManager == null) {

                            mCodecManager = new DJICodecManager(getApplicationContext(), holder, videoViewWidth,
                                    videoViewHeight);
                        }
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        // This demo might not work well on P3C and OSMO.
                        NativeHelper.getInstance().init();
                        DJIVideoStreamDecoder.getInstance().init(getApplicationContext(), holder.getSurface());
                        DJIVideoStreamDecoder.getInstance().setYuvDataListener(MainActivity.this);
                        DJIVideoStreamDecoder.getInstance().resume();
                        break;
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                videoViewWidth = width;
                videoViewHeight = height;
                Log.d(TAG, "real onSurfaceTextureAvailable4: width " + videoViewWidth + " height " + videoViewHeight);
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        //mCodecManager.onSurfaceSizeChanged(videoViewWidth, videoViewHeight, 0);
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        DJIVideoStreamDecoder.getInstance().changeSurface(holder.getSurface());
                        break;
                }

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        if (mCodecManager != null) {
                            mCodecManager.cleanSurface();
                            mCodecManager.destroyCodec();
                            mCodecManager = null;
                        }
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        DJIVideoStreamDecoder.getInstance().stop();
                        NativeHelper.getInstance().release();
                        break;
                }

            }
        };

        videostreamPreviewSh.addCallback(surfaceCallback);
    }
    @Override
    protected void onPause() {
        LOGGER.d("onPause " + this);

        if (!isFinishing()) {
            LOGGER.d("Requesting finish");
            finish();
        }
        handler = null;
        if (mCamera != null) {
            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
            }
        }
        super.onPause();
    }
    private void updateTitle(String s) {
        if (titleTv != null) {
            titleTv.setText(s);
        }
    }
    /**---Updating Data--------------**/
    private void updateUIVisibility(){
        switch (demoType) {
            case USE_SURFACE_VIEW:
            case USE_SURFACE_VIEW_DEMO_DECODER:
                videostreamPreviewSf.setVisibility(View.VISIBLE);
                videostreamPreviewTtView.setVisibility(View.GONE);
                break;

            case USE_TEXTURE_VIEW:
                videostreamPreviewSf.setVisibility(View.GONE);
                videostreamPreviewTtView.setVisibility(View.VISIBLE);
                break;
        }
    }
    private void notifyStatusChange() {

        final BaseProduct product = DJIApplication.getProductInstance();

        Log.d(TAG, "notifyStatusChange: " + (product == null ? "Disconnect" : (product.getModel() == null ? "null model" : product.getModel().name())));
        if (product != null && product.isConnected() && product.getModel() != null) {
            updateTitle(product.getModel().name() + " Connected " + demoType.name());
            sendMessageConnected();
        } else {
            updateTitle("Disconnected");
        }

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                //Log.d(TAG, "camera recv video data size: " + size);
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        if (mCodecManager != null) {
                            mCodecManager.sendDataToDecoder(videoBuffer, size);
                        }
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        DJIVideoStreamDecoder.getInstance().parse(videoBuffer, size);
                        break;

                    case USE_TEXTURE_VIEW:
                        if (mCodecManager != null) {
                            mCodecManager.sendDataToDecoder(videoBuffer, size);
                        }
                        break;
                }

            }
        };

        if (null == product || !product.isConnected()) {
            mCamera = null;
            sendMessageDisconnected();
        } else {
            sendMessageConnected();
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                mCamera = product.getCamera();
                mCamera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast("can't change mode of camera, error:"+djiError.getDescription());
                        }
                    }
                });
                if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                    VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }
    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        LOGGER.d("onDestroy " + this);
        handlerPeriodTimeData.removeCallbacks(runnable);
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager.destroyCodec();
        }
        socket.disconnect();
        socket.off("joystickPossitionChanged", joystickPossitionChanged);
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager.destroyCodec();
        }
        if (remoteController!=null) {
            remoteController.setChargeRemainingCallback(null);
            remoteController.setGPSDataCallback(null);
        }
        if (flightController!=null) {

        }
        if(baseProduct!=null){
            baseProduct.getBattery().setStateCallback(null);
        }
        Simulator simulator = ModuleVerificationUtil.getSimulator();
        if (simulator != null) {
            simulator.setStateCallback(null);
        }
        super.onDestroy();
        try {
            trimCache(this);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private void initAllKeys() {
        isSimulatorActived = FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE);
    }
    /**Sending Messages**/
    private void sendError(String message){
        JSONObject jsonError = new JSONObject();
        try {
            jsonError.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("newSystemStatus", jsonError);
    }
    private void showToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }
    private void sendMessageConnected() {
        if(socket.connected()){
            JSONObject jsonRCStatus = new JSONObject();
            try {
                jsonRCStatus.put("rCStatus",true);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.emit("newRCConnectionStatus",jsonRCStatus);
        }
    }
    private void sendMessageDisconnected() {
        if(socket.connected()){
            JSONObject jsonRCStatus = new JSONObject();
            try {
                jsonRCStatus.put("rCStatus",false);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.emit("newRCConnectionStatus",jsonRCStatus);
        }
    }
    public void onClick(View v) {

        if (v.getId() == R.id.activity_main_screen_shot) {
            handleYUVClick();
        }

    }
    private void handleYUVClick() {
        if (screenShot.isSelected()) {
            screenShot.setText("Transmit");
            screenShot.setSelected(false);
            socket.emit("disconnectSocket", new JSONObject());
            switch (demoType) {
                case USE_SURFACE_VIEW:
                case USE_TEXTURE_VIEW:
                    mCodecManager.enabledYuvData(false);
                    mCodecManager.setYuvDataCallback(null);
                    // ToDo:
                    break;
                case USE_SURFACE_VIEW_DEMO_DECODER:
                    DJIVideoStreamDecoder.getInstance().changeSurface(videostreamPreviewSh.getSurface());
                    break;
            }
            stringBuilder = null;
        } else {
            screenShot.setText("Live here");
            screenShot.setSelected(true);
            socket.emit("connectSocket", new JSONObject());
            switch (demoType) {
                case USE_TEXTURE_VIEW:
                case USE_SURFACE_VIEW:
                    mCodecManager.enabledYuvData(true);
                    mCodecManager.setYuvDataCallback(this);
                    break;
                case USE_SURFACE_VIEW_DEMO_DECODER:
                    DJIVideoStreamDecoder.getInstance().changeSurface(null);
                    break;
            }

        }
    }
    public String secToTime(int sec) {
        int seconds = sec % 60;
        int minutes = sec / 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    /**Deleting cache**/
    public void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
    /**
     * Transmit buffered data into a JPG image file
     */
    public void onPreviewSizeChosen(final Size size) {
        final float textSizePx =TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);
        tracker = new MultiBoxTracker(this);
        int cropSize = TF_OD_API_INPUT_SIZE;
        if (MODE == MainActivity.DetectorMode.YOLO) {
            detector =
                    TensorFlowYoloDetector.create(getAssets(),YOLO_MODEL_FILE,YOLO_INPUT_SIZE, YOLO_INPUT_NAME,YOLO_OUTPUT_NAMES, YOLO_BLOCK_SIZE);
            cropSize = YOLO_INPUT_SIZE;
        } else if (MODE == MainActivity.DetectorMode.MULTIBOX) {
            detector =
                    TensorFlowMultiBoxDetector.create(getAssets(),MB_MODEL_FILE, MB_LOCATION_FILE, MB_IMAGE_MEAN, MB_IMAGE_STD, MB_INPUT_NAME,
                            MB_OUTPUT_LOCATIONS_NAME,
                            MB_OUTPUT_SCORES_NAME);
            cropSize = MB_INPUT_SIZE;
        } else {
            try {
                detector = TensorFlowObjectDetectionAPIModel.create(getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
                cropSize = TF_OD_API_INPUT_SIZE;
            } catch (final IOException e) {
                LOGGER.e("Exception initializing classifier!", e);
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }
        }
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
        frameToCropTransform =ImageUtils.getTransformationMatrix(previewWidth, previewHeight,
                        cropSize, cropSize,0, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

    }
    public boolean isDebug() {
        return debug;
    }
    protected byte[] getLuminance() {
        return yuvBytes[0];
    }
    private int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    private void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }
    private Runnable processImage= new Runnable() {
        @Override
        public void run() {
            byte[] originalLuminance = getLuminance();
            if (computingDetection) {
                readyForNextImage();
                return;
            }
            computingDetection = true;
            rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
            if (luminanceCopy == null) {
                luminanceCopy = new byte[originalLuminance.length];
            }
            System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
            readyForNextImage();

            final Canvas canvas = new Canvas(croppedBitmap);
            canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
            // For examining the actual TF input.
            if (SAVE_PREVIEW_BITMAP) {
                ImageUtils.saveBitmap(croppedBitmap);
            }
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas2 = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);
            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
                case TF_OD_API:
                    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    break;
                case MULTIBOX:
                    minimumConfidence = MINIMUM_CONFIDENCE_MULTIBOX;
                    break;
                case YOLO:
                    minimumConfidence = MINIMUM_CONFIDENCE_YOLO;
                    break;
            }

            final List<Classifier.Recognition> mappedRecognitions =
                    new LinkedList<Classifier.Recognition>();

            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= minimumConfidence && result.getTitle().contains("person")) {
                    canvas2.drawRect(location, paint);
                    cropToFrameTransform.mapRect(location);
                    result.setLocation(location);
                    mappedRecognitions.add(result);
                }
            }
            mFrames =new ByteArrayOutputStream();
            cropCopyBitmap.compress(Bitmap.CompressFormat.JPEG, Constants.qualityBitmaps, mFrames);
            imViewA.setImageBitmap(cropCopyBitmap);
            outputTimeMS=System.currentTimeMillis();
            timesTampNeeded=outputTimeMS-incomingTimeMs;
            computingDetection = false;
        }
    };
    @Override
    public void onYuvDataReceived(final ByteBuffer yuvFrame, int dataSize, final int width, final int height) {
        if (count++ % Constants.fps == 0 && yuvFrame != null) {
            incomingTimeMs=System.currentTimeMillis();
            final byte[] bytes = new byte[dataSize];
            if(dataSize>0){
                yuvFrame.get(bytes);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        saveYuvDataToJPEG(bytes, width, height);
                    }
                });
            }
        }
    }
    private void saveYuvDataToJPEG(final byte[] yuvFrame, int width, int height){
        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        byte[] nu = new byte[width * height / 4]; //
        byte[] nv = new byte[width * height / 4];
        for (int i = 0; i < u.length; i++) {
            v[i] = yuvFrame[width*height + 2 * i];
            u[i] = yuvFrame[width*height + 2 * i + 1];
        }
        int uvWidth = width / 2;
        int uvHeight = height / 2;
        for (int j = 0; j < uvWidth / 2; j++) {
            for (int i = 0; i < uvHeight / 2; i++) {
                nu[2 * (i * uvWidth + j)] =  nu[2 * (i * uvWidth + j) + 1] =  u[i * uvWidth + j];
                nu[2 * (i * uvWidth + j) + uvWidth] = nu[2 * (i * uvWidth + j) + 1 + uvWidth] =  u[i * uvWidth + j + uvWidth / 2];
                nv[2 * (i * uvWidth + j)] =   nv[2 * (i * uvWidth + j) + 1] =   v[(i + uvHeight / 2) * uvWidth + j];
                nv[2 * (i * uvWidth + j) + uvWidth] =  nv[2 * (i * uvWidth + j) + 1 + uvWidth] =  v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
            }
        }
        final byte[] bytes = new byte[yuvFrame.length];
        System.arraycopy(yuvFrame, 0, bytes, 0, width*height);
        for (int i = 0; i < u.length; i++) {
            bytes[width*height + (i * 2)] = nv[i];
            bytes[width*height + (i * 2) + 1] = nu[i];
        }

        final byte[] yuv = new byte[width/Constants.quality * height/Constants.quality * 3 / 2];
        // halve yuma
        int i = 0;
        for (int y_ = 0; y_ < height; y_+=Constants.quality) {
            for (int x = 0; x < width; x+=Constants.quality) {
                yuv[i] = bytes[y_ * width + x];
                i++;
            }
        }
        // halve U and V color components
        for (int y__ = 0; y__ < height / 2; y__+=Constants.quality) {
            for (int x = 0; x < width; x += 2* Constants.quality) {
                yuv[i] = bytes[(width * height) + (y__ * width) + x];
                i++;
                yuv[i] = bytes[(width * height) + (y__* width) + (x + 1)];
                i++;
            }
        }
        height/=Constants.quality; width/=Constants.quality;
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }
        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                rgbBytes = new int[width * height];
                onPreviewSizeChosen(new Size(width,height));
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }
        isProcessingFrame = true;
        yuvBytes[0] = yuv;
        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(yuv, previewWidth, previewHeight, rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        isProcessingFrame = false;
                    }
                };
        if(handler!=null)
            handler.post(processImage);
    }
    /**Screen Events to control the aircraft**/

    public Emitter.Listener joystickPossitionChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myAwesomeTextView.setText(args[0].toString());
                }
            });

        }
    };
    public Emitter.Listener returnToHomeChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                        flightController =((Aircraft) DJIApplication.getProductInstance()).getFlightController();
                        flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null){
                                    sendError(djiError.getDescription());
                                    myAwesomeTextView.setText(djiError.getDescription());
                                }
                            }
                        });
                    }
                    else {
                        myAwesomeTextView.setText("FlightController not available Landing Go Home");
                    }
                }
            });

        }
    };
    public Emitter.Listener homeChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                        flightController =((Aircraft) DJIApplication.getProductInstance()).getFlightController();
                        //if(flightController.getState().isFlying()){
                            flightController.setHomeLocation(new LocationCoordinate2D(periodicalStateData.getAircraftLatitude(), periodicalStateData.getAircraftLongitude()), new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if(djiError!=null)
                                        sendError(djiError.getDescription());
                                }
                            });
                            JSONObject homeLocation = new JSONObject();
                            try {
                                homeLocation.put("latitude",formatLatitude.format(periodicalStateData.getAircraftLatitude()));
                                homeLocation.put("longitude",formatLongitude.format(periodicalStateData.getAircraftLongitude()));
                                updateDroneLocation();
                                cameraUpdate(); // Locate the drone's place
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            socket.emit("newHomeLocation", homeLocation);
                        //}
                    }
                    else {
                        myAwesomeTextView.setText("FlightController not available Landing Go Home");
                    }
                }
            });

        }
    };
    public Emitter.Listener landingChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                        flightController =((Aircraft) DJIApplication.getProductInstance()).getFlightController();
                        if(flightController.getState().isFlying()){
                            flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if(djiError!=null){
                                        myAwesomeTextView.setText(djiError.getDescription());
                                        sendError(djiError.getDescription());
                                    }
                                }
                            });
                        }
                    }
                    else {
                        myAwesomeTextView.setText("FlightController not available Landing");
                    }

                }
            });

        }
    };
    public Emitter.Listener takeOffChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                        flightController =((Aircraft) DJIApplication.getProductInstance()).getFlightController();
                        if(!flightController.getState().isFlying()){
                            flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if(djiError!=null){
                                        myAwesomeTextView.setText(djiError.getDescription());
                                        sendError(djiError.getDescription());
                                    }
                                    else {
                                        periodicalStateData.setReturnToHomeCanceled(true);
                                        periodicalStateData.setUrgentReturnToHomeCanceled(true);
                                    }
                                }
                            });

                        }
                        else{
                            myAwesomeTextView.setText(" TakeOff, but is flying");
                        }

                    }
                    else {
                        myAwesomeTextView.setText("FlightController not available TakeOff");
                    }


                }
            });

        }
    };
    /**Waypoints Events **/
    public Emitter.Listener addWaypointsChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    enableDisableAdd();
                }
            });

        }
    };
    public Emitter.Listener clearWaypointsChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gMap.clear();
                    waypointList.clear();
                    waypointMissionBuilder.waypointList(waypointList);
                    myAwesomeTextView.setText("clearWaypointsChanged");
                }
            });

        }
    };
    public Emitter.Listener newAltitudeWaypointsChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    altitude=Float.parseFloat(args[0].toString());
                    myAwesomeTextView.setText(altitude+"");
                }
            });

        }
    };
    public Emitter.Listener newSpeedWaypointsChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSpeed=Float.parseFloat(args[0].toString());
                    myAwesomeTextView.setText(mSpeed+"");
                }
            });

        }
    };
    public Emitter.Listener uploadWaypointsMissionChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myAwesomeTextView.setText("uploadWaypointsMissionChanged");
                    configWayPointMission();
                    getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                setResultToToast("Mission upload successfully!");
                                sendError("Mission upload successfully!");
                            } else {
                                setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                                sendError("Mission upload failed: "+error.getDescription());
                                getWaypointMissionOperator().retryUploadMission(null);
                            }
                        }
                    });
                }
            });

        }
    };
    public Emitter.Listener startWaypointsMissionChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myAwesomeTextView.setText("startWaypointsMissionChanged");
                    startWaypointMission();
                }
            });

        }
    };
    public Emitter.Listener endWaypointsMissionChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myAwesomeTextView.setText("endWaypointsMissionChanged");
                    stopWaypointMission();
                }
            });

        }
    };
    public Emitter.Listener actionAfterMissionChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(args[0].toString().compareTo("none")==0){
                        mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                    }
                    if(args[0].toString().compareTo("auto_land")==0){
                        mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                    }
                    if(args[0].toString().compareTo("go_home")==0){
                        mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                    }
                    if(args[0].toString().compareTo("back_to_1st")==0){
                        mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                    }
                    myAwesomeTextView.setText("actionAfterMissionChanged");

                }
            });

        }
    };
    public Emitter.Listener headingChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(args[0].toString().compareTo("auto")==0){
                        mHeadingMode = WaypointMissionHeadingMode.AUTO;
                    }
                    if(args[0].toString().compareTo("initial")==0){
                        mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                    }
                    if(args[0].toString().compareTo("rc_control")==0){
                        mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                    }
                    if(args[0].toString().compareTo("waypoints")==0){
                        mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                    }
                    myAwesomeTextView.setText("headingChanged");

                }
            });

        }
    };
    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){
        LatLng pos = new LatLng(periodicalStateData.getAircraftLatitude(), periodicalStateData.getAircraftLongitude());
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        if (droneMarker != null) {
            droneMarker.remove();
        }
        if (checkGpsCoordination(periodicalStateData.getAircraftLatitude(), periodicalStateData.getAircraftLongitude())) {
            droneMarker = gMap.addMarker(markerOptions);
        }
    }
    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }
    private void cameraUpdate(){
        LatLng pos = new LatLng(periodicalStateData.getAircraftLatitude(), periodicalStateData.getAircraftLongitude());
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        gMap.moveCamera(cu);
    }
    @Override
    public void onMapClick(LatLng point) {
        if (isAdd == true){
            markWaypoint(point);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }
    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }
    private void setResultToToast(final String string){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
        }
        LatLng shenzhen = new LatLng(0, 0);
        gMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Africa"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(shenzhen));
    }

    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for amap object
    }
    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            myAwesomeTextView.setText("Add ok");

        }else{
            isAdd = false;
            myAwesomeTextView.setText("Exit ok");

        }
    }
    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            if (DJISDKManager.getInstance().getMissionControl() != null){
                instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
            }
        }
        return instance;
    }
    private void startWaypointMission(){
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                sendError("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });
    }
    private void configWayPointMission(){

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("Load Waypoints succeeded");
            sendError("Load Waypoints succeeded");
        } else {
            setResultToToast("Load Waypoints failed " + error.getDescription());
            sendError("Load Waypoints failed " + error.getDescription());
        }
    }
    private void stopWaypointMission(){
        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                    sendError("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }
    public Emitter.Listener smartRTHChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(args[0].toString().compareTo("true")==0&&!periodicalStateData.isSmartRTHstate()){
                        flightController.setSmartReturnToHomeEnabled(true, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                sendError("Enabling Smart Return to Home :"+ djiError.toString());
                            }
                        });
                        periodicalStateData.setSmartRTHstate(true);
                        sendError("Enable Smart Return to Home");
                    }
                    if(args[0].toString().compareTo("false")==0&&periodicalStateData.isSmartRTHstate()){
                        flightController.setSmartReturnToHomeEnabled(false, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                sendError("Enabling Smart Return to Home :"+ djiError.toString());
                            }
                        });
                        periodicalStateData.setSmartRTHstate(false);
                        sendError("Disable Smart Return to Home");
                    }

                }
            });

        }
    };
    public Emitter.Listener lowBatteryWarningThresholdChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                        flightController.setLowBatteryWarningThreshold(Integer.parseInt(args[0].toString()), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError!=null)
                                {
                                    sendError(djiError.getDescription());
                                }
                            }
                        });
                }
            });

        }
    };
    public Emitter.Listener seriousLowBatteryWarningThresholdChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    flightController.setSeriousLowBatteryWarningThreshold(Integer.parseInt(args[0].toString()), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if(djiError!=null)
                            {
                                sendError(djiError.getDescription());
                            }
                        }
                    });
                }
            });
        }
    };
    public Emitter.Listener returnToHomeDesicionChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(args[0].toString().compareTo("true")==0){
                        if(flightController.getState().isFlying()){
                            flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                }
                            });
                            sendError("Enable Smart Return to Home");
                            periodicalStateData.setReturnToHomeCanceled(false);
                            periodicalStateData.setUrgentReturnToHomeCanceled(false);
                        }
                    }
                    if(args[0].toString().compareTo("false")==0&&periodicalStateData.isSmartRTHstate()){

                        sendError("Cancel Smart Return to Home");
                        periodicalStateData.setUrgentReturnToHomeCanceled(true);
                    }
                }
            });
        }
    };
}
