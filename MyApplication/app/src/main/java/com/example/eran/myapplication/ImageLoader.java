package com.example.eran.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.util.Pair;
import android.util.SparseBooleanArray;

import java.io.File;
import java.util.Vector;

//static class as a middleware between app parts that have difficulty of sharing items
public final class ImageLoader {

    //vector to hold tuple of bitmap and the path of the bitmap - fast loading and deleting
    private static Vector<Pair<Bitmap, String>> allPairs = new Vector<>();
    //special array of booleans with key to sign which checkboxes are selected
    private static SparseBooleanArray itemsState = new SparseBooleanArray();
    private static boolean loaded = false;
    private static boolean longPressed = false;

    /**
     * load one time all the compressed bitmaps into the static variable,
     * and load them every time we need to go back to main activity - save many IO accesses
     */
    public static void loadAllBitmaps(){
        if(!loaded){
            allPairs.clear();
            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/Pic2Text");
            if(folder.exists()) {
                for (File file : folder.listFiles()) {
                    if (!file.getAbsoluteFile().toString().endsWith(".txt")) {
                        String fullPath = file.getAbsolutePath();
                        allPairs.add(Pair.create(decodeSampledBitmapFromUri(fullPath, 220, 220), fullPath));
                    }
                }
            }
            loaded = true;
        }
    }

    /**
     * function that decodes a bitmap out of a file URI into specified size
     * @param path - the path to the file
     * @param reqWidth - width of bitmap
     * @param reqHeight - height of bitmap
     * @return - the bitmap compressed from the path.
     */
    private static Bitmap decodeSampledBitmapFromUri(String path, int reqWidth, int reqHeight) {

        Bitmap bm = null;
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        //if bitmaps still take really long to load, can reduce their quality with un-commenting the next comment
        /*
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;
        */

        return BitmapFactory.decodeFile(path, options);
    }

    /**
     * calculate the max compressed size possible in squares of 2.
     * @param options - the options of the bitmap
     * @param reqWidth - width
     * @param reqHeight - height
     * @return - the int of max compression
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * add new item to the tuple array
     * @param fullPath - path of the image to be added.
     */
    public static synchronized void addNewItem(String fullPath){
        allPairs.add(Pair.create(decodeSampledBitmapFromUri(fullPath, 75, 75), fullPath));
    }

    /**
     * remove existing item in tuple array at index
     * @param index - the index of the item to be removed.
     */
    public static synchronized void removeExistingItemAt(int index){
        allPairs.removeElementAt(index);
    }

    /**
     * find the index of the item inside the vector using full path (used in distanced activity)
     * @param fullpath - the path of the file.
     * @return - the index of the file in the array
     */
    public static int findInVector(String fullpath){
        for(int i = 0; i < getItemsCount(); i++){
            if(getFullPathAt(i).equals(fullpath)){
                return i;
            }
        }
        return -1;
    }

    /**
     * get the path (2nd tuple item) at index
     * @param index - the index of the path
     * @return - the path needed
     */
    public static String getFullPathAt(int index){
        return allPairs.get(index).second;
    }

    /**
     * get the bitmap (1st tuple item) at index
     * @param index - the index of the bitmap
     * @return - the bitmap needed
     */
    public static Bitmap getBitmapAt(int index){
        if (index < getItemsCount()) {
            return allPairs.get(index).first;
        }
        else{
            return null;
        }
    }

    /**
     * get the size of the tuple array
     * @return - the size in int units
     */
    public static int getItemsCount(){
        return allPairs.size();
    }

    /**
     * special variable that signs the long press - meaning selection mode is on
     * @return
     */
    public static boolean getLongPressed(){
        return longPressed;
    }

    /**
     * set that variable to on (selection mode on)
     */
    public static synchronized void setLongPressedOn(){
        longPressed = true;
    }

    /**
     * set that variable to off (selection mode off)
     */
    public static synchronized void setLongPressedOff(){
        longPressed = false;
    }

    /**
     * set items inside the special boolean array with keys (for checkbox uses)
     * @param index - the index to be set
     * @param state - the value put in that index
     */
    public static synchronized void setSelectedAt(int index, boolean state){
        itemsState.put(index, state);
    }

    /**
     * get the item boolean mode at index of the boolean array
     * @param index - the index of the item
     * @return - the boolean value (checked or not checked)
     */
    public static boolean getSelectedAt(int index){
        return itemsState.get(index, false);
    }

    /**
     * selection mode is over, clean all the boolean array.(if doesn't exist default value will be false in other methods)
     */
    public static void clearItemsArray(){
        itemsState.clear();
    }

    /**
     * delete all the selected values by running over all the boolean array, and deleting everything that is checked (true)
     */
    public static void deleteSelected(){
        try {
            int length = getItemsCount();
            for (int i = length - 1; i > -1; i--) {
                if (getSelectedAt(i)) {
                    String fileName = getFullPathAt(i);
                    //remove the image file at the path
                    File imageFile = new File(fileName);
                    if (imageFile.delete()) {
                        removeExistingItemAt(i); //remove it from the bitmap loading as it has been deleted.
                    }
                    //also remove the text file of the connected image file.
                    File textFile = new File(fileName.substring(0, fileName.lastIndexOf(".")) + ".txt");
                    textFile.delete();
                }
            }
            clearItemsArray();
            setLongPressedOff();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
