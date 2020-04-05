package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PERMISSIONS = 12345;
    private static final int PERMISSION_COUNT = 1;

    private boolean arePermissionsDenied() {
        for(int i = 0; i < PERMISSION_COUNT; i++) {
            if(checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(arePermissionsDenied()) {
            ((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
            recreate();
        }
        else {
            onResume();
        }
    }

    private boolean isMusicPlayerInit;
    private List<String> musicFilesList; // list with file (song) paths

    private void addMusicFilesFrom(String dirPath) {
        final File musicDir = new File(dirPath);
        if(!musicDir.exists()) { // if directory with path dirPath doesn't exist, create it
            musicDir.mkdir();
            return;
        }
        final File[] files = musicDir.listFiles(); // put all files from directory into a list
        for(File file : files) {
            final String path = file.getAbsolutePath();
            if (path.endsWith(".mp3")) { // if the file is a song, add it to musicFilesList
                musicFilesList.add(path);
            }
        }
    }

    private void fillMusicList() {
        musicFilesList.clear();
        addMusicFilesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))); //add songs from Music directory
        addMusicFilesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))); //add songs from Downloads directory
    }

    private MediaPlayer mp;

    private int playMusicFile(String path) {
            mp = new MediaPlayer();

            try {
                mp.setDataSource(path);
                mp.prepare();
                mp.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mp.getDuration();
    }

    private int clickedSong;
    private boolean flag1 = false;
    private int songPosition;
    private volatile boolean isSongPlaying;
    private int mPosition;
    private boolean flag2 = true;
    private boolean notPaused = true;
    private int songDuration;
    private boolean isAutoplayOn = false;

    private TextView songPositionTextView;
    private TextView songDurationTextView;
    private SeekBar seekBar;
    private View playbackControls;
    private Button pauseButton;
    private Button backward10;
    private Button forward10;
    private Button prevSong;
    private Button nextSong;
    private Button autoplay;

    private void playSong() {
        final String musicFilePath = musicFilesList.get(mPosition);
        songDuration = playMusicFile(musicFilePath) / 1000; // playMusicFile returns song duration in miliseconds, divided by 1000, it is converted to seconds
        seekBar.setMax(songDuration);
        seekBar.setVisibility(View.VISIBLE);
        playbackControls.setVisibility(View.VISIBLE);
        songDurationTextView.setText(String.valueOf(songDuration / 60) + ":" + String.valueOf(songDuration % 60));
        songPosition = 0; // the second from which the song starts, if the songPosition was 3, the song would start from 3rd second
        pauseButton.setText("pause");
        isSongPlaying = true;

        if(flag2) {
            new Thread() { //Thread koji updatea position na seekBaru
                public void run() { // thread is used so song isn't interrupted when seekBar is updated
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (isSongPlaying) {
                            if(songPosition < songDuration) {
                                songPosition++; // after 1 second, and if the song isn't yet finished, increment songPosition for 1 second
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                seekBar.setProgress(songPosition); // updates the seekBar
                                songPositionTextView.setText(String.valueOf(songPosition / 60) + ":" + String.valueOf(songPosition % 60)); // updates the songPosition text
                            }
                        });
                        if(songPosition == songDuration) {
                            isSongPlaying = false;
                            notPaused = true; // song is over, but not paused
                            if (isAutoplayOn) {
                                if(mPosition < musicFilesList.size() - 1) {
                                    mPosition++;
                                }
                                else {
                                    mPosition = 0;
                                }
                                clickedSong++;

                                final String musicFilePath = musicFilesList.get(mPosition);
                                songDuration = playMusicFile(musicFilePath) / 1000;
                                seekBar.setMax(songDuration);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        songDurationTextView.setText(String.valueOf(songDuration / 60) + ":" + String.valueOf(songDuration % 60));
                                        pauseButton.setText("pause");
                                    }
                                });
                                songPosition = 0;
                                isSongPlaying = true;
                            }
                        }
                    }
                }
            }.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (arePermissionsDenied())) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

        if (!isMusicPlayerInit) {
            final ListView listView = findViewById(R.id.listView);
            final TextAdapter textAdapter = new TextAdapter();
            musicFilesList = new ArrayList<>();
            fillMusicList();
            textAdapter.setData(musicFilesList);
            listView.setAdapter(textAdapter);
            seekBar = findViewById(R.id.seekBar);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int songProgress;

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    songProgress = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    songPosition = songProgress; // save songProgress globaly so it can be used in another method
                    mp.seekTo(songProgress * 1000); //
                    if(isSongPlaying) {
                        pauseButton.setText("pause");
                    }
                    else {
                        pauseButton.setText("play");
                    }
                    if(songPosition == songDuration)
                    {
                        isSongPlaying = false;
                        pauseButton.setText("");
                        //notPaused = true;
                    }
                    else {
                        if(pauseButton.getText().equals("pause")) {
                            mp.start();
                        }
                    }
                }
            });

            songPositionTextView = findViewById(R.id.currentPosition);
            songDurationTextView = findViewById(R.id.songDuration);

            pauseButton = findViewById(R.id.pauseButton);

            playbackControls = findViewById(R.id.playBackButtons);

            backward10 = findViewById(R.id.b10);

            backward10.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(songPosition - 10 <= 0)
                    {
                        songPosition = 0;
                    }
                    else {
                        songPosition -= 10;
                    }
                    mp.seekTo(songPosition * 1000);
                }
            });

            forward10 = findViewById(R.id.f10);

            forward10.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(songPosition + 10 >= songDuration)
                    {
                        songPosition = songDuration;
                    }
                    else {
                        songPosition += 10;
                    }
                    mp.seekTo(songPosition * 1000);
                }
            });

            prevSong = findViewById(R.id.prevSong);

            prevSong.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mPosition == 0)
                    {
                        mPosition = musicFilesList.size() - 1;
                        clickedSong = musicFilesList.size() - 1;
                    }
                    else {
                        mPosition--;
                        clickedSong--;
                    }

                    if (isSongPlaying) {
                        mp.pause();
                    }
                    playSong();
                }
            });

            nextSong = findViewById(R.id.nextSong);

            nextSong.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mPosition == (musicFilesList.size() - 1))
                    {
                        mPosition = 0;
                        clickedSong = 0;
                    }
                    else {
                        mPosition++;
                        clickedSong++;
                    }

                    if (isSongPlaying) {
                        mp.pause();
                    }
                    playSong();
                }
            });

            autoplay = findViewById(R.id.autoplay);

            autoplay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isAutoplayOn = !isAutoplayOn;
                }
            });


            pauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isSongPlaying) {
                        mp.pause();
                        pauseButton.setText("play");
                        notPaused = false;
                        isSongPlaying = !isSongPlaying;
                    }
                    else if (songPosition < songDuration) { // if song isn't playing and if it isn't over (which means it is paused)
                        mp.start();
                        pauseButton.setText("pause");
                        notPaused = true;
                        isSongPlaying = !isSongPlaying;
                    }
                }
            });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) { //position is the position of the song that the user clicked
                    if(!flag1) { // flag1 is initially false, this if statement will only be executed once
                        mPosition = position;
                        playSong();
                        clickedSong = position; // clickedSong will represent last clicked song
                        flag1 = true; // flat that is used to this if statement is executed only once, to establish clickedSong
                        flag2 = false; // flag that is used so there is only one thread created (instead of a thread for each song individually, increment problem solved)
                    }
                    else {
                        if (!isSongPlaying && notPaused) { // if the song is over
                            clickedSong = position;
                            mPosition = position;
                            playSong();
                        } else {
                            playbackControls.setVisibility(View.VISIBLE);
                            if ((position != clickedSong) && isSongPlaying) { // if the user clicked a different song and if the song is playing
                                clickedSong = position;
                                mPosition = position;
                                mp.pause();
                                playSong();
                            }
                            else if ((position != clickedSong) && !isSongPlaying) // if the user clicked a different song and if the song is not playing
                            {
                                clickedSong = position;
                                mPosition = position;
                                playSong();
                            }
                        }
                    }
                }
            });
            isMusicPlayerInit = true;
        }
    }

        class TextAdapter extends BaseAdapter {
            private List<String> data = new ArrayList<>();

            void setData(List<String> mData) {
                data.clear();
                data.addAll(mData);
                notifyDataSetChanged();
            }

            @Override
            public int getCount() {
                return data.size();
            }

            @Override
            public String getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
                    convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.myItem)));
                }

                ViewHolder holder = (ViewHolder) convertView.getTag();
                final String item = data.get(position);
                holder.info.setText(item.substring(item.lastIndexOf('/')+1)); // '/' is used to point at filename, +1 is used so forward slash is not printed
                return convertView;
            }

            class ViewHolder {
                TextView info;

                ViewHolder(TextView mInfo) { //konstruktor
                    info = mInfo;
                }
            }
        }
}
