package com.example.nguyenhuutinh.myapplication;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;
    private static final int REQUEST_CODE_SIGN_IN = 0;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final String TAG = "fullscreen";
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;
    private Bitmap mBitmapToSave;
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.thousandhands.com/api/v1/")
                .addConverterFactory(new ToStringConverterFactory())
                .addConverterFactory(GsonConverterFactory.create())

                .client(client)
                .build();

        ApiService service = retrofit.create(ApiService.class);
        service.getBlogs("VN", "100").enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {

                List<Post> posts = autoParse(response, new TypeReference<List<Post>>(){

                });

                String output = "";
                output +="<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
                output += "<rss version=\"2.0\"\n" +
                        "\txmlns:excerpt=\"http://wordpress.org/export/1.2/excerpt/\"\n" +
                        "\txmlns:content=\"http://purl.org/rss/1.0/modules/content/\"\n" +
                        "\txmlns:wfw=\"http://wellformedweb.org/CommentAPI/\"\n" +
                        "\txmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                        "\txmlns:wp=\"http://wordpress.org/export/1.2/\"\n" +
                        ">";
                output +="<channel>\n" +
                        "\t<wp:wxr_version>1.2</wp:wxr_version>\n" +
                        "\t<wp:author></wp:author>";
                output += "<wp:category><wp:term_id>1</wp:term_id><wp:category_nicename>tin-tuc-thousand-hands</wp:category_nicename><wp:category_parent></wp:category_parent><wp:cat_name><![CDATA[Tin tức Thousand Hands]]></wp:cat_name></wp:category>\n";
                output += "<wp:category><wp:term_id>2</wp:term_id><wp:category_nicename>am-thuc-gia-dinh</wp:category_nicename><wp:category_parent></wp:category_parent><wp:cat_name><![CDATA[Ẩm thực gia đình]]></wp:cat_name></wp:category>\n";
                output += "<wp:category><wp:term_id>3</wp:term_id><wp:category_nicename>nha-dep-cuoc-song</wp:category_nicename><wp:category_parent></wp:category_parent><wp:cat_name><![CDATA[Nhà đẹp & cuộc sống]]></wp:cat_name></wp:category>\n";
                output += "<wp:category><wp:term_id>4</wp:term_id><wp:category_nicename>meo-lam-dep</wp:category_nicename><wp:category_parent></wp:category_parent><wp:cat_name><![CDATA[Mẹo làm đẹp]]></wp:cat_name></wp:category>\n";




                if(posts != null && posts.size() > 0){

                    int attach_id = 1000;


                    for(int i = 0; i < posts.size(); i++){
                       String url = posts.get(i).image_url;
                       if(posts.get(i).content.contains("https://d3e2huyg92vyxs.cloudfront.net/gallery") && posts.get(i).content.contains("https://d3e2huyg92vyxs.cloudfront.net/gallery")){
                           url = posts.get(i).content.substring(posts.get(i).content.indexOf("https://d3e2huyg92vyxs.cloudfront.net/gallery"), (posts.get(i).content.indexOf(".png") + 4));
                           Log.d(TAG, "url1: " + url);
                       }else{
                           url = url.substring(0, url.indexOf("?"));
                           Log.d(TAG, "url2: " + url);
                       }


                        output +="<item>\n";
                        output +="<title>"+ posts.get(i).title+"</title>\n";
                        output +="<link>"+ posts.get(i).title+"</link>\n";
                        output +="<pubDate>"+converttime(posts.get(i).created_at)+"</pubDate>\n";
                        output +="<dc:creator><![CDATA["+ posts.get(i).admin.full_name+"]]></dc:creator>\n";
                        output +="<description></description>\n";
                        output +="<guid isPermaLink=\"false\">"+url+"</guid>\n";
                        output +="<content:encoded><![CDATA[]]></content:encoded>\n";
                        output +="<excerpt:encoded><![CDATA[]]></excerpt:encoded>\n";
                        output +="<wp:post_id>"+ (attach_id + i) +"</wp:post_id>\n";
                        output +="<wp:post_date>"+ converttime(posts.get(i).created_at) +"</wp:post_date>\n";
                        output +="<wp:post_name>"+ posts.get(i).title+"</wp:post_name>\n";
                        output +="<wp:status>inherit</wp:status>\n";
                        output +="<wp:post_type>attachment</wp:post_type>\n";
                        output +="<wp:attachment_url>"+url+"</wp:attachment_url>\n";
                        output +="</item>\n";

                        output +="<item>\n";
                        output +="<title>"+ posts.get(i).title+"</title>\n";
                        output += "<wp:post_name>"+posts.get(i).friendly_url+"</wp:post_name>\n";
                        output +="<wp:post_date>"+converttime(posts.get(i).created_at)+"</wp:post_date>\n";
                        output +="<description></description>\n";
                        output +="<wp:status>publish</wp:status>\n";
                        output +="<wp:post_type>post</wp:post_type>\n";
                        output +="<category domain=\"category\" nicename=\""+ posts.get(i).blog_category.friendly_url+ "\">" +
                                "<![CDATA["+posts.get(i).blog_category.name+"]]></category>\n";
                        output +="<link>"+ "https://www.thousandhands.com/vn/blogs2/"+ posts.get(i).friendly_url+ "</link>\n";
                        output +="<content:encoded><![CDATA["+ posts.get(i).content + "]]></content:encoded>\n";
                        output +="<excerpt:encoded><![CDATA[]]></excerpt:encoded>\n";
                        output +="<wp:postmeta>\n";
                        output +="<wp:meta_key>_thumbnail_id</wp:meta_key>\n";

                        output +="<wp:meta_value><![CDATA["+(attach_id + i) +"]]></wp:meta_value>\n";
                        output +="</wp:postmeta>\n";


                        output +="</item>\n";
                    }

                }
                output +="</channel>\n" +
                        "</rss>\n";
                writeToFile(output, getApplicationContext());
                Log.d(TAG, "output: " + output);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d(TAG, "error " + t.getMessage() );
            }
        });
