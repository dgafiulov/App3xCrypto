package appStart;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import app.view.app3xcrypto.MainActivity;

public class AppStart extends Application {

    private final int sdkVersion = android.os.Build.VERSION.SDK_INT;

    @Override
    public void onCreate() {
        super.onCreate();

    }

    public int getSdkVersion() {
        return sdkVersion;
    }

    private void showDialog(Context context) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle("Warning");
        alertDialog.setMessage("This app works with Android 10 and higher. Work with versions under 10 is not guaranteed!");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                Intent intent = new Intent(AppStart.this, MainActivity.class);
                startActivity(intent);
            }
        });
        alertDialog.show();
    }
}