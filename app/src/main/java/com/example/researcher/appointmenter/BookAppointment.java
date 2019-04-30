package com.example.researcher.appointmenter;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Calendar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.sql.Time;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookAppointment extends AppCompatActivity implements DatePickerDialog.OnDateSetListener,TimePickerDialog.OnTimeSetListener , ForceUpdateChecker.OnUpdateNeededListener {
    TextView name;
    TextInputLayout durationLayout;
    static Calendar dateGiven;
    TextView datePicked,timePicked;
    TextInputEditText duration;
    FirebaseAuth mAuth;
    static Time timeGiven;
    static int gYear,gMonth,gDay;
    final FirebaseFirestore db = FirebaseFirestore.getInstance();
    Map<String, Object> appointment = new HashMap<>();

    Button date,time,check;
    String userName="";

    Date selected;

    Date dateAfter;

    Spinner typeOfMeeting;
    TextView isMaamIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_book_appointment);
        ForceUpdateChecker.with(this).onUpdateNeeded(this).check();
        checkFirstRun();
        mAuth=FirebaseAuth.getInstance();
        typeOfMeeting=findViewById(R.id.typeOfMeeting);

        List<String> spinnerArray1 =  new ArrayList<String>();
        spinnerArray1.add("Individual meeting");
        spinnerArray1.add("Group meeting");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerArray1);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

//
        typeOfMeeting.setAdapter(adapter);
