package com.example.dmitro.mediaplayer;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.math.BigDecimal;

public class ListOfSongs extends ListActivity {

    private static final int UPDATE_FREQUENCY = 500;
    private static final int STEP_VALUE = 4000;

    private MediaCursorAdapter mediaAdapter;
    private TextView selelctedFile;
    private SeekBar seekbar;
    private MediaPlayer mediaPlayer;
    private ImageButton playButton;
    private ImageButton prevButton;
    private ImageButton nextButton;
    private ImageButton stopButton;

    private boolean isStarted = true;
    private String currentFile;
    private boolean isMoveingSeekBar = false;

    private final Handler handler = new Handler();

    private final Runnable updatePositionRunnable = new Runnable() {
        public void run() {
            updatePosition();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_songs);

        selelctedFile = (TextView) findViewById(R.id.selected_file);
        seekbar = (SeekBar) findViewById(R.id.seek_bar);
        playButton = (ImageButton) findViewById(R.id.play);
        prevButton = (ImageButton) findViewById(R.id.previous);
        nextButton = (ImageButton) findViewById(R.id.next);
        stopButton = (ImageButton) findViewById(R.id.stop);

        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnCompletionListener(onCompletion);
        mediaPlayer.setOnErrorListener(onError);
        seekbar.setOnSeekBarChangeListener(seekBarChanged);

        //берем все аудио файлы в системе
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

        if (null != cursor) {
            cursor.moveToFirst();

            mediaAdapter = new MediaCursorAdapter(this, R.layout.list_of_item, cursor);

            setListAdapter(mediaAdapter);

            playButton.setOnClickListener(onButtonClick);
            nextButton.setOnClickListener(onButtonClick);
            prevButton.setOnClickListener(onButtonClick);
            stopButton.setOnClickListener(onButtonClick);
        }

    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        currentFile = (String) view.getTag();

        startPlay(currentFile);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacks(updatePositionRunnable);
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.release();

        mediaPlayer = null;
    }

    private void startPlay(String file) {
        Log.i("Selected: ", file);

        selelctedFile.setText(file);
        seekbar.setProgress(0);

        mediaPlayer.stop();
        mediaPlayer.reset();

        try {
            mediaPlayer.setDataSource(file);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        seekbar.setMax(mediaPlayer.getDuration());
        playButton.setImageResource(R.drawable.ic_pause_black_24dp);

        updatePosition();

        isStarted = true;
    }

    private void stopPlay() {
        mediaPlayer.stop();
        mediaPlayer.reset();
        playButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
        handler.removeCallbacks(updatePositionRunnable);
        seekbar.setProgress(0);

        isStarted = false;
    }

    private void updatePosition() {
        handler.removeCallbacks(updatePositionRunnable);

        seekbar.setProgress(mediaPlayer.getCurrentPosition());

        handler.postDelayed(updatePositionRunnable, UPDATE_FREQUENCY);
    }

//    public class MediaCursorAdapter extends RecyclerView.Adapter{
//
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            return null;
//        }
//
//        @Override
//        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
//
//        }
//
//        @Override
//        public int getItemCount() {
//            return 0;
//        }
//    }
    private class MediaCursorAdapter extends SimpleCursorAdapter {

        public MediaCursorAdapter(Context context, int layout, Cursor c) {
            super(context, layout, c,
                    new String[]{MediaStore.Audio.AudioColumns.ARTIST,
                            MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE,
                            MediaStore.Audio.AudioColumns.DURATION},
                    new int[]{R.id.title, R.id.album, R.id.artist, R.id.duration}
            );
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView album = (TextView) view.findViewById(R.id.album);
            TextView artist = (TextView) view.findViewById(R.id.artist);
            TextView duration = (TextView) view.findViewById(R.id.duration);

            artist.setText(cursor.getString(
                    cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)));

            album.setText(cursor.getString(
                    cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)));

            title.setText(cursor.getString(
                    cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)));

            long durationInMs = Long.parseLong(cursor.getString(
                    cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));

            double durationInMin = ((double) durationInMs / 1000.0) / 60.0;

            durationInMin = new BigDecimal(Double.toString(durationInMin))
                    .setScale(2, BigDecimal.ROUND_UP).doubleValue();

            duration.setText("" + durationInMin);

            view.setTag(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.list_of_item, parent, false);

            bindView(v, context, cursor);

            return v;
        }
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.play: {
                    if (mediaPlayer.isPlaying()) {
                        handler.removeCallbacks(updatePositionRunnable);
                        mediaPlayer.pause();
                        playButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    } else {
                        if (isStarted) {
                            mediaPlayer.start();
                            playButton.setImageResource(R.drawable.ic_pause_black_24dp);

                            updatePosition();
                        } else {
                            startPlay(currentFile);
                        }
                    }

                    break;
                }
                case R.id.stop:{
                    mediaPlayer.stop();
                    playButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    mediaPlayer.seekTo(0);
                    isMoveingSeekBar = false;
                    break;
                }
                case R.id.next: {
                    int seekto = mediaPlayer.getCurrentPosition() + STEP_VALUE;

                    if (seekto > mediaPlayer.getDuration())
                        seekto = mediaPlayer.getDuration();

                    mediaPlayer.pause();
                    mediaPlayer.seekTo(seekto);
                    mediaPlayer.start();

                    break;
                }
                case R.id.previous: {
                    int seekto = mediaPlayer.getCurrentPosition() - STEP_VALUE;

                    if (seekto < 0)
                        seekto = 0;

                    mediaPlayer.pause();
                    mediaPlayer.seekTo(seekto);
                    mediaPlayer.start();

                    break;
                }
            }
        }
    };

    private MediaPlayer.OnCompletionListener onCompletion = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            stopPlay();
        }
    };

    private MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {

            return false;
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChanged = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isMoveingSeekBar = false;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isMoveingSeekBar = true;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (isMoveingSeekBar) {
                mediaPlayer.seekTo(progress);

                Log.i("OnSeekBarChangeListener", "onProgressChanged");
            }
        }
    };
}