//        signIn();
    }

    private void writeToFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("xml.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public String converttime( String time ){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = dateFormat.parse(time);

            return dateFormat2.format(date);
        } catch (ParseException e) {
        }
        return "";


    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    @JsonCreator
    public static <T> T autoParse(Response<String> response, TypeReference<T> typeRef) {
        T o = null;

        o = autoParse(response.body(), typeRef);
        if (o == null) {
            try {
                if (response.errorBody() != null) {
                    o = autoParse(response.errorBody().string(), typeRef);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return (T) o;
    }

    @JsonCreator
    public static <T> T autoParse(Object response, TypeReference<T> typeRef) {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        T o = null;
        try {
            o = mapper.readValue(response.toString(), typeRef);
        } catch (Exception e2) {
            Log.d(TAG, "autoParse: " + e2.getMessage());
            try {
                o = mapper.convertValue(response, typeRef);
            } catch (Exception e) {
                Log.d(TAG, "autoParse2: " + e.getMessage());
                try {
                    o = mapper.readValue(((ResponseBody)response).string(), typeRef);
                } catch (Exception e1) {
                    Log.d(TAG, "autoParse3: " + e1.getMessage());
                }
            }
        }

        return (T) o;
    }

    /** Start sign in activity. */
    private void signIn() {
        Log.i(TAG, "Start sign in");
        GoogleSignInClient GoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(GoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /** Build a Google SignIn client. */
    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    /** Create a new file and save it to Drive. */
    private void saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = mBitmapToSave;

        mDriveResourceClient
                .createContents()
                .continueWithTask(new Continuation<DriveContents, Task<Void>>() {

                    @Override
                    public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                        return createFileIntentSender(task.getResult(), image);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Failed to create new contents.", e);
                    }
                });
    }
    /**
     * CreateFileActivityOptions} for user to create a new photo in Drive.
     */
    private Task<Void> createFileIntentSender(DriveContents driveContents, Bitmap image) {
        Log.i(TAG, "New contents created.");
        // Get an output stream for the contents.
        OutputStream outputStream = driveContents.getOutputStream();
        // Write the bitmap data from it.
        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
        try {
            outputStream.write(bitmapStream.toByteArray());
        } catch (IOException e) {
            Log.w(TAG, "Unable to write file contents.", e);
        }

        // Create the initial metadata - MIME type and title.
        // Note that the user will be able to change the title later.
        MetadataChangeSet metadataChangeSet =
                new MetadataChangeSet.Builder()
                        .setMimeType("image/jpeg")
                        .setTitle("Android Photo.png")
                        .build();
        // Set up options to configure and display the create file activity.
        CreateFileActivityOptions createFileActivityOptions =
                new CreateFileActivityOptions.Builder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialDriveContents(driveContents)
                        .build();

        return mDriveClient
                .newCreateFileActivityIntentSender(createFileActivityOptions)
                .continueWith(new Continuation<IntentSender, Void>() {
                                  @Override
                                  public Void then(@NonNull Task<IntentSender> task) throws Exception {
                                      startIntentSenderForResult(task.getResult(), REQUEST_CODE_CREATOR, null, 0, 0, 0);
                                      return null;
                                  }
                              }
                );
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                Log.i(TAG, "Sign in request code");
                // Called after user is signed in.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Signed in successfully.");
                    // Use the last signed in account here since it already have a Drive scope.
                    mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this));
                    // Build a drive resource client.
                    mDriveResourceClient =
                            Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this));
                    // Start camera.
                    startActivityForResult(
                            new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_CODE_CAPTURE_IMAGE);
                }
                break;
            case REQUEST_CODE_CAPTURE_IMAGE:
                Log.i(TAG, "capture image request code");
                // Called after a photo has been taken.
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Image captured successfully.");
                    // Store the image data as a bitmap for writing later.
                    mBitmapToSave = (Bitmap) data.getExtras().get("data");
                    saveFileToDrive();
                }
                break;
            case REQUEST_CODE_CREATOR:
                Log.i(TAG, "creator request code");
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Image successfully saved.");
                    mBitmapToSave = null;
                    // Just start the camera again for another photo.
                    startActivityForResult(
                            new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_CODE_CAPTURE_IMAGE);
                }
                break;
        }
    }

}
