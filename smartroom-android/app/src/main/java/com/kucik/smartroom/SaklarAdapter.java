package com.kucik.smartroom;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kucik.smartroom.model.Saklar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmConfiguration;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by sucipto on 4/25/16.
 */
public class SaklarAdapter extends RecyclerView.Adapter<SaklarAdapter.SaklarHolder> {

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


    private List<Saklar> saklarList;

    // Realm
    Realm realm;

    // Context
    Context context;

    // Listener
    OnSaklarItemClickListener saklarItemClickListener;

    public class SaklarHolder extends RecyclerView.ViewHolder {
        public TextView saklarName;
        public SwitchCompat saklarSwitch;
        public ImageView saklarIcon;

        public SaklarHolder(View view) {
            super(view);

            saklarName = (TextView) view.findViewById(R.id.saklar_text);
            saklarSwitch = (SwitchCompat) view.findViewById(R.id.saklar_switch);
            saklarIcon = (ImageView) view.findViewById(R.id.saklar_icon);

            // Click Listener
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (saklarItemClickListener != null) {
                        saklarItemClickListener.onClick(v, position);
                    }
                }
            });
        }
    }

    public SaklarAdapter(List<Saklar> saklarList, String addressIn) {
        this.saklarList = saklarList;
        address=addressIn;
        OpenBluetooth();


    }

    @Override
    public SaklarHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        context = parent.getContext();

        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_recycler_saklar, parent, false);

        // Init Realm
        RealmConfiguration config = new RealmConfiguration.Builder(context)
                .name("smartroom.realm")
                .schemaVersion(1)
                .build();

        realm = Realm.getInstance(config);

        return new SaklarHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final SaklarHolder holder, final int position) {

        final Saklar saklar = saklarList.get(position);

        // Saklar Switch Listenner
        CompoundButton.OnCheckedChangeListener toggleListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {


                realm.beginTransaction();
                if (isChecked) {
                    saklar.setValue(1);
                    holder.saklarIcon.setImageResource(R.drawable.ic_lightbulb_outline_green);

                    if(position==0){
                        mConnectedThread.write("1");
                    } else if(position==1){
                        mConnectedThread.write("3");
                    }
                    else {
                        mConnectedThread.write("5");
                    }

                    Log.d(TAG,"Lampu On : "+String.valueOf(position));

                } else {
                    saklar.setValue(0);
                    holder.saklarIcon.setImageResource(R.drawable.ic_lightbulb_outline_grey);
                    if(position==0){
                        mConnectedThread.write("0");
                    } else if(position==1){
                        mConnectedThread.write("2");
                    }
                    else {
                        mConnectedThread.write("4");
                    }

                    Log.d(TAG,"Lampu Off : "+String.valueOf(position));
                }
                realm.commitTransaction();


            }
        };

        holder.saklarName.setText(saklar.getName());

        // Disable Listener sementara
        holder.saklarSwitch.setOnCheckedChangeListener(null);

        // Set Saklar dari Object / Database
        if (saklar.getValue() == 1) {
            holder.saklarIcon.setImageResource(R.drawable.ic_lightbulb_outline_green);
            holder.saklarSwitch.setChecked(true);
        } else if (saklar.getValue() == 0) {
            holder.saklarIcon.setImageResource(R.drawable.ic_lightbulb_outline_grey);
            holder.saklarSwitch.setChecked(false);
        }

        holder.saklarSwitch.setOnCheckedChangeListener(toggleListener);

    }

    @Override
    public int getItemCount() {
        return saklarList.size();
    }

    // Listener interface
    public interface OnSaklarItemClickListener {
        public void onClick(View v, int pos);
    }

    // Listenner setter
    public void setOnSaklarItemClickListener(OnSaklarItemClickListener listener) {
        this.saklarItemClickListener = listener;
    }

    /*
       ==================================BLUETOOTH AREA==============================
    */
    // fast way to call Toast
    private void msg(String s)
    {
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {

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
            //progress.dismiss();
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

    public void CloseBluetooth(){
        try     {
            btSocket.close();
            btSocket=null;
        } catch (IOException e2) {
            msg("In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    public void OpenBluetooth(){
        new ConnectBT().execute();
    }

}
