package com.kucik.smartroom;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.kucik.smartroom.model.Lampu;
import com.kucik.smartroom.model.Saklar;
import com.kucik.smartroom.view.DividerItemDecoration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by sucipto on 4/14/16.
 */
public class FragmentSaklar extends Fragment {

    //------------------------------------
    String address = null;
    private static final String TAG = "TNASamyang ";
    final int handlerState = 0;

    //Bluetooth Declaration
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
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
    Lampu lampuAll = null;
    //------------------------------------



    // Main View
    View mainView;

    // RecyclerView Saklar
    private RecyclerView recyclerView;

    // Data adapter recyclerview
    SaklarAdapter saklarAdapter;

    // Data Saklar
    List<Saklar> saklarList = new ArrayList<>();

    // Realm
    Realm realm;

    // Realm saklar result
    RealmResults<Saklar> results;



    /**
     * =============================================================================================
     */

    // Empty Constructor
    public FragmentSaklar() {
        // Nothing
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lampuAll = new Lampu("-","-","-");
        //Bluetooth SharedPref Address
        SharedPreferences prefs = getContext().getSharedPreferences("MACBluetooth", MODE_PRIVATE);
        String restoredText = prefs.getString("address", null);
        if (restoredText != null) {
            Log.d("Address BT :",prefs.getString("address", "No name defined"));
            address=prefs.getString("address", "No name defined");
            lampuAll.setLamp1(prefs.getString("lamp1", "0"));
            lampuAll.setLamp2(prefs.getString("lamp2", "0"));
            lampuAll.setLampAC(prefs.getString("lampAC", "0"));
        }
        else{
            Log.d("Address BT :","kosong");
        }
        saklarList.clear();





        // Init Realm
        RealmConfiguration config = new RealmConfiguration.Builder(getContext())
                .name("smartroom.realm")
                .schemaVersion(1)
                .build();

        realm = Realm.getInstance(config);

        getDataSaklar();
    }

    @Override
    public void onResume() {
        super.onResume();


    }
    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        saklarAdapter.CloseBluetooth();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_saklar, container, false);

        // Init Saklar RecyclerView
        setupSaklarRecycler();

        return mainView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_saklar, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.menu_refresh:
                getDataSaklar();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void setupSaklarRecycler() {
        // Init The View
        recyclerView = (RecyclerView) mainView.findViewById(R.id.saklar_recycler);

        // Set Adapter
        saklarAdapter = new SaklarAdapter(saklarList,address);
        saklarAdapter.OpenBluetooth();
        recyclerView.setAdapter(saklarAdapter);


        // Set Layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));

        // Item Decorator / Divider on list
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));

        // Add click listenner


        saklarAdapter.setOnSaklarItemClickListener(new SaklarAdapter.OnSaklarItemClickListener() {
            @Override
            public void onClick(View v, int pos) {
//                Intent saklarHistory = new Intent(getContext(),SaklarHistory.class);
                Saklar item = saklarList.get(pos);
//                saklarHistory.putExtra("saklarID",item.getId());
//                startActivity(saklarHistory);

            }
        });


        CompoundButton.OnCheckedChangeListener toggleListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    Log.d(TAG,"Check 1");
                } else {
                    Log.d(TAG,"Check 2");
                }


            }
        };


        // Populate data
        //getDataSaklar();

    }

    /**
     * Get Data Saklar dari API
     */

    private void getDataSaklar() {

        // Clear Item
        saklarList.clear();

        // Api Class
        AsyncBluetooth asyncBluetooth = new AsyncBluetooth(getContext());

        // Update subtittle
        setSubTitle("Updating data ...");

//        // Set Listenner
//        asyncBluetooth.setSaklarValueUpdateListener(new AsyncBluetooth.SaklarValueUpdateListener() {
//            @Override
//            public void onValueLoaded() {
//
//            }
//
//            @Override
//            public void onFail(){
//                setSubTitle("Update data failed");
//            }
//        });
        results = realm.where(Saklar.class).findAll();
        saklarList.clear();
        saklarList.addAll(results);

        setSubTitle("");


        // Get The Data
        results = realm.where(Saklar.class).findAll();

        if (results.size() == 0) {

            realm.beginTransaction();

            Saklar saklar1 = realm.createObject(Saklar.class);

            saklar1.setId("lampu1");
            saklar1.setName("Lampu Utama");
            saklar1.setValue(Integer.valueOf(lampuAll.getLamp1()));

            Saklar saklar2 = realm.createObject(Saklar.class);

            saklar2.setId("lampu2");
            saklar2.setName("Lampu Peringatan");
            saklar2.setValue(Integer.valueOf(lampuAll.getLamp2()));

            Saklar saklar3 = realm.createObject(Saklar.class);

            saklar3.setId("lampuAC");
            saklar3.setName("Lampu AC");
            saklar3.setValue(Integer.valueOf(lampuAll.getLampAC()));


            realm.commitTransaction();

            results = realm.where(Saklar.class).findAll();

        }

        saklarList.addAll(results);

        // Notify the adapter
        //saklarAdapter.notifyDataSetChanged();
    }

    private void setTitle(String title){
        try {
            ((MainActivity) getActivity())
                    .getSupportActionBar()
                    .setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setSubTitle(String sub){
        try {
            ((MainActivity) getActivity())
                    .getSupportActionBar()
                    .setSubtitle(sub);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /*
       ==================================BLUETOOTH AREA==============================
    */

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
                    //bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
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


}
