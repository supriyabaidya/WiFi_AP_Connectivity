package com.example.tomhardy.wifiapconnectivity;


import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataTransfer extends AppCompatActivity implements Runnable {

    public static boolean isTransferComplete;

    private final int BUFFER_SIZE = 8192;

    private InputStream is = null;
    private OutputStream os = null;

    private BufferedInputStream bis = null;
    private BufferedOutputStream bos = null;

    private DataInputStream din = null;
    private DataOutputStream dout = null;

    private FileInputStream fis = null;
    private FileOutputStream fos = null;

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_transfer);

        intent = getIntent();
        ((TextView) findViewById(R.id.status)).setText(intent.getStringExtra("TYPE"));

        try {
            is = SocketHandler.getSocket().getInputStream();
            os = SocketHandler.getSocket().getOutputStream();

            bis = new BufferedInputStream(is);
            bos = new BufferedOutputStream(os);

            din = new DataInputStream(bis);
            dout = new DataOutputStream(bos);

        } catch (IOException e) {
            e.printStackTrace();
        }

        isTransferComplete=false;

        new Thread(this).start();

        Log.d("DataTransfer","onCreate");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("DataTransfer","onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("DataTransfer","onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("DataTransfer","onDestroy");

        closeAllStream();

        SocketHandler.close();

        Toast.makeText(getApplicationContext(), "socket and all Streams are closed (onDestroy)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        isTransferComplete=true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dout.writeLong(0);            // file size= 0 as for text ,since file size is the parameter to determine whether it's a file or text
                    dout.flush();
                    dout.writeUTF("bye");                    // writing "bye" to DataOutStrem
                    dout.flush();

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();


    }

    public void sendTexts(View view) {
        final String sendMessage = (((MultiAutoCompleteTextView) findViewById(R.id.compose)).getText()).toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dout.writeLong(0);            // file size= 0 as for text ,since file size is the parameter to determine whether it's a file or text
                    dout.flush();
                    dout.writeUTF(sendMessage);                    // writing sendMessage to DataOutStrem
                    dout.flush();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.messages)).append("Me : " + sendMessage + "\n");            // appending it to textArea of the Frame
                            ((MultiAutoCompleteTextView) findViewById(R.id.compose)).setText("");
                        }
                    });

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
    }

    public void sendFiles(View view) {

        String text = "Hello there! It's a sample file\n" + ((MultiAutoCompleteTextView) findViewById(R.id.compose)).getText();

        final File root = Environment.getExternalStorageDirectory();
        final File dir = new File(root.getAbsolutePath() + "/Wifi_Ap_Connectivity");
        final String filename = "sample_sent.txt";

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {

            File file = new File(dir, filename);
            FileOutputStream fo;

            try {
                fo = new FileOutputStream(file);
                fo.write(text.getBytes());
                fo.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        ((MultiAutoCompleteTextView) findViewById(R.id.compose)).setText("");

        new Thread(new Runnable() {
            @Override
            public void run() {
                File sendFile = new File(dir, filename);
                long sendFileLength = sendFile.length();
                final String sendFileName = filename;

                try {
                    dout.writeLong(sendFileLength);            // file size= sendFileLength>0 as for text ,since file size is the parameter to determine whether it's a file or text
                    dout.flush();

                    fis = new FileInputStream(sendFile);

                    byte[] byteArray = new byte[BUFFER_SIZE];
                    int bytesRead = 0;

                    runOnUiThread(new Runnable() {

                        public void run() {
                            setEnabledOfAllButton(false);            //  disable all buttons  ////////////////////////////////////
                            ((MultiAutoCompleteTextView) findViewById(R.id.compose)).setEnabled(false);

                            ((TextView) findViewById(R.id.messages)).append("File \"" + sendFileName + "\" is being sent .......\n");                    // showing the message of sending file
                        }
                    });


////////////////////////////////// sending file using byte array until all the bytes of the file are read and then written /////////////////////////////
                    final long start = System.currentTimeMillis();                    // starting time of file sending
                    try {

                        while ((bytesRead = fis.read(byteArray)) != -1) {                // sending file using byte array
                            dout.write(byteArray, 0, bytesRead);
                            dout.flush();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    final long end = System.currentTimeMillis();   // ending time of file sending
////////////////////////////// end of sending file ////////////////////////////////////////////////////////////////////////////////////////


                    runOnUiThread(new Runnable() {

                        public void run() {
                            ((TextView) findViewById(R.id.messages)).append("File is sent (In " + (end - start) / 1000.0 + " seconds\n");                    // showing the time taken to send the file

                            setEnabledOfAllButton(true);            //  enable all buttons  ////////////////////////////////////
                            ((MultiAutoCompleteTextView) findViewById(R.id.compose)).setEnabled(true);
                        }
                    });

                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
    }


    @Override
    public void run() {

        long receiveFileLength;
        File receiveFile;
        File root = Environment.getExternalStorageDirectory();
        String downloadPath = root.getAbsolutePath() + "/Wifi_Ap_Connectivity";
        File downloadDir = new File(downloadPath);

        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {

            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            try {
                while (true) {
                    receiveFileLength = din.readLong();

                    if (receiveFileLength > 0)            // file receiving
                    {
                        Log.d("receiving", "file");
                        final String receiveFileName = "sample_received.txt";
                        receiveFile = new File(downloadDir, receiveFileName);
                        fos = new FileOutputStream(receiveFile);

                        long fileSize = receiveFileLength;
                        int bytesRead = 0;
                        byte[] byteArray = new byte[BUFFER_SIZE];

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setEnabledOfAllButton(false);        /// disable all buttons  /////////////////////////////////////////////////////
                                ((MultiAutoCompleteTextView) findViewById(R.id.compose)).setEnabled(false);

                                ((TextView) findViewById(R.id.messages)).append("File \"" + receiveFileName + "\" is being received .......\n");                    // showing the message of receiving file
                            }
                        });

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


////////////////////////////////// receiving file using byte array until all the bytes of the file are read /////////////////////////////
                        final long start = System.currentTimeMillis();                    // starting time of file receiving
                        while (fileSize > 0 && (bytesRead = din.read(byteArray, 0, (int) Math.min(byteArray.length, fileSize))) != -1) {
                            fos.write(byteArray, 0, bytesRead);
                            fileSize -= bytesRead;
                        }
                        fos.close();
                        final long end = System.currentTimeMillis();                    // ending time of file receiving
///////////////////////////////////// end of receiving file /////////////////////////////////////////////////////////////////////////////


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.messages)).append("File is received (In " + (end - start) / 1000.0 + " seconds)\n");                    // showing the time taken to receive the file

                                setEnabledOfAllButton(true);            //// enable all other buttons  //////////////////////////////////////////////////////////
                                ((MultiAutoCompleteTextView) findViewById(R.id.compose)).setEnabled(true);
                            }
                        });

                    } else                                        // text receiving
                    {
                        Log.d("receiving", "text");
                        final String receiveMessage = din.readUTF();            // receiving text

                        if (receiveMessage.equalsIgnoreCase("bye")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView) findViewById(R.id.messages)).append("\nSender has left");
                                    isTransferComplete=true;
                                }
                            });
                            break;
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView) findViewById(R.id.messages)).append("Sender : " + receiveMessage + "\n");                // set the text in the textArea
                                }
                            });
                        }
                    }
                }
            } catch (EOFException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeAllStream();
                SocketHandler.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "socket and all Streams are closed (finally)", Toast.LENGTH_SHORT).show();
                    }
                });

            }

        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "sd card or media is not present", Toast.LENGTH_SHORT).show();
                }
            });
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    public void closeAllStream() {
        try {
            if (fis != null ) {fis.close();fis = null;}
            if (fos != null) {fos.close();fos = null;}
            if (din != null) {din.close();din = null;}
            if (dout != null) {dout.close();dout = null;}
            if (bis != null) {bis.close();bis = null;}
            if (bis != null) {bos.close();bis = null;}
            if (is != null) {is.close();is = null;}
            if (os != null) {os.close();os = null;}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setEnabledOfAllButton(boolean b) {
        ((Button) findViewById(R.id.send)).setEnabled(b);
        ((Button) findViewById(R.id.send_files)).setEnabled(b);
    }
}