/*
 * ******************************************************************************
 *   Copyright (c) 2013 Gabriele Mariotti.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */

package it.gmariotti.cardslib.library.view.component;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore.Images.ImageColumns;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nostra13.universalimageloader.cache.memory.MemoryCacheAware;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.FailReason;

import it.gmariotti.cardslib.library.Constants;
import it.gmariotti.cardslib.library.R;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.util.BitmapUtils;
import it.gmariotti.cardslib.library.view.base.CardViewInterface;

/**
 * Compound View for Thumbnail Component.
 * </p>
 * It is built with base_thumbnail_layout.xml.
 * </p>
 * Please note that this is currently in a preview state.
 * This means that the API is not fixed and you should expect changes between releases.
 * </p>
 * This class load a bitmap resource using {@link android.util.LruCache} and using an
 * AsyncTask to prevent UI blocks.
 *
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public class CardThumbnailView extends FrameLayout implements CardViewInterface, TextureView.SurfaceTextureListener {

    public static final String SCHEME_DRAWABLE = "drawable";

    //--------------------------------------------------------------------------
    // Custom Attrs
    //--------------------------------------------------------------------------

    /**
     * Default Layout for Thumbnail View
     */
    protected int card_thumbnail_layout_resourceID = R.layout.base_thumbnail_layout;

    /** Global View for this Component */
    protected View mInternalOuterView;

    /**
     * CardThumbnail model
     */
    protected CardThumbnail mCardThumbnail;

    /**
     * Memory Cache
     */
    protected LruCache<String, Bitmap> mMemoryCache;

    /**
     * Used to recycle ui elements.
     */
    protected boolean mIsRecycle=false;

    /**
     * Used to replace inner layout elements.
     */
    protected boolean mForceReplaceInnerLayout =false;


    protected boolean mLoadingErrorResource = false;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public CardThumbnailView(Context context) {
        super(context);
        init(null,0);
    }

    public CardThumbnailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs,0);
    }

    public CardThumbnailView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs,defStyle);
    }

    //--------------------------------------------------------------------------
    // View
    //--------------------------------------------------------------------------

    /**
     * ImageView inside CardThumbnail
     */
    protected ImageView mImageView;

    //--------------------------------------------------------------------------
    // Init
    //--------------------------------------------------------------------------


    /**
     * Initialize
     *
     * @param attrs
     * @param defStyle
     */
    protected void init(AttributeSet attrs, int defStyle){
        //Init attrs
        initAttrs(attrs,defStyle);

        //Init View
        if(!isInEditMode())
            initView();
    }
    /**
     * Init custom attrs.
     *
     * @param attrs
     * @param defStyle
     */
    protected void initAttrs(AttributeSet attrs, int defStyle) {

        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.card_options, defStyle, defStyle);

        try {
            card_thumbnail_layout_resourceID= a.getResourceId(R.styleable.card_options_card_thumbnail_layout_resourceID, card_thumbnail_layout_resourceID);
        } finally {
            a.recycle();
        }
    }

    /**
     * Init view
     */
    protected void initView() {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInternalOuterView = inflater.inflate(card_thumbnail_layout_resourceID,this,true);

        //Get ImageVIew
        mImageView= (ImageView) findViewById(R.id.card_thumbnail_image);
        mVideoView = (TextureView) findViewById(R.id.card_thumbnail_video);
        mVideoView.setSurfaceTextureListener(this);
    }

    //--------------------------------------------------------------------------
    // Add Thumbnail
    //--------------------------------------------------------------------------

    /**
     * Adds a {@link CardThumbnail}.
     * It is important to set all thumbnail values before launch this method.
     *
     * @param cardThumbail thumbnail model
     */
    public void addCardThumbnail(CardThumbnail cardThumbail ){
        mCardThumbnail=cardThumbail;
        buildUI();
    }

    /**
     * Refresh UI
     */
    protected void buildUI() {
        if (mCardThumbnail==null) return;

        if (mIsRecycle)
            mLoadingErrorResource=false;

        //Setup InnerView
        setupInnerView();
    }


    /**
     * Sets the inner view.
     *
     */
    protected void setupInnerView(){

        //Setup Elements before load image
        if (mInternalOuterView!=null)
            mCardThumbnail.setupInnerViewElements((ViewGroup)mInternalOuterView,mImageView);

        mVideoView.setAlpha(0f);
        mImageView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                loadBitmap();
                v.removeOnLayoutChangeListener(this);
            }
        });
        mImageView.requestLayout();
        playVideo(); // try to play
        startCamera(); // try to openCamera
    }

    private Camera mCamera;

    public void startCamera() {
        if (!mCardThumbnail.isCameraEnabled()) return;
        if (mSurfaceTexture == null) return;
        if (mCamera == null) {
            mCamera = Camera.open();

            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.startPreview();
                mVideoView.setAlpha(1f);
            } catch (IOException ioe) {
            }
        }
    }

    public void stopCamera() {
        if (mCamera == null) return;
        mCamera.stopPreview();
        mCamera.release();
    }

    private boolean checkCameraHardware() {
        return checkCameraHardware(getContext());
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private void loadBitmap() {
        if (!mCardThumbnail.isExternalUsage()){
            mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if(mCardThumbnail.getDrawableResource()>0)
                loadBitmap(mCardThumbnail.getDrawableResource(), mImageView);
            else if (mCardThumbnail.getUrlResource() != null)
                loadBitmap(mCardThumbnail.getUrlResource(), mImageView);
            else if (mCardThumbnail.getFileResource() != null)
                loadBitmap(new File(mCardThumbnail.getFileResource()), mImageView);
            else
                return;
        }
    }

    //--------------------------------------------------------------------------
    // Load Bitmap and cache manage
    //--------------------------------------------------------------------------

    public void loadBitmap(int resId, ImageView imageView) {
        String uriString = SCHEME_DRAWABLE + "://" + resId;
        loadBitmap(uriString, imageView);
    }

    public void loadBitmap(File file, ImageView imageView) {
        Uri uri = Uri.fromFile(file);
        loadBitmap(uri.toString(), imageView);
    }

    private static LruCache<String, Boolean> sVideoCache;
    public static final int K = 1024;

    private static LruCache<String, Boolean> getVideoCache() {
        if (sVideoCache == null) {
            sVideoCache = new LruCache<String, Boolean>(16 * K);
        }
        return sVideoCache;
    }

    private boolean isVideoUri(String uri) {
        Boolean isVideo = getVideoCache().get(uri);
        if (isVideo == null) {
            String type = getContext().getContentResolver().getType(Uri.parse(uri));
            isVideo = new Boolean(!TextUtils.isEmpty(type) && type.startsWith("video/"));
            getVideoCache().put(uri, isVideo);
        }
        return isVideo;
    }

    public MediaPlayer mMediaPlayer;
    public static List<MediaPlayer> sMediaPlayers = new ArrayList<MediaPlayer>();
    private TextureView mVideoView;
    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Log.d("Log8", "onSurfaceTextureAvailable");
        mSurfaceTexture = surfaceTexture;
        mSurface = new Surface(mSurfaceTexture);
        playVideo();
        startCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        Log.d("Log8", "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Log.d("Log8", "onSurfaceTextureDestroyed");
        stopVideo();
        stopCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    private float mVideoWidth;
    private float mVideoHeight;

    private void calculateVideoSize(Uri uri) {
        MediaMetadataRetriever metaRetriever = null;
        try {
            metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(getContext(), uri);
            String height = metaRetriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String width = metaRetriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            mVideoHeight = Float.parseFloat(height);
            mVideoWidth = Float.parseFloat(width);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (metaRetriever != null) {
                metaRetriever.release();
            }
        }
    }

    private boolean mLandscape = true;

    private void updateTextureViewSize(TextureView view, String uri, int viewWidth, int viewHeight) {
        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (mVideoWidth > viewWidth && mVideoHeight > viewHeight) {
            scaleX = mVideoWidth / viewWidth;
            scaleY = mVideoHeight / viewHeight;
        } else if (mVideoWidth < viewWidth && mVideoHeight < viewHeight) {
            scaleY = viewWidth / mVideoWidth;
            scaleX = viewHeight / mVideoHeight;
        } else if (viewWidth > mVideoWidth) {
            scaleY = (viewWidth / mVideoWidth) / (viewHeight / mVideoHeight);
        } else if (viewHeight > mVideoHeight) {
            scaleX = (viewHeight / mVideoHeight) / (viewWidth / mVideoWidth);
        }

        // CENTER_CROP
        int pivotPointX = viewWidth / 2;
        int pivotPointY = viewHeight / 2;

        Matrix matrix = new Matrix();
        if (mLandscape) {
            matrix.setScale(scaleY, scaleX, pivotPointY, pivotPointX); // landscape
        } else {
            matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY); // portrait
        }

        view.setTransform(matrix);
        view.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }

    private static LruCache<String, Integer> sRotationCache;
    private static LruCache<String, Integer> getRotationCache() {
        if (sRotationCache == null) {
            sRotationCache = new LruCache<String, Integer>(1 * K);
        }
        return sRotationCache;
    }

    private int getVideoRotation(String uri) {
        Integer rotation = getRotationCache().get(uri);

        if (rotation != null) return rotation;

        if (Build.VERSION.SDK_INT >= 17) {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(getContext(), Uri.parse(uri));

            String s = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            rotation = Integer.valueOf(s);
        } else {
            rotation = 90; // TODO
        }

        getRotationCache().put(uri, rotation);
        return rotation;
    }

    private int getRotation(String uri) {
        Integer rotation = getRotationCache().get(uri);

        if (rotation != null) return rotation;

        if (isVideoUri(uri)) return getVideoRotation(uri);

        // image
        boolean flip = false;
        final String[] PROJECTION = {
            ImageColumns.ORIENTATION
        };
        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(Uri.parse(uri), PROJECTION, null, null, null);
            if (cursor.moveToFirst()) {
                int orientation = cursor.getInt(cursor.getColumnIndexOrThrow((ImageColumns.ORIENTATION)));
                rotation = (orientation + 360) % 360;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }

        getRotationCache().put(uri, rotation);
        return rotation;
    }

    public void loadBitmap(String uri, ImageView imageView) {
        mUri = uri;
        if (uri == null) return;
        final String videoPath = mCardThumbnail.getVideoResource();
        if (false && videoPath != null) {
            final String URI_AND_SIZE_SEPARATOR = "_";
            final String WIDTH_AND_HEIGHT_SEPARATOR = "x";
            final ImageSize targetSize = new ImageSize(imageView.getWidth(), imageView.getHeight());
            final String memoryCacheKey = new StringBuilder(uri).append(URI_AND_SIZE_SEPARATOR)
                    .append(targetSize.getWidth()).append(WIDTH_AND_HEIGHT_SEPARATOR)
                    .append(targetSize.getHeight()).toString();

            final MemoryCacheAware<String, Bitmap> memoryCache = ImageLoader.getInstance()
                    .getMemoryCache();

            if (memoryCache.get(memoryCacheKey) == null) {
                ImageLoader.getInstance().displayImage(uri, imageView);

                final Bitmap thumbnail = BitmapUtils.createVideoThumbnail(getContext(), videoPath);
                if (thumbnail != null) {
                    imageView.setImageBitmap(thumbnail);
                    final Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                    memoryCache.remove(memoryCacheKey);
                    memoryCache.put(memoryCacheKey, bitmap);
                }
            } else {
                ImageLoader.getInstance().displayImage(uri, imageView);
            }

            return;
        }

        if (isVideoUri(uri)) Log.d("Log8", "loadBitmap videoUri: " + uri);
        final View videoIndicator = (View) findViewById(R.id.ic_video);
        if (videoIndicator != null) {
            videoIndicator.setVisibility(isVideoUri(uri) ? View.VISIBLE : View.GONE);
        }

        // Disable ProgressBar
        if (true) {
            ImageLoader.getInstance().displayImage(uri, imageView);
            return;
        }

        final View progressBar = (View) findViewById(R.id.progressBar);
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            }
        };
        final Handler handler = new Handler();

        ImageLoader.getInstance().displayImage(uri, imageView, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                // onStableShowAfterFiveMilliseconds()
                handler.postDelayed(r, 1000);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view,
                FailReason failReason) {
                handler.removeCallbacks(r);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                handler.removeCallbacks(r);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view,
                Bitmap loadedImage) {
                handler.removeCallbacks(r);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private String mUri;

    public void pauseVideo() {
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.pause();
            }
        } catch (IllegalArgumentException e) {
        } catch (SecurityException e) {
        } catch (IllegalStateException e) {
        }
    }

    public void resumeVideo() {
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.start();
            }
        } catch (IllegalArgumentException e) {
        } catch (SecurityException e) {
        } catch (IllegalStateException e) {
        }
    }

    public void stopVideo() {
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
            }
        } catch (IllegalArgumentException e) {
        } catch (SecurityException e) {
        } catch (IllegalStateException e) {
        }
        String i = sPlayingPlayers.get(mMediaPlayer);
        Log.d("Log8", "remove: : " + i);
        sPlayingPlayers.remove(mMediaPlayer);
    }

    public boolean playVideo() {
        Log.d("CardThumbnailView:playVideo: ", "mCardThumbnail" + mCardThumbnail);
        if (mCardThumbnail.getUrlResource() != null) {
            mUri = mCardThumbnail.getUrlResource();
            Log.d("Log8", "getUrlResource" + mUri);
            return playVideo(mUri);
        }
        return false;
    }

    private static Map<MediaPlayer, String> sPlayingPlayers = new HashMap<MediaPlayer, String>();

    private boolean isPlaying(String uri) {
        return sPlayingPlayers.containsValue(uri);
    }

    public boolean playVideo(String uri) {
        //if (isPlaying(uri)) return true; // FIXME
        stopVideo();
        if (isVideoUri(uri) && mSurface != null) {
            calculateVideoSize(Uri.parse(uri));
            final String finalUri = uri;

            mMediaPlayer = new MediaPlayer();

            try {
                mMediaPlayer.setDataSource(getContext(), Uri.parse(uri));
                mMediaPlayer.setSurface(mSurface);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.setVolume(0f, 0f); // slient
                mMediaPlayer.setOnVideoSizeChangedListener(new OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        mLandscape = !(width > height);
                        Log.d("Log8", "video: wh: " + mVideoWidth + ", " + mVideoHeight);
                        updateTextureViewSize(mVideoView, finalUri, mImageView.getMeasuredWidth(), mImageView.getMeasuredHeight());
                        Log.d("Log8", "image: wh: " + mImageView.getMeasuredWidth() + ", " +  mImageView.getMeasuredHeight());
                    }
                }
                );
                /*
                   mLandscape = ((getVideoRotation() % 90) == 0);
                   Log.d("Log8", "video: wh: " + mVideoWidth + ", " + mVideoHeight);
                   updateTextureViewSize(mVideoView, uri, mImageView.getMeasuredWidth(), mImageView.getMeasuredHeight());
                   Log.d("Log8", "image: wh: " + mImageView.getMeasuredWidth() + ", " +  mImageView.getMeasuredHeight());
                   */

                // don't forget to call MediaPlayer.prepareAsync() method when you use constructor for
                // creating MediaPlayer
                mMediaPlayer.prepareAsync();

                // Play video when the media source is ready for playback.
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        Log.d("Log8", "onPrepared");
                        mediaPlayer.start();
                        sPlayingPlayers.put(mediaPlayer, finalUri);
                        Log.d("Log8", "play uri: " + finalUri);
                        //AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                        //animation.setDuration(300);
                        //mVideoView.startAnimation(animation);
                        mVideoView.setAlpha(1f);
                    }
                });
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //--------------------------------------------------------------------------
    // Broadcast
    //--------------------------------------------------------------------------

    /**
     * Send a successful broadcast when image is downloaded
     */
    protected void sendBroadcast(){
        sendBroadcast(true);
    }

    /**
     * Send a broadcast when image is downloaded
     *
     * @param result
     */
    protected void sendBroadcast(boolean result) {
        Intent intent = new Intent();
        intent.setAction(Constants.IntentManager.INTENT_ACTION_IMAGE_DOWNLOADED);
        intent.putExtra(Constants.IntentManager.INTENT_ACTION_IMAGE_DOWNLOADED_EXTRA_RESULT, result);
        if (mLoadingErrorResource)
            intent.putExtra(Constants.IntentManager.INTENT_ACTION_IMAGE_DOWNLOADED_EXTRA_ERROR_LOADING, true);
        else
            intent.putExtra(Constants.IntentManager.INTENT_ACTION_IMAGE_DOWNLOADED_EXTRA_ERROR_LOADING, false);

        if (mCardThumbnail != null && mCardThumbnail.getParentCard() != null)
            intent.putExtra(Constants.IntentManager.INTENT_ACTION_IMAGE_DOWNLOADED_EXTRA_CARD_ID, mCardThumbnail.getParentCard().getId());
        if (getContext() != null)
            getContext().sendBroadcast(intent);

    }

    //--------------------------------------------------------------------------
    // Getters and Setters
    //--------------------------------------------------------------------------

    @Override
    public View getInternalOuterView() {
        return null;
    }

    /**
     * Indicates if view can recycle ui elements.
     *
     * @return <code>true</code> if views can recycle ui elements
     */
    public boolean isRecycle() {
        return mIsRecycle;
    }

    /**
     * Sets if view can recycle ui elements
     *
     * @param isRecycle  <code>true</code> to recycle
     */
    public void setRecycle(boolean isRecycle) {
        this.mIsRecycle = isRecycle;
    }

    /**
     * Indicates if inner layout have to be replaced
     *
     * @return <code>true</code> if inner layout can be recycled
     */
    public boolean isForceReplaceInnerLayout() {
        return mForceReplaceInnerLayout;
    }

    /**
     * Sets if inner layout have to be replaced
     *
     * @param forceReplaceInnerLayout  <code>true</code> to recycle
     */
    public void setForceReplaceInnerLayout(boolean forceReplaceInnerLayout) {
        this.mForceReplaceInnerLayout = forceReplaceInnerLayout;
    }


}
