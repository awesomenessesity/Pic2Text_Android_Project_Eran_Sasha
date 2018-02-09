package com.example.eran.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//processed image and text activity
public class OutputActivity extends AppCompatActivity {

    //constants
    private final String INTENT_WITH_PIC = "Picture";
    private final String OLD_CAMERA_INTENT = "OldCamera";
    private final String OLD_GALLERY_INTENT = "OldGallery";
    private final String CONTINUOUS_FILE_PATH = "ContinuousFilePath";
    private final String CROPPED_PICTURE_PATH = "filepath";
    private final String NEW_FILE_PROCESS = "NewFileProcess";
    private final String EXISTING_FILE = "ExistingFile";
    private final String PROCESS_LIST = "process_list";
    private final String FONT_LIST = "font_list";
    private final String DESTINATION_LANG = "dest_list";

    EditText editText;
    int PERMISSION_REQUEST = 141;
    FileWriter fw = null;
    BufferedWriter bw = null;
    boolean isExist = false;

    //structured the ocr
    Bitmap selectedBitmap;
    ImageView imageOCR;
    String picFromCrop;

    //tesseract alorithm
    String datapathLib = "";
    private TessBaseAPI mTess;
    final String langLibEn = "tessdata/eng.traineddata";
    final String langLibHeb = "tessdata/heb.traineddata";
    final String langLibRus = "tessdata/rus.traineddata";
    final static String  ENGLISH = "eng";
    final static String  RUSSIAN = "rus";
    final static String  HEBREW = "heb";

