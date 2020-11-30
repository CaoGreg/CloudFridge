package com.cloudfridge;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class DialogAdapter extends RecyclerView.Adapter<DialogAdapter.DialogViewHolder> {

    private LayoutInflater inflater;
    private String[] myImageNameList;
    private ArrayList<FoodModel> selectedItems;
    private static final String TAG = "Dialog";


    public DialogAdapter(Context context, String[] myImageNameList){
        inflater = LayoutInflater.from(context);
        this.myImageNameList = myImageNameList;
        this.selectedItems = new ArrayList<>();
    }

    public ArrayList<FoodModel> getSelectedItems() {
        return this.selectedItems;
    }

    @Override
    public DialogAdapter.DialogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recycler_item, parent, false);
        DialogViewHolder holder = new DialogViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(DialogAdapter.DialogViewHolder holder, int position) {
        holder.name.setText(myImageNameList[position]);
    }

    @Override
    public int getItemCount() {
        return myImageNameList.length;
    }

    class DialogViewHolder extends RecyclerView.ViewHolder{

        TextView name;
        EditText expiryEditText;
        CheckBox checkBox;

        public DialogViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.itemName);
            expiryEditText = itemView.findViewById(R.id.editTextExpiry);
            checkBox = itemView.findViewById(R.id.checkbox);

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                     @Override
                     public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            if(!expiryEditText.getText().toString().equals("")) {
                                if (isValidDate(expiryEditText.getText().toString())) {
                                    selectedItems.add(new FoodModel(name.getText().toString(), expiryEditText.getText().toString()));
                                    Log.d(TAG, "Adding " + name.getText() + ", " + expiryEditText.getText());
                                }
                                else
                                {
                                    checkBox.setChecked(false);
                                    Toast.makeText(itemView.getContext(), "Please enter a valid expiry date for " + name.getText(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            else {
                                checkBox.setChecked(false);
                                Toast.makeText(itemView.getContext(), "Please enter expiry date for " + name.getText(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            for(FoodModel food: selectedItems) {
                                if (food.getName().equals(name.getText().toString()) && food.getExpiryDate().equals(expiryEditText.getText().toString())) {
                                    selectedItems.remove(food);
                                    Log.d(TAG, "Removing " + food.getName() + ", " + food.getExpiryDate());
                                }
                            }
                            // put logic here for when item was in list to remove
                        }
                     }
                 }

            );
        }

        public boolean isValidDate(String inDate) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
            dateFormat.setLenient(false);
            try {
                dateFormat.parse(inDate.trim());
            } catch (ParseException pe) {
                return false;
            }
            if (isAfterToday(dateFormat.getCalendar()))
                return true;
            return false;
        }

        public boolean isAfterToday(Calendar date)
        {
            Calendar today = Calendar.getInstance();
            if (today.getTimeInMillis() - date.getTimeInMillis() < 8.64e+7)
                return true;
            else if (date.before(today))
                return false;
            return true;
        }
    }
}