package com.kucik.smartroom;

import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.kucik.smartroom.model.Lampu;
import com.kucik.smartroom.model.SmartRoom;
import com.kucik.smartroom.view.ProgressBarAnimation;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by samyang on 4/13/17.
 */
public class FragmentHome extends Fragment {

    //------------------------------------
        String address = null;
        private static final String TAG = "TNASamyang ";
        final int handlerState = 0;

        //Bluetooth Declaration
        private ProgressDialog progress;
        public BluetoothAdapter myBluetooth = null;
        public BluetoothSocket btSocket = null;
        private ConnectedThread mConnectedThread;
        private boolean isBtConnected = false;
        //SPP UUID. Look for it
        static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        Handler bluetoothIn;

        //String Get Data
        private StringBuilder recDataString = new StringBuilder();
        Pattern pattern;
        Matcher matcher;

        //Model
        SmartRoom smartRoom = null;
        Lampu lampuAll = null;
    TextView suhuT;

    //------------------------------------

    View mainView;
    LineChart chartSuhu, chartPDAM;
    Menu mainMenu;
    AsyncBluetooth api;

    LinearLayout pulsaListrikLayout;
    SharedPreferences sharedPreferences;

    // Empty Constructor
    public FragmentHome(){
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        smartRoom = new SmartRoom("0","0","0","0","0");
        lampuAll = new Lampu("-","-","-");



        // Shared prefrence
        sharedPreferences = getContext().getSharedPreferences("LISTRIK", MODE_PRIVATE);
        //Bluetooth SharedPref Address
            SharedPreferences prefs = getContext().getSharedPreferences("MACBluetooth", MODE_PRIVATE);
            String restoredText = prefs.getString("address", null);
            if (restoredText != null) {
                Log.d("Address BT :",prefs.getString("address", "No name defined"));
                address=prefs.getString("address", "No name defined");
            }
            else{
                Log.d("Address BT :","kosong");
            }

        if(lampuAll.getLamp1().equalsIgnoreCase("-")|| lampuAll.getLamp2().equalsIgnoreCase("-")
                || lampuAll.getLampAC().equalsIgnoreCase("-")){
            Log.d(TAG,"Test Lamp");
            Log.d(TAG,lampuAll.getLamp1());
        }
        else {
            Log.d(TAG,"Test Lamp x");
        }


        new ConnectBT().execute(); //Call the class to connect

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                 //if message is what we want
                    String readMessage = (String) msg.obj;                      // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                          //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");            // determine the end-of-line
                    Log.d("SBuilder : ",recDataString.toString());
                    if (endOfLineIndex > 0) {                                   // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string

                        if (recDataString.charAt(0) == '#')                     //if it starts with # we know it is what we are looking for
                        {

                            //Humid Data
                            pattern = Pattern.compile("Humid=.*?,");
                            matcher = pattern.matcher(recDataString);
                            String rawdata="",parsedata="";
                            if (matcher.find()) {
                                rawdata=matcher.group(0);
                                parsedata=rawdata.substring(rawdata.indexOf("=")+1,rawdata.length()-1);
                                Log.d(TAG," Humid -> "+parsedata);
                                smartRoom.setHumid(parsedata);
                                if(Integer.parseInt(smartRoom.getHumid())>50)
                                    mConnectedThread.write("5");
                                else
                                    mConnectedThread.write("4");
                            }
                            //TempData
                            pattern = Pattern.compile("Temp=.*?,");
                            matcher = pattern.matcher(recDataString);
                            if (matcher.find()) {
                                rawdata=matcher.group(0);
                                parsedata=rawdata.substring(rawdata.indexOf("=")+1,rawdata.length()-1);
                                Log.d(TAG," Temp  -> "+parsedata);
                                smartRoom.setTemp(parsedata);
                            }
                            //LightData
                            pattern = Pattern.compile("Light=.*?,");
                            matcher = pattern.matcher(recDataString);
                            if (matcher.find()) {
                                rawdata=matcher.group(0);
                                parsedata=rawdata.substring(rawdata.indexOf("=")+1,rawdata.length()-1);
                                Log.d(TAG," Light -> "+parsedata);
                                smartRoom.setLight(parsedata);
                            }

                            //LightData
                            pattern = Pattern.compile("Motion=.*?,");
                            matcher = pattern.matcher(recDataString);
                            if (matcher.find()) {
                                rawdata=matcher.group(0);
                                parsedata=rawdata.substring(rawdata.indexOf("=")+1,rawdata.length()-1);
                                Log.d(TAG," Motion -> "+parsedata);
                                smartRoom.setMotion(parsedata);
                            }
                            //LightData
                            pattern = Pattern.compile("Flame=.*?,");
                            matcher = pattern.matcher(recDataString);
                            if (matcher.find()) {
                                rawdata=matcher.group(0);
                                parsedata=rawdata.substring(rawdata.indexOf("=")+1,rawdata.length()-1);
                                Log.d(TAG," Flame -> "+parsedata);
                                smartRoom.setFire(parsedata);
                            }

                            pattern = Pattern.compile("lamp1=.*?,");
                            matcher = pattern.matcher(recDataString);
                            if (matcher.find()) {
                                rawdata=matcher.group(0);
                                parsedata=rawdata.substring(rawdata.indexOf("=")+1,rawdata.length()-1);
                                Log.d(TAG," Lamp1 -> "+parsedata);
                                lampuAll.setLamp1(parsedata);
                            }
                            //Lamp Data
                            pattern = Pattern.compile("lamp2=.*?,");
                            matcher = pattern.matcher(recDataString);
                            if (matcher.find()) {
                                rawdata=matcher.group(0);
                                parsedata=rawdata.substring(rawdata.indexOf("=")+1,rawdata.length()-1);
                                Log.d(TAG," Lamp2  -> "+parsedata);
                                lampuAll.setLamp2(parsedata);
                            }
                            //Lamp Data
                            pattern = Pattern.compile("lampAC=.*?,");
                            matcher = pattern.matcher(recDataString);
                            if (matcher.find()) {
                                rawdata=matcher.group(0);
                                parsedata=rawdata.substring(rawdata.indexOf("=")+1,rawdata.length()-1);
                                Log.d(TAG," LampAC -> "+parsedata);
                                lampuAll.setLampAC(parsedata);
                            }


                            updateChart();

                            SharedPreferences.Editor editor = getContext().getSharedPreferences("MACBluetooth", MODE_PRIVATE).edit();
                            editor.putString("lamp1", lampuAll.getLamp1());
                            editor.putString("lamp2", lampuAll.getLamp2());
                            editor.putString("lampAC", lampuAll.getLampAC());
                            editor.apply();

                        }
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                    }
                }
            }
        };


    }
    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        if(btSocket==null){
            new ConnectBT().execute();

        }


        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");


    }
    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try     {
            btSocket.close();
            btSocket=null;
        } catch (IOException e2) {
            msg("In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        mainView = inflater.inflate(R.layout.fragment_home, container, false);


        // Setup api
        api = new AsyncBluetooth(getContext());

        // Setup chart
        setupChart();

        // Setup listener


        return mainView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_home, menu);
        mainMenu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.menu_home_refresh:
                updateChart();
                startAnimateRefreshMenu(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }



    private void setupChart(){
        // Setup chart suhu
//        chartSuhu = (LineChart) mainView.findViewById(R.id.home_chart_suhu);
//        chartPDAM = (LineChart) mainView.findViewById(R.id.home_chart_pdam);
//        chartSuhu.setDescription("");
//        chartPDAM.setDescription("");


        updateChart();

    }

    private void updateChart(){

        final ProgressBar progressBarListrik = (ProgressBar) mainView.findViewById(R.id.progressListrik);
        final ProgressBar progressBarLPG = (ProgressBar) mainView.findViewById(R.id.progressLpg);
        final TextView listrikUpdated = (TextView) mainView.findViewById(R.id.update_listrik_text);
        final TextView listrikProgress = (TextView) mainView.findViewById(R.id.progress_listrik_text);
        final TextView lpgUpdated = (TextView) mainView.findViewById(R.id.update_lpg_text);
        final TextView lpgProgress = (TextView) mainView.findViewById(R.id.progress_lpg_text);
        final int maxValue = sharedPreferences.getInt("KWH",100);
        final TextView suhuTT = (TextView) mainView.findViewById(R.id.suhuR);
        final TextView flame = (TextView) mainView.findViewById(R.id.flameR);
        final TextView Gerakan = (TextView) mainView.findViewById(R.id.Gerakan);


        // Reset Progressbar
        //progressBar.setProgress(0);

        suhuTT.setText(smartRoom.getTemp()+" C");
        flame.setText(smartRoom.getFire());
        if(Integer.parseInt(smartRoom.getMotion())==1)
            Gerakan.append("Gerakan Terdeteksi --> "+getDate()+"\n");

        lpgUpdated.setText("Loading...");
        lpgProgress.setText(smartRoom.getHumid()+"%");


        listrikUpdated.setText("Update : \n");
        listrikProgress.setText(smartRoom.getLight()+"%");

        ProgressBarAnimation animation = new ProgressBarAnimation(progressBarListrik,progressBarListrik.getProgress(),Integer.valueOf(smartRoom.getLight()));
        animation.setDuration(500);
        progressBarListrik.setAnimation(animation);
        //startCountAnimation(listrikProgress,500,(progressBarListrik.getProgress()*100)/maxValue,0);

        ProgressBarAnimation animation2 = new ProgressBarAnimation(progressBarLPG,progressBarLPG.getProgress(),Integer.valueOf(smartRoom.getHumid()));
        animation2.setDuration(500);
        progressBarLPG.setAnimation(animation2);



        //Chart

//        List<Map<String, String>> data=null;
//
//        ArrayList<Entry> entrySuhu = new ArrayList<>();
//        ArrayList<String> labelSuhu = new ArrayList<>();
//
//
//
//        // Date formater
//        SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");
//        Date parsed = new Date();
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
//        dateFormat.setTimeZone(TimeZone.getDefault());
//
//        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
//        timeFormat.setTimeZone(TimeZone.getDefault());
//        // end date formater
//
//
//            try{
//                //parsed = sourceFormat.parse();
//            }catch (Exception e){
//                Log.e("setSuhuLoadedListener","Error parsing date");
//                e.printStackTrace();
//            }
//            entrySuhu.add(new Entry(Float.parseFloat(smartRoom.getTemp()),entrySuhu.size()));
//            //labelSuhu.add(timeFormat.format(parsed));
//
//
//        LineDataSet dataSetSuhu = new LineDataSet(entrySuhu, "Derajat celcius");
//        dataSetSuhu.setColor(Color.parseColor("#009688"));
//        dataSetSuhu.setCircleColor(Color.parseColor("#ffcdd2"));
//        dataSetSuhu.setCircleColorHole(Color.parseColor("#f44336"));
//
//        LineData dataSuhu = new LineData(labelSuhu, dataSetSuhu);
//
//        chartSuhu.setData(dataSuhu);
//
//        // Update data
//        chartSuhu.notifyDataSetChanged();
//
//        // Animate
//        chartSuhu.animateY(1000);

    }

    private void startCountAnimation(TextView textProgress, int duration, int from, int to) {
        ValueAnimator animator = new ValueAnimator();
        animator.setObjectValues(from, to);
        animator.setDuration(duration);
        final TextView textView = textProgress;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                textView.setText("" + (int) animation.getAnimatedValue()+"%");
            }
        });
        animator.start();
    }

    private void startAnimateRefreshMenu(boolean start){
        //TODO: Gak bisa berenti muter, gak di pake dulu lah
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView)inflater.inflate(R.layout.iv_refresh, null);
        Animation rotation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
        //item.setActionView(iv);
        if(start){
            mainMenu.findItem(R.id.menu_home_refresh).setActionView(iv);
        }else {
            mainMenu.findItem(R.id.menu_home_refresh).setIcon(R.drawable.ic_cached_grey_200_36dp);
        }
    }


    /*
       ==================================BLUETOOTH AREA==============================
    */
    // fast way to call Toast
        private void msg(String s)
        {
            Toast.makeText(getActivity().getApplicationContext(),s,Toast.LENGTH_LONG).show();
        }


        private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
        {
            private boolean ConnectSuccess = true; //if it's here, it's almost connected

            @Override
            protected void onPreExecute()
            {
                progress = ProgressDialog.show(getActivity(), "Connecting...", "Please wait!!!");  //show a progress dialog
            }

            @Override
            protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
            {
                try
                {
                    if (btSocket == null || !isBtConnected)
                    {
                        myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                        BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                        btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                        btSocket.connect();//start connection
                    }
                }
                catch (IOException e)
                {
                    ConnectSuccess = false;//if the try failed, you can check the exception here
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
            {
                super.onPostExecute(result);

                if (!ConnectSuccess)
                {
                    msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                    //finish();
                }
                else
                {
                    msg("Connected.");
                    isBtConnected = true;
                    mConnectedThread = new ConnectedThread(btSocket);
                    mConnectedThread.start();

                }
                progress.dismiss();
            }
        }

        private class ConnectedThread extends Thread {
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;

            public ConnectedThread(BluetoothSocket socket) {
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams, using temp objects because
                // member streams are final
                try {
                    tmpIn = socket.getInputStream();
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) { }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            }

            public void run() {
                byte[] buffer = new byte[256];  // buffer store for the stream
                int bytes; // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs
                while (true) {

                    try {
                        bytes = mmInStream.read(buffer);            //read bytes from input buffer
                        String readMessage = new String(buffer, 0, bytes);
                        // Send the obtained bytes to the UI Activity via handler
                        bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                    } catch (IOException e) {
                        break;
                    }
                }
            }

            /* Call this from the main activity to send data to the remote device */
            public void write(String message) {
                Log.d(TAG, "...Data to send: " + message + "...");
                byte[] msgBuffer = message.getBytes();
                try {
                    mmOutStream.write(msgBuffer);
                } catch (IOException e) {
                    Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
                }
            }
        }

        private String getDate(){
            DateFormat dfDate = new SimpleDateFormat("yyyy/MM/dd");
            String date=dfDate.format(Calendar.getInstance().getTime());
            DateFormat dfTime = new SimpleDateFormat("HH:mm");
            String time = dfTime.format(Calendar.getInstance().getTime());
            return date + " " + time;
        }




}
