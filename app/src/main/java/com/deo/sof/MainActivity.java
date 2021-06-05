package com.deo.sof;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import static java.lang.StrictMath.sqrt;

public class MainActivity extends AppCompatActivity {
    
    TextView logView;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        verifyStoragePermissions(this);
        
        File rootDir = new File(Environment.getExternalStorageDirectory() + File.separator + "SOF");
        
        Button addWavHeader = findViewById(R.id.addWav);
        Button addBmpHeader = findViewById(R.id.addBmp);
        Button generateOffsets = findViewById(R.id.strip);
        logView = findViewById(R.id.textView);
        
        addWavHeader.setOnClickListener(v -> {
            File[] files = rootDir.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    String outFile = file.getPath().replace(file.getName(), "SOF_processed" + File.separatorChar) + file.getName() + ".wav";
                    createWavFile(file.getPath(), outFile, 44100, (byte) 16, 2);
                    logView.setText(logView.getText() + "\n added wav header to " + file.getName());
                }
            }
        });
        
        addBmpHeader.setOnClickListener(v -> {
            File[] files = rootDir.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    String outFile = file.getPath().replace(file.getName(), "SOF_processed" + File.separatorChar) + file.getName() + ".bmp";
                    createBmpFile(file.getPath(), outFile);
                    logView.setText(logView.getText() + "\n added bmp header to " + file.getName());
                }
            }
        });
        
        generateOffsets.setOnClickListener(v -> {
            File[] files = rootDir.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    String outFile = file.getPath().replace(file.getName(), "SOF_processed" + File.separatorChar) + file.getName();
                    generateOffset(file.getPath(), outFile + "_offset1", 1);
                    
                    logView.setText(logView.getText() + "\n offset file " + file.getName());
                }
            }
        });
    }
    
    private void createBmpFile(String inFile, String outFile) {
        
        try {
            System.out.println(inFile);
            byte[] sourceBytes = offset(Files.readAllBytes(new File(inFile).toPath()), getHeaderLength(inFile));
            int size = (int) sqrt((int) (sourceBytes.length / 3f));
            new BmpWriter().saveBitmap(outFile, sourceBytes, size, size);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void createWavFile(String inFile, String outFile, long sampleRate, byte bitsPerSample, int channels) {
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen = 0;
        long totalDataLen;
        long byteRate = bitsPerSample * sampleRate * channels / 8;
        
        byte[] data = new byte[2048];
        
        try {
            
            in = new FileInputStream(inFile);
            out = new FileOutputStream(outFile);
            totalAudioLen += in.getChannel().size();
            
            totalDataLen = totalAudioLen + 36 - skipHeader(in, inFile);
            
            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    sampleRate, channels, byteRate, bitsPerSample);
            
            while (in.read(data) != -1) {
                out.write(data);
            }
            
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private int getHeaderLength(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf("."));
        switch (extension.replace(".", "").toLowerCase().trim()) {
            case ("wav"):
                logView.setText(logView.getText() + "\n skipped 44 bytes");
                return 44;
            case ("bmp"):
                logView.setText(logView.getText() + "\n skipped 54 bytes");
                return 54;
            default:
                return 0;
        }
    }
    
    private long skipHeader(FileInputStream in, String filePath) throws IOException {
        return in.skip(getHeaderLength(filePath));
    }
    
    private byte[] offset(byte[] rawBytes, int offset) {
        byte[] data_offset = new byte[rawBytes.length - offset];
        for (int i = offset; i < rawBytes.length; i++) {
            data_offset[i - offset] = rawBytes[i];
        }
        return data_offset;
    }
    
    private void generateOffset(String inFile, String outFile, int offset) {
        try {
            FileOutputStream out1 = new FileOutputStream(outFile);
            out1.write(offset(Files.readAllBytes(new File(inFile).toPath()), offset));
            out1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate, byte bitsPerSample)
            throws IOException {
        
        out.write(new byte[]{
                'R', 'I', 'F', 'F',
                (byte) (totalDataLen & 0xff),
                (byte) ((totalDataLen >> 8) & 0xff),
                (byte) ((totalDataLen >> 16) & 0xff),
                (byte) ((totalDataLen >> 24) & 0xff),
                'W', 'A', 'V', 'E', 'f', 'm', 't', ' ',
                16, 0, 0, 0, 1, 0,
                (byte) channels, 0,
                (byte) (longSampleRate & 0xff),
                (byte) ((longSampleRate >> 8) & 0xff),
                (byte) ((longSampleRate >> 16) & 0xff),
                (byte) ((longSampleRate >> 24) & 0xff),
                (byte) (byteRate & 0xff),
                (byte) ((byteRate >> 8) & 0xff),
                (byte) ((byteRate >> 16) & 0xff),
                (byte) ((byteRate >> 24) & 0xff),
                (byte) (2 * 16 / 8), 0, bitsPerSample, 0,
                'd', 'a', 't', 'a',
                (byte) (totalAudioLen & 0xff),
                (byte) ((totalAudioLen >> 8) & 0xff),
                (byte) ((totalAudioLen >> 16) & 0xff),
                (byte) ((totalAudioLen >> 24) & 0xff)
        }, 0, 44);
    }
    
    void verifyStoragePermissions(Activity activity) {
        boolean granted = true;
        for (int i = 0; i < PERMISSIONS.length; i++) {
            granted = granted && ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        if (!granted) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, 1);
        }
    }
}