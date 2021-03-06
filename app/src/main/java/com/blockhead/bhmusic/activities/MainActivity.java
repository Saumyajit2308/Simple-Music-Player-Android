package com.blockhead.bhmusic.activities;


import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.blockhead.bhmusic.R;
import com.blockhead.bhmusic.adapters.AlbumAdapter;
import com.blockhead.bhmusic.adapters.ArtistAdapter;
import com.blockhead.bhmusic.adapters.PlaylistListAdapter;
import com.blockhead.bhmusic.adapters.RAlbumAdapter;
import com.blockhead.bhmusic.adapters.SongAdapter;
import com.blockhead.bhmusic.objects.Album;
import com.blockhead.bhmusic.objects.Artist;
import com.blockhead.bhmusic.objects.Playlist;
import com.blockhead.bhmusic.objects.Song;
import com.blockhead.bhmusic.utils.DiskLruImageCache;
import com.blockhead.bhmusic.utils.IndexableListView;
import com.blockhead.bhmusic.utils.OnSwipeTouchListener;
import com.blockhead.bhmusic.utils.XMLParser;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("unchecked")
public class MainActivity extends AppCompatActivity implements MediaPlayerControl {

    public static ArrayList<Artist> artistList;
    public static ArrayList<Album> albumList;
    public static HashMap<Long, Album> albumHashMap;
    public static TextView nowPlayingArtist, nowPlayingTitle;
    public static ImageView coverArt;
    public static ImageButton shuffleButton, repeatButton;
    private static FloatingActionButton fab;
    public static RelativeLayout fauxAB;
    public static android.support.v4.view.PagerTitleStrip pagerTitleStrip;
    public static Album currAlbum;
    public static Artist currArtist;
    public static Playlist currPlaylist;
    public static android.support.v7.app.ActionBar mActionBar;
    public static boolean artworkHeader = true;
    public static int primaryColor, accentColor;
    private static RecyclerView albumView;
    private static MusicService musicSrv = new MusicService();
    private static boolean playbackPaused = false;
    private static SeekBar seekBar;
    private static IndexableListView mListView;
    private static SongAdapter songAdt;
    private static RAlbumAdapter albumAdt;
    private static ArtistAdapter artistAdt;
    public static PlaylistListAdapter playlistAdt;
    private static String abTitle;
    private static SharedPreferences sharedPref;
    public static ArrayList<Song> songList;
    public static ArrayList<Playlist> playlistList;
    private Intent playIntent;
    private boolean musicBound = false, paused = false, loadInBackground = false;
    public static boolean improveColorSampling = true;
    private TextView timePos, timeDur;
    private ServiceConnection musicConnection;
    public static RotateAnimation repeatRotationAnimation, shuffleAnimation;
    private Drawable playDrawable, pauseDrawable;
    public static CoordinatorLayout coordLay;
    public static boolean isShowing;
    public static Context mContext;
    private MaterialDialog md;

    //DEFINE COLORS FOR USERS TO CHOOSE
    public final static int MATERIAL_RED = 0,MATERIAL_PINK=1,MATERIAL_PURPLE=2,MATERIAL_DEEPPURPLE=3,
    MATERIAL_INDIGO=4,MATERIAL_BLUE=5,MATERIAL_LIGHTBLUE=6,MATERIAL_CYAN=7,MATERIAL_TEAL=8,
    MATERIAL_GREEN=9,MATERIAL_LIGHTGREEN=10,MATERIAL_NEONGREEN=11,MATERIAL_LIME=12,MATERIAL_YELLOW=13,MATERIAL_AMBER=14,
    MATERIAL_ORANGE=15,MATERIAL_DEEPORANGE=16,MATERIAL_GREY=17,MATERIAL_BLUEGREY=18;

    //Define playlist options
    final int RENAME_PLAYLIST = 0, DELETE_PLAYLIST = 1;

