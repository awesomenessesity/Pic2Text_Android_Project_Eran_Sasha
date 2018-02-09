package com.example.eran.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import android.os.AsyncTask;
import android.widget.Toast;

//main activity with all files and pictures
public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {

    //constants
    private final String INTENT_WITH_PIC = "Picture";
    private final String NEW_CAMERA_INTENT = "NewCamera";
    private final String NEW_GALLERY_INTENT = "NewGallery";
    private final int PERMISSION_REQUEST = 5545;

    MyRecyclerViewAdapter adapter;
    ProgressDialog progressDialog;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //action bar
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //nevigation bar
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //only if we got access to files we can load pictues
        if(checkPermissions()) {
            initializeRecyclerView();
        }
    }

    /**
     * initialize the picture on main activity in asyntask
     */
    public void initializeRecyclerView(){
        //set the dialog process.
        progressDialog = new ProgressDialog(this);
        //make async task to load files so the UI wont be stuck if it takes long
        ProcessBitmapsInTheBackground doBackground = new ProcessBitmapsInTheBackground();
        doBackground.execute((Void) null);
    }

    /**
     *     check read/write permissions
     */
    public boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                //if the long press is on the we deleting files.
                if(ImageLoader.getLongPressed()){
                    deleteFiles();
                }
                else{ //if not its the initialize permission
                    initializeRecyclerView();
                }
            } else {
                Toast.makeText(this, getResources().getString(R.string.toast6_no_all_perms), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * on physical back press what will happen?
     */
    @Override
    public void onBackPressed() {
        //close the drawer if its opened
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }//if not check if long pressed, the we cancel selection mode
        else if(ImageLoader.getLongPressed()){
                ImageLoader.clearItemsArray();
                ImageLoader.setLongPressedOff();
                invalidateScree();
        }
        else{ // if none of the above, we quit the app.
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // Tag between the icons on different modes
        getMenuInflater().inflate(R.menu.main, menu);
        if(!ImageLoader.getLongPressed()) {
            MenuItem item = menu.findItem(R.id.cancel_selection);
            item.setVisible(false);
        }
        else{
            MenuItem item = menu.findItem(R.id.action_tackePic);
            item.setVisible(false);
            item = menu.findItem(R.id.action_importFile);
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //action bar buttons - names speak for themselves
        if (id == R.id.action_importFile) {
            Intent galleryIntent = new Intent(this, ImportActivity.class);
            galleryIntent.putExtra(INTENT_WITH_PIC, NEW_GALLERY_INTENT);
            startActivity(galleryIntent);
            return true;
        }
        if (id == R.id.action_tackePic) {
            Intent cameraIntent = new Intent(this, ImportActivity.class);
            cameraIntent.putExtra(INTENT_WITH_PIC, NEW_CAMERA_INTENT);
            startActivity(cameraIntent);
            return true;
        }
        if (id == R.id.action_delete) {
            if(checkPermissions()) {
                deleteFiles();
            }
            return true;
        }
        if(id == R.id.cancel_selection){
            ImageLoader.clearItemsArray();
            ImageLoader.setLongPressedOff();
            invalidateScree();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * delete the selected files.
     */
    public void deleteFiles() {
        DeleteFilesInBackground deleteFiles = new DeleteFilesInBackground();
        deleteFiles.execute((Void) null);
    }

    /**
     * update the action bar and the recycler view accordingly to deletion.
     */
    public void invalidateScree(){
        invalidateOptionsMenu();
        adapter.notifyDataSetChanged();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // navigation item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_settings) {
            Intent settingIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingIntent);
        } else if (id == R.id.nav_tips) {
            Intent tipsIntent = new Intent(this, TipsActivity.class);
            startActivity(tipsIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * asynctask to process the bitmaps in the background - not to get the UI stuck
     */
    private class ProcessBitmapsInTheBackground extends AsyncTask <Void,Void,Void> {
        @Override
        protected void onPreExecute() {
            //before loading the heavy process. let the user know whats up.
            progressDialog.setMessage("Processing Image... Please wait");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // set up the RecyclerView
            recyclerView = (RecyclerView) findViewById(R.id.rvNumbers);
            int numberOfColumns = 3;
            recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this, numberOfColumns));
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            //do the heavy load in the background
            ImageLoader.loadAllBitmaps();
            return null;
        }

        @Override
        protected void onPostExecute(Void o)
        {
            //after finishing the process - show on the UI
            adapter = new MyRecyclerViewAdapter(MainActivity.this);
            recyclerView.setAdapter(adapter);
            progressDialog.cancel();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            // Doing nothing
        }
    }

    /**
     * async task to delete the files - so the UI wont be stuck
     */
    private class DeleteFilesInBackground extends AsyncTask <Void,Void,Void>{
        @Override
        protected void onPreExecute() {
            //before loading the heavy process. let the user know whats up.
            progressDialog.setMessage("Deleting files...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        @Override
        protected Void doInBackground(Void... voids) {
            //do the heavy process here
            ImageLoader.deleteSelected();
            return null;
        }

        @Override
        protected void onPostExecute(Void o)
        {
            //after finishing the process - show on the UI
            progressDialog.cancel();
            invalidateScree();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            // Doing nothing
        }
    }
}
