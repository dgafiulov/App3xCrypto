package WorkWithFiles;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.util.Arrays;

public class FileWork {

    public Uri fileGet(int resultCode, Intent data, Context context) {
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri fileUri = data.getData();
                return fileUri;
            }
        }
        return null;
    }

    public Intent openFileChooserIntent(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        return intent;
    }
}
