package WorkWithFiles;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import app.view.app3xcrypto.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;

import cipher.Cipher;

public class FileSaver implements Runnable{

    /*
    actionType:
    0 - decode in new versions
    1 - encode in new versions
    2 - decode in old versions
    3 - encode in old versions
     */
    private int actionType;
    private Uri orig;
    private Uri newFile;
    private String key;
    private Context context;
    String newFileName;
    private boolean isInitialized = false;
    Cipher cipher = new Cipher();
    boolean wasEven;
    InputStream inputStream;
    BufferedInputStream bufferedInputStream;
    ParcelFileDescriptor pfd;
    FileOutputStream fileOutputStream;
    BufferedOutputStream bufferedOutputStream;

    @Override
    public void run() {
        if (isInitialized) {
            try {
                readerInit(orig);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            switch (actionType) {
                case 0:
                    try {
                        decodeAndWrite();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case 1:
                    try {
                        encodeAndWrite();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case 2:
                    try {
                        decodeAndWriteInOldVersions();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case 3:
                    try {
                        encodeAndWriteInOldVersions();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }
            try {
                readerDestruct();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void initSaver(int actionType, Uri orig, String key, Context context, Uri newFile) {
        this.actionType = actionType;
        this.orig = orig;
        this.key = key;
        this.context = context;
        this.newFile = newFile;
        isInitialized = true;
    }

    public void initSaver(int actionType, Uri orig, String key, Context context, String newFileName) {
        this.actionType = actionType;
        this.orig = orig;
        this.key = key;
        this.context = context;
        this.newFileName = newFileName;
        isInitialized = true;
    }

    private void writerInit(Uri uri) throws FileNotFoundException {
        pfd = context.getContentResolver().
                openFileDescriptor(uri, "w");
        fileOutputStream =
                new FileOutputStream(pfd.getFileDescriptor());
    }

    private void writerDestruct() throws IOException {
        fileOutputStream.close();
        pfd.close();
    }

    private void writeInFile(byte[] bytes) {
        try {
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            Toast.makeText(context, R.string.error_text, Toast.LENGTH_SHORT).show();
        }
    }

    private void writerInOldVersionsInit(String fileName) throws FileNotFoundException {
        File downloadDir = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        File file;
        if (fileName != null) {
            file = new File(downloadDir, fileName);
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                file = new File(downloadDir, LocalDate.now() + ".txt");
            } else {
                file = new File((Math.random() * 1000) + ".txt");
            }
        }
        fileOutputStream = new FileOutputStream(file);
        bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
    }

    private void writerInOldVersionsDestruct() throws IOException {
        bufferedOutputStream.close();
        fileOutputStream.close();
    }

    public void writeInFileInOldVersions(byte[] orig) throws IOException {
        for (byte i:orig) {
            bufferedOutputStream.write(i);
        }
    }

    private void readerInit(Uri uri) throws IOException {
        inputStream = context.getContentResolver().openInputStream(uri);
        bufferedInputStream = new BufferedInputStream(inputStream);
        wasEven = bufferedInputStream.available() % 2 == 0;
    }

    private void readerDestruct() throws IOException {
        inputStream.close();
        bufferedInputStream.close();
    }

    private void encodeAndWrite() throws IOException {
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        boolean isLastChunk;

        writerInit(newFile);

        writeInFile(new byte[]{(byte) (wasEven ? 0 : 1)});

        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
            isLastChunk = bytesRead < bufferSize;
            if (isLastChunk) {
                buffer = cipher.encodeChunk(readLastChunk(buffer, bytesRead), key, context);
            } else {
                buffer = cipher.encodeChunk(buffer, key, context);
            }
            writeInFile(buffer);
        }
        writerDestruct();
    }

    private void encodeAndWriteInOldVersions() throws IOException {
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        boolean isLastChunk;

        writerInOldVersionsInit(newFileName);

        writeInFileInOldVersions(new byte[]{(byte) (wasEven ? 0 : 1)});

        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
            isLastChunk = bytesRead < bufferSize;
            if (isLastChunk) {
                buffer = cipher.encodeChunk(readLastChunk(buffer, bytesRead), key, context);
            } else {
                buffer = cipher.encodeChunk(buffer, key, context);
            }
            writeInFileInOldVersions(buffer);
        }
        writerInOldVersionsDestruct();
    }

    private void decodeAndWrite() throws IOException{
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        long totalBytesRead = 0;
        boolean isLastChunk;

        writerInit(newFile);

        byte[] firstSymbol = new byte[1];
        bufferedInputStream.read(firstSymbol);
        boolean wasEven = firstSymbol[0] == 0;

        long fileSize = bufferedInputStream.available();

        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
            isLastChunk = fileSize - totalBytesRead <= bufferSize;
            if (isLastChunk) {
                buffer = cipher.decodeChunk(readLastChunkForDecoding(buffer, bytesRead), key);
                if (!wasEven) {
                    buffer = Arrays.copyOfRange(buffer, 0, buffer.length - 1);
                }
            } else {
                buffer = cipher.decodeChunk(buffer, key);
            }
            writeInFile(buffer);
            totalBytesRead += bytesRead;
        }

        writerDestruct();
    }

    private void decodeAndWriteInOldVersions() throws IOException{
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        long totalBytesRead = 0;
        boolean isLastChunk;

        writerInOldVersionsInit(newFileName);

        byte[] firstSymbol = new byte[1];
        bufferedInputStream.read(firstSymbol);
        boolean wasEven = firstSymbol[0] == 0;

        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
            isLastChunk = bufferedInputStream.available() - totalBytesRead <= bufferSize;
            if (isLastChunk) {
                buffer = cipher.decodeChunk(readLastChunkForDecoding(buffer, bytesRead), key);
                if (!wasEven) {
                    buffer = Arrays.copyOfRange(buffer, 0, buffer.length - 1);
                }
            } else {
                buffer = cipher.decodeChunk(buffer, key);
            }
            writeInFileInOldVersions(buffer);
            totalBytesRead += bytesRead;
        }

        writerInOldVersionsDestruct();
    }

    private byte[] readLastChunk(byte[] lastChunk, int bytesRead) {
        byte[] temp;

        if (wasEven) {
            temp = new byte[bytesRead];
            System.arraycopy(lastChunk, 0, temp, 0, bytesRead);
        } else {
            temp = new byte[bytesRead + 1];
            System.arraycopy(lastChunk, 0, temp, 0, bytesRead);
            temp[bytesRead] = 0;
        }
        return temp;
    }

    private byte[] readLastChunkForDecoding(byte[] lastChunk, int bytesRead) {
        byte[] temp = new byte[bytesRead];
        System.arraycopy(lastChunk, 0, temp, 0, bytesRead);
        return temp;
    }
}