    String txtFileName;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_output);
        //set the text
        editText = (EditText) findViewById(R.id.editText);
        editText.setText("");
        progressDialog = new ProgressDialog(this);

        //for Ocr recognition
        Intent importedIntent = getIntent();
        picFromCrop = importedIntent.getStringExtra(CROPPED_PICTURE_PATH);
        imageOCR = (ImageView) findViewById(R.id.image_output);
        String whatToDo = importedIntent.getStringExtra(INTENT_WITH_PIC);

        txtFileName = picFromCrop.substring(0, picFromCrop.lastIndexOf(".")) + ".txt";
        selectedBitmap = BitmapFactory.decodeFile(picFromCrop);
        imageOCR.setImageBitmap(selectedBitmap);
        //get the shared pref language - default "english"
        SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(this);
        String storedLanguage = sharedPref.getString(PROCESS_LIST, "English");

        switch (whatToDo){
            case CONTINUOUS_FILE_PATH: //if continuous file arrived continue processing and not new file
                isExist = true;
                //load existing
                loadTextFile(txtFileName);
                //append new
                setProcessLanguage(storedLanguage);
                //is new picture -> path
                processImage(picFromCrop);
                break;
            case NEW_FILE_PROCESS: //if new, obviously treat it as new
                isExist = false;
                setProcessLanguage(storedLanguage);
                //is new picture -> path
                processImage(picFromCrop);
                break;
            case EXISTING_FILE: //if its already existed just load it
                isExist = true;
                loadTextFile(txtFileName);
        }
    }

    /**
     * set the language of the process
     * @param language - the string of the lang
     */
    private void setProcessLanguage(String language){
        switch(language){
            case "Hebrew":
                datapathLib = getFilesDir() + "/tesseract/";
                checkFile(new File(datapathLib + "tessdata/"), langLibHeb);
                mTess = new TessBaseAPI();
                mTess.init(datapathLib, HEBREW);
                break;
            case "English":
                datapathLib = getFilesDir() + "/tesseract/";
                checkFile(new File(datapathLib + "tessdata/"), langLibEn);
                mTess = new TessBaseAPI();
                mTess.init(datapathLib, ENGLISH);
                break;
            case "Russian":
                datapathLib = getFilesDir() + "/tesseract/";
                checkFile(new File(datapathLib + "tessdata/"), langLibRus);
                mTess = new TessBaseAPI();
                mTess.init(datapathLib, RUSSIAN);
                break;
        }
    }

    /**
     * load the text file if it exists
     * @param textName - the path
     */
    public void loadTextFile(String textName)
    {
        File file = new File(textName);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            Toast.makeText(this, getResources().getString(R.string.toast8_no_file_open),Toast.LENGTH_SHORT);
        }
        //check the shared pref size - default 14
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String size = sp.getString(FONT_LIST, "14");
        editText.setTextSize(Float.valueOf(size));
        editText.setText(text);
    }

    /**
     * check permissions for read/write
     * @return - true if we got them, else request them.
     */
    private boolean checkPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode==PERMISSION_REQUEST && grantResults[0]== android.content.pm.PackageManager.PERMISSION_GRANTED
                && grantResults[1]== android.content.pm.PackageManager.PERMISSION_GRANTED ){
            Toast.makeText(this, getResources().getString(R.string.toast9_got_perm), Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, getResources().getString(R.string.toast6_no_all_perms), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.output_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //action bar buttons - speak for themselvs
        if (id == R.id.action_share) { //share text
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, editText.getText().toString());
            shareIntent.setType("text/plain");
            startActivity(Intent.createChooser(shareIntent, "Share with:"));
            return true;
        }
        if (id == R.id.action_add) {
            saveFile();
            continuousPhoto();
            return true;
        }
        if (id == android.R.id.home) {
            returnFunction();
            return true;
        }
        if (id == R.id.action_save) {
            saveFile();
            goToMain();
            return true;
        }
        if (id == R.id.action_send) {
            //create file - save the name of the file
            saveFile();
            //create intent to send the file to another apps
            intentToShareFile();
            return true;
        }
        if (id == R.id.action_translate) {
            goToTranslate();
            //GO TO GoogleTranslate
            return true;
        }
        if (id == R.id.action_delete) {
            int index = ImageLoader.findInVector(picFromCrop);
            if(index > -1){
                ImageLoader.removeExistingItemAt(index);
            }
            deleteFiles(txtFileName, picFromCrop);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * easy function to call the intent to share a file
     */
    private void intentToShareFile(){
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        intentShareFile.setType("*/*");
        intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+txtFileName));
        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Sharing File...");
        intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
        startActivity(Intent.createChooser(intentShareFile, "Share File"));
    }

    /**
     * ask the user which source of import he wants
     */
    private void continuousPhoto(){
        final Intent cameraIntent = new Intent(this, ImportActivity.class);
        AlertDialog.Builder importPicDialog;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            importPicDialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            importPicDialog = new AlertDialog.Builder(this);
        }
        importPicDialog.setMessage("Import another picture from:");
        importPicDialog.setCancelable(true);
        importPicDialog.setPositiveButton(
                "Gallery",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        cameraIntent.putExtra(INTENT_WITH_PIC, OLD_GALLERY_INTENT);
                        cameraIntent.putExtra(CONTINUOUS_FILE_PATH, picFromCrop);
                        startActivity(cameraIntent);
                        dialog.cancel();
                    }
                });

        importPicDialog.setNegativeButton(
                "Camera",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        cameraIntent.putExtra(INTENT_WITH_PIC, OLD_CAMERA_INTENT);
                        cameraIntent.putExtra(CONTINUOUS_FILE_PATH, picFromCrop);
                        startActivity(cameraIntent);
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = importPicDialog.create();
        alert11.show();
    }

    /**
     * return function activates on return button and physical return.
     * making sure he wants to quit without saving or not
     */
    private void returnFunction(){
        if(!isExist){
            AlertDialog.Builder alertDialog;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alertDialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                alertDialog = new AlertDialog.Builder(this);
            }
            alertDialog.setMessage("Are you sure you want to cancel without saving? ");
            alertDialog.setCancelable(true);
            alertDialog.setPositiveButton(
                    "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            deleteFiles(txtFileName, picFromCrop);
                            dialog.cancel();
                        }
                    });

            alertDialog.setNegativeButton(
                    "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert11 = alertDialog.create();
            alert11.show();
        }
        else {
            saveFile();
            goToMain();
        }
    }

    /**
     * save the text file and update it
     */
    private void saveFile() {
        if(checkPermissions()){
            try {
                fw = new FileWriter(txtFileName, false);
                bw = new BufferedWriter(fw);
                bw.write(this.editText.getText().toString());
                bw.flush();
                bw.close();
                fw.close();
                if(!isExist){
                    ImageLoader.addNewItem(picFromCrop);
                }
                isExist = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * call the main activity intent
     */
    private void goToMain(){
        Intent gotoMain = new Intent(this, MainActivity.class);
        startActivity(gotoMain);
        finish();
    }

    /**
     * delete the image and text files
     * @param fileName - the path of the text file
     * @param imageName - the path of the image file
     */
    private void deleteFiles(String fileName,String imageName) {
        if(checkPermissions()) {
            File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Pic2Text");
            for (File file : f.listFiles()) {
                if (fileName.equals(file.getAbsolutePath().toString()) || imageName.equals(file.getAbsolutePath().toString())) {
                    file.delete();
                }
            }
            Intent goToMain = new Intent(this, MainActivity.class);
            startActivity(goToMain);
        }
    }

    /**
     * check if the requested package is installed on the phone
     * @param packagename - the name
     * @param packageManager - manager
     * @return - true if installed
     */
    private boolean isPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packagename, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * go to google translate - app or chrome depending if package is installed.
     */
    private void goToTranslate() {
        SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(this);
        String srcLanguage = sharedPref.getString(PROCESS_LIST, "English");
        String destLanguage = sharedPref.getString(DESTINATION_LANG, "Hebrew");
        String source="", target="";

        switch(srcLanguage){
            case "English": source ="en";
                break;
            case "Hebrew":  source ="iw";
                break;
            case "Russian": source ="ru";
                break;
        }
        switch(destLanguage){
            case "English": target ="en";
                break;
            case "Hebrew":  target ="iw";
                break;
            case "Russian": target ="ru";
                break;
        }

        PackageManager pm = this.getPackageManager();
        boolean translateAppInstalled = isPackageInstalled("com.google.android.apps.translate", pm);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        PackageManager manager = getPackageManager();

        if(translateAppInstalled) {
            intent.setPackage("com.google.android.apps.translate");
        }


        Uri uri = new Uri.Builder()
                .scheme("http")
                .authority("translate.google.com")
                .path("/m/translate")
                .appendQueryParameter("q", editText.getText().toString())
                .appendQueryParameter("tl", target) // target language
                .appendQueryParameter("sl", source) // source language
                .build();
        //intent.setType("text/plain"); //not needed, but possible
        intent.setData(uri);
        startActivity(intent);
    }

    /**
     * tesseract helper function get String langLibHeb/ langLibEn
     * @param langLib - the language library
     */
    private  void copyFiles(String langLib) {
        try{
            String filepath = datapathLib + langLib;
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open(langLib);
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while((read=instream.read(buffer))!=-1){
                outstream.write(buffer,0,read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    /**
     * get the language library
     * @param dir - the folder path
     * @param langLib - the language library
     */
    private void checkFile(File dir, String langLib)
    {
        if(!dir.exists() && dir.mkdirs()){
            copyFiles(langLib);
        }
        if(dir.exists()){
            String datafilepath = datapathLib + "/"+langLib;
            File datafile = new File (datafilepath);
            if(!datafile.exists()){
                copyFiles(langLib);
            }
        }
    }

    /**
     * run the algorithm of the image selected
     * @param path - the path of the image
     */
    public void processImage(String path)
    {
        selectedBitmap =  BitmapFactory.decodeFile(path);
        imageOCR.setImageBitmap(selectedBitmap);
        //execute the heavy Image process in the background thread.
        final ProcessInTheBackground doBackground = new ProcessInTheBackground();
        doBackground.execute((Void) null);
        //limit the async task to 3.5 seconds run. if it didn't finish dam...
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run() {
                if ( doBackground.getStatus() == AsyncTask.Status.RUNNING ) {
                    doBackground.cancel(true);
                    deleteFiles(txtFileName, picFromCrop);
                    progressDialog.dismiss();
                    Toast.makeText(OutputActivity.this, getResources().getString(R.string.toast7_fail_process), Toast.LENGTH_SHORT).show();
                    goToMain();
                }
            }}, 3500 );
    }

    @Override
    public  void  onBackPressed() {
        returnFunction();
    }

    /**
     * asynctask to handle the heavy process so the UI wont get stuck.
     */
    private class ProcessInTheBackground extends AsyncTask <Void,Void,Void> {
        String OCResult = null;

        @Override
        protected void onPreExecute() {
            //before loading the heavy process. let the user know whats up.
            progressDialog.setMessage("Processing Image... Please wait");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            //do the heavy load in the background
            mTess.setImage(selectedBitmap);
            OCResult = mTess.getUTF8Text();
            return null;
        }

        @Override
        protected void onPostExecute(Void o)
        {
            //after finishing the process - show on the UI
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String size = sp.getString(FONT_LIST, "14");
            editText.setTextSize(Float.valueOf(size));
            editText.append(OCResult);
            progressDialog.cancel();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            // Doing nothing
        }
    }
}
