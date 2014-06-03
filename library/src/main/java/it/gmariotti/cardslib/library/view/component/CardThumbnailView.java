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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

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

        mImageView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                loadBitmap();
                v.removeOnLayoutChangeListener(this);
            }
        });
        mImageView.requestLayout();
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
                loadBitmap(mCardThumbnail.getErrorResourceId(), mImageView);
        }
    }

    private String mUri;
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

    private MediaPlayer mMediaPlayer;
    private TextureView mVideoView;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        Surface surface = new Surface(surfaceTexture);

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer
                .setDataSource(getContext(), Uri.parse(mUri));
            mMediaPlayer.setSurface(surface);
            mMediaPlayer.setLooping(true);

            // don't forget to call MediaPlayer.prepareAsync() method when you use constructor for
            // creating MediaPlayer
            mMediaPlayer.prepareAsync();

            // Play video when the media source is ready for playback.
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mVideoView.setVisibility(View.VISIBLE);
                    mediaPlayer.start();
                    mImageView.setVisibility(View.GONE);
                }
            });

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

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (mMediaPlayer != null) {
            // Make sure we stop video and release resources when activity is destroyed.
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    public void loadBitmap(String uri, ImageView imageView) {
        final String videoPath = mCardThumbnail.getVideoResource();
        if (isVideoUri(uri)) {
            mVideoView.setSurfaceTextureListener(this);
        }

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
                ((ImageView) view).setImageBitmap(loadedImage);
                handler.removeCallbacks(r);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
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