//        Button b=findViewById(R.id.count);
//        b.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                db.collection("appointments").get()
//                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                            @Override
//                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//
//                                if (task.isSuccessful()) {
//                                    int y=0;
//                                    for (QueryDocumentSnapshot document : task.getResult()) {
//
//                                       y++;
//                                    }
//                                    Log.d("Count:",""+y);
//
//
//                                } else {
//                                    Toast.makeText(getApplicationContext(),"Do you have internet?",Toast.LENGTH_SHORT).show();
//
//                                }
//                            }
//                        });
//
//
//
//            }
//        });


        userName=getIntent().getStringExtra("username");
        name=findViewById(R.id.name);
        durationLayout=findViewById(R.id.durationLayout);
        duration=findViewById(R.id.duration);
        name.setText("Hey " + getIntent().getStringExtra("name").toUpperCase()+" !!!\nWelcome to our Appointmenter!\nLet's book an appointment,\nAfter all that is the reason you are here, isn't it???");
        datePicked=findViewById(R.id.selectedDate);
        timePicked=findViewById(R.id.selectedTime);
        check=findViewById(R.id.check);
        datePicked.setText("Pick a date!");
        timePicked.setText("Pick a time!");
        date=findViewById(R.id.date);
        time=findViewById(R.id.time);
        isMaamIn=findViewById(R.id.isMaamIn);

        final DocumentReference docRef = db.collection("features").document("isMaamIn");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            private static final String TAG = "BookActivity";

            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);

                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    boolean isIn=Boolean.parseBoolean(snapshot.get("isMaamIn").toString());
                    if(isIn){
                        isMaamIn.setTextColor(Color.GREEN);
                        isMaamIn.setText("Status:\nMa'am is in her cabin!");


                    }
                    else{
                        isMaamIn.setTextColor(Color.RED);
                        isMaamIn.setText("Status:\nMa'am is not in her cabin\n(Her car's presence can give a clue!)");
                    }

                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });






        duration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                durationLayout.setError(null);
            }
        });
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tempDuration=duration.getText().toString();
                if(datePicked.getText().toString().equals("Pick a date!")){
                    Toast.makeText(getApplicationContext(),"Sadly, We don't know the date",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(timePicked.getText().toString().equals("Pick a time!")){
                    Toast.makeText(getApplicationContext(),"Sadly, We don't know the time",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(tempDuration.isEmpty()){
                    tempDuration="0";
                }
                int durationTime=Integer.parseInt(tempDuration);
                if(durationTime<=0){
                    durationLayout.setError("Enter a valid duration!");
                    return;

                }




                appointment.put("typeOfMeeting",typeOfMeeting.getSelectedItem().toString());
                appointment.put("hour",timeGiven.getHours());
                appointment.put("minute",timeGiven.getMinutes());
                appointment.put("day",gDay);
                appointment.put("month",gMonth);
                appointment.put("year",gYear);
                appointment.put("username",userName);
                appointment.put("name",getIntent().getStringExtra("name"));
                appointment.put("endtime","");
                appointment.put("StartedAt",null);
                appointment.put("isEnded",false);
                appointment.put("EndedAt",null);
                appointment.put("duration",durationTime);
                appointment.put("Accepted",false);
                Calendar now=Calendar.getInstance();
                now.set(Calendar.YEAR,gYear);
                now.set(Calendar.MONTH,gMonth);
                now.set(Calendar.DAY_OF_MONTH,gDay);
                now.set(Calendar.MINUTE,timeGiven.getMinutes());
                now.set(Calendar.HOUR_OF_DAY,timeGiven.getHours());
                selected=now.getTime();
                appointment.put("StartTime",new Timestamp(selected));


                now.add(Calendar.MINUTE,durationTime);
                dateAfter=now.getTime();
                appointment.put("EndTime",new Timestamp(dateAfter));

                Log.d("hey",selected.toString()+",,,"+dateAfter.toString()+"********8" +
                        "");
                Date dateobj = new Date();
                if(selected.before(dateobj)){
                    Log.d("Hey",dateobj.toString()+",,,"+selected.toString());
                    Toast.makeText(getApplicationContext(),"Time and tide waits for none!\nBook an appointment after current date and time...",Toast.LENGTH_LONG).show();
                    return;
                }



                db.collection("appointments")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                if (task.isSuccessful()) {

                                    int y=0;
                                    for (QueryDocumentSnapshot document : task.getResult()) {
//                                        Log.d("hey","Something");
//                                        y++;
                                        Date dateCurr;
                                        Date endDate;
//
                                        try {

                                            Timestamp dateStamp = (Timestamp) document.get("StartTime");
                                            dateCurr = dateStamp.toDate();

                                            dateStamp = (Timestamp) document.get("EndTime");
                                            endDate = dateStamp.toDate();
                                        }
                                        catch(Exception e){
                                            Calendar now1=Calendar.getInstance();
                                        now1.set(Calendar.YEAR,Integer.parseInt(document.get("year").toString()));
                                        now1.set(Calendar.MONTH,Integer.parseInt(document.get("month").toString()));
                                        now1.set(Calendar.DAY_OF_MONTH,Integer.parseInt(document.get("day").toString()));
                                        now1.set(Calendar.MINUTE,Integer.parseInt(document.get("minute").toString()));
                                        now1.set(Calendar.HOUR_OF_DAY,Integer.parseInt(document.get("hour").toString()));
                                        dateCurr=now1.getTime();
                                        now1.add(Calendar.MINUTE,Integer.parseInt(document.get("duration").toString()));
                                        endDate=now1.getTime();
                                        }






                                        Log.d("hey","oyyy\n"+selected.toString()+"\n"+dateCurr.toString()+"\n"+dateAfter.toString()+"\n"+endDate.toString());
                                        if((dateCurr.before(selected) && selected.before(endDate)) || (dateCurr.before(dateAfter) && dateAfter.before(endDate) ) || (selected.before(dateCurr) && dateAfter.after(endDate) ) ){
                                            Toast.makeText(getApplicationContext(),"Slot is already booked! Pick an another slot which is after "+endDate.getHours()+":"+endDate.getMinutes(),Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                    }
                                    Log.d("Count in check:",""+y);
                                    db.collection("appointments")
                                            .add(appointment)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    Log.d("hey","in adding");
                                                    Toast.makeText(getApplicationContext(),"Success",Toast.LENGTH_SHORT).show();
                                                    timePicked.setText("Pick a time!");
                                                    datePicked.setText("Pick a date!");
                                                    duration.setText("");

                                                }

                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {

                                                    Toast.makeText(getApplicationContext(),"Failure:"+e.toString(),Toast.LENGTH_SHORT).show();
                                                }
                                            });


                                } else {
                                    Toast.makeText(getApplicationContext(),"Do you have internet?",Toast.LENGTH_SHORT).show();

                                }
                            }
                        });












            }

        });
        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment datePicker = new datePickerFragment();
                datePicker.show(getSupportFragmentManager(),"date picker");
            }
        });
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment timePicker = new TimePickerFragment();
                timePicker.show(getSupportFragmentManager(),"Time Picker");
            }
        });
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        dateGiven= Calendar.getInstance();
        dateGiven.set(Calendar.YEAR,year);
        gDay=dayOfMonth;
        gYear=year;
        gMonth=month;
        dateGiven.set(Calendar.MONTH,month);
        dateGiven.set(Calendar.DAY_OF_MONTH,dayOfMonth);
        String currentdatepicked = DateFormat.getDateInstance(DateFormat.FULL).format(dateGiven.getTime());
        datePicked.setText("Selected date:\n"+currentdatepicked);


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.logout){
            Intent in=new Intent(this,EntryActivity.class);
            mAuth.signOut();
            Toast.makeText(this,"See you soon...\n(When you want to meet ma'am :p)",Toast.LENGTH_SHORT).show();
            startActivity(in);

        }
        else if(item.getItemId()==R.id.appointments){
            Intent in=new Intent(this,BookedAppointments.class);
            in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            in.putExtra("username",userName);
            in.putExtra("name",getIntent().getStringExtra("name"));
            startActivity(in);


        }
        else if(item.getItemId()==R.id.request){
            Intent in=new Intent(this,request_feature.class);
            in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            in.putExtra("username",userName);
            in.putExtra("name",getIntent().getStringExtra("name"));
            startActivity(in);


        }
        else if(item.getItemId()==R.id.editProfile){
            Toast.makeText(getApplicationContext(),"In development!",Toast.LENGTH_SHORT).show();
            return true;

//            Intent in=new Intent(this,EditProfile.class);
//            in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            in.putExtra("username",userName);
//            in.putExtra("name",getIntent().getStringExtra("name"));
//            startActivity(in);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflate = getMenuInflater();
        inflate.inflate(R.menu.logout,menu);
        return true;
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        timeGiven = new Time(hourOfDay,minute,0);
        Format formatter;
        formatter = new SimpleDateFormat("h:mm a");
        timePicked.setText("Selected time:\n"+formatter.format(timeGiven));
    }

    @Override
    public void onUpdateNeeded(final String updateUrl) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Logged in, but...")
                .setMessage("Please, update app to new version to continue using our app.")
                .setPositiveButton("Get me that!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                redirectStore(updateUrl);
                            }
                        }).setNegativeButton("I better quit this app!",
                        new DialogInterface.OnClickListener() {
                            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finishAffinity();
                            }
                        }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void redirectStore(String updateUrl) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    public void checkFirstRun() {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("ver1_5_3", true);
        if (isFirstRun) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("What's new!")
                    .setMessage("1. Demand feature option added to menu\n\n2. Status of appointments is added")
                    .setPositiveButton("That's good!",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getApplicationContext(),"Thanks! :)",Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton("Do I need it?", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(),"But we need it :)",Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    })

                    .create()


                    ;

            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();

            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("ver1_5_3", false)
                    .apply();
        }
    }


}
