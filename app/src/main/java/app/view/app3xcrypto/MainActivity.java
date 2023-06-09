package app.view.app3xcrypto;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import WorkWithFiles.FileSaver;
import WorkWithFiles.FileWork;
import app.view.app3xcrypto.databinding.ActivityMainBinding;
import appStart.AppStart;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String newName;
    private Context context;
    FileSaver fileSaver = new FileSaver();
    private FileWork fileWork = new FileWork();
    private Uri fileUri;
    private int sdkVersion = new AppStart().getSdkVersion();
    private final int chooseFileCode = 1;
    private final int saveFileCode = 2;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        bindingInit();
        setContentView(binding.getRoot());
        initView();

        ActivityCompat.requestPermissions(this,
                new String[]{READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setCorrectTextForFile();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == chooseFileCode) {
            fileUri = fileWork.fileGet(resultCode, data, context);
        } else if (requestCode == saveFileCode) {
            try {
                fileSaver.initSaver((getSwitchResult() ? 0 : 1), fileUri, getPasswordFromEditText(), context, fileWork.fileGet(resultCode, data, context));
                Thread saveThread = new Thread(fileSaver);
                saveThread.start();
            } catch (Exception e) {
                showErrorDialog();
            }
        }
    }

    public void openFileChooser(View view) {
        Intent intent = fileWork.openFileChooserIntent(view);
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, 1);
    }

    public void bindingInit() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
    }

    private void initView() {
        binding.btChooseFile.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                openFileChooser(view);
            }
        });

        binding.btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fileUri != null & !getPasswordFromEditText().equals("")) {
                    if (sdkVersion >= 29) {
                        try {
                            intentForFileSave();
                        } catch (Exception e) {
                            showErrorDialog();
                        }
                    } else {
                        showAskForNewNameDialog();
                    }
                }
            }
        });

    }

    private String getPasswordFromEditText() {
        return binding.etPasswordInput.getText().toString();
    }

    private boolean getSwitchResult() {
        return binding.swChoose.isChecked(); //true is decode, false is encode
    }

    private void setCorrectTextForFile() {
        if (fileUri != null) {
            binding.tvChosenNotChosen.setText(R.string.file_is_chosen);
        }
    }

    private void intentForFileSave() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, 2);
    }

    private void showNotCorrectVersionDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(R.string.warning);
        alertDialog.setMessage(getString(R.string.not_correct_version));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok), (dialogInterface, i) -> dialogInterface.dismiss());
        alertDialog.show();
    }

    public void showErrorDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(R.string.error);
        alertDialog.setMessage(getString(R.string.error_text));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok), (dialogInterface, i) -> dialogInterface.dismiss());
        alertDialog.show();
    }

    public void showAskForNewNameDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(R.layout.prompt, null);
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setView(promptView);
        final EditText userInput = (EditText) promptView.findViewById(R.id.newNameEt);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok), (dialogInterface, i) -> {
            dialogInterface.dismiss();
            newName = String.valueOf(userInput.getText());
            fileSaver.initSaver((getSwitchResult() ? 2 : 3), fileUri, getPasswordFromEditText(), context, newName);
            Thread saveThread = new Thread(fileSaver);
            saveThread.start();
        });
        alertDialog.show();
    }
}