package com.example.eran.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

//recycler view adapter class to do my own things in each view
public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    //constants
    private final String EXISTING_FILE_PATH = "filepath";
    private final String EXISTING_FILE = "ExistingFile";
    private final String INTENT_WITH_PIC = "Picture";

    private LayoutInflater mInflater;

    // Data is passed into the constructor
    public MyRecyclerViewAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
    }

    // Inflates the cell layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    // Binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //load the image
        Bitmap smallPic = getItem(position);
        holder.myImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.myImageView.setImageBitmap(smallPic);
        //set checkbox on and/or selected
        holder.myCheckBox.setVisibility(ImageLoader.getLongPressed() ? View.VISIBLE : View.GONE);
        holder.myCheckBox.setChecked(ImageLoader.getSelectedAt(position));
    }

    @Override
    public int getItemCount() {
        return ImageLoader.getItemsCount();
    }

    // Stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder{

        public ImageView myImageView;
        public CheckBox myCheckBox;

        public ViewHolder(final View itemView) {
            super(itemView);
            myImageView = (ImageView) itemView.findViewById(R.id.ivSmallPic);
            myCheckBox = (CheckBox) itemView.findViewById(R.id.checkBox);

            //listeners for clicks- long click
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    //if selection mode - do as normal click
                    if(ImageLoader.getLongPressed()) {
                        onItemClick(view, getAdapterPosition());
                    }
                    else{ //if not in selection mode - turn selection mode on
                        ImageLoader.setLongPressedOn();
                        ImageLoader.setSelectedAt(getAdapterPosition(), true);
                    }
                    //invalidate the views and action bars
                    notifyDataSetChanged();
                    ((MainActivity)itemView.getContext()).invalidateOptionsMenu();
                    return true;
                }
            });

            //call the on item click if normal click was pressed
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClick(view, getAdapterPosition());
                }
            });
        }
    }

    /**
     *    Convenience method for getting data at click position
      */
    public Bitmap getItem(int index) {
        return ImageLoader.getBitmapAt(index);
    }

    /**
     *     Method that executes your code for the normal click
      */
    public void onItemClick(View view, int position) {
        //if selection mode is on - toggle the check boxes.
        if(ImageLoader.getLongPressed()){
            CheckBox myCheckBox = (CheckBox) view.findViewById(R.id.checkBox);
            if (ImageLoader.getSelectedAt(position)) {
                myCheckBox.setChecked(false);
                ImageLoader.setSelectedAt(position, false);
            }
            else  {
                myCheckBox.setChecked(true);
                ImageLoader.setSelectedAt(position, true);
            }
        }
        else{ //if not normal press - go to the processed file
            String imageFullName = ImageLoader.getFullPathAt(position);
            Intent intent = new Intent(view.getContext(), OutputActivity.class);
            intent.putExtra(EXISTING_FILE_PATH ,imageFullName);
            intent.putExtra(INTENT_WITH_PIC, EXISTING_FILE);
            view.getContext().startActivity(intent);
        }
    }

}