    /* Instantiate Handler in Leak Preventative manner */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                activity.mediaPlayerMonitor();
            }
        }
    }
    private final MyHandler monitorHandler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {    //TODO: Add now playing indicator to song list adapters
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        mActionBar = getSupportActionBar();
        mContext = MainActivity.this;

        setEnterSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                Log.d("ANIM", "ENTER | Names: " + names.toString() + "\n shareElements: " + sharedElements.toString());
                super.onMapSharedElements(names, sharedElements);
            }
        });
        setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                Log.d("ANIM", "EXIT | Names: " + names.toString() + "\n shareElements: " + sharedElements.toString());
                super.onMapSharedElements(names, sharedElements);
            }
        });

        // Create global configuration and initialize ImageLoader with this config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
        .build();
        ImageLoader.getInstance().init(config);

        //Read Preferences
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        artworkHeader = sharedPref.getBoolean("artwork_header", true);
        abTitle = sharedPref.getString("main_title", "MUSIC");
        loadInBackground = sharedPref.getBoolean("always_load_artist_in_background", false);
        improveColorSampling = sharedPref.getBoolean("improve_color_sampling", true);

        int primaryColorKey, accentColorKey;
        try {
            primaryColorKey = Integer.parseInt(sharedPref.getString("primary_color_key", "4"));
            accentColorKey = Integer.parseInt(sharedPref.getString("accent_color_key", "1"));
        }
        catch(Exception e )
        {
            Log.d("BHCA", "CRASH: " + e.getMessage());
            primaryColorKey = 4;
            accentColorKey = 1;
        }

        //Toast.makeText(getApplicationContext(), "Primary:" + primaryColorKey + " Accent: " + accentColorKey, Toast.LENGTH_LONG).show();
        //Set Colors
        primaryColor = getResources().getColor(getColor(primaryColorKey));
        accentColor = getResources().getColor(getColor(accentColorKey));

        //Set ActionBar Title
        if (mActionBar != null)
            mActionBar.setTitle(abTitle);


        md = new MaterialDialog.Builder(MainActivity.this)
                .content("Preparing Library...")
                .widgetColor(MainActivity.accentColor)
                .progress(true, 0)
                .show();
        md.setCanceledOnTouchOutside(false);


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        songList = new ArrayList<>();
        albumList = new ArrayList<>();
        artistList = new ArrayList<>();
        playlistList = new ArrayList<>();

        albumHashMap = new HashMap<>();


        songAdt = new SongAdapter(this, songList);
        albumAdt = new RAlbumAdapter(this, albumList);
        artistAdt = new ArtistAdapter(this, artistList);
        playlistAdt = new PlaylistListAdapter(this, playlistList);

        GetListsTask mGetListTask = new GetListsTask();
        mGetListTask.execute();


        /////////////////
        fauxAB = (RelativeLayout) findViewById(R.id.fauxAB);
        fauxAB.setBackgroundColor(primaryColor);
        fauxAB.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeBottom() {
                nowPlayingButtonPressed(null);
            }

            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
        ////////////////



        //connect to the service
        musicConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                //get service
                musicSrv = binder.getService();
                //pass list
                musicSrv.setList(songList);
                musicBound = true;

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                musicBound = false;
            }
        };

        //Define Views
        shuffleButton = (ImageButton) findViewById(R.id.shuffleButton);
        repeatButton = (ImageButton) findViewById(R.id.repeatButton);
        pagerTitleStrip = (android.support.v4.view.PagerTitleStrip) findViewById(R.id.pager_title_strip);
        seekBar = (SeekBar) findViewById(R.id.progressBar);
        fab = (FloatingActionButton) findViewById(R.id.floating_action_button);
        nowPlayingTitle = (TextView) findViewById(R.id.trackTitle);
        nowPlayingArtist = (TextView) findViewById(R.id.trackArtist);
        coverArt = (ImageView) findViewById(R.id.coverArt);
        coordLay = (CoordinatorLayout)findViewById(R.id.main_coordinator);
        ImageButton backButton = (ImageButton)findViewById(R.id.backButton);
        ImageButton fowardButton = (ImageButton)findViewById(R.id.fowardButton);


        /* Set OnClickListeners */
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prevPressed();
            }
        });
        fowardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextPressed();
            }
        });


        //Set FAB onClickListener
        fab.setClickable(true);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabPressed();
            }
        });
        fab.setLongClickable(true);
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                nowPlayingButtonPressed(null);
                return true;
            }
        });

        //Set Colors
        fab.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        pagerTitleStrip.setBackgroundColor(primaryColor);

        //Set Animations
        repeatRotationAnimation = new RotateAnimation(180, 360,
             Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
         repeatRotationAnimation.setDuration(500);

        shuffleAnimation = new RotateAnimation(0, 360,
             Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
         shuffleAnimation.setDuration(700);

        //Define Drawables
        pauseDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_pause_white_36dp);
        playDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_play_white_36dp);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;


            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                if (fromUser)
                    progress = progressValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekTo(progress);
            }
        });

        ScheduledExecutorService myScheduledExecutorService = Executors.newScheduledThreadPool(1);
        timePos = (TextView) findViewById(R.id.currTime);
        timeDur = (TextView) findViewById(R.id.totalTime);

        myScheduledExecutorService.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        monitorHandler.sendMessage(monitorHandler.obtainMessage());
                    }
                },
                0, //initialDelay
                200, //delay
                TimeUnit.MILLISECONDS);

    }

    private static void playNext() {
        musicSrv.playNext();
        if (playbackPaused) {
            playbackPaused = false;
        }
    }

    private static void playPrev() {
        musicSrv.playPrev();
        if (playbackPaused) {
            playbackPaused = false;
        }
    }

    public static String prettyTime(int n) {
        String mins, secs;
        n = n / 1000;
        mins = (n / 60) + "";
        secs = (n % 60) + "";
        if (n % 60 < 10)
            secs = "0" + secs;

        return (mins + ":" + secs);
    }

    public static MusicService getMusicService() {
        return musicSrv;
    }

    public static int randomColor() {
        Random rand = new Random();
        int color = rand.nextInt(18);
        return getColor(color);
    }

    public static int getColor(int color)
    {
        switch(color) {
            case MATERIAL_RED:
                return (R.color.material_red);
            case MATERIAL_PINK:
                return (R.color.material_pink);
            case MATERIAL_PURPLE:
                return (R.color.material_purple);
            case MATERIAL_DEEPPURPLE:
                return (R.color.material_deeppurple);
            case MATERIAL_INDIGO:
                return (R.color.material_indigo);
            case MATERIAL_BLUE:
                return (R.color.material_blue);
            case MATERIAL_LIGHTBLUE:
                return (R.color.material_lightblue);
            case MATERIAL_CYAN:
                return (R.color.material_cyan);
            case MATERIAL_TEAL:
                return (R.color.material_teal);
            case MATERIAL_GREEN:
                return (R.color.material_green);
            case MATERIAL_LIGHTGREEN:
                return (R.color.material_lightgreen);
            case MATERIAL_LIME:
                return (R.color.material_lime);
            case MATERIAL_YELLOW:
                return (R.color.material_yellow);
            case MATERIAL_AMBER:
                return (R.color.material_amber);
            case MATERIAL_ORANGE:
                return (R.color.material_orange);
            case MATERIAL_DEEPORANGE:
                return (R.color.material_deeporange);
            case MATERIAL_GREY:
                return (R.color.material_grey);
            case MATERIAL_BLUEGREY:
                return (R.color.material_bluegrey);
            case MATERIAL_NEONGREEN:
                return (R.color.material_neongreen);
            default:
                return (R.color.material_indigo);

        }
    }

    public static void fabPressed() {
        if(musicSrv == null){
            musicSrv = new MusicService();
            musicSrv.initMusicPlayer();
        }

            if (playbackPaused) {
                musicSrv.resumePlayer();
                //seekBar.setMax(musicSrv.getDur());
                playbackPaused = false;
            } else {
                musicSrv.pausePlayer();
                playbackPaused = true;
            }
    }

    public static void prevPressed() {
        playPrev();
    }

    public static void nextPressed() {
        playNext();
    }

    public static void updatePrefs() {
        artworkHeader = sharedPref.getBoolean("artwork_header", true);
        abTitle = sharedPref.getString("main_title", "MUSIC");

        //Set ActionBar Title
        if (mActionBar != null)
            mActionBar.setTitle(abTitle);
        if(currAlbum != null) {
            if ((currAlbum.getCoverURI() != null) && !artworkHeader){
                fauxAB.setBackgroundColor(primaryColor);
                pagerTitleStrip.setBackgroundColor(primaryColor);
            }
        }

    }

    public static void updateColors(Resources res)
    {

        primaryColor = res.getColor(getColor(Integer.parseInt(sharedPref.getString("primary_color_key", "4"))));
        accentColor = res.getColor(getColor(Integer.parseInt(sharedPref.getString("accent_color_key", "1"))));

        fab.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        if(currAlbum != null ) {
            if (currAlbum.getCoverURI() == null) {
                fauxAB.setBackgroundColor(primaryColor);
                pagerTitleStrip.setBackgroundColor(primaryColor);
            }
        } else {
            fauxAB.setBackgroundColor(primaryColor);
            pagerTitleStrip.setBackgroundColor(primaryColor);
        }
    }

    private void mediaPlayerMonitor() {

        //set FAB icon
        if (musicSrv != null)
        {
            if(musicSrv.isFinished)
            {
                clearInfo();
                return;
            }
            if (musicSrv.isPng()) {
                if (fab.getDrawable() != pauseDrawable) {
                    fab.setImageDrawable(pauseDrawable);
                }

                //Set Seekbar
                seekBar.setVisibility(View.VISIBLE);

                int mediaDuration = musicSrv.getDur();
                int mediaPosition = musicSrv.getPosn();
                seekBar.setMax(mediaDuration);
                seekBar.setProgress(mediaPosition);
                timePos.setText(prettyTime(mediaPosition));
                timeDur.setText(prettyTime(mediaDuration));

                if (!musicSrv.getSongTitle().equalsIgnoreCase(nowPlayingTitle.getText().toString())) {
                    setNowPlayingInfo();
                }
            } else {
                if (fab.getDrawable() != playDrawable) {
                    fab.setImageDrawable(playDrawable);
                }
            }
        }
        else
            clearInfo();

        //PUT SET NOW PLAYING INFO BACK RIGHT HERE
    }

    public static void updateUI()
    {
        //blur and set cover
        if (musicSrv.getCoverURI() != null && artworkHeader) {
            int cTransparent = mContext.getResources().getColor(R.color.transparent);
            fauxAB.setBackgroundColor(cTransparent);
            pagerTitleStrip.setBackgroundColor(cTransparent);
            coverArt.setImageBitmap(musicSrv.getSuperBlurredCover());
        } else {
            fauxAB.setBackgroundColor(MainActivity.primaryColor);
            pagerTitleStrip.setBackgroundColor(MainActivity.primaryColor);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        isShowing = true;
        updateUI();
        super.onResume();
        if (paused) {
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        isShowing = false;
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {// Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_shuffle_all) {
            shufflePressed(null);
        }
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_about) {
            MaterialDialog md = new MaterialDialog
                    .Builder(this)
                    .customView(R.layout.about_dialog, false)
                    .negativeColor(accentColor)
                    .negativeText("Dismiss")
                    .show();
            return true;
        }
        if (id == R.id.action_now_playing) {
            nowPlayingButtonPressed(null);
            return true;
        }
        if( id == R.id.action_search){
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        for(Playlist temp : playlistList)
        {
            if(temp.isNew())
                writePlaylistToStore(temp);
            else if(temp.isChanged())
                updatePlaylistInStore(temp);
        }


        stopService(playIntent);
        if(musicSrv != null) {
            musicSrv.removeNotification(getApplicationContext());
            musicSrv = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    public void getSongList() {
        //retreive song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ALBUM_ID};
        Cursor musicCursor = musicResolver.query(musicUri, proj, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst())
        {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int trackNumberColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
            int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int isMusicColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
            int sizeColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
            int dataColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int mimeColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);
            int albumIdColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM_ID);

            //add songs to list
            do
            {
                int isMusic = musicCursor.getInt(isMusicColumn);

                if(isMusic != 0) {  //Add only music files TODO: Make this optional
                    long thisId = musicCursor.getLong(idColumn);
                    String thisTitle = musicCursor.getString(titleColumn);
                    String thisArtist = musicCursor.getString(artistColumn);
                    int thisTrack = musicCursor.getInt(trackNumberColumn);
                    int duration = musicCursor.getInt(durationColumn);
                    String thisDuration = prettyTime(duration);
                    String thisPath = musicCursor.getString(dataColumn);
                    int thisSize = musicCursor.getInt(sizeColumn);
                    long thisAlbumId = musicCursor.getLong(albumIdColumn);

                    String thisExtension = musicCursor.getString(mimeColumn);
                    thisExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(thisExtension);


                    Song temp = new Song(thisId, thisTitle, thisArtist, thisTrack,
                            thisDuration, thisPath, thisSize, thisExtension, thisAlbumId);
                    songList.add(temp);
                }
            }
            while (musicCursor.moveToNext());
        }
        if(musicCursor != null)
            musicCursor.close();
    }

    public void getAlbumList() {
        ContentResolver musicResolver = getContentResolver();
        Uri artUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String[] proj = {MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums._ID };
        Cursor coverCursor = musicResolver.query(artUri, proj, null, null, null);
        Album temp;


        if (coverCursor != null && coverCursor.moveToFirst())
        {
            int coverColumn = coverCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
            int albumColumn = coverCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
            int artistColumn = coverCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
            int idColumn = coverCursor.getColumnIndex(MediaStore.Audio.Albums._ID);

            do {
                String thisCover = coverCursor.getString(coverColumn);
                String thisAlbumTitle = coverCursor.getString(albumColumn);
                String thisArtist = coverCursor.getString(artistColumn);
                long thisId = coverCursor.getLong(idColumn);

                temp = new Album(thisAlbumTitle, thisCover, thisArtist, thisId);
                albumList.add(temp);
                if(albumHashMap.put(thisId, temp) != null)
                    Log.d("BHCA-OPTIMIZATION", "COULDNT ADD: " + thisAlbumTitle+thisArtist + " ID: " + thisId);
            }
            while (coverCursor.moveToNext());
        }

        if(coverCursor != null)
            coverCursor.close();

    }

    private void getArtistList() {
        String currArtistName;
        int matchResult;
        for (Album tempAlbum : albumList)
        {
            currArtistName = tempAlbum.getArtist();
            matchResult = artistMatch(currArtistName);
            if (matchResult == -1)
            {   //If its a new artist
                Artist temp = new Artist(currArtistName);
                temp.addDummyAlbum(currArtistName); //Add placeholder to new artists
                temp.addAlbum(tempAlbum);
                tempAlbum.setArtistObj(temp);
                artistList.add(temp);
            }
            else
            {   //If its an existing artist add album to repo
                artistList.get(matchResult).addAlbum(tempAlbum);
            }
        }
    }

    private void sortArtistsAlbums()
    {
        for( Artist a : artistList)
            a.sortAlbums();
    }

    private void getPlaylistList() {

        Playlist temp;
        String[] membersProjection = { MediaStore.Audio.Playlists.Members.AUDIO_ID};
        String[] playlistProjection = { MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME};
        ContentResolver playlistResolver = getContentResolver();
        Uri playlistUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor playlistCursor = playlistResolver.query(playlistUri, playlistProjection, null, null, null);

        /* Get playlists */
        if (playlistCursor != null && playlistCursor.moveToFirst())
        {
            int idColumn = playlistCursor.getColumnIndex(MediaStore.Audio.Playlists._ID);
            int titleColumn = playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME);

            do //Loop through playlists
            {
                long thisId = playlistCursor.getLong(idColumn);
                String thisTitle = playlistCursor.getString(titleColumn);
                temp =  new Playlist(thisTitle, thisId);
                ContentResolver tracksResolver = getContentResolver();
                Uri tracksUri = MediaStore.Audio.Playlists.Members.getContentUri("external", thisId);
                Cursor trackCursor = tracksResolver.query(tracksUri,membersProjection,null,null,null);

                if(trackCursor != null && trackCursor.moveToFirst())
                {
                    int trackIdColumn = trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
                    do //Loop through playlist members
                    {
                        long trackId = trackCursor.getLong(trackIdColumn);
                        temp.addSong(trackId);
                    }
                    while (trackCursor.moveToNext());
                }
                playlistList.add(temp);
                if(trackCursor != null)
                    trackCursor.close();
            }
            while (playlistCursor.moveToNext());
        }

        if(playlistCursor != null)
            playlistCursor.close();
    }

    public void updatePlaylistInStore(Playlist playlist)
    {
        removePlaylistFromStore(playlist);
        writePlaylistToStore(playlist);
    }

    public static void sortPlaylistList()
    {
        Collections.sort(playlistList, new Comparator<Playlist>() {
            @Override
            public int compare(Playlist lhs, Playlist rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });
    }

    private void deletePlaylist(int position)
    {
        final Playlist temp = playlistList.get(position);
        Snackbar.make(coordLay
                , "Deleted: " + temp.getTitle()
                , Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        temp.setChanged();
                        playlistList.add(temp);
                        sortPlaylistList();
                        playlistAdt.notifyDataSetChanged();
                    }
                })
                .setActionTextColor(accentColor)
                .show();
        removePlaylistFromStore(temp);
        playlistList.remove(position);
        playlistAdt.notifyDataSetChanged();
    }

    private void removePlaylistFromStore(Playlist playlist)
    {
        ContentResolver resolver = getContentResolver();
        Uri playlistsUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor c = resolver.query(playlistsUri, new String[]{"*"}, null, null, null);

        Log.d("BHCA-P", "Checking for existing playlist for " + playlist.getTitle());
        long playlistId;
        String plname = null;
        c.moveToFirst();
        do
        {
            playlistId = c.getLong(c.getColumnIndex(MediaStore.Audio.Playlists._ID));
            if (playlistId == playlist.getPlaylistId())
            {
                plname = c.getString(c.getColumnIndex(MediaStore.Audio.Playlists.NAME));
                break;
            }
        }
        while (c.moveToNext());
        c.close();

        if (plname != null)
        {
            Uri deleteUri = ContentUris.withAppendedId(playlistsUri, playlistId);
            Log.d("BHCA-P", "REMOVING Existing Playlist: " + plname);

            // delete the playlist
            resolver.delete(deleteUri, null, null);
        }

        c.close();
    }
    private void writePlaylistToStore(Playlist playlist)
    {
        ContentResolver resolver = getContentResolver();
        Uri playlistsUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor c = resolver.query(playlistsUri, new String[]{"*"}, null, null, null);

        Log.d("BHCA-P", "CREATING PLAYLIST: " + playlist.getTitle());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Audio.Playlists.NAME, playlist.getTitle());
        contentValues.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());
        Uri newPlaylistUri = resolver.insert(playlistsUri, contentValues);
        Log.d("BHCA-P", "Added PlayLIst: " + newPlaylistUri);

        Uri insUri = Uri.withAppendedPath(newPlaylistUri, MediaStore.Audio.Playlists.Members.CONTENT_DIRECTORY);

        Log.d("BHCA-P", "Playlist Members Url: " + insUri);

        if (playlist.getSize() != 0)
        {
            ArrayList<Long> songIds = playlist.getSongIds();
            Log.d("BHCA-P", "Adding Songs to PlayList...");
            int i = 0;
            do {
                long id = songIds.get(i);
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, ++i);
                cv.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, id);
                Uri u = resolver.insert(insUri, cv);
                Log.d("BHCA-P", "Added Playlist Item: " + playlist.getMembers().get(i-1).getTitle() + " with play order:  " + i);
            } while (i < playlist.getSize());
        }

        c.close();
    }

    private int artistMatch(String current) {
        //check for match
        for (int j = 0; j < artistList.size(); j++) {
            if (artistList.get(j).getName().equalsIgnoreCase(current))
                return j;
        }

        return -1;
    }

    public void songPicked(int pos) {
        musicSrv.setSong(pos);
        //seekBar.setMax(musicSrv.getDur());
        musicSrv.playSong();
        if (playbackPaused) {
            playbackPaused = false;
        }
    }

    private boolean isNewPlaylistName(String str)
    {
        for(Playlist pl : playlistList)
        {
            if(pl.getTitle().equalsIgnoreCase(str))
                return false;
        }
        return true;
    }

    private void renamePlaylistPressed(final int pos, final Context context)
    {
        View enterNameView = LayoutInflater
                .from(context)
                .inflate(R.layout.dialog_new_playlist_enter_name, null);

        final EditText editText = (EditText) enterNameView.findViewById(R.id.enter_playlist_name);
        Drawable editTextBg = ContextCompat.getDrawable(getApplicationContext(), R.drawable.edit_text_bg);
        editTextBg.setColorFilter(MainActivity.accentColor, PorterDuff.Mode.SRC_ATOP);
        editText.setBackground(editTextBg);

        String currTitle = playlistList.get(pos).getTitle();
        editText.setText(currTitle);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        editText.selectAll();

        md = new MaterialDialog
                .Builder(context)
                .title("Rename Playlist")
                .titleColor(accentColor)
                .autoDismiss(false)
                .positiveText("Save")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        View cv = dialog.getCustomView();
                        if (cv != null) {
                            String str = ((EditText) cv.findViewById(R.id.enter_playlist_name)).getText().toString();
                            if (str.isEmpty())
                            {
                                Toast.makeText(getApplicationContext()
                                        , "Must enter a name!"
                                        , Toast.LENGTH_SHORT)
                                        .show();
                            }
                            else if (!isNewPlaylistName(str))
                            {
                                Toast.makeText(getApplicationContext()
                                        , "Name is already taken!"
                                        , Toast.LENGTH_SHORT)
                                        .show();
                                editText.selectAll();
                            }
                            else
                            {
                                playlistList.get(pos).setTitle(str);
                                playlistList.get(pos).setChanged();
                                sortPlaylistList();
                                playlistAdt.notifyDataSetChanged();
                                md.dismiss();
                            }
                        }
                    }
                })
                .positiveColor(accentColor)
                .customView(enterNameView, false)
                .show();
    }

    @SuppressWarnings("unused")
    public void openPlaylistOptions(final int position, final Context context)
    {
        /* Callback for when option is chosen */
        MaterialDialog.ListCallback callback = new MaterialDialog.ListCallback()
        {
            @Override
            public void onSelection(MaterialDialog materialDialog, View view, int option, CharSequence charSequence)
            {
                switch (option)
                {
                    case RENAME_PLAYLIST:
                        renamePlaylistPressed(position, context);
                        break;
                    case DELETE_PLAYLIST:
                        String name = playlistList.get(position).getTitle();
                        deletePlaylist(position);
                        break;
                    default:
                        Toast.makeText(context,"Invalid Selection",Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        /* Create song options dialog */
        MaterialDialog dialog = new MaterialDialog
                .Builder(context)
                .title(playlistList.get(position).getTitle())
                .titleColor(accentColor)
                .items(R.array.playlist_options)
                .itemsCallback(callback)
                .negativeText("Cancel")
                .negativeColor(accentColor)
                .show();
    }

    @TargetApi(21)
    public void albumPicked(View view) {
        //musicSrv.setSong(Integer.parseInt(view.getTag().toString()));

        int pos = Integer.parseInt(view.getTag().toString());
        currAlbum = albumList.get(pos);

        Intent intent = new Intent(this, ViewAlbumActivity.class);
        if(isLollipop()) {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this,
                    Pair.create((View) fab, "fab"));

            startActivity(intent, options.toBundle());
        }
        else
        {
            startActivity(intent);
        }
    }

    @TargetApi(21)
    public void artistPicked(View view) {

        int pos = Integer.parseInt(view.getTag().toString());
        currArtist = artistList.get(pos);
        View artistImage = findViewById(R.id.artistImage);

        Intent intent = new Intent(this, ViewArtistActivity.class);
        if(isLollipop())
        {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this,
                    Pair.create((View) fab, "fab")
                    ,Pair.create(artistImage, "artistImage")
            );
            startActivity(intent, options.toBundle());
        }
        else
            startActivity(intent);
    }

    @TargetApi(21)
    public void playlistPicked(View view)
    {
        int pos = Integer.parseInt(view.getTag().toString());
        currPlaylist = playlistList.get(pos);
        Intent intent = new Intent(this, ViewPlaylistActivity.class);

        if(isLollipop())
        {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this,
                    Pair.create((View) fab, "fab")
            );
            startActivity(intent, options.toBundle());
        }
        else
            startActivity(intent);

    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else
            return 0;
    }

    @Override
    public int getDuration() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else
            return 0;
    }

    @Override
    public boolean isPlaying() {
        return (musicSrv != null && musicBound && musicSrv.isPng());
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    public void setNowPlayingInfo() {
        nowPlayingTitle.setText(musicSrv.getSongTitle());
        nowPlayingArtist.setText(musicSrv.getSongArtist());
    }

    public void clearInfo()
    {
        try
        {
            fauxAB.setBackgroundColor(MainActivity.primaryColor);
            pagerTitleStrip.setBackgroundColor(MainActivity.primaryColor);
            nowPlayingTitle.setText("-");
            nowPlayingArtist.setText("-");
            seekBar.setProgress(0);
            timePos.setText("");
            timeDur.setText("");

        }
        catch (NullPointerException e)
        {
            //TODO: Handle exception
        }
    }

    public static boolean isLollipop()
    {
        int currentapiVersion = Build.VERSION.SDK_INT;
        return (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP);
    }

    private Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getArtURL(String url) {
        try {
            XMLParser parser = new XMLParser();
            String xml = parser.getXmlFromUrl(url); // getting XML from URL
            Document doc = parser.getDomElement(xml);
            NodeList nl = doc.getElementsByTagName("image");

            for (int i = 0; i < nl.getLength(); i++) {
                Element e = (Element) nl.item(i);
                //Log.d("BHCA","Size = " + e.getAttribute("size") + " = " + parser.getElementValue(e));
                if (e.getAttribute("size").contentEquals("mega")) {
                    return parser.getElementValue(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("BHCA", e.getMessage());
        }

        return null;
    }

    private String getArtistSummary(String url) {
        try {
            XMLParser parser = new XMLParser();
            String xml = parser.getXmlFromUrl(url); // getting XML from URL
            Document doc = parser.getDomElement(xml);
            NodeList nl = doc.getElementsByTagName("bio");
            nl = nl.item(0).getChildNodes();

            for(int i=0; i < nl.getLength(); i++)
            {
                Node n = nl.item(i);

                if(n.getNodeName().equals("summary")) {
                    Text t = (Text)n.getFirstChild();
                    return t.getWholeText();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("BHCA1", e.getMessage());
        }

        return null;
    }

    public void shufflePressed(View v) {
        musicSrv.setShuffle(coordLay);
        if (musicSrv.shuffle) {
            musicSrv.resumePlayer();
            shuffleButton.startAnimation(shuffleAnimation);
            shuffleButton.setSelected(true);
            if (NowPlayingActivity.shuffleButton != null)
                NowPlayingActivity.shuffleButton.setSelected(true);
        } else {
            shuffleButton.startAnimation(shuffleAnimation);
            shuffleButton.setSelected(false);
            if (NowPlayingActivity.shuffleButton != null)
                NowPlayingActivity.shuffleButton.setSelected(false);
        }
    }

    public void playlistShufflePressed(View view)
    {
        final int pos = Integer.parseInt(view.getTag().toString());
        currPlaylist = playlistList.get(pos);
        musicSrv.setSong(pos);

        musicSrv.playPlaylist(currPlaylist, (new Random().nextInt(currPlaylist.getSize())));

        if (!musicSrv.shuffle)
        {
            musicSrv.shuffle = true;
            musicSrv.resumePlayer();
            shuffleButton.startAnimation(shuffleAnimation);
            shuffleButton.setSelected(true);
            if (NowPlayingActivity.shuffleButton != null)
                NowPlayingActivity.shuffleButton.setSelected(true);
        }

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setTag(pos);
                playlistPicked(v);
            }
        };

        Snackbar
                .make(coordLay, " Now Shuffling: ", Snackbar.LENGTH_LONG)
                .setAction(currPlaylist.getTitle(), listener)
                .setActionTextColor(accentColor)
                .show(); // Don’t forget to show!
    }

    public void repeatPressed(View v) {
        musicSrv.setRepeat(coordLay);
        Context context = getApplicationContext();

        if (musicSrv.getRepeat() == MusicService.REPEAT_ALL) {
            repeatButton.startAnimation(repeatRotationAnimation);
            repeatButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_repeat_white_24dp));
            repeatButton.setSelected(true);
        } else if (musicSrv.getRepeat() == MusicService.REPEAT_ONE) {
            repeatButton.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_repeat_once_white_24dp));
            repeatButton.startAnimation(repeatRotationAnimation);
            repeatButton.setSelected(true);
        } else {
            repeatButton.startAnimation(repeatRotationAnimation);
            repeatButton.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_repeat_white_24dp));
            repeatButton.setSelected(false);
        }
    }

    @TargetApi(21)
    public void nowPlayingButtonPressed(View v) {
        if(musicSrv == null || musicSrv.getCurrSong() == null)
        {
            Toast
                    .makeText(getApplicationContext(), "Please select song first.", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Intent intent = new Intent(this, NowPlayingActivity.class);

        if(isLollipop()) {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this,
                    Pair.create((View) fab, "fab"));
            if (musicSrv.getCoverURI() != null) {
                options = ActivityOptions.makeSceneTransitionAnimation(this,
                        Pair.create((View) coverArt, "coverArt"),
                        Pair.create((View) fab, "fab"));
            }
            startActivity(intent, options.toBundle());
        }
        else
            startActivity(intent);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Bundle args = getArguments();
            int page = args.getInt(ARG_SECTION_NUMBER);
            View rootView;
            if (page == 1) {
                rootView = inflater.inflate(R.layout.song_list, container, false);


                mListView = (IndexableListView) rootView.findViewById(R.id.song_list);

                if (mListView != null) {
                    mListView.setAdapter(songAdt);
                    mListView.setFastScrollEnabled(true);
                    mListView.setFastScrollAlwaysVisible(true);
                }
            }
            else if (page == 2)
            {
                rootView = inflater.inflate(R.layout.album_list, container, false);

                albumView = (RecyclerView) rootView.findViewById(R.id.album_grid);

                if (albumView != null) {
                    albumView.setLayoutManager(new GridLayoutManager(mContext, 2));
                    albumView.setAdapter(albumAdt);
                }
            }
            else if (page == 3)
            {
                rootView = inflater.inflate(R.layout.artist_list, container, false);
                GridView artistView = (GridView) rootView.findViewById(R.id.artistGrid);
                if (artistView != null)
                    artistView.setAdapter(artistAdt);
            }
            else if (page == 4)
            {
                rootView = inflater.inflate(R.layout.playlist_list, container, false);
                ListView playlistView = (ListView) rootView.findViewById(R.id.playlist_listview);
                if (playlistView != null)
                    playlistView.setAdapter(playlistAdt);
            }
            else
                rootView = inflater.inflate(R.layout.song_list, container, false);

            return rootView;
        }
    }

    //Begin AsyncTask Class
    public class ArtistArtTask extends AsyncTask<Void, Integer, String> {

        long t1, t2;
        private SharedPreferences.Editor mEditor = sharedPref.edit();
        private String artistName, fromWhere = "...";


        @Override
        protected void onPreExecute()
        {
            md.dismiss();
            if(!loadInBackground)
            {
                md = md.getBuilder()
                        .title("Preparing Artist Images")
                        .content("Checking cache...")
                        .widgetColor(MainActivity.accentColor)
                        .negativeText("Do In Background")
                        .negativeColor(MainActivity.accentColor)
                        .progress(false, MainActivity.artistList.size())
                        .show();
            }
        }

        @Override
        protected String doInBackground(Void... artists) {

            t1 = System.currentTimeMillis();
            String artistArtUrl = "", encodedArtistName = "", key, sumKey, artistSummary = "No Info Available.";
            String BaseURL = "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&api_key=89b0d2bf4200f9b85e3741e5c07b807d&artist=";
            Bitmap artistImage;
            ImageLoader imageLoader = ImageLoader.getInstance();
            DisplayImageOptions options = new DisplayImageOptions
                    .Builder()
                    .cacheOnDisk(true)
                    .showImageOnFail(R.drawable.default_artist_xlarge)
                    .build();

            int i = 0;
            for (Artist tempArtist : artistList)
            {
                artistName = tempArtist.getName();
                key = artistName.toLowerCase();
                key = key.replaceAll("[^a-z0-9_-]+", "");
                sumKey = key + "summary";

                if (sharedPref.contains(key))
                {
                    fromWhere = "cache...";
                    String path = sharedPref.getString(key, "");

                    /* Set Image Path & Summary */
                    tempArtist.setImagePath(path);

                    if(sharedPref.contains(sumKey))
                    {
                        artistSummary = sharedPref.getString(sumKey, artistSummary);
                        tempArtist.setSummary(artistSummary);
                    }

                    Log.d("BHCA-CACHE", "Loaded " + artistName + " from cache.");
                }
                else
                {
                    fromWhere = "online...";
                    try
                    {
                        encodedArtistName = URLEncoder.encode(artistName, "UTF-8");
                    } catch (Exception e) {
                        e.getMessage();
                    }

                    /* Set Summary, Add to Cache, & Assign to Artist */
                    artistSummary = getArtistSummary(BaseURL + encodedArtistName);
                    if(artistSummary != null)
                    {
                        mEditor.putString(sumKey, artistSummary);
                        mEditor.apply();
                    }
                    tempArtist.setSummary(artistSummary);

                    /* Set Image, Add to Cache, & Assign to Artist */
                    artistArtUrl = getArtURL(BaseURL + encodedArtistName);
                    artistImage = imageLoader.loadImageSync(artistArtUrl, options);

                    if(artistImage == null || artistName.contains("<"))
                    {
                        mEditor.putString(key, "");
                        tempArtist.setImagePath(null);
                    }
                    else
                    {
                        File file = DiskCacheUtils.findInCache(artistArtUrl, imageLoader.getDiskCache());
                        String path = file.getAbsolutePath();
                        mEditor.putString(key, path);
                        tempArtist.setImagePath(path);
                    }

                    Log.d("BHCA-CACHE", "Loaded " + artistName + " from online.");

                }
                if(!loadInBackground)
                    publishProgress(++i);
            }
            return artistArtUrl;

        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            md.setProgress(progress[0]);
            md.setContent("Loading " + artistName + " from " + fromWhere);
        }

        @Override
        protected void onPostExecute(String url) {
            t2 = System.currentTimeMillis();
            Log.d("BHCA", "Async Complete: " + (t2 - t1) + " ms");
            if(md.isShowing())
                md.dismiss();
        }

    }

    //Begin AsyncTask Class
    public class GetListsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(Void... artists) {

            getAlbumList();
            getSongList();
            getArtistList();
            getPlaylistList();

            Collections.sort(songList, new Comparator<Song>() {
                @Override
                public int compare(Song a, Song b) {
                    return a.getTitle().compareTo(b.getTitle());
                }
            });
            Collections.sort(albumList, new Comparator<Album>() {
                @Override
                public int compare(Album a, Album b) {
                    return a.getTitle().compareTo(b.getTitle());
                }
            });
            Collections.sort(artistList, new Comparator<Artist>() {
                @Override
                public int compare(Artist a, Artist b) {
                    return a.getName().compareTo(b.getName());
                }
            });
            sortPlaylistList();

            sortArtistsAlbums();

            return "Success";
        }

        @Override
        protected void onPostExecute(String url) {
            //pd.dismiss();
            if(mListView != null)
                mListView.setAdapter(songAdt);
            if (albumView != null)
                albumView.setAdapter(albumAdt);

            //Start Artist Art Async Task
            ArtistArtTask mArtistArtTask = new ArtistArtTask();
            mArtistArtTask.execute();
        }

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
                case 3:
                    return getString(R.string.title_section4).toUpperCase(l);
            }
            return null;
        }
    }


}
