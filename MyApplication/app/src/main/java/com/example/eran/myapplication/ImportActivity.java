package com.example.eran.myapplication;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

//activity of the imported imaged (gallery or camera)
public class ImportActivity extends AppCompatActivity {

    //some constants for messaging between the functions/activities
    private final int GALLERY_REQUEST = 56432;
    private final int CAMERA_REQUEST = 9858;
    private final int PIC_CROP = 12344;
    private final int GALLERY_PERMISSION = 1231;
    private final int CAMERA_PERMISSION = 15858;
    private final String INTENT_WITH_PIC = "Picture";
    private final String NEW_CAMERA_INTENT = "NewCamera";
    private final String NEW_GALLERY_INTENT = "NewGallery";
    private final String OLD_CAMERA_INTENT = "OldCamera";
    private final String OLD_GALLERY_INTENT = "OldGallery";
    private final String CONTINUOUS_FILE_PATH = "ContinuousFilePath";
    private final String CROPPED_PICTURE_PATH = "filepath";
    private final String NEW_FILE_PROCESS = "NewFileProcess";
    private final String PROCESS_LIST = "process_list";

    ImageView pictureImageView;
    private Spinner sp;
    private Uri picContentUri;
    private String cameraPicPath;
    Bitmap selectedBitmap;
    String absolutePicCropPath;
    String timeStamp;
    private boolean deleteTempFile = false;
    private boolean cameraRequest = false;
    private boolean newRequest = false;
    private String extraForOutput;
    private boolean galleryPermission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        //the picture that will be cropped
        pictureImageView = (ImageView)findViewById(R.id.picIV);
        //get the shared preferences an put them inside the spinner (processing languages)
        SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(this);
        String storedLanguage = sharedPref.getString(PROCESS_LIST, "English");
        String[] languageList = getResources().getStringArray(R.array.lang_list);
        sp = (Spinner) findViewById(R.id.spLanguage);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, languageList);
        sp.setAdapter(adapter);
        sp.setSelection(getSpinnerIndex(sp,storedLanguage));

        //get the source of the called intent, gallery, camera or continuous media
        Intent importedPic = getIntent();
        String whatToDo = importedPic.getStringExtra(INTENT_WITH_PIC);

        //new stands for new files completely, old stand for continuous media.
        switch (whatToDo){
            case NEW_CAMERA_INTENT:
                newRequest = true;
                askForCameraPicture();
                break;
            case NEW_GALLERY_INTENT:
                newRequest = true;
                askForGaleryPicture();
                break;
            case OLD_CAMERA_INTENT:
                newRequest = false;
                absolutePicCropPath = importedPic.getStringExtra(CONTINUOUS_FILE_PATH);
                askForCameraPicture();
                break;
            case OLD_GALLERY_INTENT:
                newRequest = false;
                absolutePicCropPath = importedPic.getStringExtra(CONTINUOUS_FILE_PATH);
                askForGaleryPicture();
                break;
            default:
                goToMain();
        }
    }

    /**
     * get the index of the spinner to show depending on the string we want (shared pref chosen language)
     * @param spinner - the spinner
     * @param string - the string inside spinner
     * @return - the index of that string in the spinner
     */
    private int getSpinnerIndex(Spinner spinner, String string)
    {
        int index = 0;
        for (int i = 0; i < spinner.getCount(); i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(string)){
                index = i;
                break;
            }
        }
        return index;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //gallery permissions
        if(requestCode == GALLERY_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                //if its a gallery asked for permission. go to gallery action again
                if(galleryPermission) {
                    askForGaleryPicture();
                }
                else{ //the process has same permissions so - boolean to distinct
                    goToOutputProcess();
                }
            }
            else{
                Toast.makeText(this, getResources().getString(R.string.toast2_need_gallery), Toast.LENGTH_SHORT).show();
                goToMain();
            }
        }
        //camera permissions are a bit larger
        else if (requestCode == CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                askForCameraPicture();
            }
            else{
                Toast.makeText(this, getResources().getString(R.string.toast1_need_camera), Toast.LENGTH_SHORT).show();
                goToMain();
            }
        }
    }

    /**
     * the physical button, delete all temp files created if no process been done.
     */
    @Override
    public  void  onBackPressed() {
        deleteCameraTempFile();
        deleteCroppedFile();
        goToMain();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if result is not ok, delete the camera temp file (if there is any) and go to main
        if(resultCode != RESULT_OK){
            deleteCameraTempFile();
            Toast.makeText(this, getResources().getString(R.string.toast3_pic_canceled), Toast.LENGTH_SHORT).show();
            goToMain();
        }
        else { //get the gallery image and send to to crop intent
            if (requestCode == GALLERY_REQUEST) {
                if (data != null) {
                    Uri picUri = data.getData();
                    try {
                        performCrop(picUri);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, getResources().getString(R.string.toast3_pic_canceled), Toast.LENGTH_SHORT).show();
                    goToMain();
                }
            } else if (requestCode == PIC_CROP) {
                if (data != null) {
                    if (checkGaleryPermissions()) {
                        // get the cropped bitmap
                        selectedBitmap = BitmapFactory.decodeFile(absolutePicCropPath);
                        pictureImageView.setImageBitmap(selectedBitmap);
                    }
                } //delete the temp camera file
                deleteCameraTempFile();
                //camera request, get the file provider uri and send to crop intent.
            } else if (requestCode == CAMERA_REQUEST) {
                File cameraFile = new File(cameraPicPath);
                try {
                    picContentUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", cameraFile);
                    cameraRequest = true;
                    performCrop(picContentUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Once picture is shown and user is pressing to process it. opens the process activity with the file path.
     * @param view
     */
    public void onClickGoOutput(View view){
        galleryPermission = false;
        goToOutputProcess();
    }

    /**
     * going to output process will check permissions first (the special boolean in gallery permissions)
     */
    private void goToOutputProcess(){
        if(checkGaleryPermissions()) {
            //make pref of the chosen language
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString(PROCESS_LIST, sp.getSelectedItem().toString());
            editor.commit();

            //send intent to the output
            Intent sendToOutput = new Intent(this, OutputActivity.class);
            sendToOutput.putExtra(CROPPED_PICTURE_PATH, absolutePicCropPath);
            sendToOutput.putExtra(INTENT_WITH_PIC, extraForOutput);
            startActivity(sendToOutput);
            finish();
        }
    }

    /**
     * delete the temp file that is no longer needed.
     */
    public void deleteCameraTempFile(){
        if (deleteTempFile) {
            try {
                File tempCamPic = new File(cameraPicPath);
                tempCamPic.delete();
                deleteTempFile = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * delete the cropped file if processing is canceled
     */
    public void deleteCroppedFile(){
        try {
            File croppedFile = new File(absolutePicCropPath);
            croppedFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * main idea is to create a temp file for the camera to hold while we processing it.
     * @return - the file path of the picture to be.
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "Camera_Photo_Temp";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),"CamTemp");
        storageDir.mkdirs();
        File image = File.createTempFile(imageFileName,".jpg",storageDir);
                                            /* prefix, suffix, directory*/
        // save path for the view
        cameraPicPath = image.getAbsolutePath();
        deleteTempFile = true;
        return image;
    }

    /**
     * check if we have gallery permissions. if not request for them.
     * @return - true if we got permission.
     */
    public boolean checkGaleryPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, GALLERY_PERMISSION);
            return false;
        }
    }

    /**
     * check if we got camera permission. if not request for them.
     * @return - true if we got permission.
     */
    private boolean checkCameraPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE , Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            return false;
        }
    }

    /**
     * call the gallery request intent with checkup of permissions.
     */
    public void askForGaleryPicture(){
        if(checkGaleryPermissions()) {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, GALLERY_REQUEST);
        }
    }

    /**
     * call the camera request intent with checkup of permissions.
     * including making a temporary file to hold the camera photo.
     */
    public void askForCameraPicture() {
        if(checkCameraPermissions()){
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //check if camera activity is available to handle the intent
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File picFile = null;
                try {
                    picFile = createImageFile();
                    // Continue only if the File was successfully created
                    if (picFile != null) {
                        //~~!!~~ version below API 24
                        //picFileUri = Uri.fromFile(picFile);
                        //~~!!~~ version API 24 and above
                        picContentUri = FileProvider.getUriForFile(this,BuildConfig.APPLICATION_ID + ".provider", picFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, picContentUri);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
            }
            else{
                Toast.makeText(this, getResources().getString(R.string.toast4_no_cam_activity), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * we often call the main activity - thus a function.
     */
    public void goToMain(){
        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return button
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * crop the image
     * @param picUri - the uri of the pic
     */
    private void performCrop(Uri picUri) {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            // indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            // set crop properties here
            cropIntent.putExtra("crop", true);
            cropIntent.putExtra("scale", true);
            // retrieve data on return
            cropIntent.putExtra("return-data", true);

            //only camera need write/read permissions
            if(cameraRequest) {
                cropIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                cameraRequest = false;
            }
            File pic2text = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"Pic2Text");
            if(!pic2text.exists()){
                pic2text.mkdirs();
            }

            //if high version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                timeStamp = LocalDateTime.now().toString();
            }
            else{ //if low version
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd_HH:mm:ss", Locale.ENGLISH);
                timeStamp = sdf.format(new Date());
            }
            File f;

            if(newRequest) {
                f = new File(pic2text, "/" + timeStamp + ".jpg");
                absolutePicCropPath = f.getAbsolutePath();
                extraForOutput = NEW_FILE_PROCESS;
            }
            else{
                f = new File (absolutePicCropPath);
                extraForOutput = CONTINUOUS_FILE_PATH;
            }
            
            //uri to put the crop output to.
            Uri uri = Uri.fromFile(f);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

            // start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, PIC_CROP);
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException anfe) {
            // display an error message
            Toast.makeText(this, getResources().getString(R.string.toast5_no_crop), Toast.LENGTH_SHORT).show();
        }
    }
